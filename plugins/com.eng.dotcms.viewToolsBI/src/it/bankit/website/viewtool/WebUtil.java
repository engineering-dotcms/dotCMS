package it.bankit.website.viewtool;

import it.bankit.website.cache.BankitCache;
import it.bankit.website.util.AssetsComparator;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.URIException;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import org.springframework.web.util.UriUtils;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.content.ContentMap;
import com.liferay.portal.model.User;

public class WebUtil implements ViewTool {

	private HttpServletRequest req;

	private IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
	private FolderAPI folderAPI = APILocator.getFolderAPI();
	private HTMLPageAPI pageAPI = APILocator.getHTMLPageAPI();
	private Host currentHost;
	private User user = null;
	private UserWebAPI userAPI;

	private static String delimiter = "/";

	@Override
	public void init(Object initData) {
		this.req = ((ViewContext) initData).getRequest();
		try {
			this.currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		} catch (Exception e) {
			com.dotmarketing.util.Logger.error(WebUtil.class, "Error finding current host", e);
		}

		userAPI = WebAPILocator.getUserWebAPI();
		try {
			user = userAPI.getLoggedInFrontendUser(req);

		} catch (Exception e) {
			com.dotmarketing.util.Logger.error(WebUtil.class, "Error finding the logged in user", e);
		}
	}

