package se.rupy.sprout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONObject;

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
	public static final String[] country = {
		"Afghanistan",
		"Aland Islands",
		"Albania",
		"Algeria",
		"American Samoa",
		"Andorra",
		"Angola",
		"Anguilla",
		"Antarctica",
		"Antigua and Barbuda",
		"Argentina",
		"Armenia",
		"Aruba",
		"Australia",
		"Austria",
		"Azerbaijan",
		"Bahamas",
		"Bahrain",
		"Bangladesh",
		"Barbados",
		"Belarus",
		"Belgium",
		"Belize",
		"Benin",
		"Bermuda",
		"Bhutan",
		"Bolivia",
		"Bosnia and Herzegovina",
		"Botswana",
		"Bouvet Island",
		"Brazil",
		"British Indian Ocean Territory",
		"Brunei Darussalam",
		"Bulgaria",
		"Burkina Faso",
		"Burundi",
		"Cambodia",
		"Cameroon",
		"Canada",
		"Cape Verde",
		"Cayman Islands",
		"Central African Republic",
		"Chad",
		"Chile",
		"China",
		"Christmas Island",
		"Cocos (Keeling) Islands",
		"Colombia",
		"Comoros",
		"Congo",
		"Congo, The Democratic Republic of the",
		"Cook Islands",
		"Costa Rica",
		"Cote D'Ivoire",
		"Croatia",
		"Cuba",
		"Cyprus",
		"Czech Republic",
		"Denmark",
		"Djibouti",
		"Dominica",
		"Dominican Republic",
		"Ecuador",
		"Egypt",
		"El Salvador",
		"Equatorial Guinea",
		"Eritrea",
		"Estonia",
		"Ethiopia",
		"Falkland Islands (Malvinas)",
		"Faroe Islands",
		"Fiji",
		"Finland",
		"France",
		"French Guiana",
		"French Polynesia",
		"French Southern Territories",
		"Gabon",
		"Gambia",
		"Georgia",
		"Germany",
		"Ghana",
		"Gibraltar",
		"Greece",
		"Greenland",
		"Grenada",
		"Guadeloupe",
		"Guam",
		"Guatemala",
		"Guernsey",
		"Guinea",
		"Guinea-Bissau",
		"Guyana",
		"Haiti",
		"Heard Island and McDonald Islands",
		"Holy See (Vatican City State)",
		"Honduras",
		"Hong Kong",
		"Hungary",
		"Iceland",
		"India",
		"Indonesia",
		"Iran, Islamic Republic of",
		"Iraq",
		"Ireland",
		"Isle of Man",
		"Israel",
		"Italy",
		"Jamaica",
		"Japan",
		"Jersey",
		"Jordan",
		"Kazakstan",
		"Kenya",
		"Kiribati",
		"Korea, Democratic People's Republic of",
		"Korea, Republic of",
		"Kuwait",
		"Kyrgyzstan",
		"Lao People's Democratic Republic",
		"Latvia",
		"Lebanon",
		"Lesotho",
		"Liberia",
		"Libyan Arab Jamahiriya",
		"Liechtenstein",
		"Lithuania",
		"Luxembourg",
		"Macau",
		"Macedonia",
		"Madagascar",
		"Malawi",
		"Malaysia",
		"Maldives",
		"Mali",
		"Malta",
		"Marshall Islands",
		"Martinique",
		"Mauritania",
		"Mauritius",
		"Mayotte",
		"Mexico",
		"Micronesia, Federated States of",
		"Moldova, Republic of",
		"Monaco",
		"Mongolia",
		"Montenegro",
		"Montserrat",
		"Morocco",
		"Mozambique",
		"Myanmar",
		"Namibia",
		"Nauru",
		"Nepal",
		"Netherlands",
		"Netherlands Antilles",
		"New Caledonia",
		"New Zealand",
		"Nicaragua",
		"Niger",
		"Nigeria",
		"Niue",
		"Norfolk Island",
		"Northern Mariana Islands",
		"Norway",
		"Oman",
		"Pakistan",
		"Palau",
		"Palestinian Territory",
		"Panama",
		"Papua New Guinea",
		"Paraguay",
		"Peru",
		"Philippines",
		"Poland",
		"Portugal",
		"Puerto Rico",
		"Qatar",
		"Reunion",
		"Romania",
		"Russian Federation",
		"Rwanda",
		"Saint Helena",
		"Saint Kitts and Nevis",
		"Saint Lucia",
		"Saint Pierre and Miquelon",
		"Saint Vincent and the Grenadines",
		"Samoa",
		"San Marino",
		"Sao Tome and Principe",
		"Saudi Arabia",
		"Senegal",
		"Serbia",
		"Seychelles",
		"Sierra Leone",
		"Singapore",
		"Slovakia",
		"Slovenia",
		"Solomon Islands",
		"Somalia",
		"South Africa",
		"South Georgia and the South Sandwich Islands",
		"Spain",
		"Sri Lanka",
		"Sudan",
		"Suriname",
		"Svalbard and Jan Mayen",
		"Swaziland",
		"Sweden",
		"Switzerland",
		"Syrian Arab Republic",
		"Taiwan",
		"Tajikistan",
		"Tanzania, United Republic of",
		"Thailand",
		"Togo",
		"Tokelau",
		"Tonga",
		"Trinidad and Tobago",
		"Tunisia",
		"Turkey",
		"Turkmenistan",
		"Turks and Caicos Islands",
		"Tuvalu",
		"Uganda",
		"Ukraine",
		"United Arab Emirates",
		"United Kingdom",
		"United States",
		"United States Minor Outlying Islands",
		"Uruguay",
		"Uzbekistan",
		"Vanuatu",
		"Venezuela",
		"Vietnam",
		"Virgin Islands, British",
		"Virgin Islands, U.S.",
		"Wallis and Futuna",
		"Western Sahara",
		"Yemen",
		"Zambia",
		"Zimbabwe"
		};

	private static String mail;
	private static String content, remind;

	private final static String EOL = "\r\n";

	private static HashMap cache = new HashMap();

	static {
		Data.cache(USER, new Data(USER_STATE, "UNVERIFIED"));
		Data.cache(USER, new Data(USER_STATE, "VERIFIED"));
		Data.cache(USER, new Data(USER_GENDER, "MALE"));
		Data.cache(USER, new Data(USER_GENDER, "FEMALE"));
		
		for(int i = 0; i < country.length; i++) {
			Data.cache(USER, new Data(USER_COUNTRY, country[i]));
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
					if(user.query(USER_MAIL, mail)) {
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
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse();

				String mail = event.string("mail").toLowerCase();
				String name = event.string("name");
				String pass = event.string("pass");
				String word = event.string("word");
				
				System.out.println(mail);
				
				int day = event.medium("day");
				int month = event.medium("month");
				int year = event.medium("year");

				if(name.length() > 0 && mail.length() > 0 && pass.length() > 0 && day > 0 && month > 0 && year > 0) {
					if(pass.equals(word)) {
						User user = new User();
						if(!user.query(USER_MAIL, mail)) {
							user.add(USER_MAIL, mail);
							user.add(USER_NAME, name);
							user.add(USER_PASS, pass);
							
							user.add(USER_BIRTHDAY, day + "/" + month + "-" + year);
							
							if(event.medium("country") > 0) {
								user.add(Data.cache(USER, country[event.medium("country") - 1]));
							}
							
							if(event.string("first").length() > 0) {
								user.add(USER_FIRST_NAME, event.string("first"));
							}
							
							if(event.string("last").length() > 0) {
								user.add(USER_LAST_NAME, event.string("last"));
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

	static void send(Event event, String mail, String title, String text) throws Event, Exception {
		try {
			eMail email = Post.create(User.mail, System.getProperty("address", "sprout@rupy.se"), title);
			email.addRecipient(eMail.TO, mail);
			email.send(text);
		}
		catch(Exception e) {
			event.query().put("error", Sprout.i18n("That's not an e-mail!"));
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
						event.query().put("error", Sprout.i18n("E-mail not found!"));
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
