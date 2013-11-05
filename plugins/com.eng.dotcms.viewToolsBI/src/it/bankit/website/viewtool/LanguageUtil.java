package it.bankit.website.viewtool;

import it.bankit.website.cache.BankitCache;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public class LanguageUtil implements ViewTool {

	private HttpServletRequest request;
	private LanguageAPI langAPI;

	private static String FOLDER_SEPARATOR = ".";
	private static String FOLDER_BLANK_SEPARATOR = "-";
	private static final Logger LOG = Logger.getLogger(LanguageUtil.class);

	@Override
	public void init(Object initData) {
		ViewContext context = (ViewContext) initData;
		request = context.getRequest();
		langAPI = APILocator.getLanguageAPI();
	}

	/**
	 * Path translation webapi
	 * 
	 * @param path
	 */
	public String getPathTranslation(String path) {
		if (UtilMethods.isSet(path)&&!path.equals( "/" )) {
			String cp = convertPath(path);
			return BankitCache.getInstance().getStringKey(getRequestLanguage(), cp);
		}
		return "";
	}
	
	
	public String getPathTranslation(String path, String languageId ) {
		if (UtilMethods.isSet(path) && !path.equals( "/" )) {
			LOG.info( this.getClass() + ": getPathTranslation -> languageId non intero " +  languageId );
			String cp = convertPath(path);
			Language lang = getLanguage(languageId );
			return BankitCache.getInstance().getStringKey(lang, cp);
		}
		return "";
	}


	public Language getLanguage(String languageId ) {
		Language language = null;
		try {
			 
			if (languageId != null) {
				try{
					Integer.parseInt(languageId);
					language = langAPI.getLanguage((String)languageId);
				}catch (Exception e) {
					LOG.info( this.getClass() + ": getRequestLanguage languageId non intero " +  languageId );
					language = langAPI.getDefaultLanguage();
				}				
			}
			if(languageId == null ||  UtilMethods.isSet( languageId )) {
				language = getRequestLanguage();
			}
			return language;

		} catch (Exception e) {
			LOG.info( e.getMessage() );
			language = langAPI.getDefaultLanguage();
		}
		return language;

	}
	
	/**
	 * Optimized translation webapi	  
	 * @param key
	 */
	public String get(String key) {
		return BankitCache.getInstance().getStringKey(getRequestLanguage(), key);
	}

	public Language getRequestLanguage() {
		Language language = null;

		try {
			String languageId = null;
			if (request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE) != null) {
				languageId = ((Object) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE)).toString();
			}

			if (languageId != null) {
				try{
					Integer.parseInt(languageId);
					language = langAPI.getLanguage((String)languageId);
				}catch (Exception e) {
					LOG.info( this.getClass() + ": getRequestLanguage languageId non intero " +  languageId );
					language = langAPI.getDefaultLanguage();
				}				
			}
			if (language == null) {
				language = langAPI.getDefaultLanguage();
			}
			return language;

		} catch (Exception e) {
			LOG.info( e.getMessage() );
			language = langAPI.getDefaultLanguage();
		}
		return language;

	}

	public String convertPath(String path) {
		try {
			path = path.replace("/", FOLDER_SEPARATOR).replace(" ", FOLDER_BLANK_SEPARATOR);
			if (path.startsWith(FOLDER_SEPARATOR)) {
				path = path.substring(1);
			}
			if (path.endsWith(FOLDER_SEPARATOR)) {
				path = path.substring(0, path.lastIndexOf(FOLDER_SEPARATOR));
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		return path;
	}

}
