package com.eng.dotcms.healthchecker;

public enum AddressStatus {
	JOIN,LEAVE;
	
	public String toString(){
		switch(this){
			case JOIN:
				return "JOIN";
			case LEAVE:
				return "LEAVE";
			default:
				return "NO_STATUS";
		}
	}
}
