package com.eng.dotcms.healthchecker;

public enum Operation {
	FLUSHING,RESTARTING,STARTING,NOONE;
	
	public String toString(){
		switch(this){
			case FLUSHING:
				return "FLUSHING";
			case RESTARTING:
				return "RESTARTING";
			case STARTING:
				return "STARTING";
			case NOONE:
				return "NOONE";				
			default:
				return "NO_OP";
		}
	}
	
	public static Operation fromString(String op){
		return op.equals(FLUSHING.toString())?FLUSHING:op.equals(RESTARTING.toString())?RESTARTING:op.equals(STARTING.toString())?STARTING:NOONE;
	}
}
