package com.eng.dotcms.mostresearchedterms.bean;

import com.dotmarketing.portlets.languagesmanager.model.Language;

public class MostResearchedTermsTemp {
	
	private int id;
	private StringBuilder query;
	private Language language;
	private String host;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public StringBuilder getQuery() {
		return query;
	}
	public void setQuery(StringBuilder query) {
		this.query = query;
	}
	public Language getLanguage() {
		return language;
	}
	public void setLanguage(Language language) {
		this.language = language;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	
}
