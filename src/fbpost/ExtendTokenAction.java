package fbpost;

public class ExtendTokenAction extends FBAction {
	public ExtendTokenAction() { super(FBAction.ACTION_EXTENDTOKEN); }

	public boolean validate() {
		return (this.authFile != null);
	}

	public int execute(FBPage page) {
		page.reloadAuthFrom(this.authFile);
		page.extendAccessToken();

		return 0;
	}
}