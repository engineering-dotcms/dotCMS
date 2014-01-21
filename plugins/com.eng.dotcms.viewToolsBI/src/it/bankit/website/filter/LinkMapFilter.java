package it.bankit.website.filter;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class LinkMapFilter implements Filter {

	private ContentletAPI conAPI;
	private HostWebAPI whostAPI;
	   
	public void destroy() {
	}

	public void init(FilterConfig config) throws ServletException {
		Config.setMyApp(config.getServletContext());
		conAPI = APILocator.getContentletAPI();
		whostAPI = WebAPILocator.getHostWebAPI();
		StructureCache.clearURLMasterPattern();
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String uri = request.getRequestURI();
		uri = URLDecoder.decode(uri, "UTF-8");
		Logger.debug(getClass(), "URI da controllare: " + uri);
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
			try {
				String queryIn = getQueryIdentificativo(request, uri, host );
				List<Contentlet> linkListInt = conAPI.search(queryIn.toString(), 1, 0, "Link.dataEmanazione desc", APILocator.getUserAPI().getSystemUser(), true);
				if (linkListInt.size() > 0) {
					Contentlet contentlet = linkListInt.get(0);
					String extPageURI = null;
					String allegatoId = null;
					boolean isAllegato = true;
					if (UtilMethods.isSet(contentlet.getStringProperty("allegatoId"))) {
						allegatoId = contentlet.getStringProperty("allegatoId");
					} else if (UtilMethods.isSet(contentlet.getStringProperty("allegato"))) {
						allegatoId = contentlet.getStringProperty("allegato");
					} else { 
						allegatoId = contentlet.getStringProperty("linkInterno");
						isAllegato = false;
					}
					if(isAllegato)
						extPageURI = APILocator.getIdentifierAPI().find(allegatoId ).getURI();
					else
						extPageURI = allegatoId;
					Logger.info(this.getClass(), "This link has an alias ("+ uri +")");
					response.sendRedirect(extPageURI);
					return;
				}else {
					chain.doFilter(req, res);
					return;
				}
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
			String url = uri.trim();		
			if (url.endsWith("/testLB.html" )  || 
					url.endsWith(".ico" ) || url.endsWith(".js" )  || url.endsWith(".css" ) 
					|| url.endsWith(".gif" )  || url.endsWith(".jpg" ) || url.endsWith(".jpeg" )
					|| url.equals("/")){
				exclude = true ;
			}
		}
		return exclude;
	}

	private String getQueryIdentificativo(	HttpServletRequest request, String uri , Host host ){
		HttpSession session = request.getSession();
		StringBuilder query = null;
		query = new StringBuilder();
		query.append("+structureName:Link  -Link.linkType:*E*  +Link.identificativo:" + uri + " +Link.idRemoto:*True*  +deleted:false ");
		String params = addDefaultParameterToQuery(session, host);
		query.append(params);
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
				Logger.error(LinkMapFilter.class, e.getMessage() + "addDefaultParameterToQuery  : Unable to build host in query : ", e);
			}
		}
		if(UtilMethods.isSet(session.getAttribute("com.dotmarketing.htmlpage.language"))){
			queryParam.append( " +languageId:" + session.getAttribute("com.dotmarketing.htmlpage.language") );
		}
		return queryParam.toString();
	}
}
