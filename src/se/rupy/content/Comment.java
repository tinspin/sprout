package se.rupy.content;

import se.rupy.http.Event;
import se.rupy.http.Query;
import se.rupy.sprout.Data;
import se.rupy.sprout.Node;
import se.rupy.sprout.Sprout;
import se.rupy.sprout.User;

public class Comment extends Node {
	public Comment() {
		super(COMMENT);
	}
	
	public static class Post extends Sprout {
		public String path() { return "/comment"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse();
				
				String body = event.string("body");
				Object key = event.session().get("key");
				Article article = Article.find(event.big("id"));
				
				if(article != null && body.length() > 0) {
					Comment comment = new Comment();
					User user = User.get(key);

					if(user == null) {
						comment.add(COMMENT_IP, event.remote());
					}
					else {
						comment.add(user);
					}
					
					comment.add(Data.cache(COMMENT, "SHOW"));
					comment.add(COMMENT_BODY, body);
					article.add(comment);
					article.update();
					
					Article.invalidate(article);
				}

				Sprout.redirect(event);
			}
		}
	}
	
	public static class ShowHide extends Sprout {
		static {
			Data show = new Data(COMMENT_STATE, "SHOW");
			Data hide = new Data(COMMENT_STATE, "HIDE");
			Data report = new Data(COMMENT_STATE, "REPORT");
			
			Data.cache(COMMENT, show);
			Data.cache(COMMENT, hide);
			Data.cache(COMMENT, report);
		}
		
		public String path() { return "/show:/hide"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.GET) {
				event.query().parse();
				
				long cid = event.big("comment");
				Object key = event.session().get("key");
				Article article = Article.find(event.big("id"));
				
				if(article != null && key != null) {
					if(article.permit(key) == Article.NO) {
						throw new Exception(Sprout.i18n("You are not authorized!"));
					}
					
					Node comment = article.child(cid);
					
					if(event.query().path().equals("/hide")) {
						comment.add(Data.cache(COMMENT, "HIDE"));
					}
					else {
						comment.add(Data.cache(COMMENT, "SHOW"));
					}

					//comment.update();
					
					Article.invalidate(article);
				}

				Sprout.redirect(event);
			}
		}
	}
}