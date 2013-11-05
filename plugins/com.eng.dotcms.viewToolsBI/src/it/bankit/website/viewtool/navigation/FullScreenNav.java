package it.bankit.website.viewtool.navigation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.AssetsComparator;
import com.dotmarketing.util.Config;
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

public class FullScreenNav implements ViewTool {

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

	public String createSiteMapMenu(int startFromLevel, int numberOfLevels, String path, HttpServletRequest request, boolean addHome, boolean reverseOrder)
			throws PortalException, SystemException, DotDataException, DotSecurityException {
	
		String currentPath = path;
		String startFromPath = currentPath.trim();
		if (!startFromPath.endsWith("/"))
			startFromPath = startFromPath.trim() + "/";
		return createSiteMapMenu(startFromLevel, numberOfLevels, startFromPath, currentPath, addHome, request, reverseOrder);
	}

	private String createSiteMapMenu(int startFromLevel, int numberOfLevels, String startFromPath, String currentPath, boolean addHome, HttpServletRequest request,
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

			Logger.debug(FullScreenNav.class, "\n\nFullScreenNav begins");
			Logger.debug(FullScreenNav.class, "FullScreenNav start path=" + startFromPath);
			Logger.debug(FullScreenNav.class, "FullScreenNav number of levels=" + numberOfLevels);

			if ((startFromPath == null) || (startFromPath.length() == 0)) {

				Logger.debug(FullScreenNav.class, "pagePath=" + currentPath);

				int idx1 = currentPath.indexOf("/");
				int idx2 = currentPath.indexOf("/", idx1 + 1);

				startFromPath = currentPath.substring(idx1, idx2 + 1);

				Logger.debug(FullScreenNav.class, "path=" + startFromPath);
			}

			Logger.debug(FullScreenNav.class, "FullScreenNav hostId=" + host.getIdentifier());

			java.util.List itemsList = new ArrayList();
			String folderPath = "";
			String fileName = "";
			boolean fileExists = true;

			java.io.File file = null;
			String menuId = "";
			if ("/".equals(startFromPath)) {
				fileName = hostId + "_fullScreenLevels_" + startFromLevel + "_" + numberOfLevels + "_" + reverseOrder + "_" + addHome + "_" + siteMapIdPrefix
						+ "_static.vtl";
				menuId = String.valueOf(hostId);
				String vpath = MENU_VTL_PATH + fileName;
				file = new java.io.File(vpath);
				if (!file.exists() || file.length() == 0) {

					itemsList = APILocator.getFolderAPI().findSubFolders(host, true);
					Comparator comparator = new AssetsComparator(orderDirection);
					Collections.sort(itemsList, comparator);
					for (int i = 1; i < startFromLevel; i++) {
						java.util.List<Inode> itemsList2 = new ArrayList<Inode>();
						for (Object inode : itemsList) {
							if (inode instanceof Folder) {
								itemsList2.addAll(APILocator.getFolderAPI().findMenuItems((Folder) inode, orderDirection));
							}
						}
						itemsList = itemsList2;
					}

					folderPath = startFromPath;
					fileExists = false;
				}

			} else {
				Folder folder = APILocator.getFolderAPI().findFolderByPath(startFromPath, hostId, user, true);
				try {
					Logger.debug(FullScreenNav.class, "FullScreenNav folder=" + APILocator.getIdentifierAPI().find(folder).getPath());
				} catch (Exception e) {/* do nothing */
				}

				fileName = folder.getInode() + "_fullScreenLevels_" + startFromLevel + "_" + numberOfLevels + "_" + reverseOrder + "_" + addHome + "_"
						+ siteMapIdPrefix + "_static.vtl";
				menuId = String.valueOf(folder.getInode());
				String vpath = MENU_VTL_PATH + fileName;
				file = new java.io.File(vpath);
				// file = new
				// java.io.File(Config.CONTEXT.getRealPath(MENU_VTL_PATH +
				// fileName));
				Logger.debug(FullScreenNav.class, "FullScreenNav file=" + MENU_VTL_PATH + fileName);

				if (!file.exists()) {
					itemsList = APILocator.getFolderAPI().findMenuItems(folder, orderDirection);
					for (int i = 1; i < startFromLevel; i++) {
						java.util.List<Inode> itemsList2 = new ArrayList<Inode>();
						for (Object inode : itemsList) {
							if (inode instanceof Folder) {
								itemsList2.addAll(APILocator.getFolderAPI().findMenuItems((Folder) inode, orderDirection));
							}
						}
						itemsList = itemsList2;
					}
					folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
					fileExists = false;
				}
			}
			String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;

			if (false) {
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
						stringbuf.append("<h2><a href=\"" + encodedPath + "\" class=\"parentFolder\">");
						stringbuf.append(UtilHTML.escapeHTMLSpecialChars(parent.getTitle()) + "</a></h2>\n");
					}
					stringbuf.append("#end");

					stringbuf.append("<div id=\"folderId\"><table>\n");

					// gets menu items for this folder
					Logger.debug(FullScreenNav.class, "FullScreenNav number of items=" + itemsList.size());

					// /FIRST LEVEL MENU ITEMS!!!!
					for (Object itemChild : itemsList) {

						if (itemChild instanceof Folder) {

							Folder folderChild = (Folder) itemChild;

							// recursive method here

							stringbuf = buildSubFolderSiteMapMenu(stringbuf, folderChild, numberOfLevels, 1, orderDirection, siteMapIdPrefix);

						}
					}
					stringbuf.append("</table></div>");

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

				Logger.debug(FullScreenNav.class, "End of FullScreenNav" + filePath);

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

		String thisFolderPath = APILocator.getIdentifierAPI().find(thisFolder).getPath();
		stringbuf.append("\t<li id=\"" + menuIdPrefix + thisFolder.getName() + "\">\n");
		// gets menu items for this folder
		java.util.List itemsChildrenList2 = APILocator.getFolderAPI().findMenuItems(thisFolder, orderDirection);

		// do we have any children?
		boolean nextLevelItems = (itemsChildrenList2.size() > 0 && currentLevel < numberOfLevels);

		String folderChildPath = thisFolderPath.substring(0, thisFolderPath.length() - 1);
		folderChildPath = folderChildPath.substring(0, folderChildPath.lastIndexOf("/"));

		Host host = WebAPILocator.getHostWebAPI().findParentHost(thisFolder, user, true);// DOTCMS-4099
		Identifier id = APILocator.getIdentifierAPI().find(host, thisFolderPath + "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));
		if (id != null && InodeUtils.isSet(id.getInode()))
			
		stringbuf.append("<tr><td width=\"350\">" +UtilHTML.escapeHTMLSpecialChars(APILocator.getLanguageAPI().getStringKey(langAPI.getLanguage(String.valueOf(langAPI.getDefaultLanguage().getId())), thisFolder.getTitle()))+
				"</td><td><input onClick=\"getValue(this)\" type=\"radio\" name=\"colonna\" value=\"D\">Destra<input type=\"radio\" name=\"colonna\" value=\"S\">Sinistra</td>\n");
		

		if (currentLevel < numberOfLevels) {

			if (nextLevelItems) {
				stringbuf.append("<ul>\n");
			}

			for (Object childChild2 : itemsChildrenList2) {
				if (childChild2 instanceof Folder) {
					Folder folderChildChild2 = (Folder) childChild2;

					Logger.debug(this, "folderChildChild2= " + folderChildChild2.getTitle() + " currentLevel=" + currentLevel + " numberOfLevels=" + numberOfLevels);
					if (currentLevel <= numberOfLevels) {
						stringbuf = buildSubFolderSiteMapMenu(stringbuf, folderChildChild2, numberOfLevels, currentLevel + 1, orderDirection, menuIdPrefix);
					} else {
					
						stringbuf.append("<tr><td>" + UtilHTML.escapeHTMLSpecialChars(folderChildChild2.getTitle()) + "</td><td>" +
								"<input type=\"radio\" name=\"colonna\" value=\"D\">Destra<input type=\"radio\" name=\"colonna\" value=\"S\">Sinistra</td>\n");

					}
				} 
			}
		}
		if (nextLevelItems) {
			stringbuf.append("</ul>\n");

		}
		stringbuf.append("</li>\n");
		return stringbuf;
	}

}
