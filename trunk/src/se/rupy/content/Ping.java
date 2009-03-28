package se.rupy.content;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import se.rupy.http.Service;
import se.rupy.http.Deploy;
import se.rupy.http.Event;
import se.rupy.http.Reply;
import se.rupy.sprout.Data;
import se.rupy.sprout.Node;
import se.rupy.sprout.Sprout;

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
	public Ping() {
		super(PING);
	}

	public static class Back extends Service {
		public String path() { return "/ping"; }
		public void filter(Event event) throws Event, Exception {
			try {
				new Process(event);
			}
			catch(Exception e) {
				e.printStackTrace();
				throw e;
			}
		}

		public static class Process {
			int index = 0;
			public Process(Event event) throws Exception {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Deploy.pipe(event.input(), out);

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

				Reply reply = event.reply();
				reply.type("text/xml");
				reply.output(0).flush();
			}

			String find(String tag, String body) {
				int start = body.indexOf("<" + tag + ">", index);
				int stop = body.indexOf("</" + tag + ">", index);

				if(start < 0 && stop < 0) {
					return "";
				}

				index = stop + 1;

				return body.substring(start + ("<" + tag + ">").length(), stop);
			}
		}
	}
}