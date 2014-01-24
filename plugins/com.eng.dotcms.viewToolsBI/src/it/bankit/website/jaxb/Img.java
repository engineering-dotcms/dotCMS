package it.bankit.website.jaxb;

import javax.xml.bind.annotation.XmlAttribute;

public class Img {
	
	private String src;
	private String title;
	private String caption;
	private String pause;
	private String tn;
	
	public String getSrc() {
		return src;
	}
	
	@XmlAttribute
	public void setSrc(String src) {
		this.src = src;
	}
	
	public String getTitle() {
		return title;
	}
	
	@XmlAttribute
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getCaption() {
		return caption;
	}
	
	@XmlAttribute
	public void setCaption(String caption) {
		this.caption = caption;
	}
	
	public String getPause() {
		return pause;
	}
	
	@XmlAttribute
	public void setPause(String pause) {
		this.pause = pause;
	}

	public String getTn() {
		return tn;
	}
	
	@XmlAttribute
	public void setTn(String tn) {
		this.tn = tn;
	}	
	
}
