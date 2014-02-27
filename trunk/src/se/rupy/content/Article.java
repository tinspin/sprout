package se.rupy.content;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import se.rupy.http.Event;
import se.rupy.http.Output;
import se.rupy.http.Query;
import se.rupy.http.Service;
import se.rupy.memory.Base;
import se.rupy.memory.NodeBean;
import se.rupy.memory.PollBean;
import se.rupy.pool.Connection;
import se.rupy.sprout.Data;
import se.rupy.sprout.Node;
import se.rupy.sprout.Sprout;
import se.rupy.sprout.Type;
import se.rupy.sprout.User;
import se.rupy.sprout.Sprout.Cache;

public class Article extends Node {
	public final static int PAGE = 20;
	public final static byte NO = 0;
	public final static byte USER = 1 << 0;
	public final static byte ADMIN = 1 << 1;
	public final static byte WILDCARD = PARENT | CHILD | META | POLL;
	public final static int MAX_POST_SIZE = 1024 * 1024; // 1MB

	public static long MAX = 0;
	public static HashMap cache1 = new HashMap(); // by id
	public static HashMap cache2 = new HashMap(); // by page
	public static HashMap cache3 = new HashMap(); // by user
	public static FontMetrics metric = Toolkit.getDefaultToolkit().getFontMetrics(new Font("Sans", Font.PLAIN, 8));

	public LinkedList columns;
	public int comments, pings;

	static {
		try {
			if(Sprout.SQL.driver().equals("org.postgresql.Driver") || 
					Sprout.SQL.driver().equals("oracle.jdbc.OracleDriver")) {
				MAX = Sprout.value("SELECT count(*) FROM link_table WHERE link_type = " + (ARTICLE | USER));
			}
			else {
				MAX = Sprout.value("SELECT count(*) FROM link WHERE type = " + (ARTICLE | USER));
			}
		}
		catch(SQLException s) {
			s.printStackTrace();
		}
	}

	public Article() {
		super(ARTICLE);
	}

	public int permit(Object key) throws SQLException {
		User user = User.get(key);

		if(user == null) {
			return NO;
		}

		try {
			Node node = (Node) child(USER).getFirst();

			if(user.getId() == node.getId()) {
				return USER;
			}
		}
		catch(NoSuchElementException e) {}

		if(user.child(GROUP, GROUP_NAME, "ADMIN") != null) {
			return ADMIN;
		}

		return NO;
	}

	public static long max(short type) throws Exception {
		Connection conn = Sprout.connection(false);
		PreparedStatement stmt = null;
		ResultSet result = null;
		String sql = null;
		try {
			sql = "SELECT count(*) AS count FROM poll WHERE type = " + type;
			
			if(Sprout.SQL.driver().equals("org.postgresql.Driver") || 
					Sprout.SQL.driver().equals("oracle.jdbc.OracleDriver")) {
				sql = "SELECT count(*) AS count FROM poll_table WHERE poll_type = " + type;
			}
			
			stmt = conn.prepareStatement(sql);
			result = stmt.executeQuery();
			if(result.next()) {
				return result.getLong("count");
			}
		} catch(SQLException e) {
			throw e;
		} finally {
			if(result != null) {
				result.close();
			}
			if(stmt != null) {
				stmt.close();
			}
			if(conn != null && conn.getAutoCommit()) {
				conn.close();
			}
		}

		return 0;
	}

	public static LinkedList most(short type, int start, int limit) throws SQLException {
		return most(type, start, limit, Sprout.SQL.driver().equals("com.mysql.jdbc.Driver") ? "ORDER BY value DESC" : "ORDER BY poll_value DESC");
	}

	public static LinkedList most(short type, int start, int limit, String order) throws SQLException {
		LinkedList list = new LinkedList();
		Connection conn = Sprout.connection(false);
		PreparedStatement stmt = null;
		ResultSet result = null;
		String sql = null;
		try {
			sql = "SELECT node FROM poll WHERE type = " + type + " " + order + " LIMIT " + start * limit + ", " + limit;
			
			if(Sprout.SQL.driver().equals("org.postgresql.Driver")) {
				sql = "SELECT poll_node FROM poll_table WHERE poll_type = " + type + " " + order + " OFFSET " + start * limit + " LIMIT " + limit;
			}
			
			if(Sprout.SQL.driver().equals("oracle.jdbc.OracleDriver")) {
				sql = "SELECT poll_node FROM poll_table WHERE poll_type = " + type + " AND ROWNUM BETWEEN " + start * limit + " AND " + (start * limit + limit) + " " + order;
			}
			
			stmt = conn.prepareStatement(sql);
			result = stmt.executeQuery();
			while(result.next()) {
				list.add(find(result.getLong(Sprout.SQL.driver().equals("com.mysql.jdbc.Driver") ? "node" : "poll_node")));
			}
		} catch(SQLException e) {
			throw e;
		} finally {
			if(result != null) {
				result.close();
			}
			if(stmt != null) {
				stmt.close();
			}
			if(conn != null && conn.getAutoCommit()) {
				conn.close();
			}
		}

		return list;
	}

