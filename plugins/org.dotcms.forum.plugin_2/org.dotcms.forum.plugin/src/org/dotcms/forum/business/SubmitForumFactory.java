package org.dotcms.forum.business;

public class SubmitForumFactory {
	
	public static SubmitForumAPI getSubmitForumAPI(){
		return new SubmitForumAPIImpl();
	}
}
