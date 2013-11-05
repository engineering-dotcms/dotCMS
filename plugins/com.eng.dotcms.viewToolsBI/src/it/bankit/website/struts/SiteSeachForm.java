package it.bankit.website.struts;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.ValidatorForm;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;

import com.liferay.util.ParamUtil;

public class SiteSeachForm extends ValidatorForm {

	String cerca;

	private static String defaulIT = "Inserisci un testo";
	private static String defaulEN = "Enter text";

	private static String POLICY_FILENAME = "antisamy-bankit.xml";
	private static Policy policy;
	private static AntiSamy antiSamy;

	public String getCerca() {
		return cerca;
	}

	public void setCerca(String cerca) {
		this.cerca = cerca;
	}

	public SiteSeachForm() {

	}

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {

		ActionErrors errors;
		try {
			policy = Policy.getInstance(this.getClass().getClassLoader().getResourceAsStream(POLICY_FILENAME));
		} catch (PolicyException e1) {
			e1.printStackTrace();
		}
		antiSamy = new AntiSamy(policy);

		String queryRicerca;
		
		try {
			queryRicerca = sanitizeAndDecode(ParamUtil.get(request, "cerca", "default"));
			if (queryRicerca.equals("default") || queryRicerca.trim().equals(defaulIT) || queryRicerca.trim().equals(defaulEN) || queryRicerca.length()>250) {
				errors = new ActionErrors();
				request.getSession().setAttribute(ActionMessages.GLOBAL_MESSAGE, "message.search.noResult");
				errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.search.noResult"));
				return errors;
			}
			
			String numPag = (String)ParamUtil.get(request, "curpage", "default");
			try {
				int page = Integer.parseInt(numPag);
			} catch (NumberFormatException e) {
				errors = new ActionErrors();
				request.getSession().setAttribute(ActionMessages.GLOBAL_MESSAGE, "message.search.pageError");
				errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.search.pageError"));
				return errors;
			}
			return null;
		} catch (UnsupportedEncodingException e) {
			errors = new ActionErrors();
			request.getSession().setAttribute(ActionMessages.GLOBAL_MESSAGE, "message.search.noResult");
			errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.search.noResult"));
			e.printStackTrace();
			return errors;
		} catch (ScanException e) {
			errors = new ActionErrors();
			request.getSession().setAttribute(ActionMessages.GLOBAL_MESSAGE, "message.search.noResult");
			errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.search.noResult"));
			e.printStackTrace();
			return errors;
		} catch (PolicyException e) {
			errors = new ActionErrors();
			request.getSession().setAttribute(ActionMessages.GLOBAL_MESSAGE, "message.search.noResult");
			errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.search.noResult"));
			e.printStackTrace();
			return errors;
		}
		
		

	}

	private String sanitizeAndDecode(String query) throws UnsupportedEncodingException, ScanException, PolicyException {

		String encodedString;
		if (!query.contains(" ")) {
			encodedString = URLDecoder.decode(query, "UTF-8").toLowerCase();
		} else {
			encodedString = query;
		}
		encodedString = antiSamy.scan(encodedString, antiSamy.DOM).getCleanHTML();
		return encodedString;

	}

}
