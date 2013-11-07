package com.eng.dotcms.stat.bean;

public class ClickStepBean {
	private String pageName;
	
	private Integer hintsCount;
	
	private Integer userPercentage;

	
	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	public Integer getUserPercentage() {
		return userPercentage;
	}

	public void setUserPercentage(Integer userPercentage) {
		this.userPercentage = userPercentage;
	}

	public Integer getHintsCount() {
		return hintsCount;
	}

	public void setHintsCount(Integer hintsCount) {
		this.hintsCount = hintsCount;
	}
}
