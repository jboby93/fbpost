package fbpost;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

// extendable class for implementing facebook page actions
public abstract class FBAction {
	protected int type;
	public int getType() { return this.type; }

	protected Map<String, Object> params;
	public Object getParam(String key) { return (this.params.containsKey(key) ? this.params.get(key) : null); }
	public void setParam(String key, Object value) { this.params.put(key, value); }
	public String getParamStr(String key) { return (this.params.containsKey(key) ? (String)this.params.get(key) : null); }

	public <T> void addToParamList(String key, T obj) {
		if(this.params.containsKey(key)) {
			// add to existing list
			ArrayList<T> tmplist = (ArrayList<T>)(this.params.get(key));
			tmplist.add(obj);
			this.params.put(key, tmplist);
		} else {
			// create new list
			ArrayList<T> newlist = new ArrayList<T>();
			newlist.add(obj);
			this.params.put(key, newlist);
		}
	}

	protected Map<String, Object> response;
	public Map<String, Object> getResponse() { return this.response; }
	public Object getResponse(String key) { return (this.response.containsKey(key) ? this.response.get(key) : null); }
	public String getResponseStr(String key) { return (this.response.containsKey(key) ? (String)this.response.get(key) : null); }
	
	protected String authFile = null;
	public String getAuthFile() { return this.authFile; }
	public void setAuthFile(String value) { this.authFile = value; }

	// init structures
	protected FBAction(int type) {
		this.type = type;

		this.params = new HashMap<String, Object>();
		this.response = new HashMap<String, Object>();
	}

	// method to override for subclasses -- make sure parameters are valid/exist
	public abstract boolean validate();

	// method to override for subclasses -- contains actual action logic
	public abstract int execute(FBPage page);

	// make sure all specified parameters are defined
	protected boolean hasParams(String keys[]) {
		for(String key : keys) {
			if(!this.params.containsKey(key)) return false;
		}

		return true;
	}

	// returns text from --text arg or contents of --file arg if present
	protected String getText() {
		if(this.params.containsKey("text")) {
			return (String)this.params.get("text");
		} else if(this.params.containsKey("file")) {
			try {
				return App.readFile((String)this.params.get("file"));
			} catch(Exception e) {
				App.logStackTrace(e);
				return null;
			}
		} else {
			return null;
		}
	}

	// action types
	public static final int ACTION_POST = 0;
	public static final int ACTION_COMMENT = 1;
	public static final int ACTION_IMAGEPOST = 2;
	public static final int ACTION_IMAGECOMMENT = 3;

	public static final int ACTION_SHARE = 4;

	public static final int ACTION_GETPOSTS = 5;
	public static final int ACTION_GETREACTS = 6;
	public static final int ACTION_GETCOMMENTS = 7;

	public static final int ACTION_GETSINGLEPOST = 8;
	public static final int ACTION_GETSINGLECOMMENT = 9;
	
	public static final int ACTION_EXTENDTOKEN = 990;
	public static final int ACTION_HEARTBEAT = 991;
}