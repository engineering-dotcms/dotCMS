package com.eng.dotcms.healthchecker;

import java.util.Date;

import org.jgroups.Address;
import org.jgroups.View;

public class HealthEvent {
	
	private Address address;
	private View clusterView;
	private AddressStatus status; 
	private Address writtenBy;
	private Date modDate;
	
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}
	public View getClusterView() {
		return clusterView;
	}
	public void setClusterView(View clusterView) {
		this.clusterView = clusterView;
	}	
	public AddressStatus getStatus() {
		return status;
	}
	public void setStatus(AddressStatus status) {
		this.status = status;
	}	
	public Address getWrittenBy() {
		return writtenBy;
	}
	public void setWrittenBy(Address writtenBy) {
		this.writtenBy = writtenBy;
	}
	public Date getModDate() {
		return modDate;
	}
	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(" address: ");
		sb.append(address);
		sb.append("; clusterView: ");
		sb.append(clusterView);
		sb.append("; status: ");
		sb.append(status);
		sb.append(" ]");
		return sb.toString();
	}

}
