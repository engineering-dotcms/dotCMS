package com.eng.dotcms.stat.bean;

public class SectionSearchBean {
	private String section;
	private Integer year;
	private Integer monthFrom;
	private Integer monthTo;
	
	public String getSection() {
		return section;
	}
	public void setSection(String section) {
		this.section = section;
	}
	public Integer getYear() {
		return year;
	}
	public void setYear(Integer year) {
		this.year = year;
	}
	public Integer getMonthFrom() {
		return monthFrom;
	}
	public void setMonthFrom(Integer monthFrom) {
		this.monthFrom = monthFrom;
	}
	public Integer getMonthTo() {
		return monthTo;
	}
	public void setMonthTo(Integer monthTo) {
		this.monthTo = monthTo;
	}
}
