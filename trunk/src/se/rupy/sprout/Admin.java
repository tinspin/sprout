package se.rupy.sprout;

import se.rupy.http.Event;
import se.rupy.http.Service;

public abstract class Admin extends Service {
	public static class Publish extends Service {
		public int index() { return 2; }
		public String path() { return "/admin"; }
		public void filter(Event event) throws Event, Exception {
			
		}
	}
}