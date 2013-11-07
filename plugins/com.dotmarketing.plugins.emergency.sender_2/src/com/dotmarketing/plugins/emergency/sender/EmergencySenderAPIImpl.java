package com.dotmarketing.plugins.emergency.sender;

import java.util.List;
import java.util.Map;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;


public class EmergencySenderAPIImpl extends EmergencySenderAPI{
	
	private static EmergencySenderAPIImpl esAPI = null;
	public static EmergencySenderAPIImpl getInstance(){
		if(esAPI == null){
			esAPI = new EmergencySenderAPIImpl();
		}
		return esAPI;	
	}
	
	/**
	 * Enable the script for static content sending to a delivery instance
	 */
	@Override
	public void enableEmergencySender() {
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(UPDATE_STATUS);
			dc.addParam(true);
			
			dc.loadResult();

		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
	}
	
	/**
	 * Disable the script for static content sending to a delivery instance
	 */
	@Override
	public void disableEmergencySender() {
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(UPDATE_STATUS);
			dc.addParam(false);
			
			dc.loadResult();

		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
	}
	
	/**
	 * Check the emergency sender status
	 *
	 */
	@Override
	public boolean isEnabledEmergencySender() {
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL(CHECK_STATUS);
			
			List<Map<String, Object>> res = dc.loadObjectResults();
			
			if(Integer.parseInt(res.get(0).get("is_active").toString()) != 0)
				return true;

		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
		
		return false;
	}
}
