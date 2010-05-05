package se.rupy.content;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import se.rupy.http.Deploy;
import se.rupy.sprout.Data;
import se.rupy.sprout.Node;
import se.rupy.sprout.Sprout;

public class File extends Node {
	static {
		Data.cache(FILE, new Data(FILE_TYPE, "IMAGE"));
		Data.cache(FILE, new Data(FILE_TYPE, "VIDEO"));
		Data.cache(FILE, new Data(FILE_TYPE, "AUDIO"));
	}

	public File() {
		super(FILE);
	}

	public static String path(Node file, Data name, String suffix) {
		return "file" + file.path() + "/" + name.getString() + suffix;
	}

	public static Data type(String value) {
		return Data.cache(FILE, value);
	}
	
	public static String name(String name) {
		if(name.endsWith(".jpeg") || 
			name.endsWith(".jpg") || 
			name.endsWith(".bmp")) {
			return name.substring(0, name.indexOf('.'));
		}
		
		if(name.endsWith(".avi") || 
			name.endsWith(".mov") || 
			name.endsWith(".wmv") || 
			name.endsWith(".mp4") || 
			name.endsWith(".mkv")) {
			return name.substring(0, name.indexOf('.'));
		}
		
		if(name.endsWith(".mp3") || 
			name.endsWith(".wav")) {
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