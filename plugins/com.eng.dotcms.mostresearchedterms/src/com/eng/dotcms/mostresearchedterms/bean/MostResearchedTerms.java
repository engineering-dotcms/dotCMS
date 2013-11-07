package com.eng.dotcms.mostresearchedterms.bean;

import com.dotmarketing.portlets.languagesmanager.model.Language;

public class MostResearchedTerms {
	
	private String term;
	private int occur;
	private Language language;
	private String host;
	
	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		this.term = term;
	}
	public int getOccur() {
		return occur;
	}
	public void setOccur(int occur) {
		this.occur = occur;
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
