<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.loader.CSVLoaderAPI"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.DataWrapper"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotmarketing.util.UUIDGenerator"%>
<%@page import="java.util.UUID"%>
<%@page import="org.apache.commons.collections.MapUtils"%>
<%@page import="java.io.FileWriter"%>
<%@page import="java.io.ByteArrayOutputStream"%>
<%@page import="java.io.BufferedOutputStream"%>
<%@page import="com.thoughtworks.xstream.io.xml.DomDriver"%>
<%@page import="com.thoughtworks.xstream.XStream"%>
<%@page import="java.security.MessageDigest"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotcms.enterprise.publishing.sitesearch.ESSiteSearchPublisher"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.publishing.PublisherConfig"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%

Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);

User systemUser = APILocator.getUserAPI().getSystemUser();
List<Contentlet> contents =
	APILocator.getContentletAPI().search("+structureName:News", 0, -1, null, systemUser, false);

Date now = new Date();
String nowString = new SimpleDateFormat("dd/MM/yyyy").format(now);


for(Contentlet con: contents) {
	XStream xstream = new XStream(new DomDriver());

	try {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		BufferedOutputStream output = new BufferedOutputStream(byteStream);
		
		DataWrapper data = new DataWrapper(con);

		xstream.toXML(data, output);		
		
		MessageDigest algorithm = MessageDigest.getInstance("MD5");
		algorithm.reset();
		algorithm.update(byteStream.toByteArray());
		byte messageDigest[] = algorithm.digest();
	            
		StringBuilder hexString = new StringBuilder();
		for (int i=0;i<messageDigest.length;i++) {
			hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
		}
		String foo = messageDigest.toString();


		out.println(con.getInode() + " - "+ con.getTitle()+" ---- md5 version is "+hexString.toString()+"<br />");
		
		out.print("--------------------------------------------------------<br />");
		for(String chiave: data.getOrderedMap().keySet()) {
			out.print("Key: "+chiave+" Value: "+ data.getOrderedMap().get(chiave)+"<br />");
		}
		out.print("--------------------------------------------------------<br />");
		
		
		out.print("-------------------------XML-------------------------<br />");
		xstream.toXML(data, out);
		out.print("-------------------------END XML---------------------<br />");
		
		output.close();
	} catch (Exception e) {
		e.printStackTrace();
	}		
}

%>
