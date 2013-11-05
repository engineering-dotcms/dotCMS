package it.bankit.website.viewtool.navigation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.viewtools.NavigationWebAPI;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class SelettoreNav implements ViewTool{

	private static String MENU_VTL_PATH;
	private static String SHORT_MENU_VTL_PATH;
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private HttpServletRequest request;
	private User user = null;
	private String language = null;
	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	Language lang = null;

	public int formCount = 0;

	static {
		String velocityRootPath = ConfigUtils.getDynamicVelocityPath() + java.io.File.separator;
		MENU_VTL_PATH = velocityRootPath + "menus" + java.io.File.separator;
		SHORT_MENU_VTL_PATH = ConfigUtils.getDynamicContentPath() + java.io.File.separator + "velocity" + java.io.File.separator;
	}

	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		HttpSession ses = request.getSession(false);
		if (ses != null) {
			user = (User) ses.getAttribute(WebKeys.CMS_USER);
		}
		if(language == null) {
			Object	languageID = (Object)request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
			if(languageID!= null  ) {
				language = languageID.toString();
			}
		}
		if (language == null) {
			language = String.valueOf( langAPI.getDefaultLanguage().getId() );
		}
		lang = langAPI.getLanguage(language);

		java.io.File fileFolder = new java.io.File(MENU_VTL_PATH);
		if (!fileFolder.exists()) {
			fileFolder.mkdirs();
		}
	}

	public String createSiteMapMenu(int numberOfLevels, String path, HttpServletRequest request, boolean reverseOrder) throws PortalException, SystemException, DotDataException, DotSecurityException
	{
		String currentPath = path;
		String startFromPath = currentPath.trim();
		if(!startFromPath.endsWith("/")){
			startFromPath = startFromPath.trim()+"/";
		}
		return createSiteMapMenu(numberOfLevels, startFromPath,currentPath,request,reverseOrder);
	}

	private String createSiteMapMenu(int numberOfLevels, String startFromPath, String currentPath, HttpServletRequest request,
			boolean reverseOrder) throws PortalException, SystemException, DotDataException, DotSecurityException {

		String siteMapIdPrefix = "";
		if (request.getAttribute("siteMapIdPrefix") != null) {
			siteMapIdPrefix = (String) request.getAttribute("siteMapIdPrefix") + "_";
		}

		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		String hostId = host.getIdentifier();
		StringBuffer stringbuf = new StringBuffer();
		FileOutputStream fo = null;

		int orderDirection = 1;
		if (reverseOrder) {
			orderDirection = -1;
		}
		try {

			Logger.debug(SelettoreNav.class, "\n\nSelettoreNav begins");
			Logger.debug(SelettoreNav.class, "SelettoreNav start path=" + startFromPath);
			Logger.debug(SelettoreNav.class, "SelettoreNav number of levels=" + numberOfLevels);

			if ((startFromPath == null) || (startFromPath.length() == 0)) {
				Logger.debug(SelettoreNav.class, "pagePath=" + currentPath);
				int idx1 = currentPath.indexOf("/");
				int idx2 = currentPath.indexOf("/", idx1 + 1);

				startFromPath = currentPath.substring(idx1, idx2 + 1);

				Logger.debug(SelettoreNav.class, "path=" + startFromPath);
			}

			Logger.debug(SelettoreNav.class, "SelettoreNav hostId=" + host.getIdentifier());

			java.util.List itemsList = new ArrayList();
			String fileName = "";
			boolean fileExists = true;

			java.io.File file = null;
			String menuId = "";
			Folder folder = APILocator.getFolderAPI().findFolderByPath( startFromPath, hostId, user, true );		 

			fileName = folder.getInode() + "_selettoreNavLevels_" + numberOfLevels + "_" + reverseOrder + "_" + siteMapIdPrefix + "_static.vtl";
			menuId = String.valueOf(folder.getInode());
			String vpath = MENU_VTL_PATH + fileName;
			file = new java.io.File(vpath);
			 
			Logger.debug(SelettoreNav.class, "SelettoreNav file=" + MENU_VTL_PATH + fileName);


			itemsList = APILocator.getFolderAPI().findMenuItems(folder, orderDirection);				 
	 		fileExists = false;

			String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;


			if (itemsList.size() > 0) {

				stringbuf.append("#if($EDIT_MODE)\n");
				stringbuf.append("<form action=\"${directorURL}\" method=\"post\" name=\"form_menu_" + menuId + "\" id=\"form_menu_" + menuId + "\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"cmd\" value=\"orderMenu\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"path\" value=\"" + startFromPath + "\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"hostId\" value=\"" + hostId + "\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"pagePath\" value=\"$VTLSERVLET_URI\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"referer\" value=\"$VTLSERVLET_URI\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"startLevel\" value=\"1\">\n");
				stringbuf.append("<input type=\"hidden\" name=\"depth\" value=\"1\">\n");
				stringbuf.append("<div class=\"dotMenuReorder\">\n");
				stringbuf.append("<a href=\"javascript:document.getElementById('form_menu_" + menuId + "').submit();\">");
				stringbuf.append("</a></div>\n");
				stringbuf.append("</form>");
				stringbuf.append("#end \n");

				stringbuf.append("#if($addParent && $addParent == true)");
				Folder parent = APILocator.getFolderAPI().findFolderByPath(currentPath, hostId, user, true);
				if (InodeUtils.isSet(parent.getInode())) {
					String encodedPath = UtilMethods.encodeURIComponent(APILocator.getIdentifierAPI().find(parent).getPath());
					stringbuf.append("#set($parentLink = '" + encodedPath + "')");
					stringbuf.append("#set($parentName = '" + UtilMethods.encodeURIComponent(UtilHTML.escapeHTMLSpecialChars(parent.getTitle())) + "')");
					stringbuf.append("<li><a href=\"" + encodedPath + "\" class=\"parentFolder\">");
 					stringbuf.append(UtilHTML.escapeHTMLSpecialChars(langAPI.getStringKey(lang, parent.getTitle())) + "</a>");
				}
				stringbuf.append("#end");

				// adding home folder


				// gets menu items for this folder
				Logger.debug(SelettoreNav.class, "SelettoreNav number of items=" + itemsList.size());

				// /FIRST LEVEL MENU ITEMS!!!!
				//int countItem = -1;
				stringbuf.append("#set ($navigationItems = $contents.getEmptyList())\n\n");

				for (Object itemChild : itemsList) {

					//countItem+=1;
					if (itemChild instanceof Folder) {
						Folder folderChild = (Folder) itemChild;						// recursive method here							
						stringbuf = buildSubFolderSiteMapMenu(stringbuf, folderChild, numberOfLevels, 1, orderDirection, siteMapIdPrefix);	
					} 
				}

			}

			// Specifying explicitly a proper character set encoding
			fo = new FileOutputStream(file);
			OutputStreamWriter out = new OutputStreamWriter(fo, UtilMethods.getCharsetConfiguration());

			if (stringbuf.length() == 0) {
				stringbuf.append("#if($EDIT_MODE)No menu items found#{end}");
			}

			out.write(stringbuf.toString());
			out.flush();
			out.close();

			Logger.debug(SelettoreNav.class, "End of SelettoreNav" + filePath);
			return filePath;


		} catch (Exception e) {
			// Clear the string buffer, and insert only the main hyperlink text
			// to it.
			// Ignore the embedded links.
			stringbuf.delete(0, stringbuf.length());
			Logger.error(NavigationWebAPI.class, e.getMessage(), e);
		} finally {
			if (fo != null)
				try {
					fo.close();
				} catch (IOException e) {
					Logger.error(NavigationWebAPI.class, e.getMessage(), e);
				}
		}
		return "";
	}



	private StringBuffer buildSubFolderSiteMapMenu(StringBuffer stringbuf, Folder thisFolder, int numberOfLevels, int currentLevel, int orderDirection,
			String menuIdPrefix) throws DotDataException, DotSecurityException {
		String thisFolderPath = APILocator.getIdentifierAPI().find(thisFolder).getPath();
		stringbuf.append("#set ($selItem = $contents.getEmptyMap())\n");
		stringbuf.append("#set ($_dummy  = $selItem.put(\"name\", \"" + UtilHTML.escapeHTMLSpecialChars(thisFolder.getName()) + "\"))\n");
		stringbuf.append("#set ($_dummy  = $selItem.put(\"path\", \"" + thisFolderPath + "\"))\n");
		stringbuf.append("#set ($_dummy  = $selItem.put(\"title\", \"" + thisFolder.getTitle() + "\"))\n");
		stringbuf.append("#set ($_dummy = $navigationItems.add($selItem))\n\n");

//		//stringbuf.append("<li><a href=" + thisFolderPath + ">$text.get(" +thisFolder.getName()+")</a></li> ");
//		// gets menu items for this folder
//		java.util.List itemsChildrenList2 = APILocator.getFolderAPI().findMenuItems(thisFolder, orderDirection);
//
//		// do we have any children?
//		boolean nextLevelItems = (itemsChildrenList2.size() > 0 && currentLevel < numberOfLevels);
//
//		String folderChildPath = thisFolderPath.substring(0, thisFolderPath.length() - 1);
//		folderChildPath = folderChildPath.substring(0, folderChildPath.lastIndexOf("/"));
//
//		Host host = WebAPILocator.getHostWebAPI().findParentHost(thisFolder, user, true);// DOTCMS-4099
//		Identifier id = APILocator.getIdentifierAPI().find(host, thisFolderPath + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));


		return stringbuf;
	}



}
