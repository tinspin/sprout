package se.rupy.content;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

import se.rupy.http.Event;
import se.rupy.http.Input;
import se.rupy.http.Query;
import se.rupy.sprout.Data;
import se.rupy.sprout.Node;
import se.rupy.sprout.Sprout;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/*
 * This service only supports one file at the time.
 */
public class Upload extends Sprout {
	private static int SIZE = 1024;
	
	public int index() { return 2; }
	public String path() { return "/upload"; }
	public void filter(Event event) throws Event, Exception {
		if(event.query().method() == Query.POST) {
			Node file = new File();
			Item item = new Item();
			item.path = java.io.File.separator + "upload" + java.io.File.separator + file.date();
			item = save(event, item);

			Object key = event.session().get("key");
			Article article = Article.get(key);
			Node old = article.child(FILE, FILE_NAME, item.name);
			
			if(old != null) {
				file = old;
			}
			
			if(item.name.endsWith(".jpeg") || item.name.endsWith(".jpg")) {
				resize(item, 50);
				file.add(File.type("IMAGE"));
			}

			if(item.name.endsWith(".avi") || item.name.endsWith(".mov") || item.name.endsWith(".wmv")) {
				encode(item);
				file.add(File.type("VIDEO"));
			}

			if(item.name.endsWith(".mp3")) {
				// TODO: Add audio player
				file.add(File.type("AUDIO"));
			}

			file.add(FILE_NAME, item.name);

			if(article.meta(ARTICLE_TITLE) == null) {
				article.add(ARTICLE_TITLE, i18n("Title"));
			}

			if(article.meta(ARTICLE_BODY) == null) {
				article.add(ARTICLE_BODY, i18n("Body"));
			}

			if(old == null) {
				article.add(file);
			}
			else {
				file.update();
			}
			
			Sprout.redirect(event, "/edit?id=" + article.getId());
		}

		Sprout.redirect(event, "/");
	}

	static void resize(Item item, int width) throws IOException {
		BufferedImage image = ImageIO.read(item.file);

		Image tiny = image.getScaledInstance(width, -1, Image.SCALE_AREA_AVERAGING);
		save(tiny, new java.io.File(Sprout.root + item.path + java.io.File.separator + "TINY_" + item.name));

		Image small = image.getScaledInstance(width * 4, -1, Image.SCALE_AREA_AVERAGING);
		save(small, new java.io.File(Sprout.root + item.path + java.io.File.separator + "SMALL_" + item.name));

		Image big = image.getScaledInstance(width * 8, -1, Image.SCALE_AREA_AVERAGING);
		save(big, new java.io.File(Sprout.root + item.path + java.io.File.separator + "BIG_" + item.name));
	}

	static void save(Image small, java.io.File file) throws IOException {
		int width = small.getWidth(null);
		int height = small.getHeight(null);

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		g.drawImage(small, 0, 0, null);
		g.dispose();

		FileOutputStream out = new FileOutputStream(file);
		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(image);
		param.setQuality(1, true);
		encoder.setJPEGEncodeParam(param);
		encoder.encode(image);
		out.close();
	}

	static void encode(Item item) {
		try {
			String line;
			String path = "app" + java.io.File.separator + "content" + java.io.File.separator + item.path.replace('\\', '/') + java.io.File.separator;
			System.out.println(item.path + item.name);
			Process p = Runtime.getRuntime().exec("ffmpeg -i " + path + item.name + " -deinterlace -y -b 1024k -ac 2 -ar 22050 -s 320x240 " + path + item.name.substring(0, item.name.indexOf('.')) + ".flv");
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = input.readLine()) != null) {
				System.out.println(line);
			}
			input.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static class Delete extends Sprout {
		public String path() { return "/upload/delete"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse();

				Article article = Article.find(event.big("id"));
				Node file = article.child(event.big("file"));

				if(article != null && file != null) {
					// TODO: Delete actual content on disk.

					article.remove(file);
					Article.invalidate("article", article);
				}

				Sprout.redirect(event);
			}
		}
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

				java.io.File path = new java.io.File(Sprout.root + item.path);

				if(!path.exists()) {
					path.mkdirs();
				}

				item.file = new java.io.File(Sprout.root + item.path + java.io.File.separator + item.name);
				FileOutputStream out = new FileOutputStream(item.file);

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
		java.io.File file;
	}

	/*
	 * The multipart protocol was poorly designed, so we have to check 
	 * for the boundary for every byte and handle things if the boundary 
	 * is between buffers.
	 * 
	 * TODO: An alternative way is to write everything from headers onwards 
	 * to a file and then search for the boundary starting at the end of the 
	 * file, and crop the boundary.
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
	
	public static class File extends Node {
		static {
			Data.cache(FILE, new Data(FILE_TYPE, "IMAGE"));
			Data.cache(FILE, new Data(FILE_TYPE, "VIDEO"));
			Data.cache(FILE, new Data(FILE_TYPE, "AUDIO"));
		}
		
		public File() {
			super(FILE);
		}
		
		static Data type(String value) {
			return Data.cache(FILE, value);
		}
	}
}