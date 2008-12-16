package se.rupy.sprout;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import se.rupy.http.Event;
import se.rupy.http.Service;
import se.rupy.memory.Base;
import se.rupy.memory.LinkBean;
import se.rupy.memory.NodeBean;
import se.rupy.pool.Connection;
import se.rupy.pool.Pool;
import se.rupy.pool.Settings;
import se.rupy.util.Log;

public abstract class Sprout extends Service implements Type {
	private static Base db;
	private static HashMap cache = new HashMap();
	
	static {
		db = new Base();

		try {
			MySQL mysql = new MySQL();
			db.init(new Pool(mysql, mysql));
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	protected static String i18n(String text) {
		return text;
	}
	
	public static String clean(String line) {
		line = line.replaceAll("&", "&amp;");
		line = line.replaceAll("<", "&lt;");
		line = line.replaceAll(">", "&gt;");
		return line;
	}
	
	static Data generate(short type, int length) throws Exception {
		Data data = new Data();
		data.setType(type);
		data.setValue(Event.random(length));

		while(update(Base.SELECT, data)) {
			data.setValue(Event.random(length));
		}
		
		return data;
	}
	
	static boolean update(byte type, Object o) throws SQLException {
		return update(type, o, null);
	}

	static boolean update(byte type, Object bean, Connection connection) throws SQLException {
		if(connection == null) {
			return db.update(type, bean);
		}
		else {
			return db.update(type, bean, connection);
		}
	}
	
	static long value(String sql) throws SQLException {
		return db.value(sql);
	}
	
	static Connection connection(boolean transaction) throws SQLException {
		return db.connection(transaction);
	}
	
	protected static void invalidate(String type, Article article) {
		Cache old = (Cache) cache.get(type);
		
		if(old != null) {
			old.invalid = true;
		}
		
		article.invalidate();
		
		cache.put(new Long(article.getId()), article);
	}
	
	protected static Article get(long id) throws SQLException {
		if(id == 0) {
			return null;
		}
		
		Article old = (Article) cache.get(new Long(id));

		if(old == null) {
			old = new Article();
			old.setId(id);

			db.update(Base.SELECT, old);

			old.fill(true);
			
			cache.put(new Long(id), old);
		}
		
		return old;
	}
	
	protected static Cache get(String type) throws SQLException {
		Cache old = (Cache) cache.get(type);
				
		if(old == null) {
			old = new Cache(true);
		}
		
		if(old.invalid) {
			old = new Cache(false);
			old.setType(ARTICLE | USER);
			old.setParent(-1);
			db.update(Base.SELECT, old);
			
			int index = 0;
			Iterator it = old.iterator();
			
			while(it.hasNext()) {
				NodeBean node = (NodeBean) it.next();
				Node article = new Article();
				
				article.copy(node);
				article.fill(true);

				cache.put(new Long(article.getId()), article);
				
				Node user = (Node) article.get(USER).getFirst();
				user.meta();
				
				old.set(index++, article);
			}
			
			cache.put(type, old);
		}
		
		return old;
	}
	
	protected static void redirect(Event event) throws IOException, Event {
		HashMap query = (HashMap) event.query().clone();
		event.session().put("post", query);
		String referer = event.query().header("referer");
		redirect(event, referer == null ? "/" : referer);
	}
	
	protected static void redirect(Event event, String path) throws IOException, Event {
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
	
	public static class MySQL implements Log, Settings {
		private PrintStream out;

		public MySQL() {
			try {
				out = new PrintStream(new FileOutputStream("log.txt", true));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String driver() {
			return "com.mysql.jdbc.Driver";
		}

		public String url() {
			return "jdbc:mysql://localhost/sprout";
		}

		public String user() {
			return "root";
		}

		public String pass() {
			return "";
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
				} else {
					out.println(new SimpleDateFormat("yy/MM/dd HH:mm:ss")
					.format(Calendar.getInstance().getTime())
					+ " " + o);
				}
			}
		}
	}
}
