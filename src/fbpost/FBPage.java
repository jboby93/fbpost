package fbpost;

/**
 * FBPage.java - connector to the bot's Facebook page; handles making posts and retrieving post info
 * v2.0 - 2-15-2018
 */

import java.awt.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

import facebook4j.*; 
import facebook4j.auth.AccessToken;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class FBPage {
	private Facebook fb;
	
	private String appID = "null";
	private String appSecret = "null";
	private String accessToken = "null";

	public boolean wasInitialized() { return (!this.appID.equals("null") && !this.appSecret.equals("null")); }

	private String pageAccessToken = null; 

	private String authFile = "auth/FBPageConfig.ini";

	private AccessToken access;
	
	//9-20: make it easier to test the entire program from dev machine without posting to facebook
	//if this is true, posts are sent to stdout rather than the bot's page
	private boolean _debug = false;
	public void setDebugMode(boolean value) { 
		_debug = value;
		if(_debug) {
			App.log("[FBPage instance is in debug mode -- nothing will be posted to Facebook]");
		}
	}

	public FBPage() {
		App.log("FBPage(): initializing");
		fb = new FacebookFactory().getInstance();

		App.log("FBPage(): no auth file given to constructor! must reloadAuthFrom() before using");
	}
	public FBPage(String authFile) {
		//http://facebook4j.org/en/code-examples.html
		
		App.log("FBPage(): initializing");
		fb = new FacebookFactory().getInstance();
		//AccessToken page = new AccessToken(pageAccessToken);
		
		boolean needsExtended = false;

		//9-8: load app ID, app secret, and access token from FBPageConfig.ini
		try {
			String config[] = App.readFile(authFile).split("\n");
			for(String s : config) {
				//handle each line of configuration file
				// (don't forget to trim() because java is a bitch ass nigga)
				String key = s.split("=")[0].trim();
				String value = s.replace(key + "=", "").trim(); //s.split("=")[1];

				switch(key) {
				case "AppID":
					appID = value;
					break;
				case "AppSecret":
					appSecret = value;
					break;
				case "AccessToken":
					accessToken = value;
					break;
				case "NeedsExtended":
					needsExtended = value.equals("true");
					break;
				}
			}

			this.authFile = authFile;

			App.log("FBPage(): loaded Graph API settings from " + authFile + " successfully");
		} catch(IOException e) {
			//provide default values here? or disable the functionality entirely?
			//for now, log an error and use the default class-initializer values
			App.log("FBPage(): error reading from " + authFile);
			App.logStackTrace(e);
		}

		fb.setOAuthAppId(appID, appSecret);
		fb.setOAuthAccessToken(new AccessToken(accessToken, null));
		fb.setOAuthPermissions("manage_pages, publish_actions, publish_pages, read_insights");
		

		App.log("FBPage(): manually setting the page token seemed to work, let's hope it continues to work");
		pageAccessToken = accessToken;
		App.log("FBPage(): token set. maybe.");

		if(needsExtended)
			extendAccessToken();
	} //end FBPage()
	
	public void extendAccessToken() { extendAccessToken(true); }
	public void extendAccessToken(boolean resave) {
		App.log("extendAccessToken(): attempting to extend token expiration...");
		try {
			AccessToken extended = fb.extendTokenExpiration(accessToken);
			App.log("extendAccessToken(): user access token extended! - " + extended.getToken());
			if(pageAccessToken != null) {
				AccessToken page_extended = fb.extendTokenExpiration(pageAccessToken);
				App.log("extendAccessToken(): page access token extended! - " + page_extended.getToken());

				if(resave) {
					//update the FBPageConfig file and the relevant properties
					pageAccessToken = page_extended.getToken();

					String out = "AppID=" + appID + "\n" +
						"AppSecret=" + appSecret + "\n" +
						"AccessToken=" + pageAccessToken;
					try {
						App.writeFile(this.authFile, out);
						App.log("extendAccessToken(): wrote new " + this.authFile);
					} catch(IOException io_e) {
						App.log("extendAccessToken(): error writing new " + this.authFile);
						App.logStackTrace(io_e);
					}
				}
			}
		} catch (FacebookException e) {
			// TODO Auto-generated catch block
			App.logStackTrace(e);
			//return null;
		}
	}

	public void reloadAuthFrom(String authFile) {
		boolean needsExtended = false;
		boolean needsInit = (this.appID.equals("null") || this.appSecret.equals("null"));

		//9-8: load app ID, app secret, and access token from FBPageConfig.ini
		try {
			String config[] = App.readFile(authFile).split("\n");
			for(String s : config) {
				//handle each line of configuration file
				// (don't forget to trim() because java is a bitch ass nigga)
				String key = s.split("=")[0].trim();
				String value = s.replace(key + "=", "").trim(); //s.split("=")[1];

				switch(key) {
				case "AppID":
					appID = value;
					break;
				case "AppSecret":
					appSecret = value;
					break;
				case "AccessToken":
					accessToken = value;
					break;
				case "NeedsExtended":
					needsExtended = value.equals("true");
					break;
				}
			}

			this.authFile = authFile;

			App.log("reloadAuthFrom(): loaded Graph API settings from " + authFile + " successfully");
		} catch(IOException e) {
			//provide default values here? or disable the functionality entirely?
			//for now, log an error and use the default class-initializer values
			App.log("reloadAuthFrom(): error reading from " + authFile);
			App.logStackTrace(e);
		}

		if(needsInit)
			fb.setOAuthAppId(appID, appSecret);
		fb.setOAuthAccessToken(new AccessToken(accessToken, null));
		fb.setOAuthPermissions("manage_pages, publish_actions, publish_pages, read_insights");
		

		App.log("reloadAuthFrom(): manually setting the page token seemed to work, let's hope it continues to work");
		pageAccessToken = accessToken;
		App.log("reloadAuthFrom(): token set. maybe.");

		if(needsExtended)
			extendAccessToken();
	}
	
	//normal posting
	public String postToPage(String str) {
		String r = "null";
		
		String dev_header = null;
		//dev_header = "[development bot - " + App.name + " (" + App.version + ")]";
		//comment the above line out to disable the [dev bot] indicator on posts

		if(_debug) {
			App.log("FBPage.postToPage(): [debug]");
			App.println(str);

			r = "debug_post";
		} else {
			try {
				if(dev_header != null) str = dev_header + "\n" + str;

				PostUpdate post = new PostUpdate(str);
				if(pageAccessToken != null) {
					fb.setOAuthAccessToken(new AccessToken(pageAccessToken));
					String postID = fb.postFeed(post);
					r = postID.split("_")[1];
				}
			} catch(FacebookException e) {
				App.logStackTrace(e);
			}
		}
			
		
		return r;
	} //end postToPage()

	public String postImageToPage(String imagefilename) { return postImageToPage(imagefilename, null); }
	public String postImageToPage(String imagefilename, String str) {
		String r = "null";
		
		String dev_header = null;
		//dev_header = "[development bot - " + App.name + " (" + App.version + ")]";
		//comment the above line out to disable the [dev bot] indicator on posts

		if(_debug) {
			App.log("FBPage.postToPage(): [debug]");
			App.println(str);

			r = "debug_post";
		} else {
			try {
				if(dev_header != null) str = dev_header + "\n" + str;

				PhotoUpdate post = new PhotoUpdate(new Media(new File(imagefilename)));
				if(str != null) post.setMessage(str);

				if(pageAccessToken != null) {
					fb.setOAuthAccessToken(new AccessToken(pageAccessToken));
					String postID = fb.postPhoto(post);
					r = postID;
				}
			} catch(FacebookException e) {
				App.logStackTrace(e);
			}
		}
			
		
		return r;
	} //end postToPage()

	public String postComment(String pageID, String postID, String str) {
		String r = "null";
		if(_debug) {
			App.log("FBPage.postComment(): [debug]");
			App.println("Post ID: " + postID);
			App.println(str);

			r = "debug_comment";
		} else {
			try {
				CommentUpdate comment = new CommentUpdate();
				comment.setMessage(str);

				String commentID = fb.commentPost(pageID + "_" + postID, comment);
				r = commentID;
			} catch(FacebookException e) {
				App.logStackTrace(e);
			}
		}

		return r;
	}

	public String postComment(String postID, String str) {
		String r = "null";
		if(_debug) {
			App.log("FBPage.postComment(): [debug]");
			App.println("Post ID: " + postID);
			App.println(str);

			r = "debug_comment";
		} else {
			try {
				CommentUpdate comment = new CommentUpdate();
				comment.setMessage(str);

				String commentID = fb.commentPost(postID, comment);
				r = commentID;
			} catch(FacebookException e) {
				App.logStackTrace(e);
			}
		}

		return r;
	}

	public String postImageComment(String pageID, String postID, String imagefilename) { return postImageComment(pageID, postID, imagefilename, null); }
	public String postImageComment(String pageID, String postID, String imagefilename, String str) {
		String r = "null";

		if(_debug) {
			App.log("FBPage.postImageComment(): [debug]");
			App.println("Post ID: " + postID);
			App.println(str);

			r = "debug_comment";
		} else {
			try {
				CommentUpdate comment = new CommentUpdate();
				comment.setSource(new Media(new File(imagefilename)));

				if(str != null) comment.setMessage(str);

				String commentID = fb.commentPost(pageID + "_" + postID, comment);
				r = commentID;
			} catch(FacebookException e) {
				App.logStackTrace(e);
			}
		}

		return r;
	}

	//post transmission from developer
	private static final String DEV_HEADER = "begin message from botmin:";
	public String postNote(String str) {
		String start = App.getTimeStamp() + " " + DEV_HEADER + "\n\n";
		String end = "\n\n[end message]";
		
		return postToPage(start+str+end);
	} //end postNote()

	//post system message
	private static final String SYS_HEADER = "[system message]:";
	public String postSysMessage(String str) {
		String start = App.getTimeStamp() + " " + SYS_HEADER + " ";
		
		return postToPage(start+str);
	} //end postSysMessage()

	//experimental! looking through facebook4j.FacebookImpl.java:_comment(String, CommentUpdate), this should be possible
	// (edit: holy shit it works lmao)
	// just gonna make small functions to call existing ones
	public String postCommentReply(String commentID, String str) { return postComment(commentID, str); }
	public String postImageCommentReply(String commentID, String imagefilename) { return postImageComment(commentID, imagefilename, null); }
	public String postImageCommentReply(String commentID, String imagefilename, String str) { return postImageComment(commentID, imagefilename, str); }

	public String getRandomImage() {
		File mfwDirectory = new File("bot-imgs");
		File mfws[] = mfwDirectory.listFiles();

		int r = App.rand(mfws.length - 1);
		return mfws[r].getPath();
	}

	// url sharing - returns postid or list of post ids
	public String shareToPage(String url, String toPage, String auth) {
		return shareToPages(
			null, url, 
			new ArrayList<String>(Arrays.asList(new String[] {toPage})), 
			new ArrayList<String>(Arrays.asList(new String[] {auth}))
		).get(0);
	}

	public String shareToPage(String str, String url, String toPage, String auth) {
		return shareToPages(
			str, url, 
			new ArrayList<String>(Arrays.asList(new String[] {toPage})), 
			new ArrayList<String>(Arrays.asList(new String[] {auth}))
		).get(0);
	}

	public ArrayList<String> shareToPages(String url, ArrayList<String> toPages, ArrayList<String> auths) { return shareToPages(null, url, toPages, auths); }
	public ArrayList<String> shareToPages(String str, String url, ArrayList<String> toPages, ArrayList<String> auths) {
		ArrayList<String> ids = new ArrayList<String>();
		String currentAuth = null;
		for(int i = 0; i < toPages.size(); i++) {
			try {
				PostUpdate post = new PostUpdate(new URL(url));

				if(str != null) post.setMessage(str);

				if(i < auths.size())
					currentAuth = auths.get(i);

				reloadAuthFrom(currentAuth);

				String pid = fb.postFeed(post).split("_")[1];

				ids.add(pid);
			} catch(MalformedURLException eurl) {
				App.logStackTrace(eurl);
				ids.add("null");
			} catch(FacebookException e) {
				App.logStackTrace(e);
				ids.add("null");
			}
		}

		return ids;
	}

	// ==================================================================================================
	// 
	// ==================================================================================================
	// 
	class FBPost implements Comparable<FBPost> {
		private String id; public String getId() { return id; }
		private FBPostReactions reacts; public FBPostReactions getReacts() {
			if(reacts == null)
				fetchReacts();
			return reacts;
		}

		public String getReactSummary() {
			String summary = reacts.likes + " likes, " + reacts.loves + " loves, " + reacts.wows + " wows, " + reacts.hahas + " hahas, " + reacts.sads + " sads, " + reacts.angerys + " angerys";
			
			if(reacts.thankfuls > 0)
				summary += ", " + reacts.thankfuls + " thankfuls";
			if(reacts.prides > 0)
				summary += ", " + reacts.prides + " prides";
			if(reacts.unknown > 0)
				summary += ", " + reacts.unknown + " unknown (null reaction type)";
			
			return summary;
		}

		private String text = null;
		private long datestamp = -1;

		public FBPost(String _id, FBPostReactions _reacts) {
			this.id = _id; this.reacts = _reacts;
		}

		public FBPost(String _id) {
			this.id = _id; this.reacts = null;
		}

		public String getText() {
			if(text == null) {
				//get the post text from facebook
				try {
					text = fb.getPost(id).getMessage();
				} catch(FacebookException e) {
					text = null;
					App.log("FBPost.getText(): error getting content of post " + id);
					App.logStackTrace(e);
				}
			}

			return text;
		}

		public String getDateString() {
			if(datestamp == -1) {
				//get date from facebook
				try {
					datestamp = fb.getPost(id).getCreatedTime().getTime();
				} catch(FacebookException e) {
					datestamp = -1;
					App.log("FBPost.getDateString(): error getting creation time of post " + id);
					App.logStackTrace(e);
				}
			}

			return (new Date(datestamp)).toString();
		}

		//compareTo() will allow us to easily sort these objects by react counts
		public int compareTo(FBPost o) {
			if(reacts.getTotal() <  o.getReacts().getTotal()) return -1;
			if(reacts.getTotal() == o.getReacts().getTotal()) return 0;
			
			//if not less than and not equal to, must be greater than
			return 1;
		}

		private void fetchReacts() {
			this.reacts = new FBPostReactions();

			try {
				Post p = fb.getPost(this.id);

				ResponseList<Reaction> reactions = fb.getPostReactions(this.id);
				Paging<Reaction> paging;
				do {
					for(Reaction r : reactions) {
						if(r.getType() == null) {
							this.reacts.unknown++;
						} else {
							switch(r.getType()) {
								case LIKE: this.reacts.likes++; break;
								case LOVE: this.reacts.loves++; break;
								case WOW: this.reacts.wows++; break;
								case HAHA: this.reacts.hahas++; break;
								case SAD: this.reacts.sads++; break;
								case ANGRY: this.reacts.angerys++; break;
								case THANKFUL: this.reacts.thankfuls++; break;
								case PRIDE: this.reacts.prides++; break;
							}
						}
					}

					paging = reactions.getPaging();
				} while((paging != null) && ((reactions = fb.fetchNext(paging)) != null));
			} catch(FacebookException e) {
				App.log("fetchReacts(): error getting reactions on post " + this.id);
				App.logStackTrace(e);
				this.reacts = null;
			}
		}

		public String toString() {
			String s = "post_id " + this.id + "\n";
			s += "date " + this.getDateString() + "\n";
			s += "reacts " + this.getReactSummary() + "\n";
			s += this.getText() + "\n";

			return s;
		}

		public JSONObject asJSON() {
			JSONObject obj = new JSONObject();
			obj.put("id", id);
			obj.put("text", getText());
			obj.put("date", getDateString());
			obj.put("reacts", getReacts().asJSON());

			return obj;
		}
	} //end class FBPost

	class FBComment implements Comparable<FBComment> {
		private String postid; public String getPostId() { return postid; }

		private String id; public String getId() { return id; }
		private FBPostReactions reacts; public FBPostReactions getReacts() {
			if(reacts == null)
				fetchReacts();
			return reacts;
		}

		public String getReactSummary() {
			String summary = reacts.likes + " likes, " + reacts.loves + " loves, " + reacts.wows + " wows, " + reacts.hahas + " hahas, " + reacts.sads + " sads, " + reacts.angerys + " angerys";
			
			if(reacts.thankfuls > 0)
				summary += ", " + reacts.thankfuls + " thankfuls";
			if(reacts.prides > 0)
				summary += ", " + reacts.prides + " prides";
			if(reacts.unknown > 0)
				summary += ", " + reacts.unknown + " unknown (null reaction type)";
			
			return summary;
		}

		private String text = null;
		private long datestamp = -1;

		public FBComment(String _postid, String _id, FBPostReactions _reacts) {
			this.postid = _postid; this.id = _id; this.reacts = _reacts;
		}

		public FBComment(String _postid, String _id) {
			this.postid = _postid; this.id = _id; this.reacts = null;
		}

		public String getText() {
			if(text == null) {
				//get the post text from facebook
				try {
					text = fb.getPost(id).getMessage();
				} catch(FacebookException e) {
					text = null;
					App.log("FBComment.getText(): error getting content of post " + id);
					App.logStackTrace(e);
				}
			}

			return text;
		}

		public String getDateString() {
			if(datestamp == -1) {
				//get date from facebook
				try {
					datestamp = fb.getComment(id).getCreatedTime().getTime();
				} catch(FacebookException e) {
					datestamp = -1;
					App.log("FBComment.getDateString(): error getting creation time of comment " + id);
					App.logStackTrace(e);
				}
			}

			return (new Date(datestamp)).toString();
		}

		//compareTo() will allow us to easily sort these objects by react counts
		public int compareTo(FBComment o) {
			if(reacts.getTotal() <  o.getReacts().getTotal()) return -1;
			if(reacts.getTotal() == o.getReacts().getTotal()) return 0;
			
			//if not less than and not equal to, must be greater than
			return 1;
		}

		private void fetchReacts() {
			this.reacts = new FBPostReactions();

			try {
				//Post p = fb.getComment(this.id);

				ResponseList<Reaction> reactions = fb.getPostReactions(this.id);
				Paging<Reaction> paging;
				do {
					for(Reaction r : reactions) {
						if(r.getType() == null) {
							this.reacts.unknown++;
						} else {
							switch(r.getType()) {
								case LIKE: this.reacts.likes++; break;
								case LOVE: this.reacts.loves++; break;
								case WOW: this.reacts.wows++; break;
								case HAHA: this.reacts.hahas++; break;
								case SAD: this.reacts.sads++; break;
								case ANGRY: this.reacts.angerys++; break;
								case THANKFUL: this.reacts.thankfuls++; break;
								case PRIDE: this.reacts.prides++; break;
							}
						}
					}

					paging = reactions.getPaging();
				} while((paging != null) && ((reactions = fb.fetchNext(paging)) != null));
			} catch(FacebookException e) {
				App.log("fetchReacts(): error getting reactions on comment " + this.id);
				App.logStackTrace(e);
				this.reacts = null;
			}
		}

		public String toString() {
			String s = "comment_id " + this.id + "\n";
			s += "date " + this.getDateString() + "\n";
			s += "reacts " + this.getReactSummary() + "\n";
			s += this.getText() + "\n";

			return s;
		}

		public JSONObject asJSON() {
			JSONObject obj = new JSONObject();
			obj.put("id", id);
			obj.put("text", getText());
			obj.put("date", getDateString());
			obj.put("reacts", getReacts().asJSON());

			return obj;
		}
	}

	public ArrayList<FBPost> getTopPosts(String pageID, int count, Date startTime, Date endTime) {
		ArrayList<FBPost> posts = getAllPostsFrom(pageID, startTime, endTime);

		if(posts.size() > 0) {
			//sort by reaction count (see FBPost.compareTo())
			Collections.sort(posts);

			//now our posts list is sorted in ascending order of reaction counts, so our top posts are at the end of the list
			//so let's remove elements from the front until we have the correct number of top posts
			while(posts.size() > count)
				posts.remove(0);

			//finally, reverse the order of the remaining elements to get our return value
			Collections.reverse(posts);
		}

		return posts;
	} //end getTopPosts(date range) 

	public void sortPosts(ArrayList<FBPost> posts) { sortPosts(posts, posts.size()); }
	public void sortPosts(ArrayList<FBPost> posts, int count) {
		if(posts.size() > 0) {
			//sort by reaction count (see FBPost.compareTo())
			Collections.sort(posts);

			//now our posts list is sorted in ascending order of reaction counts, so our top posts are at the end of the list
			//so let's remove elements from the front until we have the correct number of top posts
			while(posts.size() > count)
				posts.remove(0);

			//finally, reverse the order of the remaining elements to get our return value
			Collections.reverse(posts);
		}
	}

	public FBPost getPost(String id) {
		try {
			Post p = fb.getPost(id);
			return new FBPost(p.getId(), getReactionsToPost(p.getId()));
		} catch(FacebookException e) {
			App.log("[getPost] error getting post with id " + id);
			App.logStackTrace(e);

			return null;
		}
	}

	public JSONObject getJsonForPosts(String pageID, Date start, Date end, ArrayList<FBPost> posts) {
		JSONObject obj = new JSONObject();
		JSONArray list = new JSONArray();
		for(FBPost post : posts) {
			list.add(post.asJSON());
		}

		obj.put("page", pageID);
		obj.put("start", start.toString());
		obj.put("end", end.toString());
		obj.put("posts", list);

		return obj;
	}

	public JSONObject getJsonForComments(String postID, ArrayList<FBComment> comments) {
		JSONObject obj = new JSONObject();
		JSONArray list = new JSONArray();

		for(FBComment comment : comments)
			list.add(comment.asJSON());

		obj.put("postid", postID);
		obj.put("comments", list);

		return obj;
	}

	public ArrayList<FBPost> getAllPostsFrom(String pageID, Date startTime, Date endTime) {
		ArrayList<FBPost> posts = new ArrayList<FBPost>();

		App.log("getAllPosts(): starting post fetching; this could take a while");

		App.log(" start time: " + startTime.toString());
		App.log("   end time: " + endTime.toString());

		try {
			ResponseList<Post> feed = fb.getPosts(pageID); //fb.getFeed(pageID);
			Paging<Post> paging;

			boolean done = false;
			do {
				for(Post p : feed) {
					//App.log("looking at post " + p.getId());
					//if this post was made earlier than the lookback limit, we are done
					done = p.getCreatedTime().before(startTime);
					if(done) {
						App.log(" -- found post " + p.getId() + " made before start of search range (at " + p.getCreatedTime().toString() + "), so we are done", 1);
						break;
					}

					//now, if the post was made after the end date of the range, no point in analyzing it
					if(p.getCreatedTime().after(endTime)) {
						App.log(" -- skipping post " + p.getId() + " made after search range (" + p.getCreatedTime().toString() + ")", 1);
						continue;
					}

					FBPost post = new FBPost(p.getId(), getReactionsToPost(p.getId()));

					//make sure this post isn't a developer or system message
					//(or a photo upload or something with no caption)
					String postText = post.getText();
					String postTextPreview;

					if(postText == null) 
						postTextPreview = "(null)";
					else
						postTextPreview = postText.substring(0, (postText.length() >= 20 ? 20 : postText.length() - 1));

					//if we make it here, add the post to our master list of all posts from this timeframe
					posts.add(post);
					App.log(" - added post " + p.getId() + " (" + p.getCreatedTime().toString() + ") to posts pool: " + postTextPreview + "...", 1);				
				}

				paging = feed.getPaging();
			} while((paging != null) && ((feed = fb.fetchNext(paging)) != null) && !done);

			//sort by reaction count (see FBPost.compareTo())
			//Collections.sort(posts);

			//now our posts list is sorted in ascending order of reaction counts, so our top posts are at the end of the list
			//so let's remove elements from the front until we have the correct number of top posts
			//while(posts.size() > count)
			//	posts.remove(0);

			//finally, reverse the order of the remaining elements to get our return value
			//Collections.reverse(posts);

			//done!
		} catch(FacebookException e) {
			App.log("getAllPosts(): error occurred reading posts from page id " + pageID + "");
			App.logStackTrace(e);

			return null;
		}

		return posts;
	} //end getAllPostsFrom(date range) 

	public ArrayList<String> getAllPostIDsFrom(String pageID, Date startTime, Date endTime) {
		ArrayList<String> posts = new ArrayList<String>();

		App.log("getAllPosts(): starting post fetching; this could take a while");

		App.log(" start time: " + startTime.toString());
		App.log("   end time: " + endTime.toString());

		try {
			ResponseList<Post> feed = fb.getPosts(pageID); //fb.getFeed(pageID);
			Paging<Post> paging;

			boolean done = false;
			do {
				for(Post p : feed) {
					//App.log("looking at post " + p.getId());
					//if this post was made earlier than the lookback limit, we are done
					done = p.getCreatedTime().before(startTime);
					if(done) {
						App.log(" -- found post " + p.getId() + " made before start of search range (at " + p.getCreatedTime().toString() + "), so we are done", 1);
						break;
					}

					//now, if the post was made after the end date of the range, no point in analyzing it
					if(p.getCreatedTime().after(endTime)) {
						App.log(" -- skipping post " + p.getId() + " made after search range (" + p.getCreatedTime().toString() + ")", 1);
						continue;
					}

					FBPost post = new FBPost(p.getId(), getReactionsToPost(p.getId()));

					String postText = post.getText();
					String postTextPreview;

					if(postText == null) 
						postTextPreview = "(null)";
					else
						postTextPreview = postText.substring(0, (postText.length() >= 20 ? 20 : postText.length() - 1));
					
					//if we make it here, add the post to our master list of all posts from this timeframe
					posts.add(p.getId());
					App.log(" - added post " + p.getId() + " (" + p.getCreatedTime().toString() + ") to posts pool: " + postTextPreview + "...", 1);				
				}

				paging = feed.getPaging();
			} while((paging != null) && ((feed = fb.fetchNext(paging)) != null) && !done);

			//sort by reaction count (see FBPost.compareTo())
			//Collections.sort(posts);

			//now our posts list is sorted in ascending order of reaction counts, so our top posts are at the end of the list
			//so let's remove elements from the front until we have the correct number of top posts
			//while(posts.size() > count)
			//	posts.remove(0);

			//finally, reverse the order of the remaining elements to get our return value
			//Collections.reverse(posts);

			//done!
		} catch(FacebookException e) {
			App.log("getAllPosts(): error occurred reading posts from page id " + pageID + "");
			App.logStackTrace(e);

			return null;
		}

		return posts;
	} //end getAllPostsFrom(date range, return only post IDs) 

	public String convertPostIdToUrl(String postID) {
		return "https://www.facebook.com/" + postID.split("_")[0] + "/posts/" + postID.split("_")[1];
	}

	public String formatTopPostsListing(ArrayList<FBPost> posts, String fromDate, String toDate, boolean includePostText, boolean makeHtmlLinks) {
		return formatTopPostsListing(posts, fromDate, toDate, includePostText, makeHtmlLinks, null);
	}

	public String formatTopPostsListing(ArrayList<FBPost> posts, String fromDate, String toDate, boolean includePostText, boolean htmlFormat, String preheader) {
		//if we want HTML results, just let that function handle it instead of trying to cram two formatting logics into one function
		if(htmlFormat) return formatTopPostsListingForHTML(posts, fromDate, toDate, includePostText, preheader);

		String topPostOutput = "";
		if(preheader != null)
			topPostOutput = preheader + "\n\n";

		//topPostOutput += "Top " + posts.size() + " reacted-to posts from " + fromDate + " to " + toDate + ":\n";

		int tp_index = 1;

		String indent = "   "; //3 spaces

		for(FBPost p : posts) {
			topPostOutput += tp_index + ") Post URL: " + convertPostIdToUrl(p.getId()) + "\n";
			
			topPostOutput += indent + p.getReacts().getTotal() + " total reactions\n";
			topPostOutput += indent + "(" + p.getReactSummary() + ")\n";
			topPostOutput += indent + "Posted on " + p.getDateString() + "\n";
			if(includePostText) topPostOutput += indent + p.getText() + "\n";
			topPostOutput += "\n";

			tp_index++;
		}

		return topPostOutput;
	} //end formatTopPostsListing()

	//same as above, but returns a nice HTML-formatted listing
	private String formatTopPostsListingForHTML(ArrayList<FBPost> posts, String fromDate, String toDate, boolean includePostText, String preheader) {
		String topPostOutput = "<!-- begin auto-generated html, courtesy of " + App.name + " v" + App.version + "-->\n";
		topPostOutput += "<!-- report generated on " + (new Date()).toString() + " -->\n";

		if(preheader != null)
			topPostOutput += preheader + "<br /><br />\n";

		//topPostOutput += "Top " + posts.size() + " reacted-to posts from <i>" + fromDate + "</i> to <i>" + toDate + "</i>:<br />\n";

		//int tp_index = 1;

		String url_tmp = "";
		String indent = "	"; //tab for prettier html output

		//todo: additional formatting support for html
		// post info should be smaller text
		// "Top x ..." line should be a header, underlined, or something
		// possibly just wrap relevant things in <div>s; use <?php include> to embed result file on the website and use existing CSS formatting on it
		//   - this would allow changing the appearance of the results without modifying this code ever again

		topPostOutput += "<ol>\n";

		for(FBPost p : posts) {
			topPostOutput += indent + "<li>\n";

			//add link to post
			url_tmp = convertPostIdToUrl(p.getId());
			topPostOutput += indent + indent + "Post URL: <a href=\"" + url_tmp + "\" target=\"_blank\">" + url_tmp + "</a><br />\n";

			//reactions and date posted
			topPostOutput += indent + indent + "<font style=\"font-size: 12; font-style: italic; \">\n";
			topPostOutput += indent + indent + p.getReacts().getTotal() + " total reactions (" + p.getReactSummary() + ")<br />\n";
			topPostOutput += indent + indent + "Posted on " + p.getDateString() + "<br />\n";
			topPostOutput += indent + indent + "</font><br />\n";

			//post content, if included
			if(includePostText) topPostOutput += indent + indent + p.getText() + "<br /><br />\n";

			topPostOutput += indent + "</li>\n";
			//tp_index++;
		}

		topPostOutput += "</ol><br />\n";
		topPostOutput += "<!-- end auto-generated html -->\n";
		return topPostOutput;
	}

	class FBPostReactions {
		int likes = 0;
		int loves = 0;
		int hahas = 0;
		int wows = 0;
		int sads = 0;
		int angerys = 0;

		int thankfuls = 0;
		int prides = 0;

		int unknown = 0;

		int getTotal() {
			return likes + loves + wows + hahas + sads + angerys + thankfuls + prides + unknown;
		}

		public FBPostReactions() {}

		//string that can be parsed easily by other code
		public String toTermString() {
			String out = "";

			out += "LIKES " + likes + "\n";
			out += "LOVES " + loves + "\n";
			out += "HAHAS " + hahas + "\n";
			out += "WOWS " + wows + "\n";
			out += "SADS " + sads + "\n";
			out += "ANGERYS " + angerys + "\n";

			out += "THANKFULS " + thankfuls + "\n";
			out += "PRIDES " + prides + "\n";

			out += "UNKNOWN " + unknown;

			return out;
		}

		public JSONObject asJSON() {
			JSONObject obj = new JSONObject();
			obj.put("LIKES", new Integer(likes));
			obj.put("LOVES", new Integer(loves));
			obj.put("HAHAS", new Integer(hahas));
			obj.put("WOWS", new Integer(wows));
			obj.put("SADS", new Integer(sads));
			obj.put("ANGERYS", new Integer(angerys));

			obj.put("THANKFULS", new Integer(thankfuls));
			obj.put("PRIDES", new Integer(prides));

			obj.put("UNKNOWN", new Integer(unknown));

			obj.put("TOTAL", new Integer(getTotal()));

			return obj;
		}
	}

	public FBPostReactions getReactionsToPost(String postID) {
		FBPostReactions reacts = new FBPostReactions();

		try {
			Post p = fb.getPost(postID);

			ResponseList<Reaction> reactions = fb.getPostReactions(postID);
			Paging<Reaction> paging;
			do {
				for(Reaction r : reactions) {
					if(r.getType() == null) {
						reacts.unknown++;
					} else {
						switch(r.getType()) {
							case LIKE: reacts.likes++; break;
							case LOVE: reacts.loves++; break;
							case WOW: reacts.wows++; break;
							case HAHA: reacts.hahas++; break;
							case SAD: reacts.sads++; break;
							case ANGRY: reacts.angerys++; break;
							case THANKFUL: reacts.thankfuls++; break;
							case PRIDE: reacts.prides++; break;
						}
					}
				}

				paging = reactions.getPaging();
			} while((paging != null) && ((reactions = fb.fetchNext(paging)) != null));
		} catch(FacebookException e) {
			App.log("getReactionsToPost(): error getting reactions on post " + postID);
			App.logStackTrace(e);
		}

		return reacts;
	} //end getReactionsToPost()

	// =========================================================================================================
	// 
	// =========================================================================================================
	// 
	//unused in tpb98
	//http://stackoverflow.com/questions/24696250/take-comments-from-a-post-with-facebook4j
	public ArrayList<FBComment> getCommentsFromPost(String postID) {
		ArrayList<FBComment> fullComments = new ArrayList<FBComment>();

		try {
			Post p = fb.getPost(postID);
			App.log("FBPage.readComments(): post ID " + postID + " has message " + p.getMessage());
			//PagableList<Comment> comments = p.getComments();
			ResponseList<Comment> comments = fb.getPostComments(postID);
			//App.log("FBPage.readComments(): comments.getCount() = " + comments.getCount());
			Paging<Comment> paging;
			do {
				for(Comment c : comments) {
					String id = c.getId();

					String text = c.getMessage();
					FBPostReactions reacts = getReactionsToPost(id);

					fullComments.add(new FBComment(postID, id, reacts));
				}
				
				paging = comments.getPaging();
			} while ((paging != null) && ((comments = fb.fetchNext(paging)) != null));
		} catch(FacebookException e) {
			App.log("readComments(): error occurred getting comments for post " + postID);
			App.logStackTrace(e);
		}
		
		App.log("FBPage.readComments(): got " + fullComments.size() + " comments from post " + postID);
		return fullComments;
	} //end readComments()
}
