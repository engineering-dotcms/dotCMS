//package it.eng.dotcms.sitemap;
//
//import java.text.ParseException;
//import java.util.Date;
//import java.util.HashMap;
//
//import org.quartz.CronTrigger;
//import org.quartz.SchedulerException;
//
//import com.dotmarketing.business.APILocator;
//import com.dotmarketing.business.UserAPI;
//import com.dotmarketing.exception.DotDataException;
//import com.dotmarketing.plugin.PluginDeployer;
//import com.dotmarketing.plugin.business.PluginAPI;
//import com.dotmarketing.portlets.contentlet.business.HostAPI;
//import com.dotmarketing.portlets.folders.business.FolderAPI;
//import com.dotmarketing.quartz.CronScheduledTask;
//import com.dotmarketing.quartz.QuartzUtils;
//import com.dotmarketing.util.Logger;
//
//public class SitemapPluginDeployer implements PluginDeployer {
//	private String pluginId = "it.eng.dotcms.sitemap";
//	private PluginAPI pluginAPI = APILocator.getPluginAPI();
//	private UserAPI uAPI = APILocator.getUserAPI();
//	private HostAPI hAPI = APILocator.getHostAPI();
//	private FolderAPI fAPI = APILocator.getFolderAPI();
//	
//	public boolean deploy() {
//		
//		
//		try {
//			String jobName = pluginAPI.loadProperty(pluginId, "quartz.job.name");
//			String jobGroup = pluginAPI.loadProperty(pluginId, "quartz.job.group");
//			String jobDescription = pluginAPI.loadProperty(pluginId, "quartz.job.description");
//			String javaClassname = pluginAPI.loadProperty(pluginId, "quartz.job.java.classname");
//			String cronExpression = pluginAPI.loadProperty(pluginId, "quartz.job.cron.expression");
//			CronScheduledTask cronScheduledTask = new CronScheduledTask(jobName, jobGroup, jobDescription, javaClassname, new Date(), null, CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), cronExpression);
//			QuartzUtils.scheduleTask(cronScheduledTask);
//			
//		}catch(DotDataException e){
//			Logger.error(this, e.getMessage(), e);
//			return false;
//		}catch(ClassNotFoundException e){
//			Logger.error(this, e.getMessage(), e);
//			return false;
//		}catch(ParseException e){
//			Logger.error(this, e.getMessage(), e);
//			return false;
//		}catch(SchedulerException e){
//			Logger.error(this, e.getMessage(), e);
//			return false;
//		} 
//		
//		return true;
//	}
//
//	public boolean redeploy(String version) {
//		// TODO Auto-generated method stub
//		return true;
//	}
//
//}
