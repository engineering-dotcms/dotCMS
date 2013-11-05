package it.bankit.website.deploy;

import it.bankit.website.job.PublishExpireJobBI;

import java.util.Date;
import java.util.HashMap;

import org.quartz.CronTrigger;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;

public class BankitPluginDeployer implements PluginDeployer {

	private PluginAPI pAPI = APILocator.getPluginAPI();

	@Override
	public boolean deploy() {

		try {
			String jobEnabled = pAPI.loadProperty(IDeployConst.PLUGIN_ID,IDeployConst.PUBLISH_EXP_JOB_ENABLED );
			boolean enabled = Boolean.parseBoolean( jobEnabled );
			if(enabled){
				String jobcronExp = pAPI.loadProperty(IDeployConst.PLUGIN_ID,IDeployConst.PUBLISH_EXP_JOB_CRON_EXPRESSION );
				Logger.info(this.getClass(), "jobcronExp " + jobcronExp );
				String xmlSitemapCronExpression2 = pAPI.loadProperty(IDeployConst.PLUGIN_ID, jobcronExp );
				Logger.info(this.getClass(), "xmlSitemapCronExpression2 " + xmlSitemapCronExpression2 );
				CronScheduledTask cronScheduledTask2 = new CronScheduledTask( "PublishExpireJobBI", null, "PublishExpireJobBI giornaliero contenuti", 
						PublishExpireJobBI.class.getName(), new Date(), null,
						CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), "0 0/5 * * * ?" );
				QuartzUtils.scheduleTask( cronScheduledTask2 );
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}

		return true;
	}

	@Override
	public boolean redeploy(String version) {
		try {
			boolean enabled = Boolean.parseBoolean(pAPI.loadProperty(IDeployConst.PLUGIN_ID, "pubExp.job.enabled"));
			if(enabled){
				String jobcronExp = pAPI.loadProperty(IDeployConst.PLUGIN_ID,IDeployConst.PUBLISH_EXP_JOB_CRON_EXPRESSION );
				Logger.info(this.getClass(), "jobcronExp " + jobcronExp );

				String xmlSitemapCronExpression2 = pAPI.loadProperty(IDeployConst.PLUGIN_ID, jobcronExp );
				Logger.info(this.getClass(), "xmlSitemapCronExpression2 " + xmlSitemapCronExpression2 );
				CronScheduledTask cronScheduledTask2 = new CronScheduledTask( "PublishExpireJobBI", null, "PublishExpireJobBI giornaliero contenuti", 
						PublishExpireJobBI.class.getName(), new Date(), null,
						CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), "0 0/5 * * * ?" );
				QuartzUtils.scheduleTask( cronScheduledTask2 );
			}

		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		return true;
	}

}
