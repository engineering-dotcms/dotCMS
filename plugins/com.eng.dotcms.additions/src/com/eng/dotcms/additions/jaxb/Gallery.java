package com.eng.dotcms.additions.jaxb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Gallery {

	private Album album;

	public Album getAlbum() {
		return album;
	}
	
	@XmlElement
	public void setAlbum(Album album) {
		this.album = album;
	}
	
	
}
