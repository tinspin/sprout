package se.rupy.sprout;

import java.util.HashMap;
import java.util.Random;

import se.rupy.http.Event;
import se.rupy.http.Service;
import se.rupy.memory.Base;
import se.rupy.memory.DataBean;

public class Data extends DataBean implements Type {
	public static HashMap cache = new HashMap();

	public static void cache(int link, Data data) {
		HashMap hash = (HashMap) cache.get(new Integer(link));

		if(hash == null) {
			hash = new HashMap();
		}

		try {
			if(!Sprout.update(Base.SELECT, data)) {
				Sprout.update(Base.INSERT, data);
			}

			hash.put(data.getValue(), data);
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

	public Data() {}

	public Data(short type, String value) {
		setType(type);
		setValue(value);
	}

	/*
	public static class Test extends Service {
		public String path() { return "/test"; }
		public void filter(Event event) throws Event, Exception {
			event.output().print("<pre>");
			StringBuffer buffer = new StringBuffer();
			int length = 500;

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
