package it.eng.bankit.app.job;

import it.eng.bankit.deploy.IDeployConst;
import it.eng.bankit.servlet.CambiThread;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class CambiJob implements StatefulJob {

	private UserAPI userApi = APILocator.getUserAPI();

	private CambiThread action = null;

	private void init() throws JobExecutionException {
		try {
			User user = userApi.getSystemUser();
			String hostName = APILocator.getPluginAPI().loadProperty( IDeployConst.PLUGIN_ID, "bankit.host_name" );
			Host host = APILocator.getHostAPI().find( hostName, user, true );
			String importDir = APILocator.getPluginAPI().loadProperty( IDeployConst.PLUGIN_ID, "cambi.import_dir" );
			String selettorePath = APILocator.getPluginAPI().loadProperty( IDeployConst.PLUGIN_ID, "cambi.selettore.path" );
			String cambiPath = APILocator.getPluginAPI().loadProperty( IDeployConst.PLUGIN_ID, "cambi.path" );
			String indicatoriPath 	 = APILocator.getPluginAPI().loadProperty( IDeployConst.PLUGIN_ID, "indicatori.path" );
			
			action = new CambiThread( user, host, importDir, selettorePath, cambiPath, indicatoriPath , null,null );
		} catch ( Exception e ) {
			throw new JobExecutionException( "Error initializing cambi job", e );
		}
	}

	@Override
	public void execute( JobExecutionContext arg0 ) throws JobExecutionException {
		Logger.info( this.getClass(), "--------------- Running CambiJob -------------------" );
		if ( action == null ) {
			init();
		}
		try {
			action.start();
		} catch ( Exception e ) {
			throw new JobExecutionException( "Error running cambi job", e );
		}
		Logger.info( this.getClass(), "------------------ Ending ConsulWebJob ------------------" );
	}

}
