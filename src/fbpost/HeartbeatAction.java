package fbpost;

import java.util.ArrayList;
import java.util.Date;

public class HeartbeatAction extends FBAction {
	public HeartbeatAction() { super(FBAction.ACTION_HEARTBEAT); }

	public boolean validate() {
		return (this.authFile != null);
	}

	public int execute(FBPage page) {
		page.reloadAuthFrom(this.authFile);

		Date currentTime = new Date();
		Date startTime = new Date(currentTime.getTime() - (24 * 60 * 60 * 1000));
							
		ArrayList<String> hbresults = page.getAllPostIDsFrom(this.getParamStr("pageid"), startTime, currentTime);

		this.response.put("results", hbresults);
		
		return 0;
	}
}