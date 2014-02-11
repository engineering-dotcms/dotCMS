package com.eng.dotcms.healthchecker.listener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.Operation;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.eng.dotcms.healthchecker.util.HealthUtil;

public class InitHealthListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		try {
			HealthCheckerAPI healthAPI = new HealthCheckerAPI();
			String hostname = InetAddress.getLocalHost().getHostName();
			hostname = HealthUtil.getStringAddress(hostname);
			Logger.info(getClass(), "STARTING CLUSTER HEALTH INITIALIZATION...");
			HibernateUtil.startTransaction();
			// elimino i vecchi records riguardanti il nodo attuale in quanto sono in riavvio.
			healthAPI.cleanNode(hostname);
			healthAPI.insertHealthLock(hostname, Operation.STARTING);
			HibernateUtil.commitTransaction();			
			Logger.info(getClass(), "...CLUSTER HEALTH INITIALIZATION DONE!");
		} catch (UnknownHostException e) {
			Logger.warn(getClass(), "Warning. " + e.getMessage());
		} catch (DotDataException e) {
			Logger.warn(getClass(), "Warning. " + e.getMessage());
		} finally {
			DbConnectionFactory.closeConnection();
		}
	}
	
}
