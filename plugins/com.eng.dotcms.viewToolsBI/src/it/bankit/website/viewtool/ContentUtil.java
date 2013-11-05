package it.bankit.website.viewtool;

import it.bankit.website.util.AssetsComparator;
import it.bankit.website.viewtool.navigation.NavigationUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.ChildrenCondition;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.viewtools.content.ContentMap;
import com.liferay.portal.model.User;

public class ContentUtil implements ViewTool {

	private HttpServletRequest req;
	private HttpServletResponse resp;
	private ServletContext servletContext;
	private VelocityEngine velocity;
	private IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
	private FolderAPI folderAPI = APILocator.getFolderAPI();
	private TemplateAPI tAPI = APILocator.getTemplateAPI();
	private HTMLPageAPI htmlAPI = APILocator.getHTMLPageAPI();

	private Context context;
	private Host currentHost;
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private boolean ADMIN_MODE;
	private boolean PREVIEW_MODE;
	private boolean EDIT_MODE;
	private boolean EDIT_OR_PREVIEW_MODE;

	private User user = null;
	private User backuser = null;
	private UserWebAPI userAPI;
	private String linkStructureInode;

	
	private String DEFAULT_PAGE_NAME ="index.html"; 
	private String LISTING_TMPL_NAME ="Listing"; 

	
	@Override
	public void init(Object initData) {
		this.context = ((ViewContext) initData).getVelocityContext();
		this.req = ((ViewContext) initData).getRequest();
		this.resp = ((ViewContext) initData).getResponse();
		this.servletContext = ((ViewContext) initData).getServletContext();
		this.velocity = ((ViewContext) initData).getVelocityEngine();
		this.linkStructureInode = StructureCache.getStructureByVelocityVarName("Link").getInode();
		try {
			this.currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		} catch (Exception e) {
			com.dotmarketing.util.Logger.error(ContentUtil.class, "Error finding current host", e);
		}

		userAPI = WebAPILocator.getUserWebAPI();
		try {
			user = userAPI.getLoggedInFrontendUser(req);
			backuser = userAPI.getLoggedInUser(req);
		} catch (Exception e) {
			com.dotmarketing.util.Logger.error(ContentUtil.class, "Error finding the logged in user", e);
		}

		HttpSession session = req.getSession();
		ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		PREVIEW_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
		EDIT_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
		if (EDIT_MODE || PREVIEW_MODE) {
			EDIT_OR_PREVIEW_MODE = true;
		}
	}
	
	public String menuRoot(String path, String languageID) throws DotDataException, DotSecurityException{
		
		String dettaglioQuery = "";
		String menuRoot = path;	
		
		while (menuRoot.lastIndexOf("/") > -1) {
			int end = menuRoot.lastIndexOf("/");
			menuRoot = menuRoot.substring(0, end);

			dettaglioQuery = "+StructureName:Dettaglio +languageId:"+ languageID +" +parentPath:" + menuRoot+"/";
			List<Contentlet> dettagli = APILocator.getContentletAPI().search(dettaglioQuery, -1, 0, "Dettaglio.dataEmanazione desc", APILocator.getUserAPI().getSystemUser(), false);
			if(dettagli.size()>0){ 
				Contentlet dettaglio = dettagli.get(0);
				if (dettaglio.getStringProperty("menuRoot")!=null) {
					String isRoot = dettaglio.getStringProperty("menuRoot");
					if(isRoot.contains("true,"))
					return menuRoot;
				}
			}
		}
		
		return null;
	}

	public Folder getFolderByRequestURI() {
		Folder folder = null;
		try {
			int end = req.getRequestURI().lastIndexOf("/");
			String path = req.getRequestURI().substring(0, end);
			folder = folderAPI.findFolderByPath(path, currentHost, APILocator.getUserAPI().getSystemUser(), true);
			if (!UtilMethods.isSet(folder.getInode())) {
				folder = null;
			}
		} catch (Exception e) {
			com.dotmarketing.util.Logger.error(ContentUtil.class, "Error in getFolderByRequestURI ", e);
		}
		return folder;

	}

