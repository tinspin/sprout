package se.rupy.sprout;

import java.util.HashMap;

import se.rupy.memory.Base;
import se.rupy.memory.DataBean;

public class Data extends DataBean implements Type {
	static HashMap cache = new HashMap();
	
	static void cache(int link, short meta, String[] type) {
		HashMap hash = new HashMap();
		
		try {
			for(int i = 0; i < type.length; i++) {
				Data data = new Data();
				data.setValue(type[i]);
				data.setType(meta);
				
				if(!Sprout.update(Base.SELECT, data)) {
					Sprout.update(Base.INSERT, data);
				}
				
				hash.put(type[i], data);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		cache.put(new Integer(link), hash);
	}
	
	static Data cache(int link, String type) {
		HashMap data = (HashMap) cache.get(new Integer(link));
		return (Data) data.get(type);
	}
}