	public String getSystemProperty(String key){
		String value = "";
		if(UtilMethods.isSet(key)){
			value = System.getProperty(key);
		}
		return value;
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
			com.dotmarketing.util.Logger.error(WebUtil.class, "Error in getFolderByRequestURI ", e);
		}
		return folder;

	}

	public String encodeUrl(String url) throws UnsupportedEncodingException, URIException {


		String prefix = url.substring(0,url.lastIndexOf("/")+1);
		String fileName = url.substring(url.lastIndexOf("/")+1);
		String fileName1 = url.substring(url.lastIndexOf("/")+1);

		fileName = org.apache.commons.httpclient.util.URIUtil.encodePath(fileName1);
		//fileName = org.apache.commons.httpclient.util.URIUtil.encodeQuery(fileName);
		//fileName = java.net.URLEncoder.encode(fileName.toString(), "UTF-8");

		return prefix+	fileName;

	}

	public Folder getParentFolder(Folder f) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		Folder parent = folderAPI.findParentFolder(f, userAPI.getSystemUser(), false);
		if (parent != null && InodeUtils.isSet(parent.getInode())) {
			return parent;
		} else {
			return null;
		}
	}

	public Folder getRootParent(Folder f) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		Folder returnF = f;
		Folder parent = folderAPI.findParentFolder(f, userAPI.getSystemUser(), false);
		while (parent != null) {
			returnF = parent;
			parent = folderAPI.findParentFolder(parent, userAPI.getSystemUser(), false);
		}
		return returnF;
	}

	public String getFolderPathByIdentifier(String folderId) {
		Identifier id = new Identifier();
		try {
			id = identifierAPI.findFromInode(folderId.toString());
		} catch (DotDataException e) {
			e.printStackTrace();
		}
		return id.getPath();
	}

	public List<Object> sortCollection(List<Object> c, int direction) {
		@SuppressWarnings("unchecked")
		Comparator<Object> comparator = new AssetsComparator(direction);
		Collections.sort(c, comparator);
		return c;
	}

	public int getFolderLevel(Folder f) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		int level = 0;
		String fPath = getFolderPath(f);

		StringTokenizer st = new StringTokenizer(fPath.substring(1), "/");

		@SuppressWarnings("unused")
		String tmp;
		while (st.hasMoreTokens()) {
			tmp = st.nextToken();
			level += 1;
		}

		return level;

	}

	public String getFolderPath(Folder f) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		String uri = "";
		if (f != null) {
			try {
				uri = identifierAPI.loadFromCache(f.getIdentifier()).getPath();
			} catch (Exception e) {
				uri = ((Identifier) identifierAPI.find(f.getIdentifier())).getPath();
			}
		}
		return uri;
	}

	public Folder[] getSubFolder(String currentPath, boolean showOnMenu) {
		try {
			Host host = currentHost;

			Folder f = folderAPI.findFolderByPath(currentPath, host, APILocator.getUserAPI().getSystemUser(), false);
			return getSubFolder(f, showOnMenu);
		} catch (Exception ex) {
			com.dotmarketing.util.Logger.error(WebUtil.class, "Error in getSubFolder ", ex);

			return null;
		}
	}

	public Folder[] getSubFolder(Folder currentFolder, boolean showOnMenu) {
		List<Folder> fList = new ArrayList<Folder>();
		try {
			fList = BankitCache.getInstance().findSubFolders(currentFolder);
			// fList = APILocator.getFolderAPI().findSubFolders(currentFolder,
			// APILocator.getUserAPI().getSystemUser(), false);
			return fList.toArray(new Folder[fList.size()]);
		} catch (Exception ex) {
			com.dotmarketing.util.Logger.error(WebUtil.class, ex.getMessage(), ex);
			return null;
		}
	}

	public <T> Object[] toArray(List<T> lista) {

		if (lista != null) {
			return lista.toArray(new Object[lista.size()]);
		}
		return null;

	}

	public boolean isInArray(Object[] a, Object[] b) {
		return Arrays.equals(a, b);
	}

	public boolean hasTemplateListing(String pathCompleto) {
		boolean hasTempl = false;
		try {
			HTMLPage html = APILocator.getHTMLPageAPI().loadPageByPath(pathCompleto, currentHost);

			Template templ = APILocator.getTemplateAPI().findLiveTemplate(html.getTemplateId(), user, true);

			if (UtilMethods.isSet(templ)) {
				String tmplName = templ.getTitle();
				if (tmplName.startsWith("Listing")) {
					hasTempl = true;
				}
			}
		} catch (Exception e) {
			com.dotmarketing.util.Logger.error(WebUtil.class, e.getMessage(), e);
		}
		return hasTempl;
	}

	public List<File> getFiles(Folder parent, boolean respectFrontEndPermissions) {
		List<File> listaF = new ArrayList<File>();
		try {
			listaF = APILocator.getFolderAPI().getFiles(parent, user, respectFrontEndPermissions);
		} catch (Exception e) {
			com.dotmarketing.util.Logger.error(WebUtil.class, e.getMessage(), e);
		}
		return listaF;
	}

	public String getFileAssetURL(ContentMap contentlet) {
		if (contentlet.getStructure().isFileAsset()) {
			String hostName = null;
			String identifierKey = null;
			String uri = null;
			String protocol = null;
			String path = null;
			String fileName = null;
			try {
				identifierKey = (String) contentlet.get(Contentlet.IDENTIFIER_KEY);
				Identifier identifier = APILocator.getIdentifierAPI().find(identifierKey);
				ContentMap host = (ContentMap) contentlet.get(Contentlet.HOST_KEY);
				hostName = (String) host.get(Host.HOST_NAME_KEY);
				if (req.isSecure()) {
					protocol = "https://";
				} else {
					protocol = "http://";
				}
				path = identifier.getParentPath();
				fileName = (String) contentlet.get(FileAssetAPI.FILE_NAME_FIELD);
				uri = UtilMethods.encodeURIComponent(protocol + hostName + path + fileName);
				URL url = new URL(uri);
				return url.toExternalForm();
			} catch (MalformedURLException e) {
				Logger.error(WebUtil.class, "Malformed url:" + uri, e);
			} catch (DotDataException e) {
				Logger.error(WebUtil.class, "Invalid Identifier:" + identifierKey, e);
			}
		} else {
			Logger.warn(WebUtil.class, "Contentlet" + contentlet.getContentletsTitle() + " is not a File Asset Contentlet");
		}
		return "";
	}

	public boolean findLiveHTMLPage(Folder folder, String pageTitle) throws DotStateException, DotDataException, DotSecurityException {
		List<HTMLPage> pageOnFolder = pageAPI.findLiveHTMLPages(folder);
		if (pageOnFolder != null) {
			for (HTMLPage htmlPage : pageOnFolder) {
				if (htmlPage.getFriendlyName().equals(pageTitle)) {
					return htmlPage.isLive();
				}
			}
		}
		return false;
	}

	public String relativizeURL(String url) {
		String rel = "";
		if (UtilMethods.isSet(url)) {
			// TODO da migliorare attraverso la conversione ad URL ed
			// ottenimento del risultato attraverso stripping
			rel = url.substring(url.indexOf("/", 8));
		}
		return rel;
	}

	// Convenient util to encode url
	public String encodeUrlPath(String path) {
		try {
			return UriUtils.encodePath(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	public String encodeUrlQuery(String query) {
		try {
			return UriUtils.encodeQuery(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	public List<String> stringTokenizer(String s, String delimiter) {

		List<String> opt = new ArrayList<String>();

		if (s != null) {
			StringTokenizer st = new StringTokenizer(s, delimiter);

			if (st.countTokens() > 0) {
				while (st.hasMoreElements()) {
					String op = (String) st.nextToken();
					if ("true".equals(op) || "True".equals(op)) {
						opt.add("True");
					} else {
						opt.add(op);
					}
				}
			}

			return opt;
		}

		return opt;

	}

	public boolean getFirsStringUriToken(String filePath, String servletUri) {

		String pathToken = null;
		String servletToken = null;

		StringTokenizer filePathTokens = new StringTokenizer(filePath, delimiter);
		StringTokenizer servletPathTokens = new StringTokenizer(servletUri, delimiter);

		if (filePathTokens.hasMoreElements()) {
			pathToken = (String) filePathTokens.nextElement();
		}

		if (filePathTokens.hasMoreElements()) {
			servletToken = (String) servletPathTokens.nextElement();
		}

		if (servletToken != null && pathToken != null && pathToken.equals(servletToken)) {
			return true;
		} else
			return false;

	}

	public List<Object> nullLast(List<Object> listOfElements) {

		List<Object> orderedList = new ArrayList<Object>();
		List<Object> nullList = new ArrayList<Object>();

		for (Object object : listOfElements) {

			if (object instanceof ContentMap) {

				Object dataEmanazioneField = ((ContentMap) object).get("dataEmanazione");

				if (dataEmanazioneField != null && UtilMethods.isSet(((ContentMap) object).get("identifier"))) {
					nullList.add(object);
				} else {
					orderedList.add(object);
				}

			}
		}

		boolean merge = nullList.addAll(orderedList);
		return nullList;

	}


	public Number round(Object num ){

		if (num instanceof Number) {
			return   Math.round(  new Double(  String.valueOf(num)  ) );
		}
		try
		{
			return Math.round(new Double(  String.valueOf(num)  ) );
		}
		catch (NumberFormatException nfe)
		{
			return null;
		}
	}

}
