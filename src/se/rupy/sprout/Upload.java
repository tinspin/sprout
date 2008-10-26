package se.rupy.sprout;

import java.io.*;
import java.util.Random;

import se.rupy.http.*;

/*
 * This service only supports one file at the time.
 * TODO: enable dynamic static content in rupy ...
 */
public class Upload extends Sprout {
	public static int SIZE = 1024;

	public int index() { return 2; }
	public String path() { return "/upload"; }
	public void filter(Event event) throws Event, Exception {
		if(event.query().method() == Query.POST) {
			Node file = new Node(Type.FILE);
			
			Item item = new Item();
			item.path = File.separator + "upload" + File.separator + file.date();
			item = save(event, item);
			
			file.add(Type.FILE_PATH, item.path.replace('\\', '/'));
			file.add(Type.FILE_NAME, item.name);
			
			Object key = event.session().get("key");
			Article article = Article.get(key);
			
			if(article.get(Type.ARTICLE_TITLE) == null) {
				article.add(Type.ARTICLE_TITLE, i18n("Title"));
			}

			if(article.get(Type.ARTICLE_BODY) == null) {
				article.add(Type.ARTICLE_BODY, i18n("Body"));
			}
			
			article.add(file);

			Sprout.redirect(event, "/edit?id=" + article.getId());
		}
		
		Sprout.redirect(event, "/");
	}

	public static Item save(Event event, Item item) throws Event, IOException {
		String type = event.query().header("content-type");
		String boundary = "--" + unquote(type.substring(type.indexOf("boundary=") + 9));

		Input in = event.input();
		String line = in.line();

		while(line != null) {
			/*
			 * find boundary
			 */
			
			if(line.equals(boundary + "--")) {
				Sprout.redirect(event, "/");
			}
			
			if(line.equals(boundary)) {
				line = in.line();

				/*
				 * read headers; parse filename and content-type
				 */
				
				while(line != null && !line.equals("")) {
					int colon = line.indexOf(":");
					
					if (colon > -1) {
						String name = line.substring(0, colon).toLowerCase();
						String value = line.substring(colon + 1).trim();

						if(name.equals("content-disposition")) {
							item.name = unpath(unquote(value.substring(value.indexOf("filename=") + 9).trim()));
						}
						
						if(name.equals("content-type")) {
							item.type = value;
						}
					}

					line = in.line();
				}

				if(item.name == null || item.name.length() == 0) {
					Sprout.redirect(event, "/");
				}
				
				/*
				 * create path and file
				 */

				String root = "app" + File.separator + "content";
				
				File path = new File(root + item.path);

				if(!path.exists()) {
					path.mkdirs();
				}
				
				FileOutputStream out = new FileOutputStream(new File(root + item.path + File.separator + item.name));

				/*
				 * stream data
				 */
				
				Boundary bound = new Boundary();
				bound.value = ("\r\n" + boundary).getBytes();
				
				byte[] data = new byte[SIZE];
				int read = in.read(data);

				while(read > -1) {
					try {
						out.write(data, 0, bound.find(read, data, out));
					}
					catch(Boundary.EOB eob) {
						out.write(data, 0, eob.index);
						out.flush();
						out.close();
						
						// only handles one file for now, 
						// need to rewind the stream for 
						// multiple files.
						return item;
					}

					read = in.read(data);
				}
				
				throw new IOException("Boundary not found. (trailing)");
			}

			line = in.line();
		}
		
		throw new IOException("Boundary not found. (initing)");
	}
	
	static String unquote(String value) {
		int index = value.lastIndexOf("\"");

		if(index > -1) {
			return value.substring(1, index);
		}

		return value;
	}

	static String unpath(String value) {
		int index = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));

		if(index > -1) {
			return value.substring(index + 1);
		}

		return value;
	}
	
	public static class Item {
		String name;
		String path;
		String type;
	}
	
	/*
	 * The multipart protocol was poorly designed, so we have to check 
	 * for the boundary for every byte and handle things if the boundary 
	 * is between buffers.
	 * 
	 * TODO: An alternative way is to write everything from headers onwards 
	 * to a file and then search for the boundary starting at the end of the 
	 * file, and crop the boundary. Only works for one file though.
	 */
	public static class Boundary {
		byte[] value;
		int index; // remember boundary index between finds
		
		int find(int read, byte[] data, OutputStream out) throws EOB, IOException {
			int wrap = index;

			for(int i = 0; i < read; i++) {
				if(data[i] == value[index]) { 		// maybe boundary
					if(index == value.length - 1) { // boundary
						int start = i - value.length + 1;
						throw new EOB(start > -1 ? start : 0);
					}
					index++;
				}
				else { 			   // not boundary
					if(wrap > 0) { // write non-boundary from last find
						out.write(value, 0, wrap);
						wrap = 0;
					}
					index = 0;
				}
			}

			return read - index;
		}
		
		public static class EOB extends Throwable {
			int index;
			
			public EOB(int index) {
				this.index = index;
			}
		}
	}
}