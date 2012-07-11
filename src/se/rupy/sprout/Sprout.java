package se.rupy.sprout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

import se.rupy.http.Daemon;
import se.rupy.http.Event;
import se.rupy.http.Service;
import se.rupy.memory.Base;
import se.rupy.memory.LinkBean;
import se.rupy.pool.Connection;
import se.rupy.pool.Pool;
import se.rupy.pool.Settings;
import se.rupy.util.Log;

public abstract class Sprout extends Service implements Type {
	public static String ROOT = "app/content";
	private static Base BASE;
	private static Pool POOL;
	private static Properties I18N;
	private static boolean TRANSLATE;
	public static SQL SQL;

	/*
	public void create(Daemon daemon) {
		try {
			SQL sql = new SQL();
			POOL = new Pool(sql, sql);
			BASE = new Base();
			BASE.init(POOL);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}
	*/

	public void destroy() {
		POOL.close();
	}
		
	static {
		try {
			SQL = new SQL();
			POOL = new Pool(SQL, SQL);
			BASE = new Base();
			BASE.init(POOL);
		}
		catch(SQLException e) {
			e.printStackTrace();
			SQL.message(e);
		}

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(ROOT + File.separator + "i18n.txt"), "UTF-8"));
			String line = in.readLine();
			I18N = new Properties();

			while(line != null) {
				int equals = line.indexOf("=");

				if(equals > -1) {
					String name = line.substring(0, equals).trim();
					String value = line.substring(equals + 1).trim();

					int comment = value.indexOf("//");

					if(comment > 0) {
						value = value.substring(0, comment).trim();
					}

					I18N.put(name, value);
				}

				line = in.readLine();
			}

			in.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		TRANSLATE = I18N.getProperty("translate", null) != null;
	}

	public static String i18n(String text) {
		if(translate()) {
			return I18N.getProperty(text, text);
		}
		return text;
	}

	public static boolean translate() {
		return TRANSLATE;
	}
	
	public static String language() {
		return I18N.getProperty("language", "general");
	}

	public static String clean(String line) {
		line = line.replaceAll("&", "&amp;"); // leave first
		line = line.replaceAll("\"", "&quot;");
		line = line.replaceAll("'", "&apos;");
		line = line.replaceAll("<", "&lt;");
		line = line.replaceAll(">", "&gt;");
		return line;
	}

	public static Data generate(short type, int length) throws Exception {
		Data data = new Data(type, Event.random(length));

		while(update(Base.SELECT, data)) {
			data.setString(Event.random(length));
		}

		return data;
	}

	public static boolean update(byte type, Object o) throws SQLException {
		return update(type, o, null);
	}

	public static boolean update(byte type, Object o, Connection connection) throws SQLException {
		if(connection == null) {
			return BASE.update(type, o);
		}
		else {
			return BASE.update(type, o, connection);
		}
	}

	public static LinkedList from(String name, String sql) throws SQLException {
		return BASE.query(Base.FROM, name, sql);
	}

	public static LinkedList where(String name, String sql) throws SQLException {
		return BASE.query(Base.WHERE, name, sql);
	}

	public static long value(String sql) throws SQLException {
		return BASE.value(sql);
	}

	public static void find(String sql) throws SQLException {
		BASE.find(sql, connection(false), true);
	}

	public static void find(String sql, Connection connection) throws SQLException {
		BASE.find(sql, connection, true);
	}

	public static Connection connection(boolean transaction) throws SQLException {
		return BASE.connection(transaction);
	}

	public static void redirect(Event event) throws IOException, Event {
		String referer = event.query().header("referer");
		redirect(event, referer == null ? "/" : referer, true);
	}

	public static void redirect(Event event, String path) throws IOException, Event {
		redirect(event, path, false);
	}

	public static void redirect(Event event, String path, boolean forward) throws IOException, Event {
		if(forward) {
			HashMap query = (HashMap) event.query().clone();
			event.session().put("post", query);
		}

		event.reply().header("Location", path);
		event.reply().code("302 Found");
		throw event;
	}

	public static class Cache extends LinkBean {
		public boolean invalid = true;

		public Cache(boolean invalid) {
			this.invalid = invalid;
		}
	}
	
	public static class SQL implements Log, Settings {
		private PrintStream out;

		public SQL() {
			try {
				// If you are using host.rupy.se
				// out = new PrintStream(new FileOutputStream("app/your.domain.name/log.txt", true));
				out = new PrintStream(new FileOutputStream("log.txt", true));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String driver() {
			// If you are using host.rupy.se
			// return "com.mysql.jdbc.Driver";
			return System.getProperty("dbdriver", "com.mysql.jdbc.Driver");
			//return System.getProperty("dbdriver", "org.postgresql.Driver");
			//return System.getProperty("dbdriver", "oracle.jdbc.OracleDriver");
		}

		public String url() {
			// If you are using host.rupy.se
			// return "jdbc:mysql://localhost/your_domain_name";
			return System.getProperty("dburl", "jdbc:mysql://localhost/sprout");
			//return System.getProperty("dburl", "jdbc:postgresql:sprout");
			//return System.getProperty("dburl", "jdbc:oracle:thin:@localhost:1521:xe");
		}

		public String user() {
			// If you are using host.rupy.se
			// return "your_domain_name";
			return System.getProperty("dbuser", "root");
			//return System.getProperty("dbuser", "postgres");
			//return System.getProperty("dbuser", "sprout");
		}

		public String pass() {
			// If you are using host.rupy.se
			// return "Y0URP4SS";
			return System.getProperty("dbpass", "");
			//return System.getProperty("dbpass", "postgres");
			//return System.getProperty("dbpass", "sprout");
		}

		public void message(Object o) {
			if(out == null) {
				if(o instanceof Throwable) {
					((Throwable) o).printStackTrace();
				} else {
					System.out.println(new SimpleDateFormat("yy/MM/dd HH:mm:ss")
					.format(Calendar.getInstance().getTime())
					+ " " + o);
				}
			} else {
				if(o instanceof Throwable) {
					((Throwable) o).printStackTrace(out);
				} else if(o instanceof java.sql.Statement) {
					String s = o.toString();
					int index = s.indexOf(':');
					
					if(index > 0) {
						s = s.substring(index + 1, s.length()).trim();
					}
					
					out.println(new SimpleDateFormat("yy/MM/dd HH:mm:ss")
					.format(Calendar.getInstance().getTime())
					+ " " + s);
				} else {
					out.println(new SimpleDateFormat("yy/MM/dd HH:mm:ss")
					.format(Calendar.getInstance().getTime())
					+ " " + o);
				}
			}
		}
	}
}
