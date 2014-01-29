package com.eng.dotcms.healthchecker;

public enum Operation {
	FLUSHING,RESTARTING,STARTING;
	
	public String toString(){
		switch(this){
			case FLUSHING:
				return "FLUSHING";
			case RESTARTING:
				return "RESTARTING";
			case STARTING:
				return "STARTING";				
			default:
				return "NO_STATUS";
		}
	}
	
	public static Operation fromString(String op){
		return op.equals(FLUSHING.toString())?FLUSHING:op.equals(RESTARTING.toString())?RESTARTING:STARTING;
	}
}
