package com.eng.dotcms.healthchecker.timer;

import java.io.IOException;
import java.util.TimerTask;
import com.dotcms.rest.HealthService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.eng.dotcms.healthchecker.util.HealthUtil;

public class HealthCheckerCheckNetworkTimer extends TimerTask {

	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	
	@Override
	public void run() {
		try {
			checkNetwork();			
		} catch (Exception e) {
			Logger.error(getClass(), "Error in TimerTask", e);
		}
	}
	
	@SuppressWarnings("deprecation")
	private void checkNetwork(){
		try {
			HealthCheckerAPI healthAPI = new HealthCheckerAPI();
			HealthClusterViewStatus status = healthAPI.singleClusterView(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress());			
			String response = HealthUtil.callRESTService(status, "/checkNetwork");
			if(HealthService.SOCKET_EXC.equals(response)) {
				// something wrong into my network connection...stop the service.
				Logger.info(getClass(), "Problems with network interface. Trying to stop the service.");
				if(Config.getBooleanProperty("HEALTH_CHECKER_LOCK_REMOTE_PUBLISH",false)) {
					response = lockLocalRemotePublish();
					if(HealthService.STATUS_OK.equals(response))
    					Logger.info(getClass(), status.getAddress()+" Unlocked.");
					else
						Logger.warn(getClass(), status.getAddress()+" unable to unlock.");
				}
				String scriptFolder = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "script.folder");
				String stopScriptName = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "stop.script.name");
				ProcessBuilder pb = new ProcessBuilder("nohup","/bin/bash", scriptFolder+stopScriptName,"&");
				pb.start();
			}
		}catch(DotDataException e){
			Logger.error(getClass(), "DotDataException: Error in checkNetwork",e);
		}catch(IOException e){
			Logger.error(getClass(), "IOException: Error in checkNetwork",e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	private String lockLocalRemotePublish() {
		String ctrl = HealthService.STATUS_OK;
		try {
			String scriptFolder = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "script.folder");
			String lockRemotePublishScriptName = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "lock.remote.publish.script.name");
			Logger.info(getClass(), "Received ACK for block the remote publishing port.");
			ProcessBuilder pb = new ProcessBuilder("nohup","/usr/bin/sudo", scriptFolder+lockRemotePublishScriptName,"&");
			pb.start();
		} catch (DotDataException e) {
			Logger.error(getClass(), "DotDataException: Error in block port.");
			ctrl = "KO";
		} catch (IOException e) {
			Logger.error(getClass(), "IOException: Error in block port.");
			ctrl = "KO";
		}
		return ctrl;
	}
}
