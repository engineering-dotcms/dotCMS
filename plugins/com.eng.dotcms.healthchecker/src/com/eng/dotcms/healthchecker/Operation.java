package com.eng.dotcms.healthchecker;

public enum Operation {
	FLUSHING,RESTARTING,STARTING,NOONE,JOINING;
	
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
			case JOINING:
				return "JOINING";								
			default:
				return "NO_OP";
		}
	}
	
	public static Operation fromString(String op){
		if("null".equals(op) || null==op)
			return NOONE;
		return op.equals(FLUSHING.toString())?FLUSHING:op.equals(RESTARTING.toString())?RESTARTING:op.equals(STARTING.toString())?STARTING:op.equals(JOINING.toString())?JOINING:NOONE;
	}
}
