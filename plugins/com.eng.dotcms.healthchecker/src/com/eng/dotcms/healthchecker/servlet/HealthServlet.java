package com.eng.dotcms.healthchecker.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

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
		HealthClusterAdministrator clusterAdmin = new HealthClusterAdministrator();
		clusterAdmin.init();
		if(clusterAdmin.isCluster()){
			HealthChecker.INSTANCE.setClusterAdmin(clusterAdmin);			
			HealthCheckerAPI healthAPI = new HealthCheckerAPI();
			try {				
				HibernateUtil.startTransaction();
				// elimino i vecchi records riguardanti il nodo attuale in quanto sono in riavvio.
				healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress(), AddressStatus.LEAVE);
				healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress(), AddressStatus.JOIN);
				healthAPI.deleteHealthClusterView(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress());
				// inserisco il nodo nella cluster view con status JOINED.
				boolean isCreator = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView().getCreator().equals(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress());
				healthAPI.insertHealthClusterView(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress(),
						Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),isCreator,
						AddressStatus.JOIN);
				HibernateUtil.commitTransaction();
				
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
