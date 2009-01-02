package se.rupy.sprout;

import java.util.HashMap;

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
			System.out.println("Data cache is empty for " + name + ". (" + link + ")");
		}
		
		return (Data) hash.get(name);
	}

	public Data() {}
	
	public Data(short type, String value) {
		setType(type);
		setValue(value);
	}
}
