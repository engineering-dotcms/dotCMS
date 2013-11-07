package org.dotcms.forum.business;

public class ManageForumLock {
	
	public static ManageForumLock INSTANCE = new ManageForumLock();
	private boolean locked;
	
	private ManageForumLock(){
		locked = false;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
}