	public List getSortedList(Treeable[] folders, List<Treeable> listaLink) {

		for (Treeable folder : folders) {
			listaLink.add(listaLink.size(), folder);
		}
		Comparator comparator = new AssetsComparator(1);
		Collections.sort(listaLink, comparator);
		return listaLink;
	}

	public List<Contentlet> findByStructureLight(String structureInode, Folder folder, boolean working, String language) throws DotDataException, DotSecurityException {

		String parentPath = this.identifierAPI.find(folder.getIdentifier()).getURI() + "/";

		String q = "";
		q = "+StructureName:Link +Link.showOnMenu:*true* +conFolder:" + folder.getInode();
		List<Contentlet> related = APILocator.getContentletAPI().search(addDefaultsToQuery(q, language), -1, 0, null, APILocator.getUserAPI().getSystemUser(), false);
		return related;
	}

	@SuppressWarnings("unchecked")
	public List<com.dotmarketing.portlets.contentlet.business.Contentlet> findByStructure(String structureInode, Folder folder, boolean working, String language) throws DotDataException, DotStateException,
			DotSecurityException {
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);

		String parentPath = this.identifierAPI.find(folder.getIdentifier()).getURI() + "/";
		String fieldName = StructureCache.getStructureByInode(structureInode).getFieldVar("showOnMenu").getFieldContentlet();

		StringBuilder condition = new StringBuilder();

		if (working) {
			condition.append("contentletvi.workingInode=contentlet.inode").append(" and contentletvi.deleted = ").append(com.dotmarketing.db.DbConnectionFactory.getDBFalse());
		} else {
			condition.append("contentletvi.liveInode=contentlet.inode").append(" and contentletvi.deleted = ").append(com.dotmarketing.db.DbConnectionFactory.getDBFalse());
		}

		long languageId = Long.parseLong(language);

		if (!UtilMethods.isSet(languageId)) {
			languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
			condition.append(" and contentletvi.lang = ").append(languageId);
		} else {
			condition.append(" and contentletvi.lang = ").append(languageId);
		}

		hu.setQuery("select contentlet from contentlet in class " + com.dotmarketing.portlets.contentlet.business.Contentlet.class.getName() + ", contentletvi in class " + ContentletVersionInfo.class.getName()
				+ " , identifier in class  " + Identifier.class.getName() + " where type = 'contentlet' and structure_inode = '" + structureInode + "' "
				+ " and contentletvi.identifier=contentlet.identifier and "

				+ condition + " and identifier.id = contentlet.identifier and identifier.parentPath like  '" + parentPath + "' and " + fieldName + " like 'true%'");

