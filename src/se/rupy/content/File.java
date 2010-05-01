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
		return "file" + file.path() + "/" + name.getValue() + suffix;
	}

	public static Data type(String value) {
		return Data.cache(FILE, value);
	}
}