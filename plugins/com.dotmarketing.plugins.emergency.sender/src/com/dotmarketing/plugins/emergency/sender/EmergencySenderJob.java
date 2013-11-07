package com.dotmarketing.plugins.emergency.sender;

import java.io.IOException;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Logger;

/**
 * Cron job for the Most researched terms. Read from the temp table and than group by into the new table.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Jan 3, 2013 - 2:34:49 PM
 */
public class EmergencySenderJob implements StatefulJob {

	private EmergencySenderAPI esAPI = EmergencySenderAPI.getInstance();
	private String pluginId = "com.dotmarketing.plugins.emergency.sender";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		Logger.debug(this, "Start emergency sender status check");
				
		if(esAPI.isEnabledEmergencySender()) {
			try {
				String scriptName = pluginAPI.loadProperty(pluginId, "script.name");
				String scriptFolder = pluginAPI.loadProperty(pluginId, "script.folder");
				ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptFolder+scriptName);

				pb.start();
				Logger.info(this, "Starting emergency sender script");
			} catch (DotDataException e) {
				Logger.error(this, e.getMessage());
			} catch (IOException e) {
				Logger.error(this, e.getMessage());
			}
		}
		
		Logger.debug(this, "End emergency sender status check");
	}

}