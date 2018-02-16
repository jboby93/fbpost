package fbpost;

import java.util.ArrayList;

public class ShareAction extends FBAction {
	public ShareAction() { super(FBAction.ACTION_SHARE); }

	public boolean validate() {
		if(this.getParamStr("url") == null) return false;

		// should have lists of to and as
		try {
			ArrayList<String> to = (ArrayList<String>)this.getParam("to");
			ArrayList<String> as = (ArrayList<String>)this.getParam("as");

			return (to.size() > 0 && as.size() > 0);
		} catch(Exception e) {
		 	App.logStackTrace(e);
		 	return false;
		}
	}

	// requires: url, to (page ids), as (auth files)
	// optional: text (or text from --file)
	// 
	// if more page ids than auth files are provided, use the last loaded auth file for the remaining shares
	public int execute(FBPage page) {
		// ignore default authfile var
		String url = this.getParamStr("url");
		if(url == null) return 1;

		ArrayList<String> to = null;
		if(this.getParam("to") == null)
			return 2;
		else
			to = (ArrayList<String>)this.getParam("to");

		ArrayList<String> as = null;
		if(this.getParam("as") == null)
			return 3;
		else
			as = (ArrayList<String>)this.getParam("as");

		String text = this.getText();

		ArrayList<String> postids = page.shareToPages(text, url, to, as);
		this.response.put("postids", postids);

		return 0;
	}
}