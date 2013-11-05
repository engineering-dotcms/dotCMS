package it.bankit.website.viewtool.navigation;

import it.bankit.website.util.HtmlUtil;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.util.AssetsComparator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class SiteMapNav extends MenuNav {

	@Override
	public void init(Object obj) {
		super.init(obj);
	}

	public String createMenuByDepth(int startFromLevel, int maxDepth, HttpServletRequest request) throws Exception {
		String currentPath = request.getRequestURI();
		StringTokenizer st = new StringTokenizer(currentPath, "/");
		int i = 1;
		StringBuffer myPath = new StringBuffer("/");
		boolean rightLevel = false;
		while (st.hasMoreTokens()) {
			if (i++ >= startFromLevel) {
				rightLevel = true;
				break;
			}
			String myToken = st.nextToken();
			if (!st.hasMoreTokens())
				break;
			myPath.append(myToken);
			myPath.append("/");

		}
		String menuString = (rightLevel ? buildMenuItems(myPath.toString(), maxDepth, request) : "");
		java.io.File file;
		file = new java.io.File(SHORT_MENU_VTL_PATH + menuString);
		if (!file.exists()) {
			menuString = "";
		}
		return menuString;
	}

	@SuppressWarnings("unchecked")
	public String buildMenuItems(String startFromPath, int numberOfLevels, HttpServletRequest request) throws PortalException, SystemException, DotDataException, DotSecurityException {
		String currentPath = request.getRequestURI();
		Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
		String hostId = host.getIdentifier();
		StringBuffer stringbuf = new StringBuffer();

		try {

			Logger.debug(SiteMapNav.class, "SiteMapNav :: SiteMapNav [INIT] hostId=" + host.getIdentifier());
			Logger.debug(SiteMapNav.class, "SiteMapNav :: SiteMapNav start path=" + startFromPath + " number of levels= " + numberOfLevels);
			if ((startFromPath == null) || (startFromPath.length() == 0)) {
				int idx1 = currentPath.indexOf("/");
				int idx2 = currentPath.indexOf("/", idx1 + 1);
				startFromPath = currentPath.substring(idx1, idx2 + 1);
				Logger.debug(SiteMapNav.class, "path=" + startFromPath);
			}

			
			java.util.List itemsList = new ArrayList<Inode>();

			String folderPath = "";
			String fileName = "";
			boolean fileExists = true;
			java.io.File file = null;

			
			String language = ((Object) request.getSession().getAttribute("com.dotmarketing.htmlpage.language")).toString();

			fileName = hostId + "sitemap_levels" + startFromPath.replace("/", "_").replace(".", "__").trim() + "_" + numberOfLevels + "_Lang_" + language + "_static.vtl";
			file = new java.io.File(MENU_VTL_PATH + fileName);
			
			if (!file.exists() || file.length() == 0) {
				itemsList = (List<Folder>) APILocator.getFolderAPI().findSubFolders(host, true);
				Folder folderMedia = APILocator.getFolderAPI().findFolderByPath("/media", hostId, user, true);
				if (folderMedia != null && UtilMethods.isSet(folderMedia.getInode())) {
					itemsList.add(folderMedia);
				}
				folderPath = startFromPath;
				fileExists = false;
			}

			// else {
			//
			//
			// fileName = hostId + "sitemap__levels" +
			// startFromPath.replace("/", "_").replace(".", "__").trim() + "_" +
			// numberOfLevels + "_Lang_" + language + "_static.vtl";
			// file = new java.io.File(MENU_VTL_PATH + fileName);
			// Logger.debug(SiteMapNav.class, "SiteMapNav :: SiteMapNav file=" +
			// MENU_VTL_PATH + fileName);
			//
			// if (!file.exists() || file.length() == 0) {
			// file.createNewFile();
			// itemsList = cUtil.getMenuItems(folder, 1, language,false);
			// Folder folderMedia =
			// APILocator.getFolderAPI().findFolderByPath("/media", hostId,
			// user, true);
			// if( folderMedia != null && UtilMethods.isSet(
			// folderMedia.getInode() ) ){
			// itemsList.add(folderMedia);
			// }
			//
			// folderPath = startFromPath;
			// fileExists = false;
			// }
			// }

			// Comparator comparator = new AssetsComparator(1);
			// Collections.sort(itemsList, comparator);

			String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;
			if (fileExists) {
				return filePath;
			} else {

				if (itemsList.size() > 0) {

					stringbuf.append("#set ($siteMapNavigationItems = $contents.getEmptyList())\n\n");
					Logger.debug(SiteMapNav.class, "SiteMapNav :: SiteMapNav number of items=" + itemsList.size());
					String submenu;
					String submenuName;
					// /FIRST LEVEL MENU ITEMS!!!!
					boolean isLastItem = false;
					boolean isFirstItem = true;
					int index = 0;
					for (Object itemChild : itemsList) {
						index++;

						if (index == itemsList.size()) {
							isLastItem = true;
						}

						if (index > 1) {
							isFirstItem = false;
						}

						if (itemChild instanceof Folder) {

							Folder folderChild = (Folder) itemChild;
							if (folderChild.isShowOnMenu()) {
								submenuName = "_" + folderChild.getName().replace(" ", "").replace(".", "__").trim();
								submenu = getSubFolderMenuItems(folderChild, submenuName, numberOfLevels, 1, isFirstItem, isLastItem);

								stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"FOLDER\"))\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(folderChild.getTitle()) + "\"))\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"" + folderChild.getName() + "\"))\n");
								// stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \""
								// +
								// APILocator.getIdentifierAPI().loadFromCache(folderChild).getPath()
								// + "\"))\n\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \"" + wUtil.getFolderPath(folderChild) + "\"))\n\n");

								stringbuf.append(submenu + "\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"submenu\", $" + "_" + submenuName + "))\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", " + isFirstItem + "))\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", " + isLastItem + "))\n");
								stringbuf.append("#set ($_dummy = $siteMapNavigationItems.add($menuItem))\n\n");

								stringbuf.append("#set ($" + "_" + submenuName + " = $contents.getEmptyList())\n");

							} else if (itemChild instanceof Link) {
								Link link = (Link) itemChild;
								if (link.getLinkType().equals(LinkType.CODE.toString())) {
									stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"LINK\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('"
											+ UtilMethods.espaceVariableForVelocity(link.getLinkCode()) + "'), $velocityContext)))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"linkType\", \"CODE\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", " + isFirstItem + "))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", " + isLastItem + "))\n");
									stringbuf.append("#set ($_dummy = $siteMapNavigationItems.add($menuItem))\n\n");
								} else {
									stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"LINK\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"" + link.getUrl() + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"protocal\", \"" + link.getProtocal() + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"target\", \"" + link.getTarget() + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(link.getTitle()) + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", " + isFirstItem + "))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", " + isLastItem + "))\n");
									stringbuf.append("#set ($_dummy = $siteMapNavigationItems.add($menuItem))\n\n");
								}
							} else if (itemChild instanceof HTMLPage) {
								HTMLPage htmlpage = (HTMLPage) itemChild;

								stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"HTMLPAGE\"))\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"" + htmlpage.getPageUrl() + "\"))\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \"" + folderPath + "\"))\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(htmlpage.getTitle()) + "\"))\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", " + isFirstItem + "))\n");
								stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", " + isLastItem + "))\n");
								stringbuf.append("#set ($_dummy = $siteMapNavigationItems.add($menuItem))\n\n");
							} else if (itemChild instanceof IFileAsset) {
								IFileAsset fileItem = (IFileAsset) itemChild;

								if (fileItem.isWorking() && !fileItem.isDeleted()) {
									stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"FILE\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"" + fileItem.getFileName() + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \"" + folderPath + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(fileItem.getTitle()) + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", " + isFirstItem + "))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", " + isLastItem + "))\n");
									stringbuf.append("#set ($_dummy = $siteMapNavigationItems.add($menuItem))\n\n");
								}
							} else if (itemChild instanceof com.dotmarketing.portlets.contentlet.business.Contentlet) {

								Contentlet contentletItem = (Contentlet) itemChild;
								if (contentletItem.isWorking()) {

									stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"CONTENTLET\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \"" + folderPath + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"identificativo\", \"" + contentletItem.getStringProperty("identificativo") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"resourceToLink\", \"" + contentletItem.getStringProperty("resourceToLink") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"linkType\", \"" + contentletItem.getStringProperty("linkType") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"sommario\", \"" + contentletItem.getStringProperty("sommario") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"idAllegato\", \"" + contentletItem.getStringProperty("allegato") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"linkInterno\", \"" + contentletItem.getStringProperty("linkInterno") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"linkEsterno\", \"" + contentletItem.getStringProperty("linkEsterno") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"mostraTitolo\", \"" + contentletItem.getStringProperty("mostraTitolo") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"showOnMenu\", \"" + contentletItem.getStringProperty("showOnMenu") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"" + HtmlUtil.cleanText(contentletItem.getTitle()) + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"" + HtmlUtil.cleanText(contentletItem.getTitle()) + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"titoloLungo\", \"" + contentletItem.getStringProperty("titoloLungo") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"identificativo\", \"" + contentletItem.getStringProperty("identificativo") + "\"))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", " + isFirstItem + "))\n");
									stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", " + isLastItem + "))\n");
									stringbuf.append("#set ($_dummy = $siteMapNavigationItems.add($menuItem))\n\n");
								}
							}
						}
					}
					stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
				} else {
					stringbuf.append("#set ($siteMapNavigationItems = $contents.getEmptyList())\n\n");
				}

				if (stringbuf.toString().getBytes().length > 0) {
					// Specifying explicitly a proper character set encoding
					FileOutputStream fo = new FileOutputStream(file);
					OutputStreamWriter out = new OutputStreamWriter(fo, UtilMethods.getCharsetConfiguration());
					out.write(stringbuf.toString());
					out.flush();
					out.close();
					fo.close();
				} else {
					Logger.debug(SiteMapNav.class, "SiteMapNav :: Error creating static menu!!!!!");
				}

				Logger.debug(SiteMapNav.class, "SiteMapNav :: End of SiteMapNav" + filePath);

				return filePath;
			}
		} catch (Exception e) {
			// Clear the string buffer, and insert only the main hyperlink text
			// to it.
			// Ignore the embedded links.
			stringbuf.delete(0, stringbuf.length());
			Logger.error(SiteMapNav.class, e.getMessage(), e);

		}
		return "";
	}

	@SuppressWarnings("unchecked")
	private String getSubFolderMenuItems(Folder thisFolder, String submenuName, int numberOfLevels, int currentLevel, boolean isFirstItem, boolean isLastItem) throws DotStateException, DotDataException,
			DotSecurityException {
		StringBuffer stringbuf = new StringBuffer();
		stringbuf.append("#set ($" + "_" + submenuName + " = $contents.getEmptyList())\n\n");

		String language = ((Object) request.getSession().getAttribute("com.dotmarketing.htmlpage.language")).toString();

		// gets menu items for this folder
		java.util.List itemsChildrenList2 = new ArrayList();

		try {
			itemsChildrenList2 = cUtil.getMenuItems(thisFolder, 1, language, false);

			Comparator comparator = new AssetsComparator(1);
			Collections.sort(itemsChildrenList2, comparator);

		} catch (Exception e1) {
			Logger.error(SiteMapNav.class, e1.getMessage(), e1);
		}

		String folderPath = "";
		try {
			// folderPath =
			// APILocator.getIdentifierAPI().loadFromCache(thisFolder).getPath();
			folderPath = wUtil.getFolderPath(thisFolder);
		} catch (Exception e1) {
			Logger.error(SiteMapNav.class, e1.getMessage(), e1);
		}
		String folderChildPath = folderPath.substring(0, folderPath.length() - 1);
		folderChildPath = folderChildPath.substring(0, folderChildPath.lastIndexOf("/"));

		if (currentLevel < numberOfLevels) {

			String submenu;
			String subSubmenuName;
			isLastItem = false;
			isFirstItem = true;
			int index = 0;
			for (Object childChild2 : itemsChildrenList2) {
				index++;

				// Check if is last item
				if (index == itemsChildrenList2.size()) {
					isLastItem = true;
				}
				// Check if is first item
				if (index > 1) {
					isFirstItem = false;
				}

				if (childChild2 instanceof Folder) {
					Folder folderChildChild2 = (Folder) childChild2;
					String folderChildPath2 = "";
					try {
						// folderChildPath2 =
						// APILocator.getIdentifierAPI().loadFromCache(folderChildChild2).getPath();
						folderChildPath2 = wUtil.getFolderPath(folderChildChild2);
					} catch (Exception e) {
						Logger.error(SiteMapNav.class, e.getMessage(), e);
					}

					Logger.debug(this, "folderChildChild2= " + folderChildChild2.getTitle() + " currentLevel=" + currentLevel + " numberOfLevels=" + numberOfLevels);

					if (currentLevel <= numberOfLevels) {
						subSubmenuName = folderChildChild2.getName().replace(" ", "").replace(".", "__").trim();

						submenu = getSubFolderMenuItems(folderChildChild2, subSubmenuName, numberOfLevels, currentLevel + 1, isFirstItem, isLastItem);

						stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"FOLDER\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(folderChildChild2.getTitle()) + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"name\", \"" + folderChildChild2.getName() + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"path\", \"" + folderChildPath2 + "\"))\n\n");
						stringbuf.append(submenu + "\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"submenu\", $" + "_" + subSubmenuName + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
						stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");
						stringbuf.append("#set ($" + "_" + subSubmenuName + " = $contents.getEmptyList())\n");
					} else {
						stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"HTMLPAGE\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"path\", \"" + folderChildPath2 + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(folderChildChild2.getTitle()) + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
						stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");
					}
				} else if (childChild2 instanceof Link) {
					if (((Link) childChild2).isWorking() && !((Link) childChild2).isDeleted()) {
						Link link = (Link) childChild2;
						if (link.getLinkType().equals(LinkType.CODE.toString())) {
							stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"LINK\"))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"path\", $UtilMethods.evaluateVelocity($UtilMethods.restoreVariableForVelocity('"
									+ UtilMethods.espaceVariableForVelocity(link.getLinkCode()) + "'), $velocityContext)))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"linkType\", \"CODE\"))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
							stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");
						} else {
							stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"LINK\"))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"name\", \"" + link.getUrl() + "\"))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"protocal\", \"" + link.getProtocal() + "\"))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"target\", \"" + link.getTarget() + "\"))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(link.getTitle()) + "\"))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
							stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
							stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");
						}
					}
				} else if (childChild2 instanceof HTMLPage) {
					HTMLPage htmlpage = (HTMLPage) childChild2;
					if (htmlpage.isWorking() && !htmlpage.isDeleted()) {
						stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"HTMLPAGE\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"name\", \"" + htmlpage.getPageUrl() + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"path\", \"" + folderPath + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(htmlpage.getTitle()) + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
						stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");
					}
				} else if (childChild2 instanceof IFileAsset) {
					IFileAsset fileItem = (IFileAsset) childChild2;
					if (fileItem.isWorking() && !fileItem.isDeleted()) {
						stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"FILE\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"name\", \"" + fileItem.getFileName() + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"path\", \"" + folderPath + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(fileItem.getTitle()) + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
						stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");
					}
				} else if (childChild2 instanceof Contentlet) {

					Contentlet contentletItem = (Contentlet) childChild2;

					stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"CONTENTLET\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"path\", \"" + folderPath + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"linkType\", \"" + HtmlUtil.cleanText("linkType") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"idAllegato\", \"" + contentletItem.getStringProperty("allegato") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"linkInterno\", \"" + contentletItem.getStringProperty("linkInterno") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"resourceToLink\", \"" + contentletItem.getStringProperty("resourceToLink") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"linkEsterno\", \"" + contentletItem.getStringProperty("linkEsterno") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"mostraTitolo\", \"" + contentletItem.getStringProperty("mostraTitolo") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"showOnMenu\", \"" + contentletItem.getStringProperty("showOnMenu") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"name\", \"" + HtmlUtil.cleanText(contentletItem.getTitle()) + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"title\", \"" + HtmlUtil.cleanText(contentletItem.getTitle()) + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"titoloLungo\", \"" + HtmlUtil.cleanText(contentletItem.getStringProperty("titoloLungo")) + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem.put(\"identificativo\", \"" + contentletItem.getStringProperty("identificativo") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
					stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");

				}
			}
			stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
		}
		return stringbuf.toString();
	}

}