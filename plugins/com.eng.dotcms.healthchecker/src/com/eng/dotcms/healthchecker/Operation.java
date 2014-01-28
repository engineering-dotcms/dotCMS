package com.eng.dotcms.healthchecker;

public enum Operation {
	FLUSHING,RESTARTING;
	
	public String toString(){
		switch(this){
			case FLUSHING:
				return "FLUSHING";
			case RESTARTING:
				return "RESTARTING";
			default:
				return "NO_STATUS";
		}
	}
	
	public static Operation fromString(String op){
		return op.equals(FLUSHING.toString())?FLUSHING:RESTARTING;
	}
}
