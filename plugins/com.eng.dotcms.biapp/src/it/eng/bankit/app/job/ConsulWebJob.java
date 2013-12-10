package it.eng.bankit.app.job;

import it.eng.bankit.app.importing.ConsulWebImport;
import it.eng.bankit.app.util.DisplayUtil;
import it.eng.bankit.app.util.MailUtil;
import it.eng.bankit.deploy.IDeployConst;

import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

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
		if (!initialized) {
			try {
				hostApi = APILocator.getHostAPI();
				userApi = APILocator.getUserAPI();
				user = userApi.getSystemUser();
				String hostId = pluginAPI.loadProperty(	IDeployConst.PLUGIN_ID, "bankit.host");
				sourceFolder = pluginAPI.loadProperty(IDeployConst.PLUGIN_ID, "consWeb.pullPath");
				filenameIt = pluginAPI.loadProperty(IDeployConst.PLUGIN_ID,	"consWeb.files.it");
				filenameEn = pluginAPI.loadProperty(IDeployConst.PLUGIN_ID,	"consWeb.files.en");

				if (UtilMethods.isSet(hostId)) {
					host = hostApi.find(hostId, user, true);
				}
				if (host == null) {
					host = hostApi.findDefaultHost(user, true);
				}

				String remotePublishingProp = APILocator.getPluginAPI().loadProperty(IDeployConst.PLUGIN_ID,"consWeb.remotePubblishing");
				if (UtilMethods.isSet(remotePublishingProp)) {					
					remotePublishing = Boolean.parseBoolean(remotePublishingProp);
				}
				emailFrom = APILocator.getPluginAPI().loadProperty(IDeployConst.PLUGIN_ID, "consWeb.email.from");
				emailFromName = APILocator.getPluginAPI().loadProperty( IDeployConst.PLUGIN_ID, "consWeb.email.from.name");
				emailAddres = APILocator.getPluginAPI().loadProperty( IDeployConst.PLUGIN_ID, "consWeb.email.addres");
				mailer = new MailUtil(emailFrom, emailFromName, emailAddres);
				localHostName = InetAddress.getLocalHost().getHostName();
				String updateModeProp = APILocator.getPluginAPI().loadProperty(  IDeployConst.PLUGIN_ID, "consWeb.updateMode");
				if (UtilMethods.isSet(updateModeProp)) {
					updateMode = Boolean.parseBoolean(updateModeProp);
				}
				initialized = true;
			} catch (Exception e) {
				throw new JobExecutionException(
						"Errore nell'inizializzazione del job", e);
			}

		}
	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		initialize();
		startTime = new Date();
		Logger.info(this.getClass(), "--------------- Running ConsulWebJob -------------------");
		boolean toRollback = false;
		try {
			// HibernateUtil.startTransaction();
			if (user != null && host != null && UtilMethods.isSet(sourceFolder)) {
				File directory = new File(sourceFolder);
				File listaFileDirs[] = directory.listFiles();
				File fileConsulWebIta = null;
				File fileConsulWebEng = null;
				File fileConsulWebItaCSV = null;
				File fileConsulWebEngCSV = null;
				String[] arrFileIta = filenameIt.split(";");
				String[] arrFileEng = filenameEn.split(";");
				String filenameItPDF = arrFileIta[0];
				String filenameEnPDF = arrFileEng[0];
				String filenameItCSV = "";
				String filenameEnCSV = "";
				Logger.info(ConsulWebJob.class, "filenameItPDF " + filenameItPDF );
				Logger.info(ConsulWebJob.class, "filenameEnPDF " + filenameEnPDF );
				if( arrFileIta != null && arrFileIta.length == 2 ){
					filenameItCSV = arrFileIta[1];
					filenameEnCSV = arrFileEng[1];
					Logger.info(ConsulWebJob.class, "filenameItCSV " + filenameItCSV );
					Logger.info(ConsulWebJob.class, "filenameEnCSV " + filenameEnCSV );
				}

				boolean checkDir = checkDirectory(directory);
				if (checkDir) {
					Logger.info(ConsulWebJob.class,	"Controllo i file della cartella : " + sourceFolder);
					for (File file : listaFileDirs) {
						String fileName = file.getName();
						if (fileName.equals(filenameEnPDF)) {
							fileConsulWebEng = file;
						} else if (fileName.equals(filenameItPDF)) {
							fileConsulWebIta = file;
						}
						else if (UtilMethods.isSet(filenameItCSV)  &&  fileName.equals(filenameItCSV)) {
							fileConsulWebItaCSV = file;
						}
						else if (UtilMethods.isSet(filenameEnCSV)  &&  fileName.equals(filenameEnCSV)) {
							fileConsulWebEngCSV = file;
						}
					}
					String destinationPath = pluginAPI.loadProperty( IDeployConst.PLUGIN_ID, "consWeb.path").trim();

					if (fileConsulWebIta != null && fileConsulWebEng != null && UtilMethods.isSet(destinationPath)) {
						ConsulWebImport importTask = new ConsulWebImport(user,	host);
						importTask.setRemotePublication(remotePublishing);
						importTask.setUpdateMode(updateMode);
						importTask.init(destinationPath);
						if (!updateMode) {
							importTask.backupOldContentlet();
						}
						try {
							importTask.importFiles(fileConsulWebIta, fileConsulWebEng , fileConsulWebItaCSV, fileConsulWebEngCSV );
 							if (!updateMode) {
								importTask.removeOldContentlet();
							}
							fileConsulWebIta.delete();
							fileConsulWebEng.delete();
							if(fileConsulWebItaCSV != null  )
							  fileConsulWebItaCSV.delete(); 
							endTime = new Date();
							sendOkMail();
							Logger.info(ConsulWebJob.class, "Job di importazione cambi terminato con successo");
						} catch (Exception e) {
							Logger
							.warn( ConsulWebJob.class, "Errore di importazione, tento il rollback", e);
							importTask.abort();
							importTask.rollBack();
						}
					}
				} else {
					toRollback = true;
					throw new Exception("Directory " + sourceFolder + " non valida");
				}
			}

		} catch (Exception e) {
			endTime = new Date();
			toRollback = true;
			Logger.error(this.getClass(), "Error running job", e);
			sendErrorMail(e);
		} finally {
			try {
				if (toRollback) {
					// HibernateUtil.rollbackTransaction();
				} else {// Check toCommit?
					// HibernateUtil.commitTransaction();
				}
			} catch (Exception e1) {
				Logger.error(this.getClass(), "Error commit job", e1);
			}
			Logger
			.info(this.getClass(),
					"------------------ Ending ConsulWebJob ------------------");
		}
	}

	private static boolean checkDirectory(File directory) {
		if (UtilMethods.isSet(directory)) {
			return directory.isDirectory() && directory.exists();
		}
		return false;
	}

	private void sendOkMail() {

		try {
			StringBuffer esito = new StringBuffer();
			esito.append(" e si è conclusa con successo ");
			esito.append(DisplayUtil.printOreGiorno(endTime));
			esito.append(" .");
			mailer.sendMail("[SITIDOT " + localHostName	+ "] - Importazione ConsulWeb completata con successo",
					DisplayUtil.printConsulWebMailBody(host.getHostname(),
							startTime, esito.toString()));
		} catch (Exception e) {
			Logger.error(MailUtil.class, "Errore nell'invio email" + e);
		}
	}

	private void sendErrorMail(Throwable th) {
		try {
			String poolingMax = pluginAPI.loadProperty(	IDeployConst.PLUGIN_ID, IDeployConst.CAMBI_POOLING_MAXTIME);
			long poolingMaxLong = Long.parseLong(poolingMax);
			StringBuilder esito = new StringBuilder();
			if (th instanceof TimeoutException) {
				esito
				.append(", ma non si è conclusa entro il tempo massimo prestabilito di ");
				esito.append(poolingMaxLong / 1000);
				esito.append(" secondi.");
			} else {
				esito.append("e si è conclusa ");
				esito.append(DisplayUtil.printOreGiorno(endTime));
				esito.append("  con gli errori riportati in calce:\n");
				esito.append(DisplayUtil.printErrorHtml(th));
			}

			mailer.sendMail("[SITIDOT " + localHostName
					+ "] - Errore Importazione ConsulWeb", DisplayUtil
					.printConsulWebMailBody(host.getHostname(), startTime,
							esito.toString()));
		} catch (Exception ex) {
			Logger.error(MailUtil.class, "Errore nell'invio email" + ex);
		}
	}
}
