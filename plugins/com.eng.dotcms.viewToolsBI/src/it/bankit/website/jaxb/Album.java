package it.bankit.website.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Album {
	
	private String id;
	private String title;
	private String description;
	private String tn;
	private List<Img> img;
	
	public String getId() {
		return id;
	}
	
	@XmlAttribute
	public void setId(String id) {
		this.id = id;
	}
	
	public String getTitle() {
		return title;
	}
	
	@XmlAttribute
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}
	
	@XmlAttribute
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getTn() {
		return tn;
	}
	
	@XmlAttribute
	public void setTn(String tn) {
		this.tn = tn;
	}

	public List<Img> getImg() {
		return img;
	}
	
	@XmlElement
	public void setImg(List<Img> img) {
		this.img = img;
	}
	
	
}
