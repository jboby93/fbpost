package fbpost;

public class GetReactsAction extends FBAction {
	public GetReactsAction() { super(FBAction.ACTION_GETREACTS); }

	public boolean validate() {
		if(this.authFile == null) return false;

		if(this.getParamStr("pageid") == null) return false;
		if(this.getParamStr("postid") == null) return false;

		return true;
	}

	// requires: 
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

		FBPage.FBPostReactions reacts = page.getReactionsToPost(pageid + "_" + postid);

		this.response.put("reacts", reacts);
		
		return 0;
	}
}