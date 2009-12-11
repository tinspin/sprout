package se.rupy.sprout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.json.JSONObject;

import com.maxmind.geoip.LookupService;

import se.rupy.http.*;
import se.rupy.mail.*;

public class User extends Node {
	public static String host;
	public static final String[] month = {
		"January", 
		"February", 
		"Mars", 
		"April", 
		"May", 
		"June", 
		"July", 
		"August", 
		"September", 
		"October", 
		"November", 
		"December"
	};

	private static String mail;
	private static String content, remind;

	private final static String EOL = "\r\n";
	public static LookupService lookup;
	private static HashMap cache = new HashMap();
	public static String[] countryCode;
	public static String[] countryName;

	static {
		String[] exclude = {"N/A",
				"Asia/Pacific Region", 
				"Antarctica", 
				"Europe", 
				"France, Metropolitan", 
				"Guernsey", 
				"Jersey", 
				"Isle of Man", 
				"Saint Barthelemy", 
				"Saint Martin", 
				"Anonymous Proxy", 
				"Satellite Provider", 
		"Other"};

		Vector name = new Vector();
		Vector code = new Vector();
		boolean flag = true, add = true;
		String tempName, tempCode;

		for(int i = 0; i < LookupService.countryName.length; i++) {
			for(int j = 0; j < exclude.length; j++) {
				if(LookupService.countryName[i].equals(exclude[j])) {
					add = false;
				}
			}
			if(add) {
				if(Sprout.translate()) {
					name.add(Sprout.i18n(LookupService.countryCode[i]));
				}
				else {
					name.add(LookupService.countryName[i]);
				}
				code.add(LookupService.countryCode[i]);
			}
			add = true;
		}

		String[] x = new String[name.size()];
		name.toArray(x);
		String[] y = new String[code.size()];
		code.toArray(y);

		while(flag) {
			flag = false;
			for(int j = 0; j < x.length - 1; j++) {
				if(x[j].compareToIgnoreCase(x[j + 1]) > 0) {
					tempName = x[j];
					x[j] = x[j + 1];
					x[j + 1] = tempName;

					tempCode = y[j];
					y[j] = y[j + 1];
					y[j + 1] = tempCode;

					flag = true;
				}
			}
		}

		User.countryName = x;
		User.countryCode = y;

		Data.cache(USER, new Data(USER_STATE, "UNVERIFIED"));
		Data.cache(USER, new Data(USER_STATE, "VERIFIED"));
		Data.cache(USER, new Data(USER_GENDER, "MALE"));
		Data.cache(USER, new Data(USER_GENDER, "FEMALE"));

		for(int i = 0; i < countryName.length; i++) {
			Data.cache(USER, new Data(USER_COUNTRY, countryCode[i]));
		}

		host = System.getProperty("host", "localhost:9000");
		mail = System.getProperty("mail", "mail1.comhem.se");

		content = read(Sprout.root + File.separator + "mail.txt");
		remind = read(Sprout.root + File.separator + "remind.txt");
	}

	static String read(String file) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String content = null;

		try {
			InputStream in = new FileInputStream(new File(file));
			Deploy.pipe(in, out);
			content = new String(out.toByteArray());
			out.close();
			in.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}

