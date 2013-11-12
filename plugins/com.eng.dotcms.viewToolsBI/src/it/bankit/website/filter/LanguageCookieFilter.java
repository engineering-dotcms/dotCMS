package it.bankit.website.filter;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class LanguageCookieFilter implements Filter {
	private static final String alreadyFilteredAttributeName = "_it.bankit.website.filter.LanguageCokieFilter.FILTERED";
	private static final String LANGUAGE_BANKIT_FRONTEND_PARAMETER = "lang";
	private static final int COOKIE_AGE = 604800;// Una settimana in
	// secondi(60*60*24*7)
	private static final String FONT_FRONTEND_PARAMETER = "FONT";
	private static final String FONT_BACKEND_ATTRIBUTE  = "FONT_REQ";

	private LanguageAPI langAPI;

	@Override
	public void init(FilterConfig config) throws ServletException {
		langAPI = APILocator.getLanguageAPI();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		if (request.getAttribute(alreadyFilteredAttributeName) == null) {
			// Do invoke this filter...
			request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
			try {
				doFilterInternal(httpRequest, httpResponse, filterChain);
			} finally {
				// Remove the "already filtered" request attribute for this
				// request.
				request.removeAttribute(alreadyFilteredAttributeName);
			}
		} else {
			filterChain.doFilter(request, response);
		}
	}

	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		HttpSession session = request.getSession(true);
		checkFontFromRequest(request);
		if (session != null) {
			String uri = request.getRequestURI();
			String languageIdCookie = null;
			Language cookieLanguage = null;
			languageIdCookie = UtilMethods.getCookieValue(request.getCookies(), com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
			if (languageIdCookie != null && !languageIdCookie.equals("0")) {
				try {
					Integer.parseInt(languageIdCookie);
					cookieLanguage = langAPI.getLanguage(languageIdCookie);
				} catch (Exception e) {
					cookieLanguage = langAPI.getDefaultLanguage();
				}

			}
			// If backend the locale is defined by the user profile
			if (uri.startsWith("/c/")) {
				Locale curLocale = (Locale) session.getAttribute(Globals.LOCALE_KEY);
				if (curLocale == null) {
					Locale newLocale = null;
					if (cookieLanguage != null) {
						newLocale = new Locale(cookieLanguage.getLanguageCode(), cookieLanguage.getCountryCode());
					} else {
						newLocale = request.getLocale();
					}
					session.setAttribute(Globals.LOCALE_KEY, newLocale);
					session.setAttribute(com.dotmarketing.util.WebKeys.LOCALE, newLocale);
				}
			} else {
				// if frontend the locale is defined by the dotCMS frontend
				// language session variables

				Language newLanguage = null;
				String newLanguageId = null;
				Locale clientLocal = null;
				Language clientLanguage = null;
				String clientLocalId = null;
				String languageIdSession = null;

				languageIdSession = (String) session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
				// Search in parameter
				String languageWebKeyParameter = request.getParameter(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
				String languageIdParameter = request.getParameter("language_id");
				String languageBankitParameter = request.getParameter(LANGUAGE_BANKIT_FRONTEND_PARAMETER);
				if( !UtilMethods.isSet(languageBankitParameter ) ){
					if(request.getAttribute("lang") != null ){
						languageBankitParameter = (String) request.getAttribute("lang");
					}
				}
				if (UtilMethods.isSet(languageWebKeyParameter) || UtilMethods.isSet(languageIdParameter) || UtilMethods.isSet(languageBankitParameter)) {
					if (UtilMethods.isSet(languageWebKeyParameter)) {
						newLanguageId = languageWebKeyParameter;
						try {
							newLanguage = langAPI.getLanguage(newLanguageId);
						} catch (Exception e) {
							newLanguage = langAPI.getDefaultLanguage();
						}
					} else if (UtilMethods.isSet(languageIdParameter)) {
						newLanguageId = languageIdParameter;
						try {
							newLanguage = langAPI.getLanguage(newLanguageId);
						} catch (Exception e) {
							newLanguage = langAPI.getDefaultLanguage();
						}
					} else {
						if (languageBankitParameter.equalsIgnoreCase("en")) {
							newLanguage = langAPI.getLanguage("en", "US");
						} else {
							newLanguage = langAPI.getDefaultLanguage();
						}
						newLanguageId = String.valueOf(newLanguage.getId());
					}

					// Clear attributes
					if (languageWebKeyParameter != null) {
						request.removeAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
					}
					if (languageIdParameter != null) {
						request.removeAttribute("language_id");
					}

				}

				if (newLanguageId == null && cookieLanguage == null) {
					// Nessun settaggio di linguaggio, tento il recupero dal
					// client
					clientLocal = request.getLocale();
					if (clientLocal != null) {
						clientLanguage = getLanguageFromLocale(clientLocal);
						if (clientLanguage != null) {
							clientLocalId = String.valueOf(clientLanguage.getId());
						}
					}
				}

				if (languageIdSession == null) {// Nulla in sessione
					if (newLanguage != null) {// Nuovo linguaggio
						setSession(newLanguage, session);
						if (cookieLanguage == null || !languageIdCookie.equals(newLanguageId)) {
							setCookie(newLanguageId, (HttpServletResponse) response);
						}
					} else if (cookieLanguage != null) {// da cookie
						setSession(cookieLanguage, session);
					} else if (clientLocalId != null) {// linguaggio da client
						setSession(clientLanguage, session);
						if (languageIdCookie == null || !languageIdCookie.equals(clientLocalId)) {
							setCookie(clientLocalId, (HttpServletResponse) response);
						}
					} else {// default language
						setSession(langAPI.getDefaultLanguage(), session);
						String defaultLanguageId = String.valueOf(langAPI.getDefaultLanguage().getId());
						if (languageIdCookie == null || !languageIdCookie.equals(defaultLanguageId)) {
							setCookie(defaultLanguageId, (HttpServletResponse) response);
						}
					}
				} else {// language già presente in sessione
					if (newLanguage != null) {// Nuovo linguaggio
						if (!languageIdSession.equals(newLanguageId)) {
							setSession(newLanguage, session);
						}
						if (languageIdCookie == null || !languageIdCookie.equals(newLanguageId)) {
							setCookie(newLanguageId, (HttpServletResponse) response);
						}
					}
				}

			}
		}
		filterChain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		Logger.info(LanguageCookieFilter.class, "Destroying character encoding filter...");
	}

	private Language getLanguageFromLocale(Locale locale) {
		Language language = null;
		if (locale != null) {
			if (UtilMethods.isSet(locale.getLanguage()) && UtilMethods.isSet(locale.getCountry())) {
				language = langAPI.getLanguage(locale.getLanguage(), locale.getCountry());
			} else if ("it".equalsIgnoreCase(locale.getLanguage())) {
				language = langAPI.getLanguage("it", "IT");
			} else if ("en".equalsIgnoreCase(locale.getLanguage())) {
				language = langAPI.getLanguage("en", "US");
			}
		}
		return language;
	}

	private void setSession(Language newLanguage, HttpSession session) {
		Locale locale = new Locale(newLanguage.getLanguageCode(), newLanguage.getCountryCode());
		session.setAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(newLanguage.getId()));
		boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		if (ADMIN_MODE == false) {
			session.setAttribute(Globals.LOCALE_KEY, locale);
		}
		session.setAttribute(com.dotmarketing.util.WebKeys.LOCALE, locale);
	}

	private void setCookie(String newLanguageId, HttpServletResponse response) {
		Cookie languageCookie = new Cookie(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE, newLanguageId);
		languageCookie.setPath("/");
		languageCookie.setMaxAge(COOKIE_AGE);
		response.addCookie(languageCookie);
	}


	private void checkFontFromRequest( HttpServletRequest request ){
		String fontWebKeyParameter = request.getParameter( FONT_FRONTEND_PARAMETER );
		if( UtilMethods.isSet(fontWebKeyParameter) ){
			request.setAttribute(FONT_BACKEND_ATTRIBUTE, fontWebKeyParameter);
		}else{
			String actionKeyParameter = request.getParameter("action" );
			if(  UtilMethods.isSet(actionKeyParameter)  ) {
				int v = actionKeyParameter.lastIndexOf( "_setlanguage.action?");
					
				 if( v !=-1  ) {
						String sub = actionKeyParameter.substring(v+"_setlanguage.action?".length() );
						
					String ln =  sub ;
					String[] paremtro = ln.split("=");
					if( paremtro.length> 0 ){
						String paremtroS =  paremtro[0];
						if( paremtroS.equalsIgnoreCase("LANGUAGE") ){

							String languageWebKeyParameter =  paremtro[1];
							 request.setAttribute("lang", languageWebKeyParameter);
						}else if( paremtroS.equalsIgnoreCase("FONT")  ){
							request.setAttribute(FONT_BACKEND_ATTRIBUTE, paremtro[1]);
						}
					}
				}
			}else{
				request.removeAttribute(FONT_BACKEND_ATTRIBUTE );
			}
		}

	}

}
