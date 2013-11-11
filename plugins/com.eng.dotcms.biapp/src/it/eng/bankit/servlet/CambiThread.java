package it.eng.bankit.servlet;

import it.eng.bankit.app.ImportException;
import it.eng.bankit.app.importing.CambiImport;
import it.eng.bankit.app.util.CambiUtil;
import it.eng.bankit.app.util.DisplayUtil;
import it.eng.bankit.app.util.MailUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class CambiThread extends Thread {
	public static enum Status {
		Started, RemotePublishing, Finalizing, Finish, Error, Abort
	};

	private User user;
	private Host host;
	private String importDir;
	private String selettorePath;
	private String indicatoriPath;
	private String cambiPath;
	private boolean remotePublishing = false;
	private boolean updateMode = false;
	private MailUtil mailer;
	private Date startTime = null;
	private Date remotePublishStartTime = null;
	private Date endTime = null;
	private Date remotePublishEndTime = null;
	private String username = null;
	private String localHostName;
	private Throwable lastException = null;
	private Status status;
	private List<String> bundlets = new ArrayList<String>();
	private File fileCambiIta = null;
	private File fileCambiEng = null;
	private File fileIndIta = null;
	private File fileIndEng = null;
	private File fileSelIta = null;
	private File fileSelEng = null;
	private CambiImport importTask;
	private PluginAPI pAPI = APILocator.getPluginAPI();

	public CambiThread(User user, Host host, String importDir,
			String selettorePath, String cambiPath, String indicatoriPath,
			String username, String localHostName) {
		this.user = user;
		this.host = host;
		this.importDir = importDir;
		this.selettorePath = selettorePath;
		this.cambiPath = cambiPath;
		this.indicatoriPath = indicatoriPath;
		this.username = username;
		this.localHostName = localHostName;
		importTask = new CambiImport(this.user, host);

	}

	@Override
	public void run() {
		status = Status.Started;
		startTime = new Date();
		if (user != null && host != null && UtilMethods.isSet(importDir)
				&& UtilMethods.isSet(selettorePath)
				&& UtilMethods.isSet(cambiPath) && UtilMethods.isSet(username)) {
			Logger.info(CambiThread.class, "Leggo i file dalla cartella : "
					+ importDir);
			File directory = new File(importDir);
			File f[] = directory.listFiles();
			boolean checkDir = checkDirectory(directory);
			if (checkDir) {
				String path = cambiPath
						+ CambiUtil.generateCambiPath(startTime);
				String cambiFileNameIt = CambiUtil
						.generateCambiOriginalFilename(startTime, "it");
				String cambiFileNameEn = CambiUtil
						.generateCambiOriginalFilename(startTime, "en");
				for (File file : f) {
					String fileName = file.getName();
					if (fileName.indexOf(CambiUtil.INDICI_FILENAME_IT) != -1) {
						fileIndIta = file;
					} else if (fileName.indexOf(CambiUtil.INDICI_FILENAME_EN) != -1) {
						fileIndEng = file;
					} else if (fileName
							.equalsIgnoreCase(CambiUtil.RIFERIMENTI_FILENAME_IT)) {
						fileSelIta = file;
					} else if (fileName
							.equalsIgnoreCase(CambiUtil.RIFERIMENTI_FILENAME_EN)) {
						fileSelEng = file;
					} else if (fileName.equalsIgnoreCase(cambiFileNameIt)) {
						fileCambiIta = file;
					} else if (fileName.equalsIgnoreCase(cambiFileNameEn)) {
						fileCambiEng = file;
					}
				}
				try {
					String checkFilesDescription = CambiUtil.checkFiles(
							username, fileIndIta, fileIndEng, fileSelIta,
							fileSelEng, fileCambiIta, fileCambiEng);
					importTask.importFiles(path, selettorePath, indicatoriPath,
							fileCambiIta, fileCambiEng, fileIndIta, fileIndEng,
							fileSelIta, fileSelEng);
					Logger.info(CambiThread.class,
							"Completata operazione di import in DOTCMS! ");
					if (remotePublishing) {
						/*** ------ Remote pubblishing ------ ***/
						String publishBulndleId = importTask
								.startRemotePublish();
						if (UtilMethods.isSet(publishBulndleId)) {
							remotePublishStartTime = new Date();
							bundlets.add(publishBulndleId);
							status = Status.RemotePublishing;
							sendStartMail();
							importTask.waitRemotePublication(publishBulndleId);
							String unpublishBulndleId = importTask
									.finalizeRemotePublish();
							if (UtilMethods.isSet(unpublishBulndleId)) {
								bundlets.add(unpublishBulndleId);
								importTask
										.waitRemotePublication(unpublishBulndleId);
							}
							remotePublishEndTime = new Date();
						}
					}
					status = Status.Finalizing;
					/*** ----------------- Cancellazione files--------------- ***/
					deleteFiles();
					status = Status.Finish;
					endTime = new Date();
					Logger.info(CambiThread.class, "END Importazione Cambi "
							+ DisplayUtil.printAuditStatusDescription(bundlets
									.get(0), username, startTime, "success"));
					sendOkMail(checkFilesDescription);
				} catch (NoSuchUserException nsue) {
					Logger.error(CambiThread.class, nsue.getMessage());
					lastException = nsue;
					endTime = new Date();
					try {
						importTask.abort();
					} catch (Exception e1) {
						Logger.error(CambiThread.class,
								"utente non presente in dotcms", e1);
					}
					sendErrorMail(nsue);
					status = Status.Error;
				} catch (Throwable e) {
					Logger.error(CambiThread.class, e.getMessage());
					lastException = e;
					endTime = new Date();
					try {
						importTask.abort();
					} catch (Exception e1) {
						Logger
								.error(
										CambiThread.class,
										"Errore nel tentativo di cancellare la pubblicazione cambi",
										e1);
					}
					sendErrorMail(e);
					status = Status.Error;
				}
			} else {
				Logger.error(CambiThread.class,
						"Directory di importazione non valida");
				ImportException e = new ImportException(
						"Directory di importazione non valida");
				lastException = e;
				status = Status.Error;
				endTime = new Date();
				sendErrorMail(e);
				// throw new
				// ImportException("Directory di importazione non valida");
			}
		} else {
			Logger.error(CambiThread.class,
					"Parametri di importazione non validi");
			ImportException e = new ImportException(
					"Parametri di importazione non validi");
			lastException = e;
			status = Status.Error;
			endTime = new Date();
			sendErrorMail(e);
			// throw new
			// ImportException("Parametri di importazione non validi");
		}
	}

	public void abort() throws Exception {
		if (status.equals(Status.RemotePublishing)) {
			importTask.abort();
		}
		deleteFiles();
		endTime = new Date();
		status = Status.Abort;
		Logger.info(CambiThread.class, "Importazione Cambi annullata");

	}

	private void sendStartMail() {
		try {
			StringBuilder body = new StringBuilder();
			body
					.append("L'importazione dei files dei cambi è stata avviata sul server ");
			if (host != null) {
				body.append(host.getHostname());
			} else {
				body.append("sconosciuto");
			}
			if (!bundlets.isEmpty()) {
				body.append(" con Bundle-id:");
				body.append(bundlets.get(0));
			}
			body.append(" ");
			body.append(DisplayUtil.printOreGiorno(remotePublishStartTime));
			mailer.sendMail("[SITIDOT " + localHostName
					+ "] - Importazione Cambi avviata con successo", body
					.toString());
		} catch (DotDataException e) {
			Logger.error(MailUtil.class, "Errore nell'invio email", e);
		}
	}

	private void sendOkMail(String warnings) {
		try {
			StringBuilder body = new StringBuilder();
			body.append("L'importazione dei files dei cambi sull'host ");
			if (host != null) {
				body.append(host.getHostname());
			} else {
				body.append("sconosciuto");
			}
			if (!bundlets.isEmpty()) {
				body.append(" con Bundle-id:");
				body.append(bundlets.get(0));
			}
			body.append(" è stata completata con successo ");
			body.append(DisplayUtil.printOreGiorno(remotePublishEndTime));
			if (warnings.length() > 0) {
				body.append('\n');
				body.append("<h1>Warning</h1>\n");
				body.append("<p>");
				body.append(warnings);
				body.append("</p>\n");
			}
			mailer.sendMail("[SITIDOT " + localHostName
					+ "] - Importazione Cambi completata con successo", body
					.toString());
		} catch (DotDataException e) {
			Logger.error(MailUtil.class, "Errore nell'invio email", e);
		}
	}

	private void sendErrorMail(Throwable th) {

		try {
			String poolingMax = pAPI.loadProperty(
					it.eng.bankit.deploy.IDeployConst.PLUGIN_ID,
					it.eng.bankit.deploy.IDeployConst.CAMBI_POOLING_MAXTIME);
			long poolingMaxLong = Long.parseLong(poolingMax);
			StringBuilder body = new StringBuilder();
			body.append("L'importazione dei files dei cambi sull'host ");
			if (host != null) {
				body.append(host.getHostname());
			} else {
				body.append("sconosciuto");
			}
			if (!bundlets.isEmpty()) {
				body.append(" con Bundle-id:");
				body.append(bundlets.get(0));
			}
			body.append(" iniziata ");
			body.append(DisplayUtil.printOreGiorno(startTime));

			if (th instanceof TimeoutException) {
				body
						.append(", non si è conclusa entro il tempo massimo prestabilito di ");
				body.append(poolingMaxLong / 1000);
				body.append(" secondi.");
			} else {
				body.append("e si è conclusa ");
				body.append(DisplayUtil.printOreGiorno(endTime));
				body.append("  con gli errori riportati in calce:\n");
				body.append(DisplayUtil.printErrorHtml(th));
			}
			mailer.sendMail("[SITIDOT " + localHostName
					+ "] - Errore Importazione Cambi", body.toString());
		} catch (Exception ex) {
			Logger.error(MailUtil.class, "Errore nell'invio email", ex);
		}
	}

	private boolean checkDirectory(File directory) {
		if (UtilMethods.isSet(directory)) {
			return directory.isDirectory() && directory.exists();
		}
		return false;
	}

	private void deleteFiles(File... files) {
		for (File curFile : files) {
			if (curFile != null) {
				if (!curFile.delete()) {
					Logger.error(CambiThread.class,
							"Errore nella cancellazione del file:"
									+ curFile.getName());
				}
			}
		}
	}

	public boolean isRemotePublishing() {
		return remotePublishing;
	}

	public void setRemotePublishing(boolean remotePublishing) {
		this.remotePublishing = remotePublishing;
	}

	public String getImportDir() {
		return importDir;
	}

	public void setImportDir(String importDir) {
		this.importDir = importDir;
	}

	public void setMailer(MailUtil mailer) {
		this.mailer = mailer;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public Throwable getLastException() {
		return lastException;
	}

	public String getUsername() {
		return username;
	}

	public Status getStatus() {
		return status;
	}

	public List<String> getBundlets() {
		return bundlets;
	}

	public Date getRemotePublishStartTime() {
		return remotePublishStartTime;
	}

	public Date getRemotePublishEndTime() {
		return remotePublishEndTime;
	}

	public boolean isUpdateMode() {
		return updateMode;
	}

	public void setUpdateMode(boolean updateMode) {
		this.updateMode = updateMode;
		importTask.setUpdateMode(updateMode);
	}

}
