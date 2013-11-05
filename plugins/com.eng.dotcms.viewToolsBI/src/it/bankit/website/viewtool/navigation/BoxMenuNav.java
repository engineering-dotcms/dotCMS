package it.bankit.website.viewtool.navigation;

import it.bankit.website.util.AssetsComparator;
import it.bankit.website.util.HtmlUtil;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.ibm.icu.text.SimpleDateFormat;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class BoxMenuNav extends MenuNav {

	SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	

	@Override
	public void init(Object obj) {
		super.init(obj);
	}

	public String createMenuByDepth(int startFromLevel, int maxDepth, HttpServletRequest request) throws JspException, PortalException, SystemException, DotDataException, DotSecurityException {

		String currentPath = request.getRequestURI();

		StringTokenizer st = new StringTokenizer(currentPath, "/");
		int i = 0;
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

			Logger.debug(DetailMenuNav.class, "\n\nBoxMenuNav :: buildMenuItems begins");
			Logger.debug(DetailMenuNav.class, "BoxMenuNav :: buildMenuItems start path=" + startFromPath);
			Logger.debug(DetailMenuNav.class, "BoxMenuNav :: buildMenuItems number of levels=" + numberOfLevels);

			Logger.debug(DetailMenuNav.class, "BoxMenuNav :: buildMenuItems hostId=" + host.getIdentifier());

			java.util.List itemsList = new ArrayList<Inode>();
			java.util.List linkItemList = new ArrayList<Inode>();

			String folderPath = "";
			String fileName = "";
			boolean fileExists = true;

			java.io.File file = null;

			Folder folder = APILocator.getFolderAPI().findFolderByPath(startFromPath, hostId, user, true);
			String language = ((Object) request.getSession().getAttribute("com.dotmarketing.htmlpage.language")).toString();

			if ("/".equals(startFromPath)) {

				fileName = hostId + "_levels" + startFromPath.replace("/", "_").replace(".", "__").trim() + "_" + numberOfLevels + "Lang_" + language + "_static.vtl";
				file = new java.io.File(MENU_VTL_PATH + fileName);

				if (!file.exists() || file.length() == 0) {
					itemsList = cUtil.getMenuItems(folder, 1, language, false);
					folderPath = startFromPath;
					fileExists = false;
				}

			} else {

				try {
					Logger.debug(DetailMenuNav.class, "BoxMenuNav :: buildMenuItems folder=" + APILocator.getIdentifierAPI().find(folder).getPath());
				} catch (Exception e) {/* do Nothing */
				}

				fileName = folder.getInode() + "_levels" + startFromPath.replace("/", "_").replace(".", "__").trim() + "_" + numberOfLevels + "Lang_" + language + "_static.vtl";
				file = new java.io.File(MENU_VTL_PATH + fileName);
				Logger.debug(DetailMenuNav.class, "BoxMenuNav :: buildMenuItems file=" + MENU_VTL_PATH + fileName);

				if (!file.exists() || file.length() == 0) {
					file.createNewFile();
					itemsList = cUtil.getMenuItems(folder, 1, language, false);
					//folderPath = APILocator.getIdentifierAPI().loadFromCache(folder).getPath();
					folderPath = wUtil.getFolderPath(folder);
					fileExists = false;
				}
			}


			Comparator comparator = new AssetsComparator(1);
			Collections.sort(itemsList, comparator);

			String filePath = "dynamic" + java.io.File.separator + "menus" + java.io.File.separator + fileName;
			// if (false) {
			// return filePath;
			// } else {

			if (itemsList.size() > 0) {

				stringbuf.append("#set ($boxitems = $contents.getEmptyList())\n\n");
				// gets menu items for this folder
				Logger.debug(DetailMenuNav.class, "BoxMenuNav :: buildMenuItems number of items=" + itemsList.size());

				String submenu;
				String submenuName;
				// /FIRST LEVEL MENU ITEMS!!!!
				boolean isLastItem = false;
				boolean isFirstItem = true;
				int index = 0;
				for (Object itemChild : itemsList) {
					index++;
					// Check if the item is the last one
					if (index == itemsList.size()) {
						isLastItem = true;
					}
					// Check if the item is the first one
					if (index > 1) {
						isFirstItem = false;
					}

					if (itemChild instanceof Folder) {

						Folder folderChild = (Folder) itemChild;

						submenuName = "_" + folderChild.getName().replace(" ", "").replace(".", "__").trim();
						// recursive method here
						submenu = getSubFolderMenuItems(folderChild, submenuName, numberOfLevels, 1, isFirstItem, isLastItem);
						Boolean subLinkExist = (cUtil.findByStructure(s.getInode(), folderChild, EDIT_OR_PREVIEW_MODE,
								((Object) request.getSession().getAttribute("com.dotmarketing.htmlpage.language")).toString())).size() > 0;

						stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"FOLDER\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(folderChild.getTitle()) + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"" + folderChild.getName() + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"subLink\", \"" + subLinkExist + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \"" + APILocator.getIdentifierAPI().find(folderChild).getPath() + "\"))\n\n");

						stringbuf.append(submenu + "\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"submenu\", $" + "_" + submenuName + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", " + isFirstItem + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", " + isLastItem + "))\n");
						stringbuf.append("#set ($_dummy = $boxitems.add($menuItem))\n\n");

						stringbuf.append("#set ($" + "_" + submenuName + " = $contents.getEmptyList())\n");

					} else if (itemChild instanceof Contentlet) {

						Contentlet contentletItem = (Contentlet) itemChild;

						stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"type\", \"CONTENTLET\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"path\", \"" + folderPath + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"" + contentletItem.getStringProperty("titolo") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"identificativo\", \"" + contentletItem.getStringProperty("identificativo") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"resourceToLink\", \"" + contentletItem.getStringProperty("resourceToLink") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"linkType\", \"" + contentletItem.getStringProperty("linkType") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $mealnuItem.put(\"mostraSommario\", \"" + contentletItem.getStringProperty("mostraSommario") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"sommario\", \"" + contentletItem.getStringProperty("sommario") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"idAllegato\", \"" + contentletItem.getStringProperty("allegato") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"linkInterno\", \"" + contentletItem.getStringProperty("linkInterno") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"linkEsterno\", \"" + contentletItem.getStringProperty("linkEsterno") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"mostraTitolo\", \"" + contentletItem.getStringProperty("mostraTitolo") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"showOnMenu\", \"" + contentletItem.getStringProperty("showOnMenu") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"boxType\", \"" + contentletItem.getStringProperty("boxType") + "\"))\n");
							

						stringbuf.append("#set ($_dummy  = $menuItem.put(\"dataEmanazione\", \"" + sdf.format(contentletItem.getDateProperty("dataEmanazione")).toString() + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"organizzazione\", \"" + contentletItem.getStringProperty("organizzazione") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"ruolo\", \"" + contentletItem.getStringProperty("ruolo") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"autoreRuolo\", \"" + contentletItem.getStringProperty("autoreRuolo") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"evento\", \"" + contentletItem.getStringProperty("evento") + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"luogo\", \"" + contentletItem.getStringProperty("luogo") + "\"))\n");

						stringbuf.append("#set ($_dummy  = $menuItem.put(\"name\", \"" + contentletItem.getTitle() + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"title\", \"" + HtmlUtil.cleanText(contentletItem.getTitle()) + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"isFirstItem\", " + isFirstItem + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem.put(\"isLastItem\", " + isLastItem + "))\n");
						stringbuf.append("#set ($_dummy = $boxitems.add($menuItem))\n\n");

					}
				}
				stringbuf.append("#set ($menuItem = $contents.getEmptyMap())\n");
			} else {
				stringbuf.append("#set ($boxitems = $contents.getEmptyList())\n\n");
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
				Logger.debug(DetailMenuNav.class, "BoxMenuNav :: Error creating static menu!!!!!");
			}

			Logger.debug(DetailMenuNav.class, "BoxMenuNav :: End of buildMenuItems" + filePath);

			return filePath;
			// }
		} catch (Exception e) {
			// Clear the string buffer, and insert only the main hyperlink text
			// to it.
			// Ignore the embedded links.
			stringbuf.delete(0, stringbuf.length());
			Logger.error(DetailMenuNav.class, e.getMessage(), e);

		}
		return "";
	}

	@SuppressWarnings("unchecked")
	private String getSubFolderMenuItems(Folder thisFolder, String submenuName, int numberOfLevels, int currentLevel, boolean isFirstItem, boolean isLastItem) throws DotStateException, DotDataException,
			DotSecurityException {

		String language = ((Object) request.getSession().getAttribute("com.dotmarketing.htmlpage.language")).toString();

		StringBuffer stringbuf = new StringBuffer();
		stringbuf.append("#set ($" + "_" + submenuName + " = $contents.getEmptyList())\n\n");

		// gets menu items for this folder
		java.util.List itemsChildrenList2 = new ArrayList<Inode>();
		try {
			itemsChildrenList2 = cUtil.getMenuItems(thisFolder, 1, language, false);
			// APILocator.getFolderAPI().findMenuItems(thisFolder, user, true);
			// linkItemChilList = cUtil.findByStructure(s.getInode(),
			// thisFolder, EDIT_OR_PREVIEW_MODE,
			// (String)
			// request.getSession().getAttribute("com.dotmarketing.htmlpage.language"));
			// itemsChildrenList2.addAll(cUtil.findByStructure(s.getInode(),
			// thisFolder, EDIT_OR_PREVIEW_MODE,
			// ((Object)
			// request.getSession().getAttribute("com.dotmarketing.htmlpage.language")).toString()));
			// mergeList(itemsChildrenList2, linkItemChilList);

			// itemsChildrenList2.addAll(linkItemChilList);

			Comparator comparator = new AssetsComparator(1);
			Collections.sort(itemsChildrenList2, comparator);

		} catch (Exception e1) {
			Logger.error(DetailMenuNav.class, e1.getMessage(), e1);
		}

		String folderPath = "";
		try {
			//folderPath = APILocator.getIdentifierAPI().loadFromCache(thisFolder).getPath();
			folderPath = wUtil.getFolderPath(thisFolder);
		} catch (Exception e1) {
			Logger.error(DetailMenuNav.class, e1.getMessage(), e1);
		}
		String folderChildPath = folderPath.substring(0, folderPath.length() - 1);
		folderChildPath = folderChildPath.substring(0, folderChildPath.lastIndexOf("/"));

		if (currentLevel < 2) {

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
						folderChildPath2 = APILocator.getIdentifierAPI().find(folderChildChild2).getPath();
					} catch (Exception e) {
						Logger.error(BoxMenuNav.class, e.getMessage(), e);
					}

					Logger.debug(this, "folderChildChild2= " + folderChildChild2.getTitle() + " currentLevel=" + currentLevel + " numberOfLevels=" + numberOfLevels);

					if (currentLevel <= numberOfLevels) {
						subSubmenuName = folderChildChild2.getName().replace(" ", "").replace(".", "__").trim();

						// Boolean subLinkExist =
						// (cUtil.findByStructure(s.getInode(),
						// folderChildChild2, EDIT_OR_PREVIEW_MODE, ((Object)
						// request.getSession()
						// .getAttribute("com.dotmarketing.htmlpage.language")).toString())).size()
						// > 0;

						stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"FOLDER\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(folderChildChild2.getTitle()) + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"name\", \"" + folderChildChild2.getName() + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"path\", \"" + folderChildPath2 + "\"))\n\n");

						if (currentLevel < numberOfLevels) {
							submenu = getSubFolderMenuItems(folderChildChild2, subSubmenuName, numberOfLevels, currentLevel + 1, isFirstItem, isLastItem);
							stringbuf.append(submenu + "\n");
						}

						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"submenu\", $" + "_" + subSubmenuName + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
						stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");
						stringbuf.append("#set ($" + "_" + subSubmenuName + " = $contents.getEmptyList())\n");
					} else {

						boolean subLinkExist = (cUtil.findByStructure(s.getInode(), folderChildChild2, EDIT_OR_PREVIEW_MODE,
								((String) request.getSession().getAttribute("com.dotmarketing.htmlpage.language")).toString())).size() > 0;

						stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"HTMLPAGE\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"path\", \"" + folderChildPath2 + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"title\", \"" + UtilHTML.escapeHTMLSpecialChars(folderChildChild2.getTitle()) + "\"))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"subLink\", " + subLinkExist + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
						stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
						stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");
					}
				} else if (childChild2 instanceof Contentlet) {

					Contentlet contentletItem = (Contentlet) childChild2;

					stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"type\", \"CONTENTLET\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"path\", \"" + folderPath + "\"))\n");

					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"identificativo\", \"" + contentletItem.getStringProperty("identificativo") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"resourceToLink\", \"" + contentletItem.getStringProperty("resourceToLink") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"linkType\", \"" + contentletItem.getStringProperty("linkType") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"mostraSommario\", \"" + contentletItem.getStringProperty("mostraSommario") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"sommario\", \"" + HtmlUtil.cleanText(contentletItem.getStringProperty("sommario")) + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"idAllegato\", \"" + contentletItem.getStringProperty("allegato") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"linkInterno\", \"" + contentletItem.getStringProperty("linkInterno") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"linkEsterno\", \"" + contentletItem.getStringProperty("linkEsterno") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"mostraTitolo\", \"" + contentletItem.getStringProperty("mostraTitolo") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"showOnMenu\", \"" + contentletItem.getStringProperty("showOnMenu") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"boxType\", \"" + contentletItem.getStringProperty("boxType") + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"importante\", \"" + contentletItem.getStringProperty("importante") + "\"))\n");

					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"name\", \"" + HtmlUtil.cleanText(contentletItem.getTitle()) + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"title\", \"" + HtmlUtil.cleanText(contentletItem.getTitle()) + "\"))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isFirstItem\", " + isFirstItem + "))\n");
					stringbuf.append("#set ($_dummy  = $menuItem" + submenuName + ".put(\"isLastItem\", " + isLastItem + "))\n");
					stringbuf.append("#set ($_dummy = $" + "_" + submenuName + ".add($menuItem" + submenuName + "))\n\n");

				}
			}
			stringbuf.append("#set ($menuItem" + submenuName + " = $contents.getEmptyMap())\n");
		}
		return stringbuf.toString();
	}

	// private List mergeList(List a, List b) {
	// for (int i = 0; i < b.size(); i++) {
	//
	// Object[] list = (Object[]) b.get(i);
	//
	// if (list != null && list.length > 0) {
	//
	// for (int j = 0; j < list.length; j++) {
	// if (list[j] instanceof
	// com.dotmarketing.portlets.contentlet.business.Contentlet) {
	// a.add((com.dotmarketing.portlets.contentlet.business.Contentlet)
	// list[j]);
	// }
	// }
	//
	// }
	//
	// }
	// return a;
	// }

}