package it.bankit.website.util;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class ViewToolUtil {

	
 
	public static Language getRequestLanguage( HttpServletRequest request  ) {
		Language language = null;
		try {
			String languageId = null;
			if (request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE) != null) {
				languageId = ((Object) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE)).toString();
			}
			if (languageId != null) {
				language = APILocator.getLanguageAPI().getLanguage(languageId);
			}
			if (language == null) {
				language =  APILocator.getLanguageAPI().getDefaultLanguage();
			}
			return language;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return language;

	}
	
	
	public static String convertPath(String path) {
		try {
			path = path.replace( System.getProperty("file.separator"), ".").replace(" ", "-");
			if (path.startsWith(".")) {
				path = path.substring(1);
			}
			if (path.endsWith(".")) {
				path = path.substring(0, path.lastIndexOf("."));
			}
		} catch (Exception e) {
			 
		}
		return path;
	}
	 
}
