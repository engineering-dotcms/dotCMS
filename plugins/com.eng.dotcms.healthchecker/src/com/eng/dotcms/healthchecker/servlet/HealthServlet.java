package com.eng.dotcms.healthchecker.servlet;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.jgroups.Address;

import com.dotcms.rest.HealthService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.HealthClusterAdministrator;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.Operation;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.eng.dotcms.healthchecker.timer.HealthCheckerCheckNetworkTimer;
import com.eng.dotcms.healthchecker.timer.HealthCheckerTimer;
import com.eng.dotcms.healthchecker.util.HealthUtil;

public class HealthServlet extends HttpServlet {

	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	private static final long serialVersionUID = -3299155158485099069L;	
	private TimerTask timerTask = new HealthCheckerTimer();
	private TimerTask checkNetworkTimerTask = new HealthCheckerCheckNetworkTimer();
	private Timer timer = new Timer(true);
	private Timer timerCheckConn = new Timer(true);
	
	@SuppressWarnings("deprecation")
	public void init(ServletConfig config) throws ServletException {
		try{
			HealthCheckerAPI healthAPI = new HealthCheckerAPI();
			Logger.info(getClass(), "BEGIN 	Init Health Cluster Handle");		
			if(Config.getBooleanProperty("DIST_INDEXATION_ENABLED", false)){
				Date now = new Date();
				Address localAddress = CacheLocator.getCacheAdministrator().getJGroupsChannel().getLocalAddress();				
				Date lastLeave = healthAPI.getDateOfLastLeaveEvent(localAddress);
				try {
					HibernateUtil.startTransaction();
					// elimino i vecchi records riguardanti il nodo attuale in quanto sono in riavvio.
					healthAPI.cleanNode(localAddress);
					HibernateUtil.commitTransaction();	
					healthAPI.insertHealthLock(localAddress, Operation.STARTING);
					boolean isCreator = CacheLocator.getCacheAdministrator().getJGroupsChannel().getView().getCreator().equals(localAddress);
					// inserisco il nodo nella cluster view con status JOINED.
					healthAPI.insertHealthClusterView(localAddress,
							Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),isCreator,
							AddressStatus.JOIN,now,false);
					HealthClusterAdministrator clusterAdmin = new HealthClusterAdministrator();				
					clusterAdmin.init();
					HealthChecker.INSTANCE.setClusterAdmin(clusterAdmin);				
					// flush cache
					if(!Config.getBooleanProperty("HEALTH_CHECKER_ALWAYS_FLUSH_CACHE", true) && HealthUtil.needFlushCache(lastLeave, now)) {
						Logger.info(getClass(), "Need flushing cache.");									
						CacheLocator.getCacheAdministrator().flushAlLocalOnlyl();
					}
					Logger.info(getClass(), "Starting monitoring...");
					startMonitoring();
					Logger.info(getClass(), "started!");
				
					Logger.info(getClass(), "END 	Init Health Cluster Handle");
					if(Config.getBooleanProperty("HEALTH_CHECKER_LOCK_REMOTE_PUBLISH",false)){
						HealthClusterViewStatus status = healthAPI.singleClusterView(localAddress);
						Logger.info(getClass(), "The node "+localAddress+" is an endpoint for remote publish: unlock it");
						String response = unlockLocalRemotePublish();
						if(HealthService.STATUS_OK.equals(response))
							Logger.info(getClass(), status.getAddress()+" Unlocked.");					
					}
					healthAPI.deleteHealthLock(localAddress, Operation.STARTING);
				} catch (DotDataException e) {
					Logger.error(getClass(), "Error in init HealthServlet: " + e.getMessage(), e);
					
					try {
						HibernateUtil.rollbackTransaction();
					} catch (DotHibernateException e1) {
						Logger.fatal(getClass(), "DotHibernateException: " + e1.getMessage(), e);
					}
				} catch (Exception e) {
					Logger.error(getClass(), "Errore generico.", e);
				}
			}
		} finally {
			DbConnectionFactory.closeConnection();
		}
		
	}
	
	@Override
	public void destroy() {
		timer.cancel();
	}
	
	private void startMonitoring() {
		timer.scheduleAtFixedRate(timerTask, 0, 20 * 1000);
		timerCheckConn.scheduleAtFixedRate(checkNetworkTimerTask, 0, 10 * 1000);
	}
	
	private String unlockLocalRemotePublish() {
		String ctrl = HealthService.STATUS_OK;
		try {
			String scriptFolder = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "script.folder");
			String unlockRemotePublishScriptName = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "unlock.remote.publish.script.name");
			Logger.info(getClass(), "Received ACK for allow the remote publishing port.");
			ProcessBuilder pb = new ProcessBuilder("nohup","/usr/bin/sudo", scriptFolder+unlockRemotePublishScriptName,"&");
			pb.start();
		} catch (DotDataException e) {
			Logger.error(getClass(), "DotDataException: Error in allowing port.");
			ctrl = "KO";
		} catch (IOException e) {
			Logger.error(getClass(), "IOException: Error in allowing port.");
			ctrl = "KO";
		}
		return ctrl;
	}
}
