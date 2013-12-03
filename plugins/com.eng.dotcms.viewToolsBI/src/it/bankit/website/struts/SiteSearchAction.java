package it.bankit.website.struts;

import it.bankit.website.deploy.IDeployConst;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.springframework.web.util.HtmlUtils;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.ParamUtil;

public class SiteSearchAction extends DispatchAction {

	private static SiteSearchAPI siteSearchAPI = APILocator.getSiteSearchAPI();
	private static ESIndexAPI iAPI = new ESIndexAPI();
	private static PluginAPI pAPI = APILocator.getPluginAPI();
	private static LanguageAPI lAPI = APILocator.getLanguageAPI();
	private static Policy policy;
	private AntiSamy antiSamy;
	private User currentUser;
	public static String REGEX = "^[^\"'&\\/]*$";
	public static String REGEX_2 = "[\"'&\\/]{1,}";
	private static String POLICY_FILENAME = "antisamy-bankit.xml";

	private static String simpleQuery = " && -uri:(*rss.html || /homepage/search/*  || /homepage/images/* || /application/* || /interna/* || /selettore/* || /static/*) -mimeType:image* ";
	
	private static String pathToEcludeQuery = "+StructureName:webPageContent +webPageContent.title:*PathToExclude*";
	//private String pathToexclude = " && -uri:(  /pubblicazioni/econo/bollec/*/en*  || /pubblicazioni/econo/temidi/*/en* || " +" /pubblicazioni/quarigi/*en*   ";
	
	private static String excludeFrmSearch = " && -uri:(*rss.html) && -mimeType:image* ";
	private static String apice = "\"";

	//private static String indexIT;
	//private static String indexEN;
	private static String indexALL;

	@Override
	protected ActionForward dispatchMethod(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response, String name) throws Exception {
		//indexIT = pAPI.loadProperty(IDeployConst.PLUGIN_ID, "siteSearch.indexName.it");
		//indexEN = pAPI.loadProperty(IDeployConst.PLUGIN_ID, "siteSearch.indexName.en");
		indexALL = pAPI.loadProperty(IDeployConst.PLUGIN_ID, "siteSearch.indexName.all");
		policy = Policy.getInstance(Thread.currentThread().getContextClassLoader().getResourceAsStream(POLICY_FILENAME));
		antiSamy = new AntiSamy(policy);
		currentUser = APILocator.getUserAPI().getSystemUser();
		return super.dispatchMethod(mapping, form, request, response, name);
	}

