package se.rupy.content;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

import se.rupy.http.Deploy;
import se.rupy.http.Event;
import se.rupy.sprout.Data;
import se.rupy.sprout.Node;
import se.rupy.sprout.Sprout;
import se.rupy.sprout.User;

public class File extends Node {
	static {
		Data.cache(FILE, new Data(FILE_TYPE, "IMAGE"));
		Data.cache(FILE, new Data(FILE_TYPE, "VIDEO"));
		Data.cache(FILE, new Data(FILE_TYPE, "AUDIO"));
	}

	public File() {
		super(FILE);
	}

	public static String path(Event event, Node file, Data name, String suffix) {
		/*
		 * If file was stored on cluster append hostname.
		 */
		Data host = file.meta(FILE_HOST);

		if(host != null) {
			String domain = event.daemon().properties().getProperty("domain", "host.rupy.se");
			
			return "http://" + host.getString() + "." + domain.substring(domain.indexOf('.')) + "/" + User.host + "/file" + file.path() + "/" + name.getString() + suffix;
		}
		
		return "file" + file.path() + "/" + name.getString() + suffix;
	}

	//"file" + file.encoded() + URLEncoder.encode("/" + name.getString(), "UTF-8") + ".flv";
	//"file" + file.encoded() + URLEncoder.encode("/" + name.getString(), "UTF-8") + ".mp3";
	
	public static String filepath(Event event, Node file, Data name, String suffix) throws Exception {
		/*
		 * If file was stored on cluster append hostname.
		 */
		Data host = file.meta(FILE_HOST);

		if(host != null) {
			String domain = event.daemon().properties().getProperty("domain", "host.rupy.se");
			
			return "http://" + host.getString() + "." + domain.substring(domain.indexOf('.')) + "/" + User.host + "/file" + file.encoded() + URLEncoder.encode("/" + name.getString(), "UTF-8") + suffix;
		}
		
		return "file" + file.encoded() + URLEncoder.encode("/" + name.getString(), "UTF-8") + suffix;
		

	}
	
	public static Data type(String value) {
		return Data.cache(FILE, value);
	}
	
	public static String name(String name) {
		if(name.toLowerCase().endsWith(".jpeg") || 
			name.toLowerCase().endsWith(".jpg") || 
			name.toLowerCase().endsWith(".bmp")) {
			return name.substring(0, name.indexOf('.'));
		}
		
		if(name.toLowerCase().endsWith(".avi") || 
			name.toLowerCase().endsWith(".mov") || 
			name.toLowerCase().endsWith(".wmv") || 
			name.toLowerCase().endsWith(".mp4") || 
			name.toLowerCase().endsWith(".mkv")) {
			return name.substring(0, name.indexOf('.'));
		}
		
		if(name.toLowerCase().endsWith(".mp3") || 
			name.toLowerCase().endsWith(".wav")) {
			return name.substring(0, name.indexOf('.'));
		}
		
		return name;
	}
	
	public static void path(String p) {
		java.io.File path = new java.io.File(p);

		if(!path.exists()) {
			path.mkdirs();
		}
	}
	
	public static void delete(java.io.File file) {
		if(file.exists()) {
			System.gc();
			file.delete();
		}
	}
	
	public static void delete(String file) {
		delete(new java.io.File(file));
	}
	
	public static void copy(String file, String path, String to) throws IOException {
		path(path);
		
		java.io.File f = new java.io.File(file);
		java.io.File t = new java.io.File(path + "/" + to);

		InputStream in = new FileInputStream(f);
		OutputStream out = new FileOutputStream(t);

		Deploy.pipe(in, out);

		in.close();
		out.close();

		delete(f);
	}
}