package it.eng.bankit.app.job;

import it.eng.bankit.app.importing.ConsulWebImport;
import it.eng.bankit.app.util.DisplayUtil;
import it.eng.bankit.app.util.MailUtil;
import it.eng.bankit.app.util.RemotePublisher;
import it.eng.bankit.deploy.IDeployConst;

import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.springframework.util.Assert;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ConsulWebJob implements StatefulJob {

	private PluginAPI pluginAPI = APILocator.getPluginAPI();

	private HostAPI hostApi;
	private UserAPI userApi;
	private boolean initialized = false;
	private User user;
	private Host host;
	private String sourceFolder;
	private String filenameIt;
	private String filenameEn;
	private boolean remotePublishing = false;
	private String emailFrom;
	private String emailFromName;
	private String emailAddres;
	private String localHostName;
	private boolean updateMode = false;
	private MailUtil mailer;
	private Date startTime;
	private Date endTime;

	private void initialize() throws JobExecutionException {
		if ( !initialized ) {
			try {
				hostApi = APILocator.getHostAPI();
				userApi = APILocator.getUserAPI();
				user = userApi.getSystemUser();
				String hostName = pluginAPI.loadProperty( IDeployConst.pluginId, "bankit.host_name" );
				sourceFolder = pluginAPI.loadProperty( IDeployConst.pluginId, "consWeb.pullPath" );
				filenameIt=pluginAPI.loadProperty( IDeployConst.pluginId, "consWeb.files.it" );
				filenameEn=pluginAPI.loadProperty( IDeployConst.pluginId, "consWeb.files.en" );
				Assert.hasText( filenameIt, "Nessun filename impostato per la lingua italiana" );
				Assert.hasText( filenameEn, "Nessun filename impostato per la lingua inglese" );
				
				if(UtilMethods.isSet( hostName )){
					host = hostApi.find( hostName, user, true );
				}
				if(host==null){
					host=hostApi.findDefaultHost( user, true );
				}
				Assert.notNull( host, "Errore nel recuperare l'host:" + hostName );
				Assert.hasText( sourceFolder, "Parametro cambi.import_dir non impostato" );
				Host host = APILocator.getHostAPI().find( hostName, user, true );
				Assert.notNull( host, "Errore nel recuperare l'host:" + hostName );
				
				String remotePublishingProp = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "consWeb.remotePubblishing" );
				if ( UtilMethods.isSet( remotePublishingProp ) ) {
					remotePublishing = Boolean.parseBoolean( remotePublishingProp );
				}
				emailFrom = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "consWeb.email.from" );
				emailFromName = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "consWeb.email.from.name" );
				emailAddres = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "consWeb.email.addres" );
				mailer = new MailUtil( emailFrom, emailFromName, emailAddres );
				localHostName = InetAddress.getLocalHost().getHostName();
				String updateModeProp = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "consWeb.updateMode" );
				if ( UtilMethods.isSet( updateModeProp ) ) {
					updateMode = Boolean.parseBoolean( updateModeProp );
				}
				initialized = true;
			} catch ( Exception e ) {
				throw new JobExecutionException( "Errore nell'inizializzazione del job", e );
			}

		}
	}

	@Override
	public void execute( JobExecutionContext arg0 ) throws JobExecutionException {
		initialize();
		startTime = new Date();
		Logger.info( this.getClass(), "--------------- Running ConsulWebJob -------------------" );
		boolean toRollback = false;
		try {
			// HibernateUtil.startTransaction();
			if ( user != null && host != null && UtilMethods.isSet( sourceFolder ) ) {
				File directory = new File( sourceFolder );
				File f[] = directory.listFiles();
				File fileConsulWebIta = null;
				File fileConsulWebEng = null;
				boolean checkDir = checkDirectory( directory );
				if ( checkDir ) {
					Logger.info( ConsulWebJob.class, "Controllo i file della cartella : " + sourceFolder );
					for ( File file : f ) {
						String fileName = file.getName();
						if ( fileName.equals( filenameEn )  ) {
							fileConsulWebEng = file;
						} else if ( fileName.equals( filenameIt )){
							fileConsulWebIta = file;
						}
					}
					String destinationPath = pluginAPI.loadProperty( IDeployConst.pluginId, "consWeb.path" ).trim();

					if ( fileConsulWebIta != null && fileConsulWebEng != null && UtilMethods.isSet( destinationPath ) ) {
						ConsulWebImport importTask = new ConsulWebImport( user, host );
						importTask.setRemotePublication( remotePublishing );
						importTask.setUpdateMode( updateMode );
						importTask.init( destinationPath );
						if ( !updateMode ) {
							importTask.backupOldContentlet();
						}
						try {
							importTask.importFiles( fileConsulWebIta, fileConsulWebEng );
							if ( !updateMode ) {
								importTask.removeOldContentlet();
							}
							fileConsulWebIta.delete();
							fileConsulWebEng.delete();
							endTime = new Date();
							sendOkMail();
							Logger.info( ConsulWebJob.class, "Job di importazione cambi terminato con successo" );
						} catch ( Exception e ) {
							Logger.warn( ConsulWebJob.class, "Errore di importazione, tento il rollback", e );
							importTask.abort();
							importTask.rollBack();
						}
					}
				} else {
					toRollback = true;
					throw new Exception( "Directory " + sourceFolder + " non valida" );
				}
			}

		} catch ( Exception e ) {
			endTime = new Date();
			toRollback = true;
			Logger.error( this.getClass(), "Error running job", e );
			sendErrorMail( e );
		} finally {
			try {
				if ( toRollback ) {
					// HibernateUtil.rollbackTransaction();
				} else {// Check toCommit?
						// HibernateUtil.commitTransaction();
				}
			} catch ( Exception e1 ) {
				Logger.error( this.getClass(), "Error commit job", e1 );
			}
			Logger.info( this.getClass(), "------------------ Ending ConsulWebJob ------------------" );
		}
	}

	private static boolean checkDirectory( File directory ) {
		if ( UtilMethods.isSet( directory ) ) {
			return directory.isDirectory() && directory.exists();
		}
		return false;
	}

	private void sendOkMail() {

		try {
			StringBuffer esito = new StringBuffer();
			esito.append( " e si è conclusa con successo " );
			esito.append( DisplayUtil.printOreGiorno( endTime ) );
			esito.append( " ." );
			mailer.sendMail( "[SITIDOT " + localHostName + "] - Importazione ConsulWeb completata con successo", DisplayUtil.printConsulWebMailBody( host.getHostname(), startTime, esito.toString() ) );
		} catch ( Exception e ) {
			Logger.error( MailUtil.class, "Errore nell'invio email" + e );
		}
	}

	private void sendErrorMail( Throwable th ) {
		try {
			StringBuilder esito = new StringBuilder();
			if ( th instanceof TimeoutException ) {
				esito.append( ", ma non si è conclusa entro il tempo massimo prestabilito di " );
				esito.append( RemotePublisher.POOLING_MAX_TIME / 1000 );
				esito.append( " secondi." );
			} else {
				esito.append( "e si è conclusa " );
				esito.append( DisplayUtil.printOreGiorno( endTime ) );
				esito.append( "  con gli errori riportati in calce:\n" );
				esito.append( DisplayUtil.printErrorHtml( th ) );
			}

			mailer.sendMail( "[SITIDOT " + localHostName + "] - Errore Importazione ConsulWeb", DisplayUtil.printConsulWebMailBody( host.getHostname(), startTime, esito.toString() ) );
		} catch ( Exception ex ) {
			Logger.error( MailUtil.class, "Errore nell'invio email" + ex );
		}
	}
}
