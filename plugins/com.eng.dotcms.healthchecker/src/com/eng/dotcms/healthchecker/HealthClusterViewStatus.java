package com.eng.dotcms.healthchecker;

import java.util.Date;

public class HealthClusterViewStatus {
	
	private String id;
	private String address;
	private String port;
	private String protocol;
	private String status;
	private boolean creator;
	private Date modDate;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Date getModDate() {
		return modDate;
	}
	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}	
	public boolean isCreator() {
		return creator;
	}
	public void setCreator(boolean creator) {
		this.creator = creator;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(protocol);
		sb.append("://");
		sb.append(address);
		if(!"80".equals(port)){
			sb.append(":");
			sb.append(port);
		}
		return sb.toString();
	}
}
