package com.eng.dotcms.healthchecker.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.jgroups.Address;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.HealthClusterAdministrator;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;

public class HealthServlet extends HttpServlet {

	private static final long serialVersionUID = -3299155158485099069L;
	
	@SuppressWarnings("deprecation")
	public void init(ServletConfig config) throws ServletException {
		Logger.info(getClass(), "BEGIN 	Init Health Cluster Handle: change the receiver for JGroups");
		if(Config.getBooleanProperty("DIST_INDEXATION_ENABLED", false)){
			Address localAddress = CacheLocator.getCacheAdministrator().getJGroupsChannel().getLocalAddress();			
			HealthCheckerAPI healthAPI = new HealthCheckerAPI();
			try {				
				HibernateUtil.startTransaction();
				// elimino i vecchi records riguardanti il nodo attuale in quanto sono in riavvio.
				healthAPI.deleteHealthStatus(localAddress, AddressStatus.LEAVE);
				healthAPI.deleteHealthStatus(localAddress, AddressStatus.JOIN);
				healthAPI.deleteHealthClusterView(localAddress);
				boolean isCreator = CacheLocator.getCacheAdministrator().getJGroupsChannel().getView().getCreator().equals(localAddress);
				healthAPI.insertHealthClusterView(localAddress,
						Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),isCreator,
						AddressStatus.JOIN);
				HibernateUtil.commitTransaction();				
				HealthClusterAdministrator clusterAdmin = new HealthClusterAdministrator();				
				clusterAdmin.init();
				HealthChecker.INSTANCE.setClusterAdmin(clusterAdmin);				
				// inserisco il nodo nella cluster view con status JOINED.
				
			} catch (DotDataException e) {
				Logger.error(getClass(), "Error in init HealthServlet: " + e.getMessage());
				try {
					HibernateUtil.rollbackTransaction();
				} catch (DotHibernateException e1) {
					Logger.fatal(getClass(), "DotHibernateException: " + e1.getMessage());
				}
			}		
		}
		Logger.info(getClass(), "END 	Init Health Cluster Handle: change the receiver for JGroups");
	}
}
