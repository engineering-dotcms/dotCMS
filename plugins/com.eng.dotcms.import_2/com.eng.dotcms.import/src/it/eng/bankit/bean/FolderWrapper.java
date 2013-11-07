package it.eng.bankit.bean;
import java.util.HashMap;
import java.util.Map;

public class FolderWrapper {
	private String path;
	private String templateName;
	private boolean showOnMenu = false;//Default false
	private Integer sortOrder;
	private Map<String,String> translations=new HashMap<String,String>();
	private Map<String,String> accessKeys=new HashMap<String,String>();
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getTemplateName() {
		return templateName;
	}
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	public boolean isShowOnMenu() {
		return showOnMenu;
	}
	public void setShowOnMenu(boolean showOnMenu) {
		this.showOnMenu = showOnMenu;
	}
	public Integer getSortOrder() {
		return sortOrder;
	}
	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}
	public Map<String, String> getTranslations() {
		return translations;
	}
	public void setTranslations(Map<String, String> translations) {
		this.translations = translations;
	}
	public Map<String, String> getAccessKeys() {
		return accessKeys;
	}
	public void setAccessKeys(Map<String, String> accessKeys) {
		this.accessKeys = accessKeys;
	}

}
