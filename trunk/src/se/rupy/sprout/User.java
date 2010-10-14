package se.rupy.sprout;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.json.JSONObject;
import com.maxmind.geoip.LookupService;

import se.rupy.content.*;
import se.rupy.http.*;
import se.rupy.mail.*;
import se.rupy.memory.Base;

public class User extends Node {
	public static String host;
	public static final String[] month = {
		"Jan", 
		"Feb", 
		"Mar", 
		"Apr", 
		"May", 
		"Jun", 
		"Jul", 
		"Aug", 
		"Sep", 
		"Oct", 
		"Nov", 
		"Dec"
	};

	public static final String[] show = {
		"show_first_name", 
		"show_last_name", 
		"show_country", 
		"show_birthday", 
		"show_gender", 
		"show_mail", 
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

		content = read(Sprout.ROOT + java.io.File.separator + "mail.txt");
		remind = read(Sprout.ROOT + java.io.File.separator + "remind.txt");
	}

	static String read(String file) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String content = null;

		try {
			InputStream in = new FileInputStream(new java.io.File(file));
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

			if(node.meta(GROUP_NAME).getString().equals(group)) {
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

						if(user.safe(USER_PASS).equals(pass)) {
							String state = user.safe(USER_STATE);
							
							if(state.equals("VERIFIED")) {
								save(event.session(), user, event.query().bit("remember", false));

								if(ajax != null) {
									ajax.put("url", "/");
								}
							}
							else if(state.equals("UNVERIFIED")) {
								event.query().put("error", Sprout.i18n("Check your inbox!"));
							}
							else {
								event.query().put("error", Sprout.i18n("State not found!"));
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

	public static class Update extends Service {
		public int index() { return 1; }
		public String path() { return "/user:/register"; }
		public void create(Daemon daemon) throws Exception {
			try {
				if(lookup == null) {
					lookup = new LookupService(System.getProperty("user.dir") + "/res/GeoIP.dat", LookupService.GEOIP_MEMORY_CACHE);
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		public void destroy() throws Exception {
			if(lookup != null) {
				lookup.close();
			}
		}
		public void filter(Event event) throws Event, Exception {
			Object key = event.session().get("key");
			User user = User.get(key);
			event.query().parse();

			String mail = event.string("mail").toLowerCase();

			if(event.query().method() == Query.POST) {
				String name = event.string("name").toLowerCase();
				String pass = event.string("pass");
				String word = event.string("word");
				String gender = event.string("gender").toUpperCase();

				int day = event.medium("day");
				int month = event.medium("month");
				int year = event.medium("year");

				if(name.length() > 0 && mail.length() > 0 && pass.length() > 0 && day > 0 && month > 0 && year > 0) {
					String old_mail = "";
					String old_key = "";

					if(!pass.equals(word)) {
						event.query().put("error", Sprout.i18n("Passwords don't match!"));
						Sprout.redirect(event);
					}

					if(user == null) {
						user = new User();
					}
					else {
						old_mail = user.safe(USER_MAIL);
						old_key = user.safe(USER_KEY);
					}

					boolean send = !user.safe(USER_MAIL).equals(mail);

					if(send && user.query(USER_MAIL, mail)) {
						event.query().put("error", Sprout.i18n("eMail already in use!"));
						Sprout.redirect(event);
					}

					if(!user.safe(USER_NAME).equals(name)) {
						if(user.query(USER_NAME, name)) {
							event.query().put("error", Sprout.i18n("Nickname already in use!"));
							Sprout.redirect(event);
						}
						else {
							Article.reset();
						}
					}

					user.add(USER_MAIL, mail);
					user.add(USER_NAME, name);
					user.add(USER_PASS, pass);

					user.add(USER_BIRTHDAY, year + "-" + month + "-" + day); // ISO 8601

					if(gender.length() > 0) {
						user.add(Data.cache(USER, gender));
					}

					if(event.string("country").length() > 0 && !event.string("country").equals("--")) {
						user.add(Data.cache(USER, event.string("country")));
					}

					if(event.string("first").length() > 0) {
						user.add(USER_FIRST_NAME, event.string("first"));
					}

					if(event.string("last").length() > 0) {
						user.add(USER_LAST_NAME, event.string("last"));
					}

					String show = "";

					for(int i = 0; i < User.show.length; i++) {
						if(event.string(User.show[i]).length() > 0) {
							show += "1";
						}
						else {
							show += "0";
						}
					}

					Data data = Data.cache(USER, show);

					if(data == null) {
						Data.cache(USER, new Data(USER_SHOW, show));
						data = Data.cache(USER, show);
					}

					user.add(data);
					user.add(USER_IP, event.remote());

					String select = "SELECT count(*) FROM node WHERE type = " + Type.USER;
					
					if(Sprout.SQL.driver().equals("org.postgresql.Driver") || 
							Sprout.SQL.driver().equals("oracle.jdbc.OracleDriver")) {
						select = "SELECT count(*) FROM node_table WHERE node_type = " + Type.USER;
					}
					
					if(Sprout.value(select) == 0) {
						user.add(Group.name("ADMIN"));
					}

					String live = event.daemon().properties.getProperty("live");

					if(send) {
						user.add(Sprout.generate(USER_KEY, 16));
					}

					if(live == null || !live.equals("true")) {
						user.add(Data.cache(USER, "VERIFIED"));

						save(event.session(), user, false);

						send = false;
					}

					boolean verify = false;
					
					if(send) {
						String url = "http://" + host + "/login?key=" + user.safe(USER_KEY);
						String copy = content.replaceAll("@@url@@", url);
						copy = copy.replaceAll("@@key@@", user.safe(USER_KEY));

						if(send(event, mail, Sprout.i18n("Welcome!"), copy)) {
							user.add(Data.cache(USER, "UNVERIFIED"));
							user.update();

							verify = true;
						}
						else {
							event.query().put("mail", old_mail);
							
							if(old_mail.length() > 0) {
								user.add(USER_MAIL, old_mail);
								user.add(USER_KEY, old_key);
							}
							
							Sprout.redirect(event);
						}
					}
					else {
						user.update();
					}

					String picture = event.string("picture");
					String profile = "file" + user.path() + "/picture.jpeg";

					if(picture.length() > 0 && !picture.startsWith(profile)) {
						File.copy(Sprout.ROOT + "/" + picture, Sprout.ROOT + "/file" + user.path(), "picture.jpeg");

						Node file = new File();
						Node old = user.child(FILE, FILE_TYPE, "IMAGE");

						if(old != null) {
							file = old;
							file.setDate(System.currentTimeMillis());
						}

						file.add(File.type("IMAGE"));

						if(old == null) {
							user.add(file);
						}
						else {
							file.update();
						}

						event.query().put("picture", profile + "?time=" + System.currentTimeMillis());
					}

					if(send && verify) {
						Sprout.redirect(event, "/verify");
					}
					
					if(event.query().path().equals("/user")) {
						event.query().put("error", Sprout.i18n("Profile saved!"));
					}
					else {
						Sprout.redirect(event, "/");
					}
				}
				else {
					event.query().put("error", Sprout.i18n("Fill in the mandatory fields!"));
				}

				Sprout.redirect(event);
			}
			else if(event.query().path().equals("/user") && user != null && mail.length() == 0) {
				event.query().put("mail", user.safe(USER_MAIL));
				event.query().put("name", user.safe(USER_NAME));
				event.query().put("country", user.safe(USER_COUNTRY));
				event.query().put("first", user.safe(USER_FIRST_NAME));
				event.query().put("last", user.safe(USER_LAST_NAME));
				event.query().put("gender", user.safe(USER_GENDER).toLowerCase());
				event.query().put("pass", user.safe(USER_PASS));
				event.query().put("word", user.safe(USER_PASS));

				String birthday = user.safe(USER_BIRTHDAY);

				if(birthday.length() > 0) {
					StringTokenizer token = new StringTokenizer(birthday, "-");

					event.query().put("year", token.nextToken());
					event.query().put("month", token.nextToken());
					event.query().put("day", token.nextToken());
				}

				String show = user.safe(USER_SHOW);

				if(show.length() > 0) {
					for(int i = 0; i < User.show.length; i++) {
						if(show.charAt(i) == '1') {
							event.query().put(User.show[i], "on");
						}
					}
				}

				Node picture = (Node) user.child(FILE, FILE_TYPE, "IMAGE");

				if(picture != null) {
					String profile = "file" + user.path() + "/picture.jpeg";
					event.query().put("picture", profile + "?time=" + System.currentTimeMillis());
				}
			}
		}
	}

	static boolean send(Event event, String mail, String title, String text) throws Event, Exception {
		try {
			eMail email = Post.create(User.mail, System.getProperty("address", "sprout@rupy.se"), title);
			email.addRecipient(eMail.TO, mail);
			email.send(text);
		}
		catch(Exception e) {
			event.query().put("error", Sprout.i18n("That's not an eMail!"));
			System.out.println(e.getMessage());
			return false;
		}

		return true;
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

						String copy = remind.replaceAll("@@name@@", user.meta(USER_NAME).getString());
						copy = copy.replaceAll("@@pass@@", user.meta(USER_PASS).getString());

						if(send(event, mail, Sprout.i18n("Reminder!"), copy)) {
							event.query().put("error", Sprout.i18n("Reminder sent!"));
						}
						else {
							Sprout.redirect(event);
						}
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
		if(user != null) {
			String key = user.safe(USER_KEY);
			session.put("key", key);
			cache.put(key, user);
		
			if(remember) {
				long time = (long) 1000 * 60 * 60 * 24 * 365;
				session.key(key, User.host, System.currentTimeMillis() + time);
			}
		}
	}

	static void kill(Object key) {
		cache.remove(key);
	}

	public static class Timeout extends Service {
		public String path() { return "/:/login:/user:/register:/publish:/upload:/picture:/edit:/label"; }
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
			event.reply().header("X-UA-Compatible", "IE=7");

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

	public static class Nick extends Service {
		public String path() { return "/nick"; }
		public void filter(Event event) throws Event, Exception {
			event.query().parse();
			JSONObject ajax = new JSONObject();

			if(event.string("name").length() == 0) {
				ajax.put("name", "found");
			}
			else {
				Data name = new Data(Type.USER_NAME, event.string("name"));

				if(Sprout.update(Base.SELECT, name)) {
					ajax.put("name", "found");
				}
				else {
					ajax.put("name", "available");
				}
			}

			event.output().print(ajax.toString());
		}
	}

	public static class Folder extends Service {
		public String path() { return null; }
		public void filter(Event event) throws Event, Exception {
			System.out.println(event.query().path());

			User user = new User();

			if(user.query(USER_NAME, event.query().path().substring(1))) {
				user.fill(10, 0, 10);
				
				event.daemon().chain("/header").filter(event);
				
				Output out = event.output();
				
				out.println("<table>");
				
				Node node = user.child(FILE, FILE_TYPE, "IMAGE");
				
				if(node != null) {
					out.println("<tr><td colspan=\"2\"><img src=\"/file" + user.path() + "/picture.jpeg?time=" + System.currentTimeMillis() + "\">");
				}
				
				out.println("<tr><td>" + Sprout.i18n("Nickname") + ":&nbsp;&nbsp;</td><td>" + user.safe(USER_NAME) + "</td></tr>");
				
				out.println("<tr><td>" + Sprout.i18n("First&nbsp;Name") + ":&nbsp;&nbsp;</td><td>" + user.safe(USER_FIRST_NAME) + "</td></tr>");
				
				out.println("<tr><td>" + Sprout.i18n("Last&nbsp;Name") + ":&nbsp;&nbsp;</td><td>" + user.safe(USER_LAST_NAME) + "</td></tr>");
				
				for(int i = 0; i < User.countryCode.length; i++) {
					if(User.countryCode[i].equals(user.safe(USER_COUNTRY))) {
						out.println("<tr><td>" + Sprout.i18n("Country") + ":&nbsp;&nbsp;</td><td><img src=\"res/flag/" + User.countryCode[i].toLowerCase() + ".png\" style=\"vertical-align: middle;\"/></td></tr>");
					}
				}
				
				out.println("</table></div></body></html>");
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
		public String path() { return "/publish:/upload:/picture:/edit:/label"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().path().equals("/picture")) {
				return;
			}

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
