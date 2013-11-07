package it.eng.bankit.deploy;

import it.eng.bankit.app.job.ConsulWebJob;
import it.eng.bankit.app.util.DotFolderUtil;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.quartz.CronTrigger;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;

public class ApplicationPluginDeployer implements PluginDeployer {

	private static PluginAPI pAPI;
	private static org.apache.log4j.Logger LOG = Logger.getLogger( ApplicationPluginDeployer.class );
	private static boolean initialized = false;

	@Override
	public boolean deploy() {

		try {
			LOG.info( "DEPLOY PLUGIN APPLICAZIONI " );
			initialize();
		} catch ( Exception e ) {
			Logger.error( this, e.getMessage(), e );
			return false;
		}

		return true;
	}

	@Override
	public boolean redeploy( String version ) {
		LOG.info( "RE-DEPLOY PLUGIN APPLICAZIONI " );

		try {
			jobConfig();
		} catch ( Exception e ) {
			Logger.error( this, e.getMessage(), e );
			return false;
		}

		return true;
	}

	public static boolean isInitialized() {
		return initialized;
	}

	public static void initialize() throws Exception {
		if ( !initialized ) {
			pAPI = APILocator.getPluginAPI();
			checkExternalizedProperties();
			jobConfig();
			initialized = true;
		}
	}

	public static boolean checkExternalizedProperties() {
		boolean override = false;
		try {
			InputStream is = null;
			is = ClassLoader.getSystemClassLoader().getResourceAsStream( IDeployConst.pluginId + File.separatorChar + "plugin.properties" );
			if ( is != null ) {

				Properties props = new Properties();
				// Try loading properties from the file (if found)
				props.load( is );
				for ( Object curKeyObj : props.keySet() ) {// Override keys
					String key = (String) curKeyObj;
					String value = props.getProperty( key );
					String curValue = pAPI.loadProperty( IDeployConst.pluginId, key );
					if ( curValue == null || !curValue.equals( value ) ) {
						pAPI.saveProperty( IDeployConst.pluginId, key, value );
						override = true;
					}

				}

			}
		} catch ( Exception e ) {
		}
		return override;
	}

	private static void jobConfig() throws Exception {

		String cronExpression = pAPI.loadProperty( IDeployConst.pluginId, "consWeb.job.cron.expression" );

		CronScheduledTask cronScheduledTask = new CronScheduledTask( "ConsulWebJob", null, "Aggiornamento giornaliero allegati Consulenze", ConsulWebJob.class.getName(), new Date(), null,
				CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), cronExpression );

		QuartzUtils.scheduleTask( cronScheduledTask );
	}
}
