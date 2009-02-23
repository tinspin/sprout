package se.rupy.content;

import se.rupy.http.Event;
import se.rupy.http.Query;
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
					
					comment.add(COMMENT_BODY, body);
					article.add(comment);
					article.update();
					
					Article.invalidate(article);
				}

				Sprout.redirect(event);
			}
		}
	}
}