	public ActionForward advanced(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Logger.warn(SiteSearchAction.class, "[INIT ] method advanced ");
		int nPage;

		if (UtilMethods.isSet(ParamUtil.get(request, "cerca", ""))) {
			String prequery = ParamUtil.get(request, "advQuery", "");
			String query = ParamUtil.get(request, "cerca", "").trim();
			query = sanitizeAndDecode(query);
			
			String lingua = "";
			lingua = request.getParameter("lingua");			 
			String page = ParamUtil.get(request, "curpage", "").trim();
			
			if (Integer.parseInt(page) == 0 || Integer.parseInt(page) == 1) {
				nPage = 1;
			} else {
				nPage = Integer.parseInt(page);
			}
			
			if (prequery.equals("")) {
				String[] sectionsFilter = request.getParameterValues("sectionFilter");
				String criteria = request.getParameter("select");

				if (criteria.equals("<#AND>")) {
					query = "(" + query.replaceAll(" {1,}", " && ") + ")";

				} else if (criteria.equals("<#ACCRUE>")) {
					query = "(" + query.replaceAll(" {1,}", " || ") + ")";
				} else {
					query = apice + query + apice;
				}
				
				query = aggiungiPesoQuery(query);
				String reSql = pathToExclude(currentUser) ;
				if( UtilMethods.isSet(reSql)){
					query = query + reSql;
				}
				
				if ("it".equals(lingua)) {
					query = query.concat(" && language:1 ");
				} else if ("en".equals(lingua)) {
					query = query.concat(" && language:103 ");
				} 

				if (sectionsFilter != null && sectionsFilter.length > 0) {
					query = query.concat(excludeFrmSearch);
					query = query.concat("&& +uri:");
					for (int i = 0; i < sectionsFilter.length; i++) {
						if (i == 0) {
							query = query.concat("(");
						}
						if (i != 0) {
							query = query.concat("|| ");
						}
						query = query.concat(sectionsFilter[i] + "* ");

						if (i == sectionsFilter.length - 1) {
							query = query.concat(")");
						}
					}
				} else {
					query = query.concat(simpleQuery);
				}
			} else {
				query = prequery;
			}
			
			String indexName = "";
			List<String> indices =  siteSearchAPI.listIndices();
			
			indexName = iAPI.getAliasToIndexMap(indices).get(indexALL);

			Logger.info(SiteSearchAction.class, "Query:" + query);
			Logger.info(SiteSearchAction.class, "Index name: " + indexName);
			
			SiteSearchResults results = siteSearchAPI.search(indexName, query, (nPage - 1) * 10, 10);
			request.setAttribute("searchType", "advanced");
			request.setAttribute("lingua", lingua);
			request.setAttribute("advQuery", query);
			request.setAttribute("risultatiRicerca", results);
			request.setAttribute("textQuery", ParamUtil.get(request, "cerca", "").trim());
			request.setAttribute("PAG_RIC_NAV", page);
			Logger.info(SiteSearchAction.class, "Tipo di ricerca :advanced " + query + " - lingua selezionata " + lingua );
			Logger.info(SiteSearchAction.class, "[END ] method advanced ");
			return mapping.findForward("normalSearchPage");
		}
		return null;
	}

	public ActionForward search(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Logger.info(SiteSearchAction.class, "[INIT] method search " );		
 		String indexName = "";
		int nPage;
		String query = ParamUtil.get(request, "cerca", "");		
		
		if (UtilMethods.isSet( query )) {
			String page = ParamUtil.get(request, "curpage", "").trim();
			int pg = 0 ;
			try{
				pg = Integer.parseInt(page) ;
			}catch (Exception e) {
				Logger.info(SiteSearchAction.class," Errore parse int page " + page );
			}
			if (pg == 0 || pg == 1) {
				nPage = 1;
			} else {
				nPage = pg;
			}
 			Logger.info( SiteSearchAction.class, "Input textQuery " + query + "");
			query = sanitizeAndDecode(query);
			Logger.info( SiteSearchAction.class, "Dopo sanitize textQuery " + query + "");
			query = sanitizeLuceneParameter(query);
			query = "(" + query.replaceAll(" {1,}", " && ") + ")";
			query = aggiungiPesoQuery(query);
			
			String language = getRequestLanguage(request);
			if( UtilMethods.isSet(language)){
				query = query + language;
			}
			
			String reSql = pathToExclude(currentUser) ;
			if( UtilMethods.isSet(reSql)){
				query = query + reSql;
			}
			String concatQuery = query.concat(simpleQuery);			
			indexName = iAPI.getAliasToIndexMap(siteSearchAPI.listIndices()).get(indexALL);
			if (indexName != null) {
				Logger.info(SiteSearchAction.class, "Indice di ricerca selezionato: " + indexName);
				Logger.info( SiteSearchAction.class, "Ricerca semplice. Query FINALE lucene " + concatQuery + "");
				SiteSearchResults results = siteSearchAPI.search(indexName, concatQuery, (nPage - 1) * 10, 10);			
				request.setAttribute("risultatiRicerca", results);
				request.setAttribute("textQuery", ParamUtil.get(request, "cerca", "").trim());
				request.setAttribute("PAG_RIC_NAV", page);
				Logger.info(SiteSearchAction.class, "[END ] method search ");
				return mapping.findForward("normalSearchPage");
			} else {
				Logger.info(SiteSearchAction.class, "Indice di ricerca non presente o errato.");
			}
		}

		return null;

	}
    
	private String aggiungiPesoQuery(String q){
		
		String pesata = "";
		
		if(UtilMethods.isSet(q)){
			pesata = "(title:"+q+"^2 || "+q+" || content:"+q+"^3) ";
			Logger.info(SiteSearchAction.class, "Aggiunto peso alla query: "+pesata);
			return pesata;
		}
		
		return q;
	}

	public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response) throws Exception {
		return mapping.findForward("advancedSearchPage");
	}

	@SuppressWarnings("unused")
	private String queryControl(String query) {

		String specialCharCheck = "*";
		String returnString;//

		if (query.startsWith(specialCharCheck) && query.endsWith(specialCharCheck)) {
			returnString = query;
			return returnString;

		} else if (query.startsWith(specialCharCheck) && !query.endsWith(specialCharCheck)) {

			returnString = query.concat(specialCharCheck);
			return returnString;

		} else if (!query.startsWith(specialCharCheck) && query.endsWith(specialCharCheck)) {

			returnString = specialCharCheck.concat(query);
			return returnString;

		} else {
			returnString = specialCharCheck.concat(query).concat(specialCharCheck);
			return returnString;
		}
	}
	
	private String pathToExclude(User user) {
		String badPath = "";
		Contentlet pathToExcludeContent;
		Logger.info(SiteSearchAction.class, "Query per escludere i path: "+pathToEcludeQuery);
		try {
			List<Contentlet> pathContent = APILocator.getContentletAPI().search(pathToEcludeQuery, 1, 0, "modDate desc", user, true);
			if (pathContent.size()>0) {
				
				Logger.info(SiteSearchAction.class, "Esiste almeno un contenuto che risponde alla query: "+pathToEcludeQuery);
				pathToExcludeContent = pathContent.get(0);
				badPath = pathToExcludeContent.getStringProperty("body")+ " ";
				Logger.info(SiteSearchAction.class, "Body del contenuto \"PathToExclude\": "+badPath);
				return " && "+badPath;
			}
		} catch (Exception e) {
			Logger.info(SiteSearchAction.class, "Problemi nell'individuazione del content per l'esclusione dei path nella siteSearchAction");
		}

		return badPath;
		
	}
	
	
	private String getRequestLanguage(HttpServletRequest request) throws DotDataException {
		
		String languageId;		
		if (request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE) != null) {
			languageId = ((Object) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE)).toString();
		} else {
			return " && language:1 ";
		}
		try {
			Integer.parseInt(languageId);
			Language l = lAPI.getLanguage(languageId);
			if ("it".equals(l.getLanguageCode())) {
				return " && language:1 ";
			} else if ("en".equals(l.getLanguageCode())) {
				return " && language:103 ";
			} 
		} catch (Exception e) {
			return " && language:1 ";
		}
		
		return " && language:1 ";
		
	}

	private String sanitizeAndDecode(String query) throws UnsupportedEncodingException, ScanException, PolicyException {
		String encodedString;
		if (!query.contains(" ")) {
			encodedString = URLDecoder.decode(query, "UTF-8").toLowerCase();
		} else {
			encodedString = query;
		}
		
		int numError = antiSamy.scan(encodedString, AntiSamy.DOM).getNumberOfErrors();
		
		
		encodedString = antiSamy.scan(encodedString, AntiSamy.DOM).getCleanHTML();
		encodedString = HtmlUtils.htmlUnescape(encodedString);
		return encodedString;

	}


	private String sanitizeLuceneParameter(String query) {
		if ((query == null) || 
				  query.trim().equals("") || query.trim().equals("null")) {
			return "";
		} else {
			query = query.replace("\"", "\\\"");
			query = query.replace("'", "\\'");
			query = query.replace("\\", "\\\\");
			query = query.replace("^", "\\^");
			query = query.replace("?", "\\?");
			query = query.replace("-", "\\-");
			query = query.replace("!", "\\!");
			query = query.replace("(", "\\(");
			query = query.replace(")", "\\)");
			query = query.replace("[", "\\[");
			query = query.replace("]", "\\]");
			query = query.replace("{", "\\{");
			query = query.replace("}", "\\}");
		}
		return query.trim();
	}
}
