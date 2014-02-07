package com.dotcms.rest;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;

@Path("/health")
public class HealthService extends WebResource {

	private HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	public static String STATUS_OK = "OK";
	public static String CONNECTION_EXC = "CONNECT_EXC";
	public static String SOCKET_EXC = "SOCK_EXC";
	
	@SuppressWarnings("deprecation")
	@GET
	@Path("/joinCluster")
	public String flushCache() {
		String ctrl = STATUS_OK;
		try {
			Logger.info(getClass(), "Received ACK for flush cache event when I rejoin the cluster.");
			CacheLocator.getCacheAdministrator().flushAlLocalOnlyl();
			Logger.info(getClass(), "Cache flushed correctly.");
			healthAPI.deleteHealthStatus(CacheLocator.getCacheAdministrator().getJGroupsChannel().getLocalAddress(), AddressStatus.LEFT);
			if(Config.getBooleanProperty("HEALTH_CHECKER_LOCK_REMOTE_PUBLISH",false))
				unlockRemotePublish();
		} catch (DotDataException e) {
			Logger.error(getClass(), "Error in rejoin and flush cache.");
			ctrl = "KO";
		}
		return ctrl;
	}
	
	@GET
	@Path("/forceJoinCluster")
	public String restart() {
		String ctrl = STATUS_OK;
		try {
			String scriptFolder = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "script.folder");
			String scriptName = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "script.name");
			Logger.info(getClass(), "Received ACK for restart the system.");
			ProcessBuilder pb = new ProcessBuilder("nohup","/bin/bash", scriptFolder+scriptName,"&");
			pb.start();
			if(Config.getBooleanProperty("HEALTH_CHECKER_LOCK_REMOTE_PUBLISH",false))
				lockRemotePublish();
		} catch (DotDataException e) {
			Logger.error(getClass(), "DotDataException: Error in restart system.");
			ctrl = "KO";
		} catch (IOException e) {
			Logger.error(getClass(), "IOException: Error in restart system.");
			ctrl = "KO";
		}
		return ctrl;
	}
	
	@GET
	@Path("/lockRemotePublish")
	public String lockRemotePublish() {
		String ctrl = STATUS_OK;
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
	
	@GET
	@Path("/unlockRemotePublish")
	public String unlockRemotePublish() {
		String ctrl = STATUS_OK;
		try {
			String scriptFolder = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "script.folder");
			String unlockRemotePublishScriptName = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "unlock.remote.publish.script.name");
			Logger.info(getClass(), "Received ACK for allow the remote publishing port.");
			ProcessBuilder pb = new ProcessBuilder("nohup","/usr/bin/sudo", scriptFolder+unlockRemotePublishScriptName,"&");
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
	
	@GET
	@Path("/checkNetwork")
	public String checkNetwork() {
		return STATUS_OK;
	}
}
