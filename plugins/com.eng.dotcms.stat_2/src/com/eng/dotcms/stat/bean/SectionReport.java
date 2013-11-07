package com.eng.dotcms.stat.bean;

import java.util.Date;

public class SectionReport {
	private Date date;
	private String page;
	private Integer hints;
	
	
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getPage() {
		return page;
	}
	public void setPage(String page) {
		this.page = page;
	}
	public Integer getHints() {
		return hints;
	}
	public void setHints(Integer hints) {
		this.hints = hints;
	}
}
