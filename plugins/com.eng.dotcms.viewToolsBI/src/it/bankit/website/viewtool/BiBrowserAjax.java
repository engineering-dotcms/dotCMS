package it.bankit.website.viewtool;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.browser.ajax.BrowserAjax;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class BiBrowserAjax {

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private IdentifierAPI identAPI = APILocator.getIdentifierAPI();
	//	private String activeHostId = "";
	//	private String activeFolderInode = "";
	//	private List<String> openFolders = new ArrayList<String>();
	//
	//	private String lastSortBy = "name";
	boolean lastSortDirectionDesc = false;

	public Identifier getIdentifierByInode(String childInode) throws DotIdentifierStateException, DotDataException {
		try {
			if(identAPI.loadFromCache( childInode )!=null){
				return identAPI.loadFromCache( childInode );
			} else {
				return identAPI.findFromInode(childInode);
			}
			
		} catch (NumberFormatException e) {
			return new Identifier();
		}
	}

	public String getFolderPath(String inode) throws DotDataException, DotSecurityException, DotRuntimeException, PortalException, SystemException {
		Identifier id = getIdentifierByInode(inode);

		String uri = id.getParentPath()+id.getAssetName();
		return uri;
	}

	public boolean userHasRole(String roleName) throws PortalException, SystemException, DotDataException, DotSecurityException {

		User loggedInUser = null;
		boolean retVal = false;
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		loggedInUser = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());

		if (roleName == null) {
			return retVal;
		}

		List<Role> roles;
		try {
			roles = APILocator.getRoleAPI().loadRolesForUser(loggedInUser.getUserId());
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return retVal;
		}

		for (Role r : roles) {
			if (roleName.equals(r.getName())) {
				retVal = true;
			}
		}
		return retVal;

	}

	private Map<String, Object> folderMap(Folder f) throws DotDataException, DotSecurityException {
		//		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		//		HostAPI hostAPI = APILocator.getHostAPI();
		Map<String, Object> folderMap = new HashMap<String, Object>();
		folderMap.put("type", "folder");
		folderMap.put("name", f.getName());
		folderMap.put("id", f.getInode());
		folderMap.put("inode", f.getInode());
		folderMap.put("defaultFileType", f.getDefaultFileType());
		folderMap.put("title", f.getTitle());
		//	String currentPath = hostAPI.findParentHost(f, userWebAPI.getSystemUser(), false).getHostname();
		String fullPath = APILocator.getIdentifierAPI().find(f).getPath();
		String absolutePath = "/" + f.getName();

		folderMap.put("fullPath", fullPath);
		folderMap.put("absolutePath", absolutePath);
		return folderMap;
	}

	public List<Map<String, Object>> getFolderSubfolders(String folderInode) throws PortalException, SystemException, DotDataException, DotSecurityException {

		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		WebContext ctx = WebContextFactory.get();
		User user = userWebAPI.getLoggedInUser(ctx.getHttpServletRequest());
		Role[] roles = new Role[] {};
		try {
			roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
		} catch (DotDataException e1) {
			Logger.error(BrowserAjax.class, e1.getMessage(), e1);
		}
		FolderAPI folderAPI = APILocator.getFolderAPI();

		//		Identifier id = null;
		
		Folder parentFolder = APILocator.getFolderAPI().find(folderInode, user, false);
		
		List<Folder> folders = folderAPI.findSubFolders(parentFolder, user, false);

		List<Map<String, Object>> foldersToReturn = new ArrayList<Map<String, Object>>(folders.size());

		for (Folder f : folders) {
			List permissions = new ArrayList();
			try {
				permissions = permissionAPI.getPermissionIdsFromRoles(f, roles, user);
			} catch (DotDataException e) {
				Logger.error(this, "Could not load permissions : ", e);
			}
			if (permissions.contains(PERMISSION_READ)) {
				foldersToReturn.add(folderMap(f));
			}
		}
		return foldersToReturn;
	}

}
