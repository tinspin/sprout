package se.rupy.sprout;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import se.rupy.http.Event;
import se.rupy.http.Service;

public abstract class Admin extends Sprout {
	public static HashMap cache = new HashMap();
	
	public static Node get(Object key) {
		if(key == null) {
			return null;
		}
		
		return (Node) Admin.cache.get(key);
	}
	
	public static class Permit extends Service {
		public int index() { return 2; }
		public String path() { return "/admin:/search"; }
		public void filter(Event event) throws Event, Exception {
			Object key = event.session().get("key");
			
			if(key != null) {
				User user = User.get(key);
				
				if(user != null) { // && user.permit("ADMIN")) {
					return;
				}
			}
			
			event.output().println("<pre>unauthorized</pre>");
			throw event;
		}
	}
	
	public static class Search extends Service {
		public int index() { return 3; }
		public String path() { return "/search"; }
		public void filter(Event event) throws Event, Exception {
			event.query().parse();
			
			User user = User.get(event.session().get("key"));
			String key = user.get(USER_KEY).getValue();
			
			int link = event.query().medium("link");
			short meta = event.query().small("meta");
			String value = event.query().string("value");
			
			if(link > 0 && meta > 0 && value.length() > 0) {
				Node node = new Node(link);
				node.query(meta, value);
				node.fill(10, 0, 10);
				cache.put(key, node);
			}
			
			Sprout.redirect(event);
		}
	}
}