	static String from(String query) {
		/*
		 * poor performing but easier to understand query
		 * 
		return "FROM node n " + // node
		// title and body joins
		"LEFT JOIN meta m1 ON (n.id = m1.node) " + // entry - data (body and title)
		"LEFT JOIN data d1 ON (m1.data = d1.id) " + // body
		"LEFT JOIN data d2 ON (m1.data = d2.id) " + // title
		// name joins
		"LEFT JOIN link l1 ON (n.id = l1.parent " + // article - user
		"AND l1.type = " + (ARTICLE | USER) + ") " + // ignore COMMENT | USER
		"LEFT JOIN node n2 ON (l1.child = n2.id) " + // user
		"LEFT JOIN meta m2 ON (n2.id = m2.node) " + // user - data (name)
		"LEFT JOIN data d3 ON (m2.data = d3.id) " + // name
		"WHERE " + 
		"(d1.type = 400 AND d1.value LIKE \"%" + query + "%\") OR " + // title
		"(d2.type = 401 AND d2.value LIKE \"%" + query + "%\") OR " + // body
		"(d3.type = 100 AND d3.value LIKE \"%" + query + "%\")" + // name
		 */
		
		if(Sprout.SQL.driver().equals("org.postgresql.Driver")) {
			return "FROM node_table n, meta_table m1, data_table d1, data_table d2, link_table l1, node_table n2, meta_table m2, data_table d3 " + 
			"WHERE ((d1.data_type = 200 AND d1.data_value LIKE ?) OR " + 
			"(d2.data_type = 201 AND d2.data_value LIKE ?) OR " + 
			"(d3.data_type = 100 AND d3.data_value LIKE ?)) AND " + 
			"(n.node_id = m1.meta_node AND m1.meta_data = d1.data_id AND m1.meta_data = d2.data_id AND n.node_id = l1.link_parent AND l1.link_type = " + 
			(ARTICLE | USER) + " AND l1.link_child = n2.node_id AND n2.node_id = m2.meta_node AND m2.meta_data = d3.data_id)";
		}

		if(Sprout.SQL.driver().equals("oracle.jdbc.OracleDriver")) {
			return "FROM node_table n, meta_table m1, data_table d1, data_table d2, link_table l1, node_table n2, meta_table m2, data_table d3 " + 
			"WHERE ((d1.data_type = 200 AND dbms_lob.instr(d1.data_value, ?, 1, 1) > 0) OR " + 
			"(d2.data_type = 201 AND dbms_lob.instr(d2.data_value, ?, 1, 1) > 0) OR " + 
			"(d3.data_type = 100 AND dbms_lob.instr(d3.data_value, ?, 1, 1) > 0)) AND " + 
			"(n.node_id = m1.meta_node AND m1.meta_data = d1.data_id AND m1.meta_data = d2.data_id AND n.node_id = l1.link_parent AND l1.link_type = " + 
			(ARTICLE | USER) + " AND l1.link_child = n2.node_id AND n2.node_id = m2.meta_node AND m2.meta_data = d3.data_id)";
		}

		return "FROM node n, meta m1, data d1, data d2, link l1, node n2, meta m2, data d3 " + 
		"WHERE ((d1.type = 200 AND d1.value LIKE ?) OR " + 
		"(d2.type = 201 AND d2.value LIKE ?) OR " + 
		"(d3.type = 100 AND d3.value LIKE ?)) AND " + 
		"(n.id = m1.node AND m1.data = d1.id AND m1.data = d2.id AND n.id = l1.parent AND l1.type = " + 
		(ARTICLE | USER) + " AND l1.child = n2.id AND n2.id = m2.node AND m2.data = d3.id)";
	}

