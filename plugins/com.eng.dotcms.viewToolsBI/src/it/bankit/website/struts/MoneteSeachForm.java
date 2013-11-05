package it.bankit.website.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.ValidatorForm;

import com.liferay.util.ParamUtil;

public class MoneteSeachForm extends ValidatorForm {

	String cerca;
	
	private static String defaulIT = "Cerca nel sito";
	private static String defaulEN = "Search";

	public String getCerca() {
		return cerca;
	}

	public void setCerca(String cerca) {
		this.cerca = cerca;
	}

	public MoneteSeachForm() {

	}

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		
		ActionErrors errors;
		
		String queryRicerca = ParamUtil.get(request, "ricerca_testo", "default");
		
		if (queryRicerca.equals("default") || queryRicerca.trim().equals(defaulIT) || queryRicerca.trim().equals(defaulEN)) {
			
			errors = new ActionErrors();
			
			request.getSession().setAttribute(ActionMessages.GLOBAL_MESSAGE, "message.search.noResult");
			errors.add(ActionMessages.GLOBAL_MESSAGE,new ActionMessage("message.search.noResult"));
			return errors;

		} 
		
		return null; 

	}

}
