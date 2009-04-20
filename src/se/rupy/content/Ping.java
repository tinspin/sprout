package se.rupy.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import se.rupy.http.Output;
import se.rupy.http.Service;
import se.rupy.http.Deploy;
import se.rupy.http.Event;
import se.rupy.http.Reply;
import se.rupy.sprout.Data;
import se.rupy.sprout.Node;
import se.rupy.sprout.Sprout;
import se.rupy.sprout.User;

/*
 * <?xml version="1.0"?>
 * <methodCall>
 * <methodName>pingback.ping</methodName>
 * <params>
 * <param><value><string>http://blog.rupy.se/2008/09/11/tada/</string></value></param>
 * <param><value><string>http://sprout.rupy.se/article?id=48</string></value></param>
 * </params></methodCall>
 */

public class Ping extends Node {
	static {
		System.setProperty("sun.net.client.defaultReadTimeout", "5000");
		System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
	}
	
	public Ping() {
		super(PING);
	}

	public static void call(Article article) throws Exception {
		new Out(article);
	}

	public static class Back extends Service {
		public String path() { return "/ping"; }
		public void filter(Event event) throws Event, Exception {
			try {
				new In(event);
				Reply reply = event.reply();
				reply.type("text/xml");
				Output out = reply.output();
				out.println("<?xml version=\"1.0\"?>");
				out.println("<methodResponse>");
				out.println("  <params>");
				out.println("    <param>");
				out.println("      <value>");
				out.println("        <string>Pingback registered.</string>");
				out.println("      </value>");
				out.println("    </param>");
				out.println("  </params>");
				out.println("</methodResponse>");
			}
			catch(Exception e) {
				e.printStackTrace();
				throw e;
			}
		}
	}

	public static class In extends Search {
		public In(Event event) throws Exception {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Deploy.pipe(event.input(), out); // TODO: Limit

			String body = new String(out.toByteArray(), "UTF-8");
			String from = find("string", body);
			String to = find("string", body);

			if(from.length() > 0 && to.length() > 0) {
				index = to.indexOf("article?id=");
				long id = Long.parseLong(to.substring(index + "article?id=".length()));
				index = 0;

				Article article = Article.find(id);

				if(article != null) {
					Iterator it = article.child(PING).iterator();
					boolean found = false;

					while(it.hasNext()) {
						Node ping = (Node) it.next();

						if(ping.meta(PING_URL).getValue().equals(from)) {
							found = true;
							System.out.println("Pingback '" + ping.meta(PING_TITLE).getValue() + "' allready added!");
							break;
						}
					}

					if(!found) {
						HttpURLConnection conn = (HttpURLConnection) new URL(from).openConnection();
						conn.setRequestMethod("GET");
						int code = conn.getResponseCode();

						if(code == 200) {
							Deploy.pipe(conn.getInputStream(), out);
						} else {
							System.out.println("Code: " + code);
						}

						body = new String(out.toByteArray(), "UTF-8");
						String title = find("title", body);

						if(title.length() > 0 && body.indexOf(to) > -1) {
							Ping ping = new Ping();
							ping.add(PING_TITLE, title);
							ping.add(PING_URL, from);
							ping.add(Data.cache(PING, "SHOW"));
							article.add(ping);

							System.out.println("Pingback '" + title + "' added!");
						}
						else {
							System.out.println("URL not found!");
						}
					}
				}
			}
		}
	}

	public static class Out extends Search {
		HttpURLConnection conn;
		public Out(Article article) throws Exception {
			String body = article.meta(ARTICLE_BODY).getValue();
			String url = find("a", "href", body);

			while(url.length() > 0) {
				System.out.println(url);

				conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setRequestMethod("POST");
				int code = conn.getResponseCode();
				String ping = conn.getHeaderField("X-Pingback");

				System.out.println(ping);

				if(ping != null) {
					conn = (HttpURLConnection) new URL(ping).openConnection();
					conn.setDoOutput(true);

					StringBuffer buffer = new StringBuffer();
					buffer.append("<?xml version=\"1.0\"?>\n");
					buffer.append("<methodCall>\n");
					buffer.append("  <methodName>pingback.ping</methodName>\n");
					buffer.append("  <params>\n");
					buffer.append("    <param><value><string>http://" + User.host + "/article?id=" + article.getId() + "</string></value></param>\n");
					buffer.append("    <param><value><string>" + url + "</string></value></param>\n");
					buffer.append("  </params>\n");
					buffer.append("</methodCall>\n");

					System.out.println(buffer.toString());
					
					OutputStream out = conn.getOutputStream();
					out.write(buffer.toString().getBytes("UTF-8"));
					code = conn.getResponseCode();

					InputStream in = null;

					if (code == 200) {
						in = conn.getInputStream();
					} else if (code < 0) {
						throw new IOException("HTTP response unreadable.");
					} else {
						in = conn.getErrorStream();
					}

					Deploy.pipe(in, System.out);
					in.close();
				}

				url = find("a", "href", body);
			}
		}
	}

	public static class Search {
		int index = 0;

		String find(String element, String body) {
			int start = body.indexOf("<" + element + ">", index);
			int stop = body.indexOf("</" + element + ">", index);

			if(start < 0 || stop < 0) {
				return "";
			}

			index = stop + 1;

			return body.substring(start + ("<" + element + ">").length(), stop);
		}

		String find(String element, String attribute, String body) {
			int start = body.indexOf("<" + element, index);
			int stop = body.indexOf(">", start);
			
			if(start < 0 || stop < 0) {
				return "";
			}

			index = body.indexOf("</" + element + ">", index) + ("</" + element + ">").length() + 1;
			
			//System.out.println(index + " " + start + " " + stop);
			
			String tag = body.substring(start + ("<" + element).length(), stop);

			int length = (attribute + "=\"").length();

			start = tag.indexOf(attribute + "=\"");
			stop = tag.indexOf("\"", start + length + 1);

			return tag.substring(start + length, stop);
		}
	}
}