	public static long max(String query) throws Exception {
		Connection conn = Sprout.connection(false);
		PreparedStatement stmt = null;
		ResultSet result = null;
		String sql = null;
		try {
			sql = "SELECT count(DISTINCT n.id) AS count " + from(query);
			
			if(Sprout.SQL.driver().equals("org.postgresql.Driver") || 
					Sprout.SQL.driver().equals("oracle.jdbc.OracleDriver")) {
				sql = "SELECT count(DISTINCT n.node_id) AS count " + from(query);
			}
			
			stmt = conn.prepareStatement(sql);
			
			byte[] data = ("%" + query + "%").getBytes("UTF-8");

			if(Sprout.SQL.driver().equals("oracle.jdbc.OracleDriver")) {
				data = query.getBytes("UTF-8");
			}
			
			stmt.setBytes(1, data);
			stmt.setBytes(2, data);
			stmt.setBytes(3, data);
			
			result = stmt.executeQuery();
			if(result.next()) {
				return result.getLong("count");
			}
		} catch(SQLException e) {
			throw e;
		} finally {
			if(result != null) {
				result.close();
			}
			if(stmt != null) {
				stmt.close();
			}
			if(conn != null && conn.getAutoCommit()) {
				conn.close();
			}
		}

		return 0;
	}

	public static LinkedList query(String query, int start, int limit) throws Exception {
		LinkedList list = new LinkedList();
		Connection conn = Sprout.connection(false);
		PreparedStatement stmt = null;
		ResultSet result = null;
		String sql = null;
		try {
			sql = "SELECT DISTINCT n.id " + from(query) + " ORDER BY n.date DESC LIMIT " + start * limit + ", " + limit;
			
			if(Sprout.SQL.driver().equals("org.postgresql.Driver")) {
				sql = "SELECT DISTINCT n.node_date, n.node_id " + from(query) + " ORDER BY n.node_date DESC OFFSET " + start * limit + " LIMIT " + limit;
			}
			
			if(Sprout.SQL.driver().equals("oracle.jdbc.OracleDriver")) {
				sql = "SELECT DISTINCT n.node_date, n.node_id " + from(query) + " AND ROWNUM BETWEEN " + start * limit + " AND " + (start * limit + limit) + " ORDER BY n.node_date DESC";
			}
			
			stmt = conn.prepareStatement(sql);
			
			byte[] data = ("%" + query + "%").getBytes("UTF-8");

			if(Sprout.SQL.driver().equals("oracle.jdbc.OracleDriver")) {
				data = query.getBytes("UTF-8");
			}
			
			stmt.setBytes(1, data);
			stmt.setBytes(2, data);
			stmt.setBytes(3, data);
			
			result = stmt.executeQuery();
			while(result.next()) {
				list.add(find(result.getLong(Sprout.SQL.driver().equals("com.mysql.jdbc.Driver") ? "id" : "node_id")));
			}
		} catch(SQLException e) {
			throw e;
		} finally {
			if(result != null) {
				result.close();
			}
			if(stmt != null) {
				stmt.close();
			}
			if(conn != null && conn.getAutoCommit()) {
				conn.close();
			}
		}

		return list;
	}
	
	public static void reset() throws SQLException {
		cache1.clear();
		cache2.clear();
	}
	
	protected static void invalidate(Article article) throws SQLException {
		cache2.clear();
		article.invalidate();
		cache1.put(new Long(article.getId()), article);
		article.count();
	}

	public void count() throws SQLException {
		comments = count(COMMENT, COMMENT_STATE);
		pings = count(PING, PING_STATE);
	}

	int count(int type, short state) throws SQLException {
		Iterator it = child(type).iterator();
		int count = 0;

		while(it.hasNext()) {
			Node node = (Node) it.next();
			Data data = node.meta(state);

			if(data != null && data.getString().equals("SHOW")) {
				count++;
			}
		}

		return count;
	}

	public static Article find(long id) throws SQLException {
		if(id == 0) {
			return null;
		}

		Article old = (Article) cache1.get(new Long(id));

		if(old == null) {
			old = new Article();
			old.setId(id);

			Sprout.update(Base.SELECT, old);

			old.fill(10, 0, 100);

			cache1.put(new Long(id), old);

			old.count();
		}

		return old;
	}

