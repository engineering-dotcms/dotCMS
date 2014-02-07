package com.eng.dotcms.healthchecker.filter;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jgroups.Address;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.util.WebKeys;

public class HealthFilter implements Filter {
	
	private HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	
	private static String DEFAULT_PATH_TO_PAGE = "/application/error/out-of-cluster.html";
	
	@Override
	public void destroy() {
		
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletResponse res = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest) request;
		try {
			if(null!=WebAPILocator.getUserWebAPI().getLoggedInUser(req)){
				Address localAddress = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress();
				if(healthAPI.isLeaveNode(localAddress)){
					Host host = WebAPILocator.getHostWebAPI().getCurrentHost(req);
						
					String ep_originatingHost = host.getHostname();
					String ep_errorCode = "406";
					String pointer = (String) VirtualLinksCache.getPathFromCache(host.getHostname() + ":/cms406Page");
					if (!UtilMethods.isSet(pointer)) {
						pointer = (String) com.dotmarketing.cache.VirtualLinksCache.getPathFromCache("/cms406Page");
					}
					
					// if we have a virtual link, see if the page exists.  pointer will be set to null if not
					if (UtilMethods.isSet(pointer)) {
						if (pointer.startsWith("/")) {
						// if the virtual link is a relative path, the path is validated within the current host
							pointer = com.dotmarketing.cache.LiveCache.getPathFromCache(pointer, host);	
							Logger.debug(this, "cms406Page relative path is: " + pointer + " - host: " + host.getHostname() + " and pointer: " + pointer);
						} else {
							// if virtual link points to a host or alias in dotCMS server, the path needs to be validated.
							// Otherwise, the original external pointer is kept for the redirect
							try {
								URL errorPageUrl = new URL(pointer);
								String errorPageHost = errorPageUrl.getHost();
								String errorPagePath = errorPageUrl.getPath();
								
								Logger.debug(this, "cms406Page - errorPageHost: " + errorPageHost + " and errorPagePath: " + errorPagePath);
								
								Host internalHost = WebAPILocator.getHostWebAPI().findByName(errorPageHost, WebAPILocator.getUserWebAPI().getAnonymousUser(), true);
								Host internalAlias = WebAPILocator.getHostWebAPI().findByAlias(errorPageHost, WebAPILocator.getUserWebAPI().getAnonymousUser(), true);
								
								// 406 Virtual Link is pointing to a host in dotCMS
								if ( internalHost != null) {				
									String absPointer = com.dotmarketing.cache.LiveCache.getPathFromCache(errorPagePath, internalHost);
									if (absPointer == null) {
										pointer = null;
									}
									Logger.debug(this, "cms406Page absolute internal path is: " + pointer + " - internalHost: " + internalHost.getHostname() + " and errorPagePath: " + errorPagePath);
								
								// 406 Virtual Link is poiting to an alias in dotCMS
								} else if ( internalAlias != null) {
									String absPointer = com.dotmarketing.cache.LiveCache.getPathFromCache(errorPagePath, internalAlias);
									if (absPointer == null) {
										pointer = null;
									}
									Logger.debug(this, "cms406Page absolute internal path is: " + pointer + " - internalAlias: " + internalAlias.getHostname() + " and errorPagePath: " + errorPagePath);
								
								// 406 Virtual Link is pointing to an external page
								} else {
									Logger.debug(this, "cms406Page absolute external path is: " + pointer);
								}
									
							} catch (Exception e){
								Logger.error(this, "cms406Page path is incorrect: " + pointer + e.getMessage(), e);
								pointer = null;
							}
						}
					}else{
						req.getSession().removeAttribute(WebKeys.USER_ID);
						req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
						req.getSession().removeAttribute("PENDING_ALERT_SEEN");
						req.getSession().removeAttribute("createAccountForm");
						req.getSession().removeAttribute("checkoutForm");
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.LOGGED_IN_USER_CATS);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.LOGGED_IN_USER_TAGS);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.USER_FAVORITES);
				        
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION);
				        request.getRequestDispatcher(DEFAULT_PATH_TO_PAGE).forward(req, res);						
					}
					
					// if we have virtual link and page exists, redirect or forward
					if(UtilMethods.isSet(pointer) ){
						req.getSession().removeAttribute(WebKeys.USER_ID);
						req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
						req.getSession().removeAttribute("PENDING_ALERT_SEEN");
						req.getSession().removeAttribute("createAccountForm");
						req.getSession().removeAttribute("checkoutForm");
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.LOGGED_IN_USER_CATS);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.LOGGED_IN_USER_TAGS);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.USER_FAVORITES);
				        
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION);
				        req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION);
	
						if (pointer.startsWith("/")) {
							Logger.debug(this, "cms406Page forwarding to relative path: " + pointer);			
							request.getRequestDispatcher(pointer).forward(req, res);
						} else {
							pointer = pointer + "?ep_originatingHost="+ep_originatingHost+"&ep_errorCode="+ep_errorCode;
							Logger.debug(this, "cms406Page redirecting to absolute path: " + pointer);
							res.sendRedirect(pointer);
						}
						return;
					}
							
				}else{
					chain.doFilter(request, response);
				}
					
			}else
				chain.doFilter(request, response);
			
		} catch (PortalException e) {
			e.printStackTrace();
			chain.doFilter(request, response);
		} catch (SystemException e) {
			e.printStackTrace();
			chain.doFilter(request, response);
		} catch (DotDataException e) {
			e.printStackTrace();
			chain.doFilter(request, response);
		} catch (DotSecurityException e) {
			e.printStackTrace();
			chain.doFilter(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			chain.doFilter(request, response);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
	

}
