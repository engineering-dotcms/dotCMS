package com.eng.dotcms.mostresearchedterms;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.eng.dotcms.mostresearchedterms.util.Util;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

/**
 * 
 * Filter that we apply to ALL the URL that made a search on ElasticSearch and insert into the temporary table the query string
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Dec 20, 2012 - 5:09:04 PM
 */
public class MostResearchedTermsFilter implements Filter{
	
	private FilterConfig filterConfig = null;
	private String POLICY_FILENAME = "antisamy-bankit.xml";
	private MostResearchedTermsAPI mrtAPI;
	private LanguageAPI langAPI;
	private HostWebAPI hostWebAPI;
	private AntiSamy antiSamy;
	private Policy policy;
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest)request;		
		try {
			String htmlFieldName = filterConfig.getInitParameter("query.htmlFieldName");
			StringBuilder parameters = Util.retrieveParameters(htmlFieldName, httpReq, antiSamy);
			if(null!=parameters){
				Host currentHost = hostWebAPI.getCurrentHost(httpReq);
				long language = 0;
				if (!UtilMethods.isSet((String) httpReq.getSession().getAttribute(WebKeys.HTMLPAGE_LANGUAGE)) 
						&& !UtilMethods.isSet(httpReq.getParameter(WebKeys.HTMLPAGE_LANGUAGE))) {
					language = langAPI.getDefaultLanguage().getId();
				}else if(UtilMethods.isSet(httpReq.getParameter(WebKeys.HTMLPAGE_LANGUAGE))){
					language = Long.parseLong(httpReq.getParameter(WebKeys.HTMLPAGE_LANGUAGE));
				}else
					language = Long.parseLong((String)httpReq.getSession().getAttribute(WebKeys.HTMLPAGE_LANGUAGE));
				mrtAPI.insertTempQuery(parameters,language,currentHost.getIdentifier());				
			}else
				Logger.error(this, "The query input text name is mandatory.");
		} catch (DotDataException e) {			
			Logger.error(this, e.getMessage(), e);
		} catch (SystemException e){
			Logger.error(this, e.getMessage(), e);
		} catch (PortalException e){
			Logger.error(this, e.getMessage(), e);
		} catch (DotSecurityException e) {			
			Logger.error(this, e.getMessage(), e);
		} catch (ScanException e) {
			Logger.error(this, e.getMessage(), e);
		} catch (PolicyException e) {
			Logger.error(this, e.getMessage(), e);
		}
		chain.doFilter(request, response);		
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		Config.setMyApp(config.getServletContext());
		filterConfig = config;		
		mrtAPI = new MostResearchedTermsAPIImpl();
		langAPI = APILocator.getLanguageAPI();
		hostWebAPI = WebAPILocator.getHostWebAPI();
		try {
			policy = Policy.getInstance(this.getClass().getClassLoader().getResourceAsStream(POLICY_FILENAME));
			antiSamy = new AntiSamy(policy);
		} catch (PolicyException e) {
			Logger.error(this, e.getMessage(), e);
		}
		
	}

}
