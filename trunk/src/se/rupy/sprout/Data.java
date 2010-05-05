package se.rupy.sprout;

import java.util.HashMap;

import se.rupy.http.Event;
import se.rupy.http.Service;
import se.rupy.memory.Base;
import se.rupy.memory.DataBean;

public class Data extends DataBean implements Type {
	public static HashMap cache = new HashMap();
	private boolean done;
	protected Data() {}

	public Data(short type, String value) {
		if(value.length() == 0) {
			throw new RuntimeException("Empty data value will break the database index.");
		}

		setType(type);
		setString(value);
	}
	
	public Data(short type, byte[] value) {
		if(value.length == 0) {
			throw new RuntimeException("Empty data value will break the database index.");
		}

		setType(type);
		setValue(value);
	}

	protected boolean done() {
		return done;
	}

	protected void done(boolean done) {
		this.done = done;
	}

	public void setValue(byte[] value) {
		if(value.length == 0) {
			throw new RuntimeException("Empty data value will break the database index.");
		}

		try {
			super.setValue(value);
		}
		catch(Exception e) {
			// Fingers crossed!
		}
	}

	public void setString(String value) {
		if(value.length() == 0) {
			throw new RuntimeException("Empty data value will break the database index.");
		}

		try {
			setValue(value.getBytes("UTF-8"));
		}
		catch(Exception e) {
			// Fingers crossed!
		}
	}

	public String getString() {
		try {
			return new String(getValue(), "UTF-8");
		}
		catch(Exception e) {
			// Fingers crossed!
		}
		
		return null;
	}

	public static void cache(int link, Data data) {
		HashMap hash = (HashMap) cache.get(new Integer(link));

		if(hash == null) {
			hash = new HashMap();
		}

		try {
			if(!Sprout.update(Base.SELECT, data)) {
				Sprout.update(Base.INSERT, data);
			}

			hash.put(data.getString(), data);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		cache.put(new Integer(link), hash);
	}

	public static Data cache(int link, String name) {
		HashMap hash = (HashMap) cache.get(new Integer(link));

		if(hash == null) {
			System.out.println("Data cache for '" + name + "' is empty. (" + link + ")");
		}
		else {
			Data data = (Data) hash.get(name);

			if(data == null) {
				//System.out.println("Data cache for '" + name + "' is empty. (" + hash + ")");
			}

			return data;
		}

		return null;
	}

	public boolean equals(Object o) {
		if(o instanceof Data) {
			Data d = (Data) o;
			return id == d.getId();
		}
		if(o instanceof String) {
			String s = (String) o;
			return getString().equals(s);
		}
		return false;
	}

	public String toString() {
		return getString();
	}
	/*
	 * Use this to test MySQL index performance.
	 * 
	public static class Test extends Service {
		// select * from data where value = 'YC32WW2B7KFK4DYDBD8FFFTG3VXKGFBH3R7VX79DQ6C3X2V3CKJBG4398R9J4P8W' and type = 100;
		public String path() { return "/test"; }
		public void filter(Event event) throws Event, Exception {
			event.output().print("<pre>");
			StringBuffer buffer = new StringBuffer();
			int length = 2000;

			for(int i = 0; i < length; i++) {
				buffer.append("INSERT INTO data (value, type) VALUES ");

				for(int j = 0; j < length; j++) {
					buffer.append("('" + Event.random(64) + "', " + Type.USER_NAME + ")");

					if(j < length - 1) {
						buffer.append(", \n");
					}
					else {
						buffer.append(";");
					}
				}

				Sprout.find(buffer.toString());
				buffer.delete(0, buffer.length());

				event.output().print(((i + 1) * length) + "\n");
				event.output().flush();
			}

			event.output().print("</pre>");
		}
	}
	*/
}
