package com.dotmarketing.plugins.emergency.sender;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

import org.quartz.CronTrigger;
import org.quartz.SchedulerException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;

public class EmergencySenderPluginDeployer implements PluginDeployer {
	private String pluginId = "com.dotmarketing.plugins.emergency.sender";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	
	public boolean deploy() {
		final String SQL_MYSQL = 
				"CREATE TABLE IF NOT EXISTS emergency_sender_status ( "+
					"is_active tinyint "+
				")";
		
		final String SQL_ORACLE = 
				"CREATE TABLE emergency_sender_status ( "+
					"is_active number(1,0) DEFAULT 0 "+
				")";
		
		final String SQL_MSSQL = 
				"CREATE TABLE emergency_sender_status ( "+
						"is_active tinyint DEFAULT 0 "+
					")";
		
		
		final String SQL_PGRS = 
				"CREATE TABLE emergency_sender_status ( "+
						"is_active bool "+
					")";
		
		final String SQL_INSERT = "INSERT INTO emergency_sender_status values(0) ";
		
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				try {
					dc.setSQL(SQL_PGRS);
					dc.loadResult();
					
					dc.setSQL(SQL_INSERT);
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				
				try {
					dc.setSQL(SQL_MYSQL);
					dc.loadResult();
					
					dc.setSQL(SQL_INSERT);
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				try {
					dc.setSQL(SQL_MSSQL);
					dc.loadResult();
					
					dc.setSQL(SQL_INSERT);
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}else{
				try {
					dc.setSQL(SQL_ORACLE);
					dc.loadResult();
					
					dc.setSQL(SQL_INSERT);
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}			
			}
		}finally{
			DbConnectionFactory.closeConnection();
		}
		
		try {
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
		
		return true;
	}

	public boolean redeploy(String version) {
		// TODO Auto-generated method stub
		return true;
	}

}