		return (List<com.dotmarketing.portlets.contentlet.business.Contentlet>) hu.list();
	}

	public boolean isFileAsset(Contentlet contentlet) {
		return contentlet.getStructure().isFileAsset();
	}

	// Convenient method for velocity to add object to collection without return
	// nothing avoiding #set($trash = $collection.add($element))
	public <T> void addToCollection(Collection<T> collection, T obj) {
		collection.add(obj);
	}

	// Convenient method for velocity to add object to collection without return
	// nothing avoiding #set($trash = $collection.add($element))
	public <T> void addToCollection(Collection<T> collection, Collection<T> collection2) {
		collection.addAll(collection2);
	}

	public List<Contentlet> getRelatedContentlets(String relationshipInode, String contentletInode, boolean hasParent) throws DotDataException, DotSecurityException {
		Relationship relationship = (Relationship) InodeFactory.getInode(relationshipInode, Relationship.class);
		Contentlet contentlet = conAPI.find(contentletInode, user, true);

		return RelationshipFactory.getAllRelationshipRecords(relationship, contentlet, hasParent, !EDIT_OR_PREVIEW_MODE, "");
	}

	public List<Contentlet> pullRelatedContentlets(String typeValue, String contentletInode, boolean hasParent) throws DotDataException, DotSecurityException {
		Relationship relationship = RelationshipFactory.getRelationshipByRelationTypeValue(typeValue);
		Contentlet contentlet = conAPI.find(contentletInode, user, true);
		return RelationshipFactory.getAllRelationshipRecords(relationship, contentlet, hasParent, !EDIT_OR_PREVIEW_MODE, "");
	}

	/*
	 * Convenient methods for parsing widget in separate velocity context
	 */
	public String parseWidget(String inode) throws DotDataException, DotSecurityException, ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException {
		String result = "";
		Contentlet widget = conAPI.find(inode, user, true);
		Structure widgetStructure = widget.getStructure();
		if (widgetStructure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET) {
			List<Field> fields = FieldFactory.getFieldsByStructure(widgetStructure.getInode());
			Map<String, Object> values = widget.getMap();
			Field field = widgetStructure.getFieldVar("widgetPreexecute");
			String widgetPreexecute = field.getValues().trim();
			field = widgetStructure.getFieldVar("widgetCode");
			String widgetCode = field.getValues().trim();
			result = parseWidget(fields, values, widgetPreexecute, widgetCode);
		}
		return result;
	}

	public String parseWidget(ContentMap widget) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException {
		String result = "";
		Structure widgetStructure = widget.getStructure();
		if (widgetStructure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET) {
			List<Field> fields = FieldFactory.getFieldsByStructure(widgetStructure.getInode());
			Map<String, Object> values = new HashMap<String, Object>(fields.size());
			for (Field field : fields) {
				values.put(field.getVelocityVarName(), widget.get(field.getVelocityVarName()));
			}
			Field field = widgetStructure.getFieldVar("widgetPreexecute");
			String widgetPreexecute = field.getValues().trim();
			field = widgetStructure.getFieldVar("widgetCode");
			String widgetCode = field.getValues().trim();
			result = parseWidget(fields, values, widgetPreexecute, widgetCode);
		}
		return result;
	}

	private String parseWidget(List<Field> fields, Map<String, Object> values, String widgetPreexecute, String widgetCode) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException,
			IOException {
		String result = "";
		ChainedContext widgetContext = new ChainedContext(velocity, req, resp, servletContext);
		StringWriter firstEval = new StringWriter();
		StringWriter secondEval = new StringWriter();
		StringBuilder widgetExecuteCode = new StringBuilder();
		for (Field field : fields) {
			widgetContext.put(field.getVelocityVarName(), values.get(field.getVelocityVarName()));
		}

		if (UtilMethods.isSet(widgetPreexecute)) {
			widgetExecuteCode.append(widgetPreexecute + "\n");
		}

		if (UtilMethods.isSet(widgetCode)) {
			widgetExecuteCode.append(widgetCode);
		}

		VelocityUtil.getEngine().evaluate(widgetContext, firstEval, "", widgetExecuteCode.toString());
		VelocityUtil.getEngine().evaluate(widgetContext, secondEval, "", firstEval.toString());
		result = secondEval.toString();
		return result;
	}

	public List<Object> getMenuItems(Folder folder, int orderDirection, String language, boolean shows) throws DotDataException, DotStateException, DotSecurityException {
		List<Folder> folders = new ArrayList<Folder>();
		folders.add(folder);
		return getMenuItems(folders, orderDirection, language, shows);
	}

	public List<Object> getMenuItems(List<Folder> folders, int orderDirection, String language, boolean shows) throws DotDataException, DotStateException, DotSecurityException {

		List<Object> menuList = new ArrayList<Object>();

		for (Folder folder : folders) {
			ChildrenCondition folderCondition = new ChildrenCondition();
			ChildrenCondition contentCondition = new ChildrenCondition();
			contentCondition.deleted = false;
			contentCondition.live = true;

			// List<HTMLPage> htmlPages = folderAPI.getHTMLPag(cur,
			// APILocator.getUserAPI().getSystemUser(), false);
			// htmlAPI.getWorkingHTMLPageByPageURL(req.getRequestURI(), folder);

			// int level = getLevel(req.getRequestURI());
			// List<Folder> listSubFolders = new ArrayList<Folder>();
			// for (Treeable curFolder : subFolders) {
			//
			// if (curFolder instanceof Folder) {
			// Folder cur = ((Folder) curFolder);
			// List<HTMLPage> htmlPages = folderAPI.getHTMLPages(cur,
			// APILocator.getUserAPI().getSystemUser(), false);
			// Template t = null;
			// for (HTMLPage htmlPage : htmlPages) {
			// if (htmlPage.getPageUrl().equals("index.html"))
			// t = (Template)
			// APILocator.getVersionableAPI().findLiveVersion(htmlPage.getTemplateId(),
			// APILocator.getUserAPI().getSystemUser(), false);
			// break;
			// }
			//
			// if (!firstLevel) {
			// listSubFolders.add((Folder) curFolder);
			// if (htmlPages.size() > 0 && !t.getTitle().contains("Listing")) {
			// if (((Folder) curFolder).isShowOnMenu()) {
			//
			// } else {
			// menuList.addAll(getLinksRecursive((Folder) curFolder, language,
			// 1));
			// }
			// }
			// } else {
			// if (((Folder) curFolder).isShowOnMenu()) {
			// listSubFolders.add((Folder) curFolder);
			// }
			// // else {
			// // if (!firstLevel) {
			// // menuList.addAll(getLinksRecursive((Folder) curFolder,
			// language, 1));
			// // }
			// // }
			// }
			//
			// }
			// }

			List<Contentlet> linkItemList = null;
			List<HTMLPage> htmlPages = folderAPI.getHTMLPages(folder, APILocator.getUserAPI().getSystemUser(), false);
			Template template = null;
			for (HTMLPage htmlPage : htmlPages) {
				if (htmlPage.getPageUrl().equals( DEFAULT_PAGE_NAME )) {
					template = (Template) APILocator.getVersionableAPI().findLiveVersion(htmlPage.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
					break;
				}

			}

			if( (htmlPages != null && htmlPages.size()  > 0 ) && 
					template!= null && !template.getTitle().contains( LISTING_TMPL_NAME )  && !template.getTitle().contains("D5")) {

				List<Treeable> subFolders = getChildrenClass(folder, Folder.class, folderCondition, shows);
				menuList.addAll(subFolders);
			}

			linkItemList = findByStructureLight(linkStructureInode, folder, EDIT_OR_PREVIEW_MODE, language);
			// gets all subitems

			// menuList.addAll(filesListSubChildren);
			if (linkItemList != null) {
				menuList.addAll(linkItemList);
			}

		}

		return menuList;
	}

	// public List<com.dotmarketing.portlets.contentlet.business.Contentlet>
	// getLinksRecursive(Folder folder, String language, int recursion) throws
	// DotStateException,
	// DotDataException, DotSecurityException {
	// List<com.dotmarketing.portlets.contentlet.business.Contentlet> links =
	// new
	// ArrayList<com.dotmarketing.portlets.contentlet.business.Contentlet>();
	// ChildrenCondition searchCondition = new ChildrenCondition();
	//
	// // get for current folder
	// links.addAll(findByStructure(linkStructureInode, folder,
	// EDIT_OR_PREVIEW_MODE, language));
	// // gets all subfolders
	// if (recursion > 0) {
	// List<Treeable> subFolders = getChildrenClass(folder, Folder.class,
	// searchCondition);
	// for (Iterator<Treeable> iterator = subFolders.iterator();
	// iterator.hasNext();) {
	// Folder curFolder = (Folder) iterator.next();
	// if (!curFolder.isShowOnMenu()) {
	// links.addAll(getLinksRecursive(curFolder, language, recursion - 1));
	// }
	// }
	// }
	// return links;
	// }

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, boolean onlyShows) throws DotStateException, DotDataException {
		return getChildrenClass(parent, clazz, null, null, 0, 1000, onlyShows);
	}

	protected List<Treeable> getChildrenClass(Host host, Class clazz, boolean onlyShows) throws DotStateException, DotDataException {
		Identifier identifier = APILocator.getIdentifierAPI().find(host.getIdentifier());
		return getChildrenClass(identifier, clazz, null, null, 0, 1000, onlyShows);
	}

	protected List<Treeable> getChildrenClass(Host host, Class clazz, ChildrenCondition cond, boolean onlyShows) throws DotStateException, DotDataException {
		Identifier identifier = APILocator.getIdentifierAPI().find(host.getIdentifier());
		return getChildrenClass(identifier, clazz, cond, null, 0, 1000, onlyShows);
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond, boolean onlyShows) throws DotStateException, DotDataException {
		return getChildrenClass(parent, clazz, cond, null, 0, 1000, onlyShows);
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition condition, String orderby, boolean onlyShows) throws DotStateException, DotDataException {
		return getChildrenClass(parent, clazz, condition, orderby, 0, 1000, onlyShows);
	}

	protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond, String orderBy, int offset, int limit, boolean onlyShows) throws DotStateException, DotDataException {
		Identifier identifier = APILocator.getIdentifierAPI().find(parent.getIdentifier());
		return getChildrenClass(identifier, clazz, cond, orderBy, offset, limit, onlyShows);
	}

	@SuppressWarnings("unchecked")
	protected List<Treeable> getChildrenClass(Identifier identifier, Class clazz, ChildrenCondition cond, String orderBy, int offset, int limit, boolean onlyShows) throws DotStateException, DotDataException {

		String tableName;

		try {
			Object obj;
			obj = clazz.newInstance();

			if (obj instanceof Treeable) {
				tableName = ((Treeable) obj).getType();
			} else {
				throw new DotStateException("Unable to getType for child asset");
			}
		} catch (InstantiationException e) {
			throw new DotStateException("Unable to getType for child asset");
		} catch (IllegalAccessException e) {
			throw new DotStateException("Unable to getType for child asset");
		}

		String versionTable = UtilMethods.getVersionInfoTableName(tableName);

		HibernateUtil dh = new HibernateUtil(clazz);
		String sql = "SELECT {" + tableName + ".*} " + " from " + tableName + " " + tableName + ",  inode " + tableName + "_1_, identifier " + tableName + "_2_ ";

		if (cond != null && versionTable != null && (cond.deleted != null || cond.working != null || cond.live != null))
			sql += ", " + versionTable;

		sql += " where " + tableName + "_2_.parent_path = ? " + " and " + tableName + ".identifier = " + tableName + "_2_.id " + " and " + tableName + "_1_.inode = " + tableName + ".inode " + " and ";

		if (cond != null && cond.deleted != null)
			if (versionTable != null)
				sql += versionTable + ".deleted=" + ((cond.deleted) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse()) + " and ";
			else
				sql += " deleted=" + ((cond.deleted) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse()) + " and ";

		if (cond != null && cond.working != null)
			if (versionTable != null)
				sql += versionTable + ".working_inode" + (cond.working ? "=" : "<>") + tableName + "_1_.inode and ";
			else
				sql += " working=" + ((cond.working) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse()) + " and ";

		if (cond != null && cond.live != null)
			if (versionTable != null)
				sql += versionTable + ".live_inode" + (cond.live ? "=" : "<>") + tableName + "_1_.inode and ";
			else
				sql += " live=" + ((cond.live) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse()) + " and ";

		if (onlyShows)
			sql += tableName + ".show_on_menu=" + DbConnectionFactory.getDBTrue() + " and ";

		sql += tableName + "_1_.type = '" + tableName + "' " + " and " + tableName + "_2_.host_inode = ? ";

		// if (cond != null && cond.showOnMenu != null)
		// sql += " and " + tableName + ".show_on_menu=" + (cond.showOnMenu ?
		// DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse());

		if (orderBy != null) {
			sql = sql + " order by " + orderBy;
		}

		dh.setSQLQuery(sql);
		dh.setFirstResult(offset);
		dh.setMaxResults(limit);
		if (identifier.getHostId().equals(Host.SYSTEM_HOST)) {
			dh.setParam("/");
			dh.setParam(identifier.getId());
		} else {
			dh.setParam(identifier.getURI() + "/");
			dh.setParam(identifier.getHostId());
		}

		return (List<Treeable>) dh.list();
	}

	private int getLevel(String reqURI) {

		NavigationUtil nu = new NavigationUtil();
		return nu.stringTokenizer(reqURI.substring(0, reqURI.lastIndexOf("/")), "/") - 1;

	}

	private String addDefaultsToQuery(String query, String language) {
		String q = query;

		q += " +languageId:" + language;

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