package se.rupy.content;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import se.rupy.http.Event;
import se.rupy.http.Query;
import se.rupy.http.Service;
import se.rupy.memory.Base;
import se.rupy.memory.NodeBean;
import se.rupy.sprout.Node;
import se.rupy.sprout.Sprout;
import se.rupy.sprout.User;
import se.rupy.sprout.Sprout.Cache;

public class Article extends Node {
	public static int MAX_POST_SIZE = 1024 * 1024; // 1MB
	public static HashMap cache = new HashMap();
	public static FontMetrics metric = Toolkit.getDefaultToolkit().getFontMetrics(new Font("Sans", Font.PLAIN, 8));
	public LinkedList columns;

	public Article() {
		super(ARTICLE);
	}

	protected static void invalidate(String type, Article article) {
		Cache old = (Cache) cache.get(type);
		
		if(old != null) {
			old.invalid = true;
		}
		
		article.invalidate();
		
		cache.put(new Long(article.getId()), article);
	}
	
	public static Article find(long id) throws SQLException {
		if(id == 0) {
			return null;
		}
		
		Article old = (Article) cache.get(new Long(id));

		if(old == null) {
			old = new Article();
			old.setId(id);

			Sprout.update(Base.SELECT, old);

			old.fill(10, 0, 10);
			
			cache.put(new Long(id), old);
		}
		
		return old;
	}
	
	public static Cache get(String type) throws SQLException {
		Cache old = (Cache) cache.get(type);
				
		if(old == null) {
			old = new Cache(true);
		}
		
		if(old.invalid) {
			old = new Cache(false);
			old.setType(ARTICLE | USER);
			old.setParent(-1);
			Sprout.update(Base.SELECT, old);
			
			int index = 0;
			Iterator it = old.iterator();
			
			while(it.hasNext()) {
				NodeBean node = (NodeBean) it.next();
				Node article = new Article();
				
				article.copy(node);
				article.fill(10, 0, 10);

				cache.put(new Long(article.getId()), article);
				
				Node user = (Node) article.get(USER).getFirst();
				user.meta();
				
				old.set(index++, article);
			}
			
			cache.put(type, old);
		}
		
		return old;
	}
	
	public static Object remove(Object key) {
		return cache.remove(key);
	}

	public static Article get(Object key) {
		Article article = (Article) Article.cache.get(key);

		if(article == null) {
			article = new Article();
			Article.cache.put(key, article);
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
			String column = "", text = get(ARTICLE_BODY).getValue();
			int length = 0, begin = 0, end = COLUMN_WIDTH / CHARACTER_WIDTH;

			// loop until all overflowing data has been columnized
			while(end < text.length() - COLUMN_WIDTH / CHARACTER_WIDTH) {

				// loop until the column is full
				while(length < COLUMN_WIDTH) {
					end = text.indexOf(" ", end) + 1;
					column = text.substring(begin, end);
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

	public static class Publish extends Service {
		public int index() { return 2; }
		public String path() { return "/article/edit"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse(MAX_POST_SIZE);

				String title = event.string("title");
				String body = event.string("body");
				Object key = event.session().get("key");

				if(title.length() > 0 && body.length() > 0 && key != null) {
					Article article = get(key);
					User user = User.get(key);

					if(article == null) {
						article = new Article();
						Article.cache.put(key, article);
					}

					article.add(ARTICLE_TITLE, title);
					article.add(ARTICLE_BODY, body);
					article.add(user);
					article.update();

					Article.invalidate("article", article);
					Article.cache.put(key, new Article());

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
					article.setId(id);
					Sprout.update(Base.SELECT, article); // select date
					article.fill(10, 0, 10);
				}

				if(article.get(ARTICLE_TITLE) != null) {
					event.query().put("title", article.get(ARTICLE_TITLE).getValue());
				}

				if(article.get(ARTICLE_BODY) != null) {
					event.query().put("body", article.get(ARTICLE_BODY).getValue());
				}

				Article.cache.put(key, article);

				Sprout.redirect(event);
			}

			Sprout.redirect(event, "/");
		}
	}
	
	public class Category extends Node {
		public Category() {
			super(CATEGORY);
		}
	}
}
