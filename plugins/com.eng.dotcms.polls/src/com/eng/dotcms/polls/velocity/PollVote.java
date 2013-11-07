package com.eng.dotcms.polls.velocity;

import java.io.Serializable;
import java.util.Date;

public class PollVote implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String poll;
	private String choice;
	private String user;
	private String identifier;
	private String inode;
	private String title;
	private Date modDate;
	private String modUser;
	
	public String getPoll() {
		return poll;
	}
	public void setPoll(String poll) {
		this.poll = poll;
	}
	public String getChoice() {
		return choice;
	}
	public void setChoice(String choice) {
		this.choice = choice;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getInode() {
		return inode;
	}
	public void setInode(String inode) {
		this.inode = inode;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Date getModDate() {
		return modDate;
	}
	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}
	public String getModUser() {
		return modUser;
	}
	public void setModUser(String modUser) {
		this.modUser = modUser;
	}
	
	

}
