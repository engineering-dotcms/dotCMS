package com.dotcms.publishing;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XStreamInstance {
	
	public static XStreamInstance INSTANCE = new XStreamInstance();
	
	private XStream xstream;
	
	private XStreamInstance() {
		xstream = new XStream(new DomDriver());
	}
	
	public XStream getXStream() {
		return xstream;
	}
	
	
}
