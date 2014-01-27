package com.dotcms.rest;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;

@Path("/health")
public class HealthService extends WebResource {

	private HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	
	@SuppressWarnings("deprecation")
	@GET
	@Path("/joinCluster")
	public String flushCache() {
		String ctrl = "OK";
		try {
			Logger.info(getClass(), "Retrieve ACK for flush cache event when I rejoin the cluster.");
			CacheLocator.getCacheAdministrator().flushAlLocalOnlyl();
			Logger.info(getClass(), "Cache flushed correctly.");
			healthAPI.deleteHealthStatus(CacheLocator.getCacheAdministrator().getJGroupsChannel().getLocalAddress(), AddressStatus.LEAVE);
		} catch (DotDataException e) {
			Logger.error(getClass(), "Error in rejoin and flush cache.");
			ctrl = "KO";
		}
		return ctrl;
	}
	
	@GET
	@Path("/forceJoinCluster")
	public String restart() {
		String ctrl = "OK";
		try {
			String scriptFolder = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "script.folder");
			String scriptName = pluginAPI.loadProperty("com.eng.dotcms.healthchecker", "script.name");
			Logger.info(getClass(), "Retrieve ACK for restart the system.");
			ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptFolder+scriptName);
			pb.start();
		} catch (DotDataException e) {
			Logger.error(getClass(), "DotDataException: Error in restart system.");
			ctrl = "KO";
		} catch (IOException e) {
			Logger.error(getClass(), "IOException: Error in restart system.");
			ctrl = "KO";
		}
		return ctrl;
	}
}
