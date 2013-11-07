package com.eng.dotcms.mostresearchedterms;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.quartz.CronTrigger;
import org.quartz.SchedulerException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.content.submit.PluginDeployer;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_ADD_TERMS_TABLE;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_ADD_TEMP_TERMS_SEQ;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_ADD_TEMP_TERMS_TABLE;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_CHECK_TABLES;

/**
 * Plugin deployer. Create the tables, if not exist, and schedule the cron job.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Jan 3, 2013 - 2:34:11 PM
 */
public class MostResearchedTermsDeployer extends PluginDeployer {
	
	private String pluginId = "com.eng.dotcms.mostresearchedterms";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	
	@Override
	public boolean deploy() {
		try{
			DotConnect dc = new DotConnect();			
			try {
				if(!existsTables(dc)){
					// create tables
					dc.setSQL(ORACLE_ADD_TERMS_TABLE);
					dc.loadResult();
					dc.setSQL(ORACLE_ADD_TEMP_TERMS_SEQ);
					dc.loadResult();
					dc.setSQL(ORACLE_ADD_TEMP_TERMS_TABLE);
					dc.loadResult();
				}
				//scheduled job
				String jobName = pluginAPI.loadProperty(pluginId, "quartz.job.name");
				String jobGroup = pluginAPI.loadProperty(pluginId, "quartz.job.group");
				String jobDescription = pluginAPI.loadProperty(pluginId, "quartz.job.description");
				String javaClassname = pluginAPI.loadProperty(pluginId, "quartz.job.java.classname");
				String cronExpression = pluginAPI.loadProperty(pluginId, "quartz.job.cron.expression");
				CronScheduledTask cronScheduledTask = new CronScheduledTask(jobName, jobGroup, jobDescription, javaClassname, new Date(), null, CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), cronExpression);
				QuartzUtils.scheduleTask(cronScheduledTask);				
			}catch(DotDataException e){
				Logger.error(this, e.getMessage(), e);
				return false;
			}catch(ClassNotFoundException e){
				Logger.error(this, e.getMessage(), e);
				return false;
			}catch(ParseException e){
				Logger.error(this, e.getMessage(), e);
				return false;
			}catch(SchedulerException e){
				Logger.error(this, e.getMessage(), e);
				return false;
			}
		}finally{
			DbConnectionFactory.closeConnection();
		}
		
		
		return true;
	}
	
	protected boolean existsTables(DotConnect dc) throws DotDataException{
		dc.setSQL(ORACLE_CHECK_TABLES);		
		Map<String, Object> row = dc.loadObjectResults().get(0); 
		if(Integer.parseInt(row.get("exist").toString())==2)
			return true;
		else
			return false;
	}
}
