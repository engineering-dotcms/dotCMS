package com.eng.dotcms.healthchecker.servlet;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.AddressStatus;
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
			HibernateUtil.startTransaction();
			// elimino i vecchi records riguardanti il nodo attuale in quanto sono in riavvio.
			cleanNode(hostname);
			healthAPI.insertHealthLock(hostname, Operation.STARTING);
			HibernateUtil.commitTransaction();			
			Logger.info(getClass(), "...CLUSTER HEALTH INITIALIZATION DONE!");
		} catch (UnknownHostException e) {
			Logger.warn(getClass(), "Warning. " + e.getMessage());
		} catch (DotDataException e) {
			Logger.warn(getClass(), "Warning. " + e.getMessage());
		}
		
	}
	
	private void cleanNode(String localAddress) throws DotDataException {
		healthAPI.deleteHealthStatus(localAddress, AddressStatus.LEFT);
		healthAPI.deleteHealthStatus(localAddress, AddressStatus.JOIN);
		healthAPI.deleteHealthClusterView(localAddress);
		healthAPI.deleteHealthLock(localAddress, Operation.RESTARTING);
		healthAPI.deleteHealthLock(localAddress, Operation.FLUSHING);
		healthAPI.deleteHealthLock(localAddress, Operation.JOINING);
//		healthAPI.deleteHealthLock(localAddress, Operation.STARTING);
	}
	
}
