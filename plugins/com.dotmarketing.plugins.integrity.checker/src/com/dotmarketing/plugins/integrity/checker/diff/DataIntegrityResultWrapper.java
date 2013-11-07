package com.dotmarketing.plugins.integrity.checker.diff;

import java.util.Date;

public class DataIntegrityResultWrapper {
	//Local
	private String localInode;
	private String localMD5;
	private Date localCheckDate;
	
	//Remote
	private String remoteInode;
	private String remoteMD5;
	private Date remoteCheckDate;
	
	
	public String getLocalInode() {
		return localInode;
	}
	public void setLocalInode(String localInode) {
		this.localInode = localInode;
	}
	public String getLocalMD5() {
		return localMD5;
	}
	public void setLocalMD5(String localMD5) {
		this.localMD5 = localMD5;
	}
	public Date getLocalCheckDate() {
		return localCheckDate;
	}
	public void setLocalCheckDate(Date localCheckDate) {
		this.localCheckDate = localCheckDate;
	}
	public String getRemoteInode() {
		return remoteInode;
	}
	public void setRemoteInode(String remoteInode) {
		this.remoteInode = remoteInode;
	}
	public String getRemoteMD5() {
		return remoteMD5;
	}
	public void setRemoteMD5(String remoteMD5) {
		this.remoteMD5 = remoteMD5;
	}
	public Date getRemoteCheckDate() {
		return remoteCheckDate;
	}
	public void setRemoteCheckDate(Date remoteCheckDate) {
		this.remoteCheckDate = remoteCheckDate;
	}
}
