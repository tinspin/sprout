package se.rupy.sprout;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import se.rupy.http.*;

public class User extends Node {
	private static HashMap cache = new HashMap();

	public User() {
		super(USER);
	}
	
	public static User get(Object key) throws SQLException {
		if(key == null) {
			return null;
		}
		
		User user = (User) User.cache.get(key);
		
		if(user == null) {
			user = new User();
			user.query(Type.USER_KEY, key);
			user.meta();			
			user.cache.put(key, user);
		}
		
		return user;
	}
	
	public static class Login extends Service {
		public int index() { return 1; }
		public String path() { return "/login"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse();
				
				String name = event.string("name");
				String pass = event.string("pass");

				if(name.length() > 0 && pass.length() > 0) {
					User user = new User();
					if(user.query(USER_NAME, name)) {
						user.meta();
						
						if(user.get(USER_PASS).getValue().equals(pass)) {
							save(event.session(), user);
						}
						else {
							event.query().put("error", "Wrong password!");
						}
					}
					else {
						event.query().put("error", "User not found!");
					}
				}

				Sprout.redirect(event);
			}
		}
	}

	public static class Register extends Service {
		public int index() { return 1; }
		public String path() { return "/register"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse();

				String name = event.string("name");
				String mail = event.string("mail");
				String pass = event.string("pass");
				String word = event.string("word");

				if(name.length() > 0 && mail.length() > 0 && pass.length() > 0) {
					if(pass.equals(word)) {
						User user = new User();
						if(!user.query(USER_NAME, name)) {
							user.add(USER_NAME, name);
							user.add(USER_MAIL, mail);
							user.add(USER_PASS, pass);
							user.add(Sprout.generate(USER_KEY, 16));
							user.update();

							save(event.session(), user);
							
							Sprout.redirect(event, "/");
						}
						else {
							event.query().put("error", "Name already in use!");
						}
					}
					else {
						event.query().put("error", "Passwords don't match!");
					}
				}
				
				Sprout.redirect(event);
			}
		}
	}

	static void save(Session session, User user) {
		String key = user.get(USER_KEY).getValue();
		session.put("key", key);
		cache.put(key, user);
	}
	
	public static class Timeout extends Service {
		public String path() { return "/:/login:/register:/article/edit:/upload:/edit"; }
		public void session(Session session, int type) throws Exception {
			String key = (String) session.get("key");

			switch(type) {
			case Service.CREATE: break;
			case Service.TIMEOUT: 
				User.cache.remove(key);
				Article.remove(key);
				break;
			}
		}

		public void filter(Event event) throws Event, Exception {
			event.reply().header("Cache-Control", "no-cache");

			HashMap post = (HashMap) event.session().get("post");
			
			if(post != null) {
				Iterator it = post.keySet().iterator();

				while(it.hasNext()) {
					Object key = it.next();
					Object value = post.get(key);

					event.query().put(key, value);
				}

				event.session().remove("post");
			}
		}
	}

	public static class Identify extends Service {
		public int index() { return 1; }
		public String path() { return "/article/edit:/upload:/edit"; }
		public void filter(Event event) throws Event, Exception {
			String key = (String) event.session().get("key");

			if(key != null) {
				User user = (User) cache.get(key);
				
				if(user != null) {
					return;
				}
			}
			
			//Sprout.redirect(event, "/login");
		}
	}
}
