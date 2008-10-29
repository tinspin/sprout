package se.rupy.sprout;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.LinkedList;

import se.rupy.http.Event;
import se.rupy.http.Query;
import se.rupy.http.Service;
import se.rupy.memory.Base;

public class Article extends Node {
	public static int MAX_POST_SIZE = 1024 * 1024; // 1MB
	public static HashMap cache = new HashMap();
	public static FontMetrics metric = Toolkit.getDefaultToolkit().getFontMetrics(new Font("Sans", Font.PLAIN, 8));
	public LinkedList columns;

	public Article() {
		super(ARTICLE);
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

					Sprout.invalidate("article", article);

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
				Article article = (Article) Article.cache.get(key);

				if(article == null) {
					article = new Article();
				}

				if(article.getId() != id) {
					article.setId(id);
					Sprout.update(Base.SELECT, article); // select date
					article.meta();
					article.link(ALL);
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
}
