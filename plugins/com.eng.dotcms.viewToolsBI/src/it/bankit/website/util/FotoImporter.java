package it.bankit.website.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class FotoImporter {
	private static String templateName = "Dettaglio Foto";
	private User user;
	private User loggedUser;
	private Host host;
	private Structure structure;
	private Structure fileAssetStructure;
	private List<Permission> permissionList;
	private Language it;
	private Language en;
	private Template template;
	private boolean initialized = false;
	private String thumbnailIdentifier = null;
	private boolean resetThId = false;

	private void init() throws Exception {

		user = APILocator.getUserAPI().getSystemUser();
		host = APILocator.getHostAPI().findDefaultHost(user, true);
		structure = StructureCache.getStructureByVelocityVarName("Foto");
		fileAssetStructure = StructureCache.getStructureByVelocityVarName("Fileasset");
		permissionList = APILocator.getPermissionAPI().getPermissions(structure);

		it = APILocator.getLanguageAPI().getDefaultLanguage();
		en = APILocator.getLanguageAPI().getLanguage("en", "US");
		List<Template> templates = APILocator.getTemplateAPI().findTemplatesAssignedTo(host);
		for (Template curTemplate : templates) {
			if (curTemplate.getTitle().equalsIgnoreCase(templateName)) {
				template = curTemplate;
				break;
			}
		}
		initialized = true;
	}

	public String importFoto(User loggedUser, String parentFolderId, String folderName, Map<String, Object> propertiesIt, Map<String, Object> propertiesEn) throws Exception {
		thumbnailIdentifier = null;
		resetThId = false;

		if (loggedUser == null) {
			this.loggedUser = user;
		} else {
			this.loggedUser = loggedUser;
		}

		if (!initialized)
			init();
		thumbnailIdentifier = null;
		Folder folder = findOrCreateFolder(parentFolderId, folderName);
		Contentlet fotoIt = generateContentlet((String) propertiesIt.get("titolo"), folder, it);
		fotoIt = setExtraProperties(fotoIt, propertiesIt, folder, it);
		fotoIt = APILocator.getContentletAPI().checkin(fotoIt, permissionList, loggedUser, true);
		// APILocator.getContentletAPI().publish(fotoIt, user, true);

		String identifier = fotoIt.getIdentifier();

		String titolo = (String) propertiesIt.get("titolo");
		if (propertiesEn.get("titolo") != null) {
			titolo = (String) propertiesEn.get("titolo");
		}

		Contentlet fotoEn = generateContentlet(titolo, folder, en);
		fotoEn = setExtraProperties(fotoEn, propertiesEn, folder, en);
		fotoEn.setIdentifier(identifier);
		fotoEn = APILocator.getContentletAPI().checkin(fotoEn, permissionList, loggedUser, true);
		// APILocator.getContentletAPI().publish(fotoEn, user, true);
		if (fotoEn == null || !UtilMethods.isSet(fotoEn.getInode())) {
			throw new Exception("Errore nell'importazione foto");
		}
		createPageOnFolder(folderName, folder);
		clearTempFiles(propertiesIt);
		return identifier;

	}

	private Folder findOrCreateFolder(String parentFolderId, String folderName) throws Exception {
		Folder parentFolder = APILocator.getFolderAPI().find(parentFolderId, user, false);
		String finalPath = (APILocator.getIdentifierAPI().findFromInode(parentFolder.getInode())).getPath() + folderName;
		if (parentFolder == null || !parentFolderId.equals(parentFolder.getInode())) {
			throw new Exception("Errore nel reecuperare il folder con id:" + parentFolderId);
		}

		Folder folder = APILocator.getFolderAPI().findFolderByPath(finalPath, host, user, true);
		if (folder == null || !InodeUtils.isSet(folder.getInode())) {
			folder = createFolderObject(folderName, escapePath(finalPath), parentFolder);
		}
		return folder;
	}

	private Folder createFolderObject(String name, String title, Folder parent) throws Exception {
		Folder folder = new Folder();
		folder.setName(name);
		folder.setTitle(title);
		folder.setShowOnMenu(false);
		folder.setSortOrder(0);
		folder.setHostId(host.getIdentifier());
		folder.setDefaultFileType(structure.getInode());

		Identifier newIdentifier = APILocator.getIdentifierAPI().createNew(folder, parent);

		if (newIdentifier != null)
			folder.setIdentifier(newIdentifier.getId());
		APILocator.getFolderAPI().save(folder, user, true);
		return folder;
	}

	private Contentlet generateContentlet(String titolo, Folder folder, Language lang) {
		Date data = new Date();
		Contentlet foto = new Contentlet();
		foto.setStructureInode(structure.getInode());
		foto.setHost(host.getIdentifier());
		foto.setFolder(folder.getInode());
		foto.setProperty("hostFolder", folder.getInode());
		foto.setLanguageId(lang.getId());
		foto.setProperty("title", titolo);
		foto.setLastReview(data);
		foto.setDateProperty("timeCreated", data);
		return foto;
	}

	private Contentlet generateThumbnailFileAsset(String titolo, String titoloOrig, Folder folder, Language lang, File thFile) throws IOException, DotContentletStateException, DotStateException,
			DotSecurityException, DotDataException {

		Date data = new Date();
		Contentlet th = new Contentlet();
		th.setStructureInode(fileAssetStructure.getInode());
		th.setHost(host.getIdentifier());
		th.setFolder(folder.getInode());
		th.setProperty("hostFolder", folder.getInode());
		th.setLanguageId(lang.getId());
		th.setProperty("title", titolo);
		th.setProperty("fileName", titolo);
		th.setProperty("description", "Thumbnail dell'immagine " + titoloOrig);
		th.setBinary(FileAssetAPI.BINARY_FIELD, thFile);
		th.setLastReview(data);
		th.setDateProperty("timeCreated", data);
		if (thumbnailIdentifier != null) {
			th.setIdentifier(thumbnailIdentifier);
			resetThId = true;
		}
		try {

			th = APILocator.getContentletAPI().checkin(th, loggedUser, true);

			if (thumbnailIdentifier == null) {
				thumbnailIdentifier = th.getIdentifier();

			}
			if (resetThId) {
				thumbnailIdentifier = null;
				resetThId = false;
			}

			// APILocator.getContentletAPI().publish(th, user, true);

		} catch (Exception e) {
			thumbnailIdentifier = null;
			resetThId = false;
			e.printStackTrace();
		}

		return th;
	}

	private Contentlet setExtraProperties(Contentlet foto, Map<String, Object> properties, Folder f, Language l) throws Exception {
		Logger.error(this.getClass(), "Thumbnail identifier: " + thumbnailIdentifier);
		Logger.error(this.getClass(), "SONO DENTRO EXTRA PROPERTIES IN" + foto.getLanguageId());
		if (properties.containsKey("abstract")) {
			foto.setProperty("sommario", properties.get("abstract"));
		}
		if (properties.containsKey("autore")) {
			foto.setProperty("autoreHw", properties.get("autore"));
		}

		File imageOriginal = null;
		File thumbernailOriginal = null;
		if (properties.containsKey("binary1")) {
			imageOriginal = (File) properties.get("binary1");
		}
		if (properties.containsKey("binary2")) {
			thumbernailOriginal = (File) properties.get("binary2");
		}

		Logger.error(this.getClass(), "Image original esiste?  " + imageOriginal.exists());

		if (imageOriginal != null) {
			File tempFile = new File(imageOriginal.getParent(), UUIDGenerator.generateUuid() + imageOriginal.getName());
			com.liferay.util.FileUtil.copyFile(imageOriginal, tempFile, false);
			foto.setBinary(FileAssetAPI.BINARY_FIELD, tempFile);
			foto.setProperty(FileAssetAPI.FILE_NAME_FIELD, imageOriginal.getName());
		}

		if (thumbernailOriginal != null) {
			File tempFile1 = new File(thumbernailOriginal.getParent(), UUIDGenerator.generateUuid() + thumbernailOriginal.getName());
			File tempFile2 = new File(thumbernailOriginal.getParent().toString().replaceAll("binary2", "binary1"), UUIDGenerator.generateUuid() + thumbernailOriginal.getName());
			com.liferay.util.FileUtil.copyFile(thumbernailOriginal, tempFile1, false);
			com.liferay.util.FileUtil.copyFile(thumbernailOriginal, tempFile2, false);
			foto.setBinary("thumbnail", tempFile1);
			Logger.error(this.getClass(), "NOME ORGINALE: " + thumbernailOriginal.getName());
			generateThumbnailFileAsset(thumbernailOriginal.getName(), imageOriginal.getName(), f, l, tempFile2);
		}

		return foto;
	}

	public HTMLPage createPageOnFolder(String friendlyName, Folder folder) throws DotDataException, DotStateException, DotSecurityException {
		HTMLPage pageCreated = null;
		String path = (APILocator.getIdentifierAPI().findFromInode(folder.getInode())).getPath();
		String pageUrl = "index.html";
		String title = escapePath(path);

		HTMLPage workingPage = APILocator.getHTMLPageAPI().getWorkingHTMLPageByPageURL(pageUrl, folder);
		if (workingPage == null) {
			// User user = APILocator.getUserAPI().getSystemUser();
			HTMLPage htmlPage = new HTMLPage();
			htmlPage.setParent(folder.getInode());
			htmlPage.setFriendlyName(friendlyName);
			htmlPage.setHttpsRequired(false);
			htmlPage.setIDate(new Date());
			htmlPage.setMetadata("");
			htmlPage.setModDate(new Date());
			htmlPage.setModUser(loggedUser.getUserId());
			htmlPage.setOwner(loggedUser.getUserId());
			htmlPage.setPageUrl(pageUrl);
			htmlPage.setRedirect("");
			htmlPage.setShowOnMenu(false);
			htmlPage.setSortOrder(1);
			htmlPage.setStartDate(new Date());
			htmlPage.setTitle(title);
			htmlPage.setType("htmlpage");
			pageCreated = APILocator.getHTMLPageAPI().saveHTMLPage(htmlPage, template, folder, loggedUser, true);

		}
		return pageCreated;
	}
	private void clearTempFiles(Map<String, Object> properties) throws Exception {
		File img = (File) properties.get("binary1");
		if (img != null && img.exists()) {
			if (!img.delete()) {
				throw new Exception("Errore nella cancellazione del file:" + img.getPath());
			}
		}
		File thumb = (File) properties.get("binary2");
		if (thumb != null && thumb.exists()) {
			if (!thumb.delete()) {
				throw new Exception("Errore nella cancellazione del file:" + thumb.getPath());
			}
		}
	}

	public static String escapePath(String path) {
		String pathProcessed = path.trim().replace("/", ".").replace(" ", "-");
		if (pathProcessed.startsWith(".")) {
			pathProcessed = pathProcessed.substring(1);
		}
		if (pathProcessed.endsWith(".")) {
			pathProcessed = pathProcessed.substring(0, pathProcessed.length() - 1);
		}
		return pathProcessed;
	}
}
