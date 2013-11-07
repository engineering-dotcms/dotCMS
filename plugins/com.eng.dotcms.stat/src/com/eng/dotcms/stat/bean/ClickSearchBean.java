package com.eng.dotcms.stat.bean;

import java.util.Date;

public class ClickSearchBean {
	
	public static enum PageType {
		STARTING,
		LANDING
	}
	
	private String pageId;
	
	private PageType pageType;
	
	private Date dateFrom;
	
	private Date dateTo;
	
	private Integer depth;
	
	private Integer steps;
	
	private Integer minUserPercentage;
	

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public PageType getPageType() {
		return pageType;
	}

	public void setPageType(PageType pageType) {
		this.pageType = pageType;
	}

	public Date getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}

	public Date getDateTo() {
		return dateTo;
	}

	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}

	public Integer getDepth() {
		return depth;
	}

	public void setDepth(Integer depth) {
		this.depth = depth;
	}

	public Integer getSteps() {
		return steps;
	}

	public void setSteps(Integer steps) {
		this.steps = steps;
	}

	public Integer getMinUserPercentage() {
		return minUserPercentage;
	}

	public void setMinUserPercentage(Integer minUserPercentage) {
		this.minUserPercentage = minUserPercentage;
	}
}
