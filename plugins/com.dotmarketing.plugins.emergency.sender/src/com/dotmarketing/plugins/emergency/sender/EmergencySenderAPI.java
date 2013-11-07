package com.dotmarketing.plugins.emergency.sender;


public abstract class EmergencySenderAPI {
	
	private static EmergencySenderAPI esAPI = null;
	public static EmergencySenderAPI getInstance(){
		if(esAPI == null){
			esAPI = EmergencySenderAPIImpl.getInstance();
		}
		return esAPI;	
	}
	
	protected String UPDATE_STATUS = "UPDATE emergency_sender_status set is_active = ? ";
	
	protected String CHECK_STATUS = "SELECT * FROM emergency_sender_status ";
	
	public abstract void enableEmergencySender();
	
	public abstract void disableEmergencySender();
	
	public abstract boolean isEnabledEmergencySender();
}
