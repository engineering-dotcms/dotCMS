package it.bankit.website.ajax;

import it.bankit.website.deploy.IDeployConst;
import it.bankit.website.exception.BadScpExitException;
import it.bankit.website.util.FotoImporter;
import it.bankit.website.util.MediaXmlReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.directwebremoting.io.FileTransfer;
import org.w3c.dom.Document;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ReadMediaFiles {
	private MediaXmlReader reader;
	private FotoImporter fotoImporter;
	// private String hostName = "videoenc-coll.ac.bankit.it";//Collaudo
	// private String hostName = "videoenc.ac.bankit.it";//Produzione

//	private static String pluginId = "com.eng.dotcms.viewToolsBI";
	private PluginAPI pAPI = APILocator.getPluginAPI();

	public ReadMediaFiles() {
		reader = new MediaXmlReader();
		fotoImporter = new FotoImporter();
	}

	public Map<String, Object> readImagMapeFile(FileTransfer file, String language, HttpSession session) {
		
		if (file.getMimeType().equalsIgnoreCase("text/xml")) {
			Map<String, Object> properties = null;
			try {
				if (language != null) {
					properties = reader.read(file.getInputStream(), language);
				} else {
					properties = reader.read(file.getInputStream());
				}
				String type = (String) properties.get(MediaXmlReader.TYPE_PROPERTY);

				if (type != null && type.equalsIgnoreCase(MediaXmlReader.FOTO_TYPE)) {
					String imageUrl = (String) properties.get(MediaXmlReader.IMAGE_PROPERTY);
					if (UtilMethods.isSet(imageUrl)) {
						downloadImage(imageUrl, "binary1", session);
					}
					String thumbernailUrl = (String) properties.get(MediaXmlReader.THUMBERNAIL_PROPERTY);
					if (UtilMethods.isSet(thumbernailUrl)) {
						downloadImage(thumbernailUrl, "binary2", session);
					}
					return properties;
				} else {
					Logger.info(ReadMediaFiles.class, "File di tipo errato(" + type + ")");
					return null;
				}
			} catch (Exception e) {
				Logger.error(ReadMediaFiles.class, "Error reading file", e);
			}
		} else {
			Logger.warn(ReadMediaFiles.class, "File:" + file.getFilename() + " invalid mimeType:" + file.getMimeType());
		}
		return null;
	}

	public String importImageFile(FileTransfer file, String parentFolderId, HttpSession session) throws DotDataException {
		String response = null;

		String cutStartPath = pAPI.loadProperty(IDeployConst.PLUGIN_ID, "cutStartPath");
		if (cutStartPath == null) {
			cutStartPath = "";
		}
		if (file.getMimeType().equalsIgnoreCase("text/xml")) {
			User loggedUser = null;
			try {
				loggedUser = APILocator.getUserAPI().loadUserById((String) session.getAttribute("USER_ID")) ;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			Map<String, Object> propertiesIt = null;
			Map<String, Object> propertiesEn = null;
			try {
				Document doc = reader.buildDocument( file.getInputStream() );
				propertiesIt = reader.read(doc, "it");
				propertiesEn = reader.read(doc, "en");
				String type = (String) propertiesIt.get(MediaXmlReader.TYPE_PROPERTY);

				if (type != null && type.equalsIgnoreCase(MediaXmlReader.FOTO_TYPE)) {
					String folderName = file.getFilename().substring(0, file.getFilename().lastIndexOf('.'));
					File imageFile = null;
					File thumFile = null;
					String imageUrl = cutStartPath + (String) propertiesIt.get(MediaXmlReader.IMAGE_PROPERTY);
					System.out.println( "=== importImageFile  type: " + type ) ;					
					System.out.println( "importImageFile  imageUrl: " + imageUrl  ) ;					
					System.out.println( "importImageFile  propertiesIt: " + propertiesIt  ) ;					
					
					if (UtilMethods.isSet(imageUrl)) {
						Logger.info(this.getClass(), "Url SCP da cui scaricare:" + imageUrl.replaceAll("\n", "").trim());
						try {
							imageFile = downloadImage(imageUrl.replaceAll("\n", "").trim(), "binary1", session);
						} catch (BadScpExitException e) {
							response = e.getMessage();
							Logger.error(ReadMediaFiles.class, response, e);
						}
						propertiesIt.put("binary1", imageFile);
						propertiesEn.put("binary1", imageFile);
					}
					String thumbnailUrl = cutStartPath + (String) propertiesIt.get(MediaXmlReader.THUMBERNAIL_PROPERTY);
					System.out.println( "thumbnailUrl  " + thumbnailUrl ) ;					
					if (UtilMethods.isSet(thumbnailUrl)) {
						System.out.println( "Eseguo il download img   " + thumbnailUrl ) ;	
						thumFile = downloadImage(thumbnailUrl.replaceAll("\n", "").trim(), "binary2", session);
						propertiesIt.put("binary2", thumFile);
						propertiesEn.put("binary2", thumFile);
					}
					String identifier = fotoImporter.importFoto(loggedUser,  parentFolderId, folderName, propertiesIt, propertiesEn);
					response = "Foto (" + identifier + ") \"" + propertiesIt.get("titolo") + "\" importata";
					Logger.info(ReadMediaFiles.class, response);

				} else {
					response = "File di tipo errato(" + type + ")";
					Logger.info(ReadMediaFiles.class, response);
				}
			} catch (FileAssetValidationException e) {
				if (e.getMessage().equalsIgnoreCase("message.contentlet.fileasset.filename.already.exists")) {
					response = "Content already exist!";
					Logger.error(ReadMediaFiles.class, response, e);
				}
			} catch (BadScpExitException e) {
				response = e.getMessage();
				Logger.error(ReadMediaFiles.class, response, e);
			} catch (Exception e) {
				response = "Error reading file";
				Logger.error(ReadMediaFiles.class, response, e);
			}
		} else {
			response = "File:" + file.getFilename() + " invalid mimeType:" + file.getMimeType();
			Logger.warn(ReadMediaFiles.class, response);
		}

		return response;

	}

	public Map<String, Object> readVideoFile(FileTransfer file, String language, HttpSession session) {
		Map<String, Object> properties = null;
		if (file.getMimeType().equalsIgnoreCase("text/xml")) {
			try {
				if (language != null) {
					properties = reader.read(file.getInputStream(), language.toLowerCase());
				} else {
					properties = reader.read(file.getInputStream());
				}
				String type = (String) properties.get(MediaXmlReader.TYPE_PROPERTY);
				if (type != null && ( type.equalsIgnoreCase(MediaXmlReader.VIDEO_TYPE)  ||   type.equalsIgnoreCase(MediaXmlReader.AUDIO_TYPE )) ){
					Logger.info(ReadMediaFiles.class , "File " + file.getFilename() +"letto! Tipo File : " + type );
					String filename = file.getFilename();
					String vid = filename.substring(0, filename.lastIndexOf("."));
					properties.put("vid", vid);
					return properties;
				} else {
					Logger.info(ReadMediaFiles.class, "File di tipo errato(" + type + ")");
					return null;
				}
			} catch (IOException e) {
				Logger.error(ReadMediaFiles.class, "Error reading file", e);
			}
		} else {
			Logger.warn(ReadMediaFiles.class, "File:" + file.getFilename() + " invalid mimeType:" + file.getMimeType());
		}
		return null;
	}

	private File downloadImage(String imagePath, String fieldName, HttpSession session) throws DotDataException, BadScpExitException {
		System.out.println( "downloadImage - INIZIO METODO imagePath : " + imagePath  );		
		String userId = session.getAttribute("USER_ID").toString();
		String fileName = imagePath.substring(imagePath.lastIndexOf('/') + 1);
		System.out.println( "downloadImage - fileName (dopo sub) : " + imagePath  );		
		fileName = ContentletUtil.sanitizeFileName(fileName);
		// fileName=fileName.substring( 0,fileName.lastIndexOf( '.' ) );
		String fName = APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + userId + File.separator + fieldName;
		File tempUserFolder = new File( fName );
		if (!tempUserFolder.exists()){
			tempUserFolder.mkdirs();
		}
		String to = tempUserFolder.getAbsolutePath() + File.separator + fileName;
		System.out.println( "downloadImage - to : " + to  );		
		
		File file = new File(to);
		if (!file.exists()) {
			Logger.info(this.getClass(), "IL FILE "+ to +" NON ESISTE!");
			String format = fileName.substring(fileName.lastIndexOf('.') + 1);
			System.out.println( "downloadImage -  VARIBILI SCP : " );
			System.out.println( "downloadImage -        imagePath " + imagePath  );
			System.out.println( "downloadImage -        hostScp   " + System.getProperty("hostScp")  );
			System.out.println( "downloadImage -        imagePath " + imagePath.substring(0, imagePath.lastIndexOf(format))  );
			System.out.println( "downloadImage -        format    " + format );
			
			
			String from = System.getProperty("hostScp") + imagePath.substring(0, imagePath.lastIndexOf(format)) + format;
			String hostScp = System.getProperty("hostScp").trim();
			
			String userScp = pAPI.loadProperty(IDeployConst.PLUGIN_ID, "userScp").trim();
			int portScp = 22;
			try{				
				portScp = Integer.parseInt(pAPI.loadProperty(IDeployConst.PLUGIN_ID, "portScp").trim());
			}catch (Exception e) {
			}
			System.out.println( "downloadImage -        from    " + from );
			System.out.println( "downloadImage -        to      " + to );
			int returnCode = ScpFrom.fileTransfer(hostScp,userScp, portScp , from, to);
			System.out.println( "downloadImage - RISULTATO SCP returnCode " + returnCode );
			if (returnCode != 0){
				throw new BadScpExitException("SCP exit code: " + returnCode + ". Please control your import xml file");
			}				
			return file;
		}
		Logger.info (this.getClass(), "IL FILE "+ to +" ESISTE!");
		return file;
	}
}
