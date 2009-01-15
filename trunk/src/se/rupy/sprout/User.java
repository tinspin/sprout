package se.rupy.sprout;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import se.rupy.http.*;
import se.rupy.mail.*;

public class User extends Node {
	private static String host;
	private static String mail;

	private final static String EOL = "\r\n";

	private static HashMap cache = new HashMap();

	static {
		Data unverified = new Data(USER_STATE, "UNVERIFIED");
		Data verified = new Data(USER_STATE, "VERIFIED");

		Data.cache(USER, unverified);
		Data.cache(USER, verified);
	}

	public User() {
		super(USER);
	}

	public boolean permit(String group) throws SQLException {
		Iterator it = get(GROUP).iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();

			if(node.get(GROUP_NAME).getValue().equals(group)) {
				return true;
			}
		}

		return false;
	}

	public static User get(Object key) throws SQLException {
		if(key == null) {
			return null;
		}

		User user = (User) User.cache.get(key);

		if(user == null) {
			user = new User();
			
			if(user.query(USER_KEY, key)) {
				user.fill(10, 0, 10);
				user.cache.put(key, user);
			}
			else {
				return null;
			}
		}

		return user;
	}

	public static class Login extends Service {
		public int index() { return 1; }
		public String path() { return "/login"; }
		public void filter(Event event) throws Event, Exception {
			if(User.host == null) {
				User.host = event.daemon().properties.getProperty("host", "localhost");
			}

			event.query().parse();

			if(event.query().method() == Query.POST) {
				String mail = event.string("mail");
				String pass = event.string("pass");

				if(mail.length() > 0 && pass.length() > 0) {
					User user = new User();
					if(user.query(USER_MAIL, mail)) {
						user.fill(10, 0, 10);
						
						if(user.get(USER_PASS).getValue().equals(pass)) {
							if(user.get(USER_STATE).getValue().equals("VERIFIED")) {
								save(event.session(), user, event.bit("remember"));
							}
							else {
								event.query().put("error", Sprout.i18n("Verify your e-mail first!"));
							}
						}
						else {
							event.query().put("error", Sprout.i18n("Wrong password!"));
						}
					}
					else {
						event.query().put("error", Sprout.i18n("User not found!"));
					}
				}

				Sprout.redirect(event);
			}

			if(event.query().method() == Query.GET) {
				String key = event.string("key");

				if(key.length() > 0) {
					User user = new User();
					if(user.query(USER_KEY, key)) {
						user.fill(10, 0, 10);
						user.add(Data.cache(USER, "VERIFIED"));
						save(event.session(), user, false);
					}
					else {
						throw new Exception(Sprout.i18n("Key not found!"));
					}
					
					Sprout.redirect(event);
				}
			}
		}
	}

	public static class Logout extends Service {
		public String path() { return "/logout"; }
		public void filter(Event event) throws Event, Exception {
			Session session = event.session();
			kill(session.get("key"));
			session.remove("key");
			Sprout.redirect(event);
		}
	}

	public static class Register extends Service {
		public int index() { return 1; }
		public String path() { return "/register"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse();

				if(User.host == null) {
					User.host = event.daemon().properties.getProperty("host", "localhost");
				}

				if(User.mail == null) {
					User.mail = event.daemon().properties.getProperty("mail", "mail.bazooka.nu");
				}

				String mail = event.string("mail");
				String name = event.string("name");
				String pass = event.string("pass");
				String word = event.string("word");

				if(name.length() > 0 && mail.length() > 0 && pass.length() > 0) {
					if(pass.equals(word)) {
						User user = new User();
						if(!user.query(USER_MAIL, mail)) {
							user.add(USER_MAIL, mail);
							user.add(USER_NAME, name);
							user.add(USER_PASS, pass);
							user.add(Sprout.generate(USER_KEY, 16));
							user.add(USER_IP, event.remote());
							user.add(Data.cache(USER, "UNVERIFIED"));

							if(Sprout.value("SELECT count(*) FROM node WHERE type = " + Type.USER) == 0) {
								user.add(Group.name("ADMIN"));
							}

							if(host.equals("localhost")) {
								eMail email = Post.create(User.mail, "group@rupy.se", "Welcome!");
								email.addRecipient(eMail.TO, mail);
								StringBuffer content = new StringBuffer();

								content.append("Glad to see you joined!<br>" + EOL);
								content.append("<br>" + EOL);
								content.append("You need to verify this e-mail address by<br>" + EOL);
								content.append("browsing to the following address:<br>" + EOL);
								content.append("<br>" + EOL);
								content.append("&nbsp;&nbsp;<a href=\"http://" + host + "/login?key=" + 
										user.get(USER_KEY).getValue() + "\">http://" + host + "/login?key=" + 
										user.get(USER_KEY).getValue() + "</a><br>" + EOL);
								content.append("<br>" + EOL);
								content.append("For future reference, this is your personal<br>" + EOL);
								content.append("serial key:<br>" + EOL);
								content.append("<br>" + EOL);
								content.append("&nbsp;&nbsp;<i>" + user.get(USER_KEY).getValue() + "</i><br>" + EOL);
								content.append("<br>" + EOL);
								content.append("It should be kept safe, and used to identify<br>" + EOL);
								content.append("and authenticate yourself in official inquiries.<br>" + EOL);
								content.append("<br>" + EOL);
								content.append("Cheers,<br>" + EOL);
								content.append("<br>" + EOL);
								content.append("/rupy<br>" + EOL);

								try {
									email.send(content.toString());
								}
								catch(Exception e) {
									event.query().put("error", Sprout.i18n("That's not an e-mail!"));
									e.printStackTrace();
									return;
								}
							}

							user.update();

							//save(event.session(), user, false);

							Sprout.redirect(event, "/verify.html");
						}
						else {
							event.query().put("error", Sprout.i18n("Mail already in use!"));
						}
					}
					else {
						event.query().put("error", Sprout.i18n("Passwords don't match!"));
					}
				}

				Sprout.redirect(event);
			}
		}
	}

	static void save(Session session, User user, boolean remember) {
		String key = user.get(USER_KEY).getValue();
		session.put("key", key);
		cache.put(key, user);
		
		if(remember) {
			long time = (long) 1000 * 60 * 60 * 24 * 365;
			session.key(key, User.host, System.currentTimeMillis() + time);
		}
	}

	static void kill(Object key) {
		cache.remove(key);
	}

	public static class Timeout extends Service {
		public String path() { return "/:/login:/register:/article/edit:/upload:/edit:/admin:/search"; }
		public void session(Session session, int type) throws Exception {
			String key = (String) session.get("key");

			switch(type) {
			case Service.CREATE: 
				if(session.key() != null && session.key().length() == 16) {
					save(session, get(key), true);
				}
			case Service.TIMEOUT: 
				kill(key);
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
		public String path() { return "/article/edit:/upload:/edit:/admin:/search"; }
		public void filter(Event event) throws Event, Exception {
			String key = (String) event.session().get("key");

			if(key == null) {
				event.output().println("<pre>unauthorized</pre>");
				throw event;
			}
		}
	}

	public static class Group extends Node {
		static {
			Data admin = new Data(GROUP_NAME, "ADMIN");

			Data.cache(GROUP, admin);
			Node.cache(GROUP, admin);
		}

		public Group() {
			super(GROUP);
		}

		static Node name(String name) {
			return Node.cache(GROUP, name);
		}
	}
}
