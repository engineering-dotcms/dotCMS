package com.eng.dotcms.mostresearchedterms.util;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.eng.dotcms.mostresearchedterms.MostResearchedTermsAPI;
import com.eng.dotcms.mostresearchedterms.MostResearchedTermsAPIImpl;
import com.eng.dotcms.mostresearchedterms.bean.MostResearchedTerms;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class MostResearchedTermsViewTool implements ViewTool {
	
	private HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	private LanguageAPI languageAPI = APILocator.getLanguageAPI();
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	
	private static String PLUGIN_ID = "com.eng.dotcms.mostresearchedterms";
	private static String DEFAULT_LIMIT_QUERY = "limit.terms";
	
	private MostResearchedTermsAPI mrtAPI;
	
	public MostResearchedTermsViewTool() {
		mrtAPI = new MostResearchedTermsAPIImpl();
	}
	
	@Override
	public void init(Object initData) {
		// TODO Auto-generated method stub

	}

	public List<String> getAllTerms(HttpServletRequest request, int limit){
		if(-1==limit)
			try {
				limit = Integer.parseInt(pluginAPI.loadProperty(PLUGIN_ID, DEFAULT_LIMIT_QUERY));
			} catch (NumberFormatException e1) {
				limit = 50;				
			} catch (DotDataException e1) {
				limit = 50;
			}
		List<String> terms = new ArrayList<String>();
		try {
			long language = 0;
			if(!UtilMethods.isSet(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE)) 
					&& !UtilMethods.isSet((String)request.getSession().getAttribute(WebKeys.HTMLPAGE_LANGUAGE))){
				language = languageAPI.getDefaultLanguage().getId();			
			}else if(UtilMethods.isSet(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE))){
				language = Long.parseLong(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE));
			}else
				language = Long.parseLong((String)request.getSession().getAttribute(WebKeys.HTMLPAGE_LANGUAGE));							
			List<MostResearchedTerms> listMrt = mrtAPI.findAllTerms(language, hostWebAPI.getCurrentHost(request).getIdentifier(), limit);
			for(MostResearchedTerms m:listMrt)
				terms.add(m.getTerm());
			
			return terms;
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		} catch (SystemException e) {			
			Logger.error(this, e.getMessage(), e);
			return null;
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		} 			
	}
	
	public Integer getOccurByTerm(String term, HttpServletRequest request){
		try {
			long language = 0;
			if(!UtilMethods.isSet(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE)) 
					&& !UtilMethods.isSet((String)request.getSession().getAttribute(WebKeys.HTMLPAGE_LANGUAGE))){
				language = languageAPI.getDefaultLanguage().getId();			
			}else if(UtilMethods.isSet(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE))){
				language = Long.parseLong(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE));
			}else
				language = Long.parseLong((String)request.getSession().getAttribute(WebKeys.HTMLPAGE_LANGUAGE));							
			return mrtAPI.findOccurByTerm(term, language, hostWebAPI.getCurrentHost(request).getIdentifier());
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		} catch (SystemException e) {			
			Logger.error(this, e.getMessage(), e);
			return null;
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		} 			
	}
}
