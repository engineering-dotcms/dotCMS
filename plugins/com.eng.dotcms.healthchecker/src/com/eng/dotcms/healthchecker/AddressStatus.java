package com.eng.dotcms.healthchecker;

public enum AddressStatus {
	JOIN,LEFT;
	
	public String toString(){
		switch(this){
			case JOIN:
				return "JOIN";
			case LEFT:
				return "LEFT";
			default:
				return "NO_STATUS";
		}
	}
}