	public static Cache get(String type, int start, int limit) throws SQLException {
		String key = type + "|" + start + "|" + limit;
		Cache old = (Cache) cache2.get(key);

		if(old == null) {
			old = new Cache(true);
		}

		if(old.invalid) {
			old = new Cache(false);
			old.setType(ARTICLE | USER);
			old.setParent(-1);
			old.setStart(start);
			old.setLimit(limit);
			Sprout.update(Base.SELECT, old);

			int index = 0;
			Iterator it = old.iterator();

			while(it.hasNext()) {
				NodeBean node = (NodeBean) it.next();
				Article article = new Article();

				article.copy(node);
				
				//System.out.println(article);
				
				article.fill(10, 0, 100);
				
				//System.out.println(article);

				cache1.put(new Long(article.getId()), article);

				article.count();

				//Node user = (Node) article.child(USER).getFirst();
				//user.meta();

				old.set(index++, article);
			}

			cache2.put(key, old);
		}

		return old;
	}

	public static Article get(Object key) throws SQLException {
		Article article = (Article) cache3.get(key);
		User user = (User) User.get(key);

		if(article == null && user != null) {
			article = new Article();
			article.add(user);
			cache3.put(key, article);
		}

		return article;
	}

	public void invalidate() {
		columns = null;
	}

	/*
	 * Columnize the text.
	 */

	public static int COLUMN_WIDTH = 2000;
	public static int CHARACTER_WIDTH = 6;

	public LinkedList columnize() {
		if(columns == null) {
			columns = new LinkedList();
			String column = "", text = safe(ARTICLE_BODY);
			int length = 0, begin = 0, end = COLUMN_WIDTH / CHARACTER_WIDTH;
			
			// loop until all overflowing data has been columnized
			while(end < text.length() - COLUMN_WIDTH / CHARACTER_WIDTH) {

				// loop until the column is full
				while(length < COLUMN_WIDTH) {
					end = text.indexOf(" ", end) + 1;
					column = text.substring(begin, end);
					
					if(column.indexOf("<pre>") > -1) {
						end = text.indexOf("</pre>", end) + 6;
						column = text.substring(begin, end);
					}
					
					length = metric.stringWidth(column);
					
					//System.out.print(".");

					if(end >= text.lastIndexOf(" ")) {
						break;
					}
				}

				columns.add(column);

				begin = end;
				end += COLUMN_WIDTH / CHARACTER_WIDTH;
				length = 0;
			}

			// write remaining text into last column
			columns.add(text.substring(begin, text.length()));
		}

		return columns;
	}

