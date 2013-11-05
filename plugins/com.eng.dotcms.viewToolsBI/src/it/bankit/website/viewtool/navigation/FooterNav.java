package it.bankit.website.viewtool.navigation;

import it.bankit.website.viewtool.LanguageUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

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
import com.dotmarketing.portlets.links.model.Link;
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

public class FooterNav implements ViewTool {

	private static String MENU_VTL_PATH;
	private static String SHORT_MENU_VTL_PATH;
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private HttpServletRequest request;
	private User user = null;
	private LanguageAPI langAPI = APILocator.getLanguageAPI();

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
		if (ses != null)
			user = (User) ses.getAttribute(WebKeys.CMS_USER);

		java.io.File fileFolder = new java.io.File(MENU_VTL_PATH);
		if (!fileFolder.exists()) {
			fileFolder.mkdirs();
		}
	}
 
	public String createSiteMapMenu(int numberOfLevels, String path, HttpServletRequest request, boolean reverseOrder)
	throws PortalException, SystemException, DotDataException, DotSecurityException {
		String currentPath = path;
		String startFromPath = currentPath.trim();
		if (!startFromPath.endsWith("/"))
			startFromPath = startFromPath.trim() + "/";
		return createSiteMapMenu(numberOfLevels, startFromPath, currentPath, request, reverseOrder);
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

			Logger.debug(FooterNav.class, "FooterNav begins");
			Logger.debug(FooterNav.class, "FooterNav start path=" + startFromPath + "number of levels=" + numberOfLevels);
 
			if ((startFromPath == null) || (startFromPath.length() == 0)) {

				Logger.debug(FooterNav.class, "pagePath=" + currentPath);
				int idx1 = currentPath.indexOf("/");
				int idx2 = currentPath.indexOf("/", idx1 + 1);
				startFromPath = currentPath.substring(idx1, idx2 + 1);

				Logger.debug(FooterNav.class, "path=" + startFromPath);
			}

			Logger.debug(FooterNav.class, "FooterNav hostId=" + host.getIdentifier());

			java.util.List itemsList = new ArrayList();
		 
			String fileName = "";
			boolean fileExists = true;

			java.io.File file = null;
			String menuId = "";

			Folder folder = APILocator.getFolderAPI().findFolderByPath(startFromPath, hostId, user, true);
			fileName = folder.getInode() + "_siteMapLevels_" + numberOfLevels + "_" + reverseOrder + "_" + siteMapIdPrefix + "_static.vtl";
			menuId = String.valueOf(folder.getInode());
			String vpath = MENU_VTL_PATH + fileName;
			file = new java.io.File(vpath);
			String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;

			if( folder != null && UtilMethods.isSet( folder.getInode() )){
				Logger.debug(FooterNav.class, "FooterNav file=" + MENU_VTL_PATH + fileName);
				if (!file.exists()) {
					itemsList = APILocator.getFolderAPI().findMenuItems(folder, orderDirection);
					fileExists = false;
				}
	
				if (fileExists) {
					return filePath;
				} else {

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
							stringbuf.append("<a href=\"" + encodedPath + "\" class=\"parentFolder\">");
							stringbuf.append(UtilHTML.escapeHTMLSpecialChars(parent.getTitle()) + "</a>");
						}
						stringbuf.append("#end");

						// gets menu items for this folder
						Logger.debug(FooterNav.class, "FooterNav number of items=" + itemsList.size());


						stringbuf.append("#set ($navigationItems = $contents.getEmptyList())\n\n");
						// /FIRST LEVEL MENU ITEMS!!!!
						for (Object itemChild : itemsList) {

							if (itemChild instanceof Folder) {

								Folder folderChild = (Folder) itemChild;

								// recursive method here

								stringbuf = buildSubFolderSiteMapMenu(stringbuf, folderChild, numberOfLevels, 1, orderDirection, siteMapIdPrefix);

							}  else if (itemChild instanceof Link) {


								Link link = (Link) itemChild;
								stringbuf.append("#set ($footItem = $contents.getEmptyMap())\n");
								stringbuf.append("#set ($_dummy  = $footItem.put(\"type\", \"" + "LINK" + "\"))\n");
								stringbuf.append("#set ($_dummy  = $footItem.put(\"linkType\", \"" + link.getLinkType() + "\"))\n");
								stringbuf.append("#set ($_dummy  = $footItem.put(\"name\", \"" + UtilHTML.escapeHTMLSpecialChars(link.getTitle()) + "\"))\n");
								stringbuf.append("#set ($_dummy  = $footItem.put(\"url\", \"" + link.getUrl() + "\"))\n");
								stringbuf.append("#set ($_dummy  = $footItem.put(\"uri\", \"" + link.getURI() + "\"))\n");
								stringbuf.append("#set ($_dummy  = $footItem.put(\"path\", \"" 
										+ (startFromPath.substring(1)+UtilHTML.escapeHTMLSpecialChars(new LanguageUtil().convertPath(link.getTitle()))).replace("/", ".") + "\"))\n");
								stringbuf.append("#set ($_dummy  = $footItem.put(\"linkExt\", \"" + link.getProtocal()+link.getUrl() + "\"))\n");
								stringbuf.append("#set ($_dummy = $navigationItems.add($footItem))\n\n");
							}
						}

					}
					// Specifying explicitly a proper character set encoding
					fo = new FileOutputStream(file);
					OutputStreamWriter out = new OutputStreamWriter(fo, UtilMethods.getCharsetConfiguration());

					out.write(stringbuf.toString());
					out.flush();
					out.close();

					Logger.debug(FooterNav.class, "End of FooterNav" + filePath);

					return filePath;
				}
			}else {
				// Specifying explicitly a proper character set encoding
				stringbuf.append("#set ($navigationItems = $contents.getEmptyList())\n\n") ;
						fo = new FileOutputStream(file);
				OutputStreamWriter out = new OutputStreamWriter(fo, UtilMethods.getCharsetConfiguration());
				out.write(stringbuf.toString());
				
				out.flush();
				out.close();

				Logger.debug(FooterNav.class, "End of FooterNav" + filePath);
				return filePath;
			}
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

		String thisFolderPath = APILocator.getIdentifierAPI().loadFromCache( thisFolder.getIdentifier() ).getPath();


		stringbuf.append("#set ($footItem = $contents.getEmptyMap())\n");
		stringbuf.append("#set ($_dummy  = $footItem.put(\"name\", \"" + UtilHTML.escapeHTMLSpecialChars(thisFolder.getName()) + "\"))\n");
		stringbuf.append("#set ($_dummy  = $footItem.put(\"path\", \"" + thisFolderPath + "\"))\n");
		stringbuf.append("#set ($_dummy = $navigationItems.add($footItem))\n\n");
		//stringbuf.append("<a href=" + thisFolderPath + ">" + UtilHTML.escapeHTMLSpecialChars(thisFolder.getName()) + "</a> | ");
		// gets menu items for this folder
	//	java.util.List itemsChildrenList2 = APILocator.getFolderAPI().findMenuItems(thisFolder, orderDirection);

		// do we have any children?
//		boolean nextLevelItems = (itemsChildrenList2.size() > 0 && currentLevel < numberOfLevels);

//		String folderChildPath = thisFolderPath.substring(0, thisFolderPath.length() - 1);
//		folderChildPath = folderChildPath.substring(0, folderChildPath.lastIndexOf("/"));
//
//		Host host = WebAPILocator.getHostWebAPI().findParentHost(thisFolder, user, true);// DOTCMS-4099
//		Identifier id = APILocator.getIdentifierAPI().find(host, thisFolderPath + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));


		return stringbuf;
	}

	public int stringTokenizer(String str, String delim) {

		StringTokenizer st = new StringTokenizer(str, delim);
		return st.countTokens();

	}

}