		return content;
	}

	public User() {
		super(USER);
	}

	public boolean permit(String group) throws SQLException {
		Iterator it = child(GROUP).iterator();

		while(it.hasNext()) {
			Node node = (Node) it.next();

			if(node.meta(GROUP_NAME).getValue().equals(group)) {
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

	/*
            <!--a href="" class="arrow" onclick="document.login.submit(); return false;">
              <span>[[ i18n("Login") ]]</span>
            </a-->
	 */

	public static class Login extends Service {
		public int index() { return 1; }
		public String path() { return "/login"; }
		public void filter(Event event) throws Event, Exception {
			event.query().parse();

			if(event.query().method() == Query.POST) {
				JSONObject ajax = null;

				if(event.bit("ajax")) {
					ajax = new JSONObject();
				}

				String mail = event.string("mail").toLowerCase();
				String pass = event.string("pass");

				if(mail.length() > 0 && pass.length() > 0) {
					User user = new User();
					if(user.query(USER_MAIL, mail) || user.query(USER_NAME, mail)) {
						user.fill(10, 0, 10);

						if(user.meta(USER_PASS).getValue().equals(pass)) {
							if(user.meta(USER_STATE).getValue().equals("VERIFIED")) {
								save(event.session(), user, event.bit("remember"));

								if(ajax != null) {
									ajax.put("url", "/");
								}
							}
							else {
								event.query().put("error", Sprout.i18n("Check your inbox!"));
							}
						}
						else {
							event.query().put("error", Sprout.i18n("Wrong password!"));
							event.query().put("remind", "yes");

							if(ajax != null) {
								ajax.put("remind", "yes");
							}
						}
					}
					else {
						event.query().put("error", Sprout.i18n("User not found!"));
					}
				}

				if(ajax != null) {
					if(event.query().string("error").length() > 0) {
						ajax.put("error", event.query().string("error"));
					}

					event.output().print(ajax.toString());
					throw event;
				}
				else {
					Sprout.redirect(event);
				}
			}
			else if(event.query().method() == Query.GET) {
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
		public void create() throws Exception {
			try {
				lookup = new LookupService(System.getProperty("user.dir") + "/res/GeoIP.dat", LookupService.GEOIP_MEMORY_CACHE);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		public void destroy() throws Exception {
			lookup.close();
		}
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse();

				String mail = event.string("mail").toLowerCase();
				String name = event.string("name").toLowerCase();
				String pass = event.string("pass");
				String word = event.string("word");
				String gender = event.string("gender").toUpperCase();

				int day = event.medium("day");
				int month = event.medium("month");
				int year = event.medium("year");

				if(name.length() > 0 && mail.length() > 0 && pass.length() > 0 && day > 0 && month > 0 && year > 0) {
					if(!pass.equals(word)) {
						event.query().put("error", Sprout.i18n("Passwords don't match!"));
						Sprout.redirect(event);
					}

					User user = new User();

					if(user.query(USER_MAIL, mail)) {
						event.query().put("error", Sprout.i18n("eMail already in use!"));
						Sprout.redirect(event);
					}

					if(user.query(USER_NAME, name)) {
						event.query().put("error", Sprout.i18n("Nickname already in use!"));
						Sprout.redirect(event);
					}

					user.add(USER_MAIL, mail);
					user.add(USER_NAME, name);
					user.add(USER_PASS, pass);
					user.add(Data.cache(USER, gender));
					user.add(USER_BIRTHDAY, day + "/" + month + "-" + year);

					if(event.string("country").length() > 0 && !event.string("country").equals("--")) {
						user.add(Data.cache(USER, event.string("country")));
					}

					if(event.string("first").length() > 0) {
						user.add(USER_FIRST_NAME, event.string("first"));
					}

					if(event.string("last").length() > 0) {
						user.add(USER_LAST_NAME, event.string("last"));
					}
					
					if(event.string("twitter").length() > 0) {
						user.add(USER_TWITTER, event.string("twitter"));
					}

					String show = "";

					if(event.string("show_first_name").length() > 0) {
						show += "1";
					}
					else {
						show += "0";
					}

					if(event.string("show_last_name").length() > 0) {
						show += "1";
					}
					else {
						show += "0";
					}

					if(event.string("show_country").length() > 0) {
						show += "1";
					}
					else {
						show += "0";
					}

					if(event.string("show_birthday").length() > 0) {
						show += "1";
					}
					else {
						show += "0";
					}

					if(event.string("show_gender").length() > 0) {
						show += "1";
					}
					else {
						show += "0";
					}

					if(event.string("show_mail").length() > 0) {
						show += "1";
					}
					else {
						show += "0";
					}

					Data data = Data.cache(USER, show);

					if(data == null) {
						Data.cache(USER, new Data(USER_SHOW, show));
						data = Data.cache(USER, show);
					}

					user.add(data);
					user.add(Sprout.generate(USER_KEY, 16));
					user.add(USER_IP, event.remote());

					if(Sprout.value("SELECT count(*) FROM node WHERE type = " + Type.USER) == 0) {
						user.add(Group.name("ADMIN"));
					}

					String live = event.daemon().properties.getProperty("live");

					if(live == null || !live.equals("true")) {
						user.add(Data.cache(USER, "VERIFIED"));
						user.update();

						System.out.println(user);

						save(event.session(), user, false);
						Sprout.redirect(event, "/");
					}
					else {
						user.add(Data.cache(USER, "UNVERIFIED"));
					}

					String key = user.meta(USER_KEY).getValue();
					String url = "http://" + host + "/login?key=" + key;
					String copy = content.replaceAll("@@url@@", url);
					copy = copy.replaceAll("@@key@@", key);

					send(event, mail, Sprout.i18n("Welcome!"), copy);

					user.update();

					Sprout.redirect(event, "/verify");
				}

				Sprout.redirect(event);
			}
		}
	}

	static void send(Event event, String mail, String title, String text) throws Event, Exception {
		try {
			eMail email = Post.create(User.mail, System.getProperty("address", "sprout@rupy.se"), title);
			email.addRecipient(eMail.TO, mail);
			email.send(text);
		}
		catch(Exception e) {
			event.query().put("error", Sprout.i18n("That's not an eMail!"));
			System.out.println(e.getMessage());
			Sprout.redirect(event);
		}
	}

	public static class Remind extends Service {
		public String path() { return "/remind"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse();

				String mail = event.string("mail").toLowerCase();

				if(mail.length() > 0) {
					User user = new User();
					if(user.query(USER_MAIL, mail)) {
						user.meta();

						String copy = remind.replaceAll("@@name@@", user.meta(USER_NAME).getValue());
						copy = copy.replaceAll("@@pass@@", user.meta(USER_PASS).getValue());

						send(event, mail, Sprout.i18n("Reminder!"), copy);

						event.query().put("error", Sprout.i18n("Reminder sent!"));
					}
					else {
						event.query().put("error", Sprout.i18n("eMail not found!"));
					}
				}

				Sprout.redirect(event);
			}
		}
	}

	static void save(Session session, User user, boolean remember) {
		String key = user.meta(USER_KEY).getValue();
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
		public String path() { return "/:/login:/register:/publish:/upload:/edit:/admin:/search"; }
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

	public static class Folder extends Service {
		public String path() { return null; }
		public void filter(Event event) throws Event, Exception {
			System.out.println(event.query().path());
			
			User user = new User();
			
			if(user.query(USER_NAME, event.query().path().substring(1))) {
				user.fill(10, 0, 10);
				// TODO: Really show articles...
				event.output().print("<pre>Show " + user.meta(USER_NAME).getValue() + "s articles!</pre>");
			}
			else {
				event.reply().code("404 Not Found");
				event.reply().output().print(
						"<pre>'" + event.query().path() + "' was not found.</pre>");
			}
		}
	}
	
	public static class Identify extends Service {
		public int index() { return 1; }
		public String path() { return "/publish:/upload:/edit:/admin:/search"; }
		public void filter(Event event) throws Event, Exception {
			String key = (String) event.session().get("key");

			if(key == null) {
				event.output().println("<pre>" + Sprout.i18n("You need to login!") + "</pre>");
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
