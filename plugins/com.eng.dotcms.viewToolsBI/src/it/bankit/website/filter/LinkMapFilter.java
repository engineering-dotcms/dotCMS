package it.bankit.website.filter;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class LinkMapFilter implements Filter {

	private ContentletAPI conAPI;
	private UserWebAPI wuserAPI;
	private HostWebAPI whostAPI;


	public void destroy() {
	}

	public void init(FilterConfig config) throws ServletException {
		Config.setMyApp(config.getServletContext());
		conAPI = APILocator.getContentletAPI();
		wuserAPI = WebAPILocator.getUserWebAPI();
		whostAPI = WebAPILocator.getHostWebAPI();
		StructureCache.clearURLMasterPattern();

	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String uri = request.getRequestURI();
		uri = URLDecoder.decode(uri, "UTF-8");
		if ( excludeURI(uri) ) {
			chain.doFilter(req, res);
			return;
		}
		Host host;
		try {
			host = whostAPI.getCurrentHost(request);
		} catch (PortalException e) {
			Logger.error(this, "Unable to retrieve current request host for URI " + uri);
			throw new ServletException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, "Unable to retrieve current request host for URI  " + uri);
			throw new ServletException(e.getMessage(), e);
		} catch (DotDataException e) {
			Logger.error(this, "Unable to retrieve current request host for URI  " + uri);
			throw new ServletException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, "Unable to retrieve current request host for URI  " + uri);
			throw new ServletException(e.getMessage(), e);
		}

		if (uri.endsWith("/"))
			uri = uri.substring(0, uri.length() - 1);

		String pointer = null;
		if (host != null) {
			pointer = VirtualLinksCache.getPathFromCache(host.getHostname() + ":" + uri);
		}
		if (!UtilMethods.isSet(pointer)) {
			pointer = VirtualLinksCache.getPathFromCache(uri);
		}
		if (UtilMethods.isSet(pointer)) {
			uri = pointer;
		}

		Structure struct  = StructureCache.getStructureByVelocityVarName("Link");
		if (uri != null && ( struct != null  && UtilMethods.isSet(struct.getInode() ) ) ){
			StringTokenizer st = new StringTokenizer(uri , "/", false);
			int numTok = st.countTokens();
			int count = 1;
			String identifier = "";
			String path = "/";
			while (st.hasMoreTokens()) {
				if (numTok == count) {
					identifier = st.nextToken();
				} else {
					if ("/".equals(path)) {
						path += st.nextToken();
					} else {
						path += "/" + st.nextToken();
					}
				}
				count += 1;
			}
			Folder folder = null;
			try {
				 // APILocator.getIdentifierAPI();
				//	Identifier id =  iApi.loadFromCache(whostAPI.getCurrentHost(request)  , path );
				folder = APILocator.getFolderAPI().findFolderByPath(path, whostAPI.getCurrentHost(request), wuserAPI.getLoggedInUser(request), true);

				if ( UtilMethods.isSet(folder.getInode()) &&  (!"".equals(identifier) ) && !folder.getInode().contains("system_folder")) {
					String query = getQueryLinkEsterno(request, identifier, path, folder , host );
					List<Contentlet> linkList = conAPI.search(query.toString(), 1, 0, "modDate desc", APILocator.getUserAPI().getSystemUser(), true);
					if (linkList.size() > 0) {
						if (linkList.get(0).getStringProperty("linkType").equals("E")) {
							Contentlet c = linkList.get(0);
							String extPageURI = "";
							String lEsterno = c.getStringProperty("linkEsterno");
							if (lEsterno.startsWith("http://") || lEsterno.startsWith("https://") ) {
								extPageURI = lEsterno;
							} else {
								extPageURI = "http://" + lEsterno;
							}
							//Logger.debug(this.getClass().getName() , "Trovato un link ESTERNO -pagina a cui redirigere " + extPageURI);
							response.sendRedirect(extPageURI);
							return;
						}
					}else {
						String queryIn = getQueryLinkInterno( request , identifier, uri, folder , host );
						List<Contentlet> linkListInt = conAPI.search(queryIn.toString(), 1, 0, "Link.dataEmanazione desc", APILocator.getUserAPI().getSystemUser(), true);
						if (linkListInt.size() > 0) {
							Logger.debug(this.getClass().getName() , "trovato un link con idRemoto = true ");
							Contentlet contentlet = linkListInt.get(0);
							String lType = contentlet.getStringProperty("linkType");
							String extPageURI = null;
							if( lType.equalsIgnoreCase("I") ){
								extPageURI = contentlet.getStringProperty("linkInterno");
							}else {
								//Logger.info(this.getClass().getName() , "Sono ELSE" + extPageURI);
								FileAsset fa =APILocator.getFileAssetAPI().fromContentlet(contentlet);
								String identifierFA = fa.getIdentifier();
								System.out.println( "identifierFA " + identifierFA  );
								
								extPageURI = APILocator.getIdentifierAPI().find(identifierFA ).getURI();
								System.out.println( "File da recuperare  " + identifierFA  );
								request.getRequestDispatcher(extPageURI).forward(request, response);
							}
							return;

						}else {
							chain.doFilter(req, res);
							return;
						}
					}
				}
			} catch (PortalException e3) {
				Logger.warn(this, "PortalException", e3);
			} catch (SystemException e3) {
				Logger.warn(this, "SystemException", e3);
			} catch (DotRuntimeException e3) {
				Logger.warn(this, "DotRuntimeException", e3);
			} catch (DotDataException e3) {
				Logger.warn(this, "DotDataException", e3);
			} catch (DotSecurityException e3) {
				Logger.warn(this, "DotSecurityException	", e3);
			}
		}
		chain.doFilter(req, res);
	}





	private  boolean excludeURI(String uri) {
		boolean exclude = CMSFilter.excludeURI(uri);
		if( !exclude ){
			if (uri.trim().endsWith("/testLB.html" )  ){
				exclude = true ;
			}
		}
		return exclude;
	}

	private String getQueryLinkEsterno(	HttpServletRequest request  ,  String identifier , String path , Folder folder , Host host ){
		HttpSession session = request.getSession();
		//Logger.info(this.getClass(), "[INIT]getQueryLinkEsterno -  Verifico se è un link ESTERNO " + identifier  );
		StringBuilder query = null;
		query = new StringBuilder();
		query.append("+structureName:Link  +Link.linkType:*E* +Link.identificativo:" + identifier + " +conFolder:" + folder.getInode() + " +deleted:false ");
		String params = addDefaultParameterToQuery(session, host);
		query.append(params);
		//Logger.info(this.getClass(), "[END]getQueryLinkEsterno query: " + query.toString()   );
		return query.toString();
	}


	private String getQueryLinkInterno(	HttpServletRequest request  ,  String identifier , String uri , Folder folder , Host host ){

		//Logger.info(this.getClass(), "[INIT]getQueryLinkInterno -  Verifico se è un link interno ( di tipo A o I )" + identifier  );
		HttpSession session = request.getSession();
		StringBuilder query = null;
		query = new StringBuilder();
		query.append("+structureName:Link  +Link.linkType:*A*  +Link.identificativo:" + uri + " +Link.idRemoto:*True*  +deleted:false ");
		String params = addDefaultParameterToQuery(session, host);
		query.append(params);
		//Logger.info(this.getClass(), "[END]getQueryLinkInterno query: " + query.toString()   );
		Logger.info(this.getClass(), "[END]getQueryLinkInterno query: " + query.toString()   );
		return query.toString();

	}

	private String addDefaultParameterToQuery( HttpSession session , Host host ){
		StringBuilder queryParam = new StringBuilder("");
		boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		boolean EDIT_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
		if (EDIT_MODE || ADMIN_MODE) {
			queryParam.append("+working:true ");
		} else {
			queryParam.append("+live:true ");
		}
		if (host != null) {
			try {
				queryParam.append("+(conhost:" + host.getIdentifier() + ") " );
			} catch (Exception e) {
				Logger.error(LinkMapFilter.class, e.getMessage() + "getQueryLinkInterno  : Unable to build host in query : ", e);
			}
		}
		if(UtilMethods.isSet(session.getAttribute("com.dotmarketing.htmlpage.language"))){
			queryParam.append( " +languageId:" + session.getAttribute("com.dotmarketing.htmlpage.language") );
		}
		return queryParam.toString();
	}

}
