package com.eng.dotcms.healthchecker;

import org.jgroups.Address;
import org.jgroups.View;

public class Health {
	
	private Address address;
	private View clusterView;
	private AddressStatus status; 
	private Address writtenBy;
	
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
