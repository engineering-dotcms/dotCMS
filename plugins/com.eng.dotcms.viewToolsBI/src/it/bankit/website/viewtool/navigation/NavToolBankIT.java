package it.bankit.website.viewtool.navigation;

import it.bankit.website.util.AssetsComparator;
import it.bankit.website.viewtool.WebUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.navigation.NavResult;
import com.dotmarketing.viewtools.navigation.NavTool;
import com.dotmarketing.viewtools.navigation.NavToolCache;
import com.liferay.portal.model.User;

public class NavToolBankIT implements ViewTool {

	private boolean ADMIN_MODE;
	private boolean PREVIEW_MODE;
	private boolean EDIT_MODE;
	private static boolean EDIT_OR_PREVIEW_MODE;

	private static NavToolCache navCache = null;
	private static FolderAPI fAPI = null;
	private static WebUtil wUtil = new WebUtil();
	private static IdentifierAPI identifierAPI = null;
	private Host currenthost = null;
	private static User user = null;
	private HttpServletRequest request = null;
	private static HttpSession session = null;
	private static String linkStructureInode;

	static {
		try {
			user = APILocator.getUserAPI().getSystemUser();
		} catch (DotDataException e) {
			Logger.error(NavTool.class, e.getMessage(), e);
		}
	}
	
	public NavToolBankIT(){
		super();
	}

