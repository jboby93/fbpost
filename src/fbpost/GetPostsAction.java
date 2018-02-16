package fbpost;

import java.util.Date;
import java.util.ArrayList;

public class GetPostsAction extends FBAction {
	public GetPostsAction() { super(FBAction.ACTION_GETPOSTS); }

	public boolean validate() {
		if(this.authFile == null) return false;

		if(this.getParamStr("pageid") == null) return false;
		if(this.getParam("starttime") == null) return false;
		if(this.getParam("endtime") == null) return false;
		if(this.getParamStr("format") == null) return false;

		return true;
	}

	public int execute(FBPage page) {
		if(this.authFile == null)
			return 1;
		if(!this.params.containsKey("pageid"))
			return 4;

		page.reloadAuthFrom(this.authFile);
		
		String pageid = this.getParamStr("pageid");
		Date starttime = (Date)this.params.get("starttime");
		Date endtime = (Date)this.params.get("endtime");

		String format = this.getParamStr("format");
		String saveas = this.getParamStr("saveas");
		boolean sortPosts = (this.params.containsKey("sort") ? (Boolean)this.getParam("sort") : false);

		ArrayList<FBPage.FBPost> posts = page.getAllPostsFrom(pageid, starttime, endtime);
		if(sortPosts) page.sortPosts(posts);

		String report_out = null;
		if(format.equalsIgnoreCase("json")) {
			report_out = page.getJsonForPosts(pageid, starttime, endtime, posts).toString();
		} else {
			report_out = page.formatTopPostsListing(posts, starttime.toString(), endtime.toString(), true, format.equalsIgnoreCase("html"), "Page ID: " + pageid);
		}

		this.response.put("output", report_out);
		
		if(saveas != null) {
			try {
				App.writeFile(saveas, report_out);
				App.log("[getposts] wrote report to file " + saveas);
			} catch(Exception e) {
				App.log("[getposts] couldn't write report to file " + saveas);
				App.logStackTrace(e);
			}
		}

		return 0;
	}
}