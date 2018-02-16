package fbpost;

public class PostAction extends FBAction {

	private boolean isImagePost = false;

	public PostAction() {
		super(FBAction.ACTION_POST);
		//this.isImagePost = hasImage;

		//this.setParam("text", str);
	}

	public boolean validate() {
		// needs minimum of text
		if(this.authFile == null) return false;
		return (this.getText() != null);
	}

	public int execute(FBPage page) {
		if(this.authFile == null)
			return 1;

		page.reloadAuthFrom(this.authFile);

		this.isImagePost = this.params.containsKey("image");

		String res = null;
		if(this.isImagePost) {
			// image post
			String imagefile = (String)this.params.get("image");

			// text is optional
			String mesg = this.getText();
			res = page.postImageToPage(imagefile, mesg);
		} else {
			// regular text post
			String text = this.getText();
			if(text == null)
				return 2;

			res = page.postToPage(text);
		}
		
		// return the response
		this.response.put("postid", res);
		
		return 0;
	}
}