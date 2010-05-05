package se.rupy.content;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import se.rupy.http.Deploy;
import se.rupy.http.Event;
import se.rupy.http.Input;
import se.rupy.http.Output;
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
	public String path() { return "/upload:/picture"; }
	public void filter(Event event) throws Event, Exception {
		if(event.query().method() == Query.POST) {
			if(event.query().path().equals("/picture")) {
				// Because this is called from hidden iframe, we won't see the error stacktrace!
				try {
					Item item = new Item();
					item.path = "file";
					item = save(event, item);

					String path = frame(item);
					Output out = event.output();

					out.println("<script>");
					out.println("  var doc = window.top.document;");
					out.println("  doc.user.picture.value = '" + path + "';");
					out.println("  doc.picture.src = '" + path + "';");
					out.println("</script>");
				}
				catch(Exception e) {
					e.printStackTrace();
					throw e;
				}
			}

			if(event.query().path().equals("/upload")) {
				File file = new File();
				Item item = new Item();
				item.path = "file";
				item = save(event, item);

				Node article = article(event, file, item);
				Sprout.redirect(event, "/edit?id=" + article.getId());
			}
		}

		//Sprout.redirect(event, "/");
	}

	static String frame(Item item) throws IOException {
		BufferedImage image = ImageIO.read(item.file);

		int width = image.getWidth();
		int height = image.getHeight();

		if(width > height) {
			image = image.getSubimage((width - height) / 2, 0, height, height);
		}

		if(width < height) {
			image = image.getSubimage(0, (height - width) / 2, width, width);
		}

		String name = item.name.substring(0, item.name.indexOf('.'));
		String path = item.path + "/" + name + ".jpeg";

		Image tiny = image.getScaledInstance(50, 50, Image.SCALE_AREA_AVERAGING);
		save(tiny, new java.io.File(Sprout.ROOT + "/" + path));
		item.file.delete();

		return path;
	}

	static Node article(Event event, Node file, Item item) throws Exception {
		Object key = event.session().get("key");
		Article article = Article.get(key);
		String name = File.name(item.name);
		Node old = article.child(FILE, FILE_NAME, name);

		if(old != null) {
			file = old;
			file.setDate(System.currentTimeMillis());
			Article.invalidate(article);
		}

		file.add(FILE_NAME, name);

		if(old == null) {
			article.add(file);
		}
		else {
			file.update();
		}
		
		boolean delete = false;
		
		if(item.name.endsWith(".jpeg") || item.name.endsWith(".jpg") || item.name.endsWith(".bmp")) {
			resize(item, file, 200);
			file.add(File.type("IMAGE"));
			delete = true;
		}
		else if(item.name.endsWith(".avi") || item.name.endsWith(".mov") || item.name.endsWith(".wmv") || item.name.endsWith(".mp4") || item.name.endsWith(".mkv")) {
			video(item, file);
			file.add(File.type("VIDEO"));
			delete = true;
		}
		else if(item.name.endsWith(".mp3") || item.name.endsWith(".wav")) {
			if(item.name.endsWith(".wav")) {
				audio(item, file);
			}
			else {
				File.copy(Sprout.ROOT + "/file/" + item.name, Sprout.ROOT + "/file" + file.path(), name + ".mp3");
			}
			file.add(File.type("AUDIO"));
			delete = true;
		}

		if(delete) {
			File.delete(item.file);
		}
		else {
			File.copy(Sprout.ROOT + "/file/" + item.name, Sprout.ROOT + "/file" + file.path(), name);
		}

		return article;
	}

	static void resize(Item item, Node file, int width) throws IOException {
		BufferedImage image = ImageIO.read(item.file);
		String name = item.name.substring(0, item.name.indexOf('.'));

		File.path(Sprout.ROOT + "/file" + file.path());
		
		Image small = image.getScaledInstance(width, -1, Image.SCALE_AREA_AVERAGING);
		save(small, new java.io.File(Sprout.ROOT + "/file" + file.path() + "/" + name + "-" + width + ".jpeg"));
		
		Image big = image.getScaledInstance(width * 2, -1, Image.SCALE_AREA_AVERAGING);
		save(big, new java.io.File(Sprout.ROOT + "/file" + file.path() + "/" + name + "-" + width * 2 + ".jpeg"));
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
		out.flush();
		out.close();
	}

	static void video(Item item, Node file) {
		String name = item.name.substring(0, item.name.indexOf('.'));
		try {
			String line;
			File.path(Sprout.ROOT + "/file" + file.path());
			String path = Sprout.ROOT + "/" + item.path + "/";
			String to = Sprout.ROOT + "/file" + file.path() + "/" + name + ".flv";
			File.delete(to);
			Process p = Runtime.getRuntime().exec("ffmpeg -i " + path + item.name + " -deinterlace -y -b 1024k -ac 2 -ar 22050 -s 320x240 " + to);
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

	static void audio(Item item, Node file) {
		String name = item.name.substring(0, item.name.indexOf('.'));
		try {
			String line;
			File.path(Sprout.ROOT + "/file" + file.path());
			String path = Sprout.ROOT + "/" + item.path + "/";
			String to = Sprout.ROOT + "/file" + file.path() + "/" + name + ".mp3";
			File.delete(to);
			Process p = Runtime.getRuntime().exec("ffmpeg -i " + path + item.name + " " + to);
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

				java.io.File path = new java.io.File(Sprout.ROOT + "/" + item.path);

				if(!path.exists()) {
					path.mkdirs();
				}

				item.file = new java.io.File(Sprout.ROOT + "/" + item.path + "/" + item.name);
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

		public String toString() {
			return name + " " + path + " " + type;
		}
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
}