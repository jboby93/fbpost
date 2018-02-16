package fbpost;

import java.util.ArrayList;

public class GetCommentsAction extends FBAction {
	public GetCommentsAction() { super(FBAction.ACTION_GETCOMMENTS); }

	public boolean validate() {
		if(this.authFile == null) return false;

		if(this.getParamStr("pageid") == null) return false;
		if(this.getParamStr("postid") == null) return false;

		return true;
	}

	public int execute(FBPage page) {
		if(this.authFile == null)
			return 1;
		if(!this.params.containsKey("pageid"))
			return 4;
		if(!this.params.containsKey("postid"))
			return 5;

		page.reloadAuthFrom(this.authFile);
		
		String pageid = (String)this.params.get("pageid");
		String postid = (String)this.params.get("postid");

		ArrayList<FBPage.FBComment> comments = page.getCommentsFromPost(pageid + "_" + postid);
		this.response.put("comments", comments);

		return 0;
	}
}