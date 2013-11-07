package org.dotcms.forum.velocity.bean;

import java.util.Date;
import java.util.List;

public class Topic implements Comparable<Topic>{
	
	private String identifier;
	private String inode;
	private String title;
	private String urlTitle;
	private String description;
	private String owner;
	private Date lastModified;
	private Date modDate;
	private List<Thread> threads;
	
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
	public List<Thread> getThreads() {
		return threads;
	}
	public void setThreads(List<Thread> threads) {
		this.threads = threads;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getUrlTitle() {
		return urlTitle;
	}
	public void setUrlTitle(String urlTitle) {
		this.urlTitle = urlTitle;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}	
	public Date getModDate() {
		return modDate;
	}
	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}
	@Override
	public int compareTo(Topic o) {
		if(this.modDate.after(o.getModDate()))
			return -1;
		else if(this.modDate.before(o.getModDate()))
			return 1;
		else
			return 0;
	}
	
}
