package se.rupy.sprout;

import se.rupy.http.Event;
import se.rupy.http.Query;

public class Comment extends Node {
	public Comment() {
		super(COMMENT);
	}
	
	public static class Post extends Sprout {
		public String path() { return "/article/comment/post"; }
		public void filter(Event event) throws Event, Exception {
			if(event.query().method() == Query.POST) {
				event.query().parse();
				
				String body = event.string("body");
				Object key = event.session().get("key");
				Article article = get(event.big("id"));
				
				if(article != null && body.length() > 0) {
					Comment comment = new Comment();
					User user = User.get(key);

					if(user != null) {
						comment.add(user);
					}

					comment.add(COMMENT_BODY, body);
					article.add(comment);
					article.update();
					
					Sprout.invalidate("article", article);
				}

				Sprout.redirect(event);
			}
		}
	}
}