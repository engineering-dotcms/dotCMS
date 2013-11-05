package it.bankit.website.struts;

import it.bankit.website.deploy.IDeployConst;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

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

public class MoneteSiteSearchAction extends DispatchAction {

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


	private static String simpleQuery = " && -uri:(*rss.html) -mimeType:image* ";
	private static String pathToEcludeQuery = "+StructureName:webPageContent +webPageContent.title:*PathToExcludeMIU*";

	private static String excludeFrmSearch = " && -uri:(*rss.html) && -mimeType:image* ";
	private static String apice = "\"";
	private static String indexALL;

	@Override
	protected ActionForward dispatchMethod(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response, String name) throws Exception {

		indexALL = pAPI.loadProperty(IDeployConst.PLUGIN_ID,
				"moneteSiteSearch.indexName.all");

		policy = Policy.getInstance(this.getClass().getClassLoader()
				.getResourceAsStream(POLICY_FILENAME));
		antiSamy = new AntiSamy(policy);
		currentUser = APILocator.getUserAPI().getSystemUser();
		return super.dispatchMethod(mapping, form, request, response, name);
	}

	public ActionForward unspecified(ActionMapping mapping, ActionForm lf,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		MoneteSeachForm form = (MoneteSeachForm) lf;
		String indexName = "";
		int nPage;

		if (UtilMethods.isSet(ParamUtil.get(request, "ricerca_testo", ""))) {

			String query = ParamUtil.get(request, "ricerca_testo", "");
			String page = ParamUtil.get(request, "curpage", "").trim();

			if (Integer.parseInt(page) == 0 || Integer.parseInt(page) == 1) {
				nPage = 1;
			} else {
				nPage = Integer.parseInt(page);
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
			SiteSearchResults results = siteSearchAPI.search(indexName, query,
					(nPage - 1) * 10, 10);

			request.setAttribute("risultatiRicerca", results);
			request.setAttribute("textQuery", query);
			request.setAttribute("PAG_RIC_NAV", page);

			return mapping.findForward("normalSearchPage");

		}

		return null;

	}

	private String aggiungiPesoQuery(String q) {

		String pesata = "";

		if (UtilMethods.isSet(q)) {
			pesata = "(title:" + q + "^2 || " + q + " || content:" + q + "^3) ";
			Logger.info(SiteSearchAction.class, "Aggiunto peso alla query: "
					+ pesata);
			return pesata;
		}

		return q;
	}

	private String sanitizeAndDecode(String query)
			throws UnsupportedEncodingException, ScanException, PolicyException {

		String encodedString;
		if (!query.contains(" ")) {
			encodedString = URLDecoder.decode(query, "UTF-8").toLowerCase();
		} else {
			encodedString = query;
		}
		encodedString = antiSamy.scan(encodedString, antiSamy.DOM)
				.getCleanHTML();
		return encodedString;

	}

	private String getRequestLanguage(HttpServletRequest request)
			throws DotDataException {

		String languageId;
		if (request.getSession().getAttribute(
				com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE) != null) {
			languageId = ((Object) request.getSession().getAttribute(
					com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE))
					.toString();
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
