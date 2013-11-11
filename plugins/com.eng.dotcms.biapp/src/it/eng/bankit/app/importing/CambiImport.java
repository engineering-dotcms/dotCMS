/**
 * 
 */
package it.eng.bankit.app.importing;

import it.eng.bankit.app.util.DotFolderUtil;
import it.eng.bankit.deploy.IDeployConst;
import it.eng.bankit.servlet.CambiServlet;
import it.eng.bankit.servlet.CambiThread;

import java.io.File;
import java.util.Date;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author cesare
 * 
 */
public class CambiImport extends AbstractImport {

	private Folder folderCambi;
	private Folder folderSelettore;
	private Folder folderIndicatori;
	private CambiInsert cambiInsert;
	private IndicatoriInsert indicatoriInsert;
	private SimpleContentInsert simpleContentInsert;
	private boolean dolbyRemotePubblishing=false;
	private boolean running=true;

	/**
	 * @param userImport
	 * @param host
	 */
	public CambiImport( User userImport, Host host ) {
		super( userImport, host );
		cambiInsert = new CambiInsert( userImport, host );
		indicatoriInsert = new IndicatoriInsert( userImport, host );
		simpleContentInsert = new SimpleContentInsert( userImport, host );
	}

	protected void init( String cambiPath, String selettorePath , String indicatoriPath ) throws Exception {
		Logger.info( CambiServlet.class, "Verifico se sono presenti in DOTCMS i folder per i cambi. Se non esistono sarano creati "  );
		if ( !initialized ) {
			if ( UtilMethods.isSet( cambiPath ) ) {
				folderCambi = DotFolderUtil.findOrCreateFolder( cambiPath, false, host, user );
			}
			if ( UtilMethods.isSet( selettorePath ) ) {
				folderSelettore = DotFolderUtil.findOrCreateFolder( selettorePath, false, host, user );
			}
			if ( UtilMethods.isSet( indicatoriPath ) ) {
				folderIndicatori = DotFolderUtil.findOrCreateFolder( indicatoriPath, false, host, user );
			}
			String dolbyRemotePubblishingProp = APILocator.getPluginAPI().loadProperty( IDeployConst.PLUGIN_ID, "cambi.dolbyRemotePubblishing" );
			if ( UtilMethods.isSet( dolbyRemotePubblishingProp ) ) {
				dolbyRemotePubblishing = Boolean.parseBoolean( dolbyRemotePubblishingProp );
			}
			cambiInsert.init();
			indicatoriInsert.init();
			simpleContentInsert.init();
			clear();
			super.init();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.eng.bankit.app.importing.AbstractImport#saveContentletsFile(java.io
	 * .File, com.dotmarketing.portlets.folders.model.Folder,
	 * com.dotmarketing.portlets.languagesmanager.model.Language)
	 */
	public void importFiles(  String cambiPath, String selettorePath, String indicatoriPath, File cambiFileIt, File cambiFileEn, File indicatoriFileIt, File indicatoriFileEn, File selettoreFileIt, File selettoreFileEn )
			throws Exception {
		init( cambiPath, selettorePath , indicatoriPath );
		
		cambiInsert.saveContentletsFile( cambiFileIt, folderCambi, langIt );
		checkRunning();
		cambiInsert.saveContentletsFile( cambiFileEn, folderCambi, langEn );
		checkRunning();
		// contentToBackup.addAll( cambiInsert.contentToBackup );
		contentToPublish.addAll( cambiInsert.contentToPublish );
		contentToUnPublish.addAll( cambiInsert.contentToUnPublish );
		Logger.info( CambiThread.class, "Inseriti i cambi di riferimento della giornata " );

		simpleContentInsert.saveContentletsFile( selettoreFileIt, folderSelettore, langIt );
		checkRunning();
		simpleContentInsert.saveContentletsFile( selettoreFileEn, folderSelettore, langEn );
		checkRunning();
		// contentToBackup.addAll( selettoreInsert.contentToBackup );
		contentToPublish.addAll( simpleContentInsert.contentToPublish );
		contentToUnPublish.addAll( simpleContentInsert.contentToUnPublish );
		Logger.info( CambiThread.class, "Aggiornati i cambi presenti nel selettore " );
		
		
		indicatoriInsert.saveContentletsFile( indicatoriFileIt, folderIndicatori, langIt );
		checkRunning();
		indicatoriInsert.saveContentletsFile( indicatoriFileEn, folderIndicatori , langEn );
		checkRunning();
		// contentToBackup.addAll( indicatoriInsert.contentToBackup );
		contentToPublish.addAll( indicatoriInsert.contentToPublish );
		contentToUnPublish.addAll( indicatoriInsert.contentToUnPublish );
		Logger.info( CambiThread.class, "Aggiornati Indicatori in homepage " );

	}

	public String startRemotePublish() throws Exception {
		String publishBundleId=null;
		if ( rPublisher.remoteServerAvaiable() ) {
			if ( !contentToPublish.isEmpty() ) {
				publishBundleId = rPublisher.publish( contentToPublish );
				publishStartTime = new Date();
//				if (dolbyRemotePubblishing){//Pezzotto doppia pubblicazione per essere sicuri che il remote publisher vada a buon fine
//					Logger.info( this.getClass(), "Remote Pubblishing ping bundle (" + publishBundleId + ")" );
//					Thread.sleep( 90000 );
//					publishBundleId = rPublisher.publish( contentToPublish );
//				}//Fine pezzotto
				Logger.info( this.getClass(), "Remote Pubblishing avviato (Bundle-id:" + publishBundleId + ")" );
			}
		} else {
			throw new Exception( "nessun server remoto disponibile, impossibile effettuare pubblicazione remota" );
		}
		return publishBundleId;
	}
	
	public String finalizeRemotePublish() throws Exception {
		String unpublishBundleId=null;
		if ( rPublisher.remoteServerAvaiable() ) {
			if ( !contentToUnPublish.isEmpty() ) {
				unpublishBundleId = rPublisher.publish( contentToPublish );
				unpublishStartTime = new Date();
				Logger.info( this.getClass(), "Cancellazione remota avviata (Bundle-id:" + unpublishBundleId + ")" );
			}
		} else {
			throw new Exception( "nessun server remoto disponibile, impossibile effettuare pubblicazione remota" );
		}
		return publishBundleId;
	}
	public void setDolbyRemotePubblishing(boolean dolbyRemotePubblishing){
		this.dolbyRemotePubblishing=dolbyRemotePubblishing;
	}
	public void setUpdateMode(boolean updateMode){
		cambiInsert.setUpdateMode(updateMode);
		indicatoriInsert.setUpdateMode(updateMode);
		simpleContentInsert.setUpdateMode(updateMode);
	}
	
	public void abort() throws Exception{
		running=false;
		rPublisher.abortPubblications();
	}
	
	private void checkRunning() throws InterruptedException{
		if(!running){
			throw new InterruptedException("Importazione cambi interrotta");
		}
	}
}
