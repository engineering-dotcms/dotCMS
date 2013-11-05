package it.bankit.website.job;

import it.bankit.website.deploy.IDeployConst;

import java.io.File;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Logger;

public class ChangeJob implements Job {

	//private String pluginId = "com.dotcms.viewToolsBI-0.10.1-SNAPSHOT";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();

	private static org.apache.log4j.Logger LOG = Logger.getLogger(ChangeJob.class);

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		Logger.info(ChangeJob.class, "--------------- Running Change Job -------------------");

		try {
			String sourceFolder = pluginAPI.loadProperty(IDeployConst.PLUGIN_ID, "consWeb.dir.path");
			System.out.println("Nome della directory: "+sourceFolder);

			File directory = new File(sourceFolder);
			File f[] = directory.listFiles();
			
			for (File file : f) {
				System.out.println("-->"+file.getName());
			}
			
		} catch (DotDataException e) {
			LOG.error("SOurce Folder not found!", e);
		}
		
		Logger.info(ChangeJob.class, "------------------ Ending Change Job ------------------");

	}

}
