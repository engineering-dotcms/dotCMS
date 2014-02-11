package com.eng.dotcms.healthchecker.servlet;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.Operation;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.eng.dotcms.healthchecker.util.HealthUtil;

public class InitHealthServlet extends HttpServlet {

	private static final long serialVersionUID = -435887443748366784L;
	private HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			hostname = HealthUtil.getStringAddress(hostname);
			Logger.info(getClass(), "STARTING CLUSTER HEALTH INITIALIZATION...");
			healthAPI.insertHealthLock(hostname, Operation.STARTING);
			Logger.info(getClass(), "...CLUSTER HEALTH INITIALIZATION DONE!");
		} catch (UnknownHostException e) {
			Logger.warn(getClass(), "Warning. " + e.getMessage());
		} catch (DotDataException e) {
			Logger.warn(getClass(), "Warning. " + e.getMessage());
		}
		
	}
	
	
}
