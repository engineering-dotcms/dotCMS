package com.eng.dotcms.mostresearchedterms.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;

public class Util {
	
	public static StringBuilder retrieveParameters(String htmlFieldName, HttpServletRequest req, AntiSamy antiSamy) throws ScanException, PolicyException {
		StringBuilder parameters = new StringBuilder();
		String[] _parameters = htmlFieldName.split("[,]");
		if(_parameters.length==1){
			if(null!=req.getParameter(_parameters[0]))
				parameters.append(antiSamy.scan(req.getParameter(_parameters[0]),AntiSamy.DOM).getCleanHTML());
		}else	
			for(int i=0; i<_parameters.length; i++){
				String value = req.getParameter(_parameters[i]);
				if(null!=value){
					if(i>0 && parameters.length()>0)
						parameters.append("|");
					parameters.append(antiSamy.scan(value,AntiSamy.DOM).getCleanHTML());
				}
			}
		return parameters.length()>0?parameters:null;
	}
	
	public static List<String> getQueryTermsList(String aQuery){
		String[] queries = aQuery.split("[|]");
		List<String> result = new ArrayList<String>();
		for(String query: queries){
			String[] terms_for_query = query.split("[ ]");
			result.addAll(Arrays.asList(terms_for_query));
		}
		return result;
	}
}