	public NavToolBankIT(Host currenthost) {
		super();
		this.linkStructureInode = StructureCache.getStructureByVelocityVarName(
				"Link").getInode();
		currenthost = currenthost;
		fAPI = APILocator.getFolderAPI();
		identifierAPI = APILocator.getIdentifierAPI();
		navCache = CacheLocator.getNavToolCache();
		ADMIN_MODE = (session
				.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		PREVIEW_MODE = ((session
				.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
		EDIT_MODE = ((session
				.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
		if (EDIT_MODE || PREVIEW_MODE) {
			EDIT_OR_PREVIEW_MODE = true;
		} else {
			EDIT_OR_PREVIEW_MODE = false;
		}
	}

	@Override
	public void init(Object initData) {
		ViewContext context = (ViewContext) initData;
		try {
			this.linkStructureInode = StructureCache
					.getStructureByVelocityVarName("Link").getInode();
			this.request = context.getRequest();
			session = request.getSession();
			currenthost = WebAPILocator.getHostWebAPI().getCurrentHost(
					context.getRequest());
			fAPI = APILocator.getFolderAPI();
			identifierAPI = APILocator.getIdentifierAPI();
			navCache = CacheLocator.getNavToolCache();
		} catch (Exception e) {
			Logger.warn(this, e.getMessage(), e);
		}

		ADMIN_MODE = (session
				.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		PREVIEW_MODE = ((session
				.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
		EDIT_MODE = ((session
				.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
		if (EDIT_MODE || PREVIEW_MODE) {
			EDIT_OR_PREVIEW_MODE = true;
		} else {
			EDIT_OR_PREVIEW_MODE = false;
		}
	}

	protected static NavResult getNav(Host host, String path)
			throws DotDataException, DotSecurityException {

		// aggiungo anche i link che sono showOnMenu
		String language = "";
		if (((Object) session
				.getAttribute("com.dotmarketing.htmlpage.language")) != null) {
			language = ((Object) session
					.getAttribute("com.dotmarketing.htmlpage.language"))
					.toString();
		}

		if (path != null && path.contains(".")) {
			path = path.substring(0, path.lastIndexOf("/"));
		}

		Folder folder = !path.equals("/") ? fAPI.findFolderByPath(path, host,
				user, true) : fAPI.findSystemFolder();
		String folderPath = wUtil.getFolderPath(folder);
		if (folder == null || !UtilMethods.isSet(folder.getIdentifier()))
			return null;

		NavResult result = navCache.getNav(host.getIdentifier(), folder
				.getInode());

		if (result == null) {
			String parentId;
			if (!folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)) {
				Identifier ident = APILocator.getIdentifierAPI().find(folder);
				parentId = ident.getParentPath().equals("/") ? FolderAPI.SYSTEM_FOLDER
						: fAPI.findFolderByPath(ident.getParentPath(), host,
								user, false).getInode();
			} else
				parentId = null;
			result = new NavResultBankIT(parentId, host.getIdentifier(), folder
					.getInode());
			Identifier ident = APILocator.getIdentifierAPI().find(folder);
			result.setHref(ident.getURI());
			result.setTitle(folder.getTitle());
			result.setOrder(folder.getSortOrder());
			result.setType("folder");
			result.setPermissionId(folder.getPermissionId());
			List<NavResult> children = new ArrayList<NavResult>();
			List<String> folderIds = new ArrayList<String>();
			result.setChildren(children);
			result.setChildrenFolderIds(folderIds);

			List menuItems;
			if (path.equals("/"))
				menuItems = fAPI.findSubFolders(host, true);
			else
				menuItems = fAPI.findMenuItems(folder, user, true);

			List<Contentlet> linkList = findByStructureLight(
					linkStructureInode, folder, language);
			menuItems.addAll(linkList);
			Comparator comparator = new AssetsComparator(1);
			Collections.sort(menuItems, comparator);

			for (Object item : menuItems) {
				if (item instanceof Folder) {
					Folder itemFolder = (Folder) item;
					ident = APILocator.getIdentifierAPI().find(itemFolder);
					NavResult nav = new NavResultBankIT(folder.getInode(), host
							.getIdentifier(), itemFolder.getInode());
					nav.setTitle(itemFolder.getTitle());
					nav.setHref(ident.getURI());
					nav.setOrder(itemFolder.getSortOrder());
					nav.setType("folder");
					nav.setPermissionId(itemFolder.getPermissionId());
					// it will load lazily its children
					folderIds.add(itemFolder.getInode());
					children.add(nav);
				} else if (item instanceof Contentlet) {

					Contentlet contentletItem = (Contentlet) item;
					NavResult nav = new NavResultBankIT(folder.getInode(), host
							.getIdentifier());
					nav.setHref(folderPath);
					nav.setType("contentlet");
					nav.setTitle(contentletItem.getTitle());
					nav.setPermissionId(contentletItem.getInode());
					children.add(nav);
				} else if (item instanceof HTMLPage) {

					HTMLPage itemPage = (HTMLPage) item;
					ident = APILocator.getIdentifierAPI().find(itemPage);
					NavResult nav = new NavResultBankIT(folder.getInode(), host
							.getIdentifier());
					nav.setTitle(itemPage.getTitle());
					nav.setHref(ident.getURI());
					nav.setOrder(itemPage.getSortOrder());
					nav.setType("htmlpage");
					nav.setPermissionId(itemPage.getPermissionId());
					children.add(nav);
				} else if (item instanceof Link) {
					Link itemLink = (Link) item;
					String a = itemLink.getURI();
					String h = itemLink.getUrl();
					NavResult nav = new NavResultBankIT(folder.getInode(), host
							.getIdentifier());
					if (itemLink.getLinkType().equals(LinkType.CODE.toString())
							&& LinkType.CODE.toString() != null
							&& (LinkType.CODE.toString().contains("$") || LinkType.CODE
									.toString().contains("#"))) {
						nav.setHref(itemLink.getLinkCode());
					} else if (itemLink.getLinkType().equals(
							LinkType.EXTERNAL.toString())) {
						nav.setHref(itemLink.getWorkingURL());
					} else {
						nav.setHref(itemLink.getUrl().replaceAll(
								host.getTitle(), ""));
					}
					nav.setTitle(itemLink.getTitle());
					nav.setOrder(itemLink.getSortOrder());
					nav.setType(itemLink.getLinkType());
					nav.setPermissionId(itemLink.getPermissionId());
					children.add(nav);
				} else if (item instanceof IFileAsset) {
					IFileAsset itemFile = (IFileAsset) item;
					ident = APILocator.getIdentifierAPI().find(
							itemFile.getPermissionId());
					NavResult nav = new NavResultBankIT(folder.getInode(), host
							.getIdentifier());
					nav.setTitle(itemFile.getFriendlyName());
					nav.setHref(ident.getURI());
					nav.setOrder(itemFile.getMenuOrder());
					nav.setType("file");
					nav.setPermissionId(itemFile.getPermissionId());
					children.add(nav);
				}
			}

			// navCache.putNav(host.getIdentifier(), language + "_" +
			// folder.getInode(), result);

			navCache.putNav(host.getIdentifier(), folder.getInode(), result);
		}

		return result;
	}

	public NavResult getNav() throws DotDataException, DotSecurityException {
		return getNav((String) request
				.getAttribute("javax.servlet.forward.request_uri"));
	}

	public NavResult getNav(String path) throws DotDataException,
			DotSecurityException {

		Host host = currenthost;
		if (path.startsWith("//")) {
			List<RegExMatch> find = RegEX.find(path, "^//(\\w+)/(.+)");
			if (find.size() == 1) {
				String hostname = find.get(0).getGroups().get(0).getMatch();
				path = "/" + find.get(0).getGroups().get(1).getMatch();
				host = APILocator.getHostAPI()
						.findByName(hostname, user, false);
			}
		}

		return getNav(host, path);
	}

	public static List<Contentlet> findByStructureLight(String structureInode,
			Folder folder, String language) throws DotDataException,
			DotSecurityException {

		String parentPath = identifierAPI.find(folder.getIdentifier()).getURI()
				+ "/";

		String q = "";
		q = "+StructureName:Link +Link.showOnMenu:*true* +conFolder:"
				+ folder.getInode();
		List<Contentlet> related = APILocator.getContentletAPI().search(
				addDefaultsToQuery(q, language), -1, 0, null,
				APILocator.getUserAPI().getSystemUser(), false);
		return related;
	}

	private static String addDefaultsToQuery(String query, String language) {
		String q = query;

		// q += " +languageId:" + language;

		if (!(query.contains("live:") || query.contains("working:"))) {
			if (EDIT_OR_PREVIEW_MODE) {
				q += " +working:true ";
			} else {
				q += " +live:true ";
			}

		}

		if (!UtilMethods.contains(query, "deleted:")) {
			q += " +deleted:false ";
		}

		return q;
	}

}
