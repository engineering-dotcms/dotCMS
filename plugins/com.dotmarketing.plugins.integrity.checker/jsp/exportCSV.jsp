<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.loader.DataIntegrityCsvExportThread"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.loader.ExportCsvStatus"%>
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

if(ExportCsvStatus.isExporting) {
	out.print("The export process is already running - "+ExportCsvStatus.currentCount + " of "+ExportCsvStatus.totalContent);
} else {
	new Thread(new DataIntegrityCsvExportThread(Config.getStringProperty("DATA_INTEGRITY_CHECK_EXPORT_PATH"))).start();
}

%>
