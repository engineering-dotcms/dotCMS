package com.eng.dotcms.additions.publisher;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.quartz.CronTrigger;
import org.quartz.SchedulerException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.content.submit.PluginDeployer;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.util.Logger;

public class AdditionsDeployer extends PluginDeployer {
	
	private static String PLUGIN_ID = "com.eng.dotcms.additions";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	
	@Override
	public boolean deploy() {
		
		try {
			String jobPutName = pluginAPI.loadProperty(PLUGIN_ID, "job.name");
			String jobPutGroup = pluginAPI.loadProperty(PLUGIN_ID, "job.group");
			String jobPutDescription = pluginAPI.loadProperty(PLUGIN_ID, "job.description");
			String javaPutClassname = pluginAPI.loadProperty(PLUGIN_ID, "job.classname");
			String cronPutExpression = pluginAPI.loadProperty(PLUGIN_ID, "job.cron");
			boolean enable = Boolean.parseBoolean(pluginAPI.loadProperty(PLUGIN_ID, "job.enable"));
			if(enable){
				CronScheduledTask cronPutScheduledTask = new CronScheduledTask(jobPutName, jobPutGroup, jobPutDescription, javaPutClassname, new Date(), null, CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), cronPutExpression);
				if(getQuartzJob()!=null) {
	                QuartzUtils.pauseJob("trigger19","group19");
	                QuartzUtils.removeTaskRuntimeValues("trigger19","group19");
	                QuartzUtils.removeJob("trigger19", "group19");
	            }
				QuartzUtils.scheduleTask(cronPutScheduledTask);	
			}
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage());
			return false;
		} catch (SchedulerException e) {
			Logger.error(this, e.getMessage());
			return false;
		} catch (ParseException e) {
			Logger.error(this, e.getMessage());
			return false;
		} catch (ClassNotFoundException e) {
			Logger.error(this, e.getMessage());
			return false;
		}
			
		
		return true;
	}
	
	public ScheduledTask getQuartzJob() {
	    try {
	        List<ScheduledTask> sched = QuartzUtils.getScheduledTasks("trigger19");
	        if(sched.size()==0) {
	            return null;
	        }
	        else {
	            return sched.get(0);
	        }
	    }
	    catch(Exception ex) {
	        throw new RuntimeException(ex);
	    }
	}

}
