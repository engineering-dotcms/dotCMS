package com.eng.dotcms.healthchecker.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.HealthClusterAdministrator;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;

public class HealthServlet extends HttpServlet {

	private static final long serialVersionUID = -3299155158485099069L;
	
	public void init(ServletConfig config) throws ServletException {
		Logger.info(getClass(), "BEGIN 	Init Health Cluster Handle: change the receiver for JGroups");
		HealthClusterAdministrator clusterAdmin = new HealthClusterAdministrator();
		clusterAdmin.init();
		HealthChecker.INSTANCE.setClusterAdmin(clusterAdmin);
		HealthCheckerAPI healthAPI = new HealthCheckerAPI();
		try {
			healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress(), AddressStatus.LEAVE);
			healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress(), AddressStatus.JOIN);
		} catch (DotDataException e) {
			Logger.error(getClass(), "Error in init HealthServlet: " + e.getMessage());
		}		
		Logger.info(getClass(), "END 	Init Health Cluster Handle: change the receiver for JGroups");
	}
}