	public static class Delete extends Service {
		public String path() { return "/delete"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.GET) {
				event.query().parse();

				long id = event.big("id");
				Object key = event.session().get("key");

				if(key != null) {
					Article article = find(id);

					if(article.permit(key) == NO) {
						throw new Exception(Sprout.i18n("You are not authorized!"));
					}

					article.delete(WILDCARD);

					cache1.remove(new Long(id));
					cache2.clear();

					MAX--;
				}

				Sprout.redirect(event);
			}
		}
	}

	public static class Publish extends Service {
		public int index() { return 2; }
		public String path() { return "/publish"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse(MAX_POST_SIZE);

				String title = event.string("title");
				String body = event.string("body");
				Object key = event.session().get("key");

				if(title.length() > 0 && body.length() > 0 && key != null) {
					Article article = get(key);
					User user = User.get(key);

					if(article.permit(key) == NO) {
						throw new Exception(Sprout.i18n("You are not authorized!"));
					}

					if(article.getId() == 0) {
						MAX++;
					}

					Calendar date = Calendar.getInstance();
					
					int day = event.medium("day");
					int month = event.medium("month");
					int year = event.medium("year");

					if(day > 0 && month > 0 && year > 0) {
						date.set(Calendar.DAY_OF_MONTH, day);
						date.set(Calendar.MONTH, month - 1);
						date.set(Calendar.YEAR, year);
					}
					
					article.setDate(date.getTimeInMillis());
					article.add(ARTICLE_TITLE, title);
					article.add(ARTICLE_BODY, body);
					article.update();

					Article.invalidate(article);
					cache3.remove(key);
					Ping.call(article);

					Sprout.redirect(event, "/");
				}

				Sprout.redirect(event);
			}
		}
	}

	public static class Edit extends Service {
		public int index() { return 2; }
		public String path() { return "/edit"; }
		public void filter(Event event) throws Event, Exception {
			event.query().parse();

			long id = event.big("id");
			Object key = event.session().get("key");

			if(key != null) {
				Article article = (Article) Article.get(key);

				if(id > 0 && article.getId() != id) {
					article = find(id);
				}
				
				Calendar date = Calendar.getInstance();
				date.setTimeInMillis(article.getDate());
				
				event.query().put("day", date.get(Calendar.DAY_OF_MONTH));
				event.query().put("month", date.get(Calendar.MONTH) + 1);
				event.query().put("year", date.get(Calendar.YEAR));
				
				if(article.meta(ARTICLE_TITLE) != null) {
					event.query().put("title", article.meta(ARTICLE_TITLE).getString());
				}

				if(article.meta(ARTICLE_BODY) != null) {
					event.query().put("body", article.meta(ARTICLE_BODY).getString());
				}

				cache3.put(key, article);
				Sprout.redirect(event);
			}

			Sprout.redirect(event, "/");
		}
	}

	public static class Widget extends Service {
		public String path() { return "/widget"; }
		public void filter(Event event) throws Event, Exception {
			event.query().parse();

			long id = event.big("id");

			if(id > 0) {
				Article article = find(id);

				if(article != null) {
					StringBuffer buffer = new StringBuffer();
					article.print(buffer, 0);
					JSONObject json = XML.toJSONObject(buffer.toString());
					event.reply().type("application/javascript");
					Output out = event.output();
					String element = User.host.replace('.', '_') + "_" + article.getId();
					out.println("var article = " + json + ";");
					out.println("document.getElementById('" + element + "_head').innerHTML = article.item.head;");
					out.println("document.getElementById('" + element + "_body').innerHTML = article.item.body;");
				}
			}
		}
	}

	protected void print(StringBuffer buffer, int level) {
		padding(buffer, level);
		buffer.append("<item>\n");
		padding(buffer, level + 1);
		buffer.append("<id>" + getId() + "</id>\n");
		padding(buffer, level + 1);
		buffer.append("<head>" + Sprout.clean(safe(ARTICLE_TITLE)) + "</head>\n");
		padding(buffer, level + 1);
		buffer.append("<link>\n");
		padding(buffer, level + 2);
		buffer.append("<html>/article?id=" + getId() + "</html>\n");
		padding(buffer, level + 2);
		buffer.append("<json>/article.js?id=" + getId() + "</json>\n");
		padding(buffer, level + 1);
		buffer.append("</link>\n");
		padding(buffer, level + 1);
		buffer.append("<body>" + Sprout.clean(safe(ARTICLE_BODY)) + "</body>\n");
		padding(buffer, level + 1);
		buffer.append("<date>" + getDate() + "</date>\n");

		LinkedList children = null;

		try {
			children = child(Node.ALL);
		}
		catch(SQLException e) {
			e.printStackTrace();
		}

		Iterator it = children.iterator();

		while(it.hasNext()) {
			Node child = (Node) it.next();

			switch(child.getType()) {
			case USER: {
				padding(buffer, level + 1);
				buffer.append("<user>" + child.safe(USER_NAME) + "</user>\n");
			} break;
			case COMMENT: {
				Data state = child.meta(COMMENT_STATE);
				boolean show = (state == null ? false : state.getString().equals("SHOW"));
				if(show) {
					Data from = child.meta(COMMENT_IP);

					if(from == null) {
						try {
							from = ((Node) child.child(USER).getFirst()).meta(USER_NAME);
						}
						catch(SQLException e) {
							e.printStackTrace();
						}
					}

					padding(buffer, level + 1);
					buffer.append("<post>\n");
					padding(buffer, level + 2);
					buffer.append("<body>" + child.safe(COMMENT_BODY) + "</body>\n");
					padding(buffer, level + 2);
					buffer.append("<from>" + from.getString() + "</from>\n");
					padding(buffer, level + 2);
					buffer.append("<date>" + child.getDate() + "</date>\n");
					padding(buffer, level + 1);
					buffer.append("</post>\n");
				}
			} break;
			case FILE: {
				padding(buffer, level + 1);
				buffer.append("<file>\n");
				padding(buffer, level + 2);
				buffer.append("<type>" + child.safe(FILE_TYPE) + "</type>\n");
				padding(buffer, level + 2);
				buffer.append("<path>file" + child.path() + "/" + Sprout.clean(child.safe(FILE_NAME)) + "</path>\n");
				padding(buffer, level + 1);
				buffer.append("</file>\n");
			} break;
			case PING: {
				Data state = child.meta(PING_STATE);
				boolean show = (state == null ? false : state.getString().equals("SHOW"));
				if(show) {
					padding(buffer, level + 1);
					buffer.append("<ping>" + Sprout.clean(child.safe(PING_URL)) + "</ping>\n");
				}
			} break;
			}
		}

		padding(buffer, level);
		buffer.append("</item>\n");
	}

	public class Label extends Node {
		public Label() {
			super(LABEL);
		}
	}
}
