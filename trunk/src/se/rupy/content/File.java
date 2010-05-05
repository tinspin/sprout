package se.rupy.content;

import se.rupy.sprout.Data;
import se.rupy.sprout.Node;

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
}