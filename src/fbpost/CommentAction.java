package fbpost;

public class CommentAction extends FBAction {
	private boolean isImageComment = false;

	public CommentAction() {
		super(FBAction.ACTION_COMMENT);
		//this.isImageComment = hasImage;
		
		//this.setParam("text", str);
	}

	public boolean validate() {
		if(this.authFile == null) return false;

		if(this.getParamStr("pageid") == null) return false;
		if(this.getParamStr("postid") == null) return false;

		return (this.getText() != null || this.params.containsKey("image"));
	}

	public int execute(FBPage page) {
		if(this.authFile == null)
			return 1;
		if(!this.params.containsKey("pageid"))
			return 4;
		if(!this.params.containsKey("postid"))
			return 5;

		String pageid = (String)this.params.get("pageid");
		String postid = (String)this.params.get("postid");

		this.isImageComment = this.params.containsKey("image");

		page.reloadAuthFrom(this.authFile);

		String res = null;
		if(this.isImageComment) {
			// image post
			String imagefile = (String)this.params.get("image");

			// text is optional
			String mesg = this.getText();
			res = page.postImageComment(pageid, postid, imagefile, mesg);
		} else {
			// regular text post
			String text = this.getText();
			if(text == null)
				return 2;

			res = page.postComment(pageid, postid, text);
		}
		
		// return the response
		this.response.put("commentid", res);
		
		return 0;
	}
}