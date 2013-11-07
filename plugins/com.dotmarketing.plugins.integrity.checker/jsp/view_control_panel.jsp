<%@page import="com.dotmarketing.plugin.business.PluginAPI"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.loader.ExportCsvStatus"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.loader.DataIntegrityCsvExportThread"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.loader.DataIntegrityCsvLoadThread"%>
<%@page import="java.io.File"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.loader.LoadStatus"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.loader.CSVLoaderAPI"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.diff.DataIntegrityResultWrapper"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.diff.DataIntegrityCheckAPI"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotcms.publisher.business.DotPublisherException"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotcms.publisher.business.PublisherAPI"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ include file="/html/plugins/com.dotmarketing.plugins.integrity.checker/init.jsp" %>

<%
PluginAPI pAPI = APILocator.getPluginAPI();
String pluginId = "com.dotmarketing.plugins.integrity.checker";

String messageImport = null;
String messageExport = null;

String csvFolder = pAPI.loadProperty(pluginId, "DATA_INTEGRITY_CHECK_CSV_FOLDER");

if(request.getParameter("doImport") !=null){
	if(!LoadStatus.isLoading) {
		try {
			messageImport = LanguageUtil.get(pageContext, "data-integrity-import-started");
			String localPath = csvFolder+File.separator+"local.csv";
			String remotePath = csvFolder+File.separator+"remote.csv";
			File local = new File(localPath);
			File remote = new File(remotePath);
			
			if(local.exists() && remote.exists())
				new Thread(new DataIntegrityCsvLoadThread(localPath, remotePath)).start();
			else
				messageImport = LanguageUtil.get(pageContext, "data-integrity-file-not-found");
		} catch(Exception e) {
			messageImport = e.toString();
		}
	} else {
		messageImport = LanguageUtil.get(pageContext, "data-integrity-isLoading");
	}
}

if(request.getParameter("refresh") !=null){
	if(LoadStatus.isLoading) {
		messageImport = LanguageUtil.get(pageContext, "data-integrity-isLoading");
	}
	
	if(ExportCsvStatus.isExporting) {
		messageExport = LanguageUtil.get(pageContext, "data-integrity-export-started") + " - "+ ExportCsvStatus.currentCount + " of "+ExportCsvStatus.totalContent;
	}
}


if(request.getParameter("doExport") !=null){

	try {
		messageExport = LanguageUtil.get(pageContext, "data-integrity-export-started");
		
		if(!ExportCsvStatus.isExporting)
			new Thread(new DataIntegrityCsvExportThread(
					pAPI.loadProperty(pluginId,"DATA_INTEGRITY_CHECK_EXPORT_PATH"),
					pAPI.loadProperty(pluginId,"DATA_INTEGRITY_LUCENE_QUERY")
					)).start();
		else
			messageExport = LanguageUtil.get(pageContext, "data-integrity-export-started") + " - "+ ExportCsvStatus.currentCount + " of "+ExportCsvStatus.totalContent;
	} catch(Exception e) {
		messageExport = e.toString();
	}
}

%>

<script type="text/javascript">
   	dojo.require("dijit.Tooltip");
   
   	function doImport(){
   		if(confirm("Are you sure?")) {
			var urlParams="doImport=true";
			
			refreshDiffList("control-panel","view_control_panel", urlParams);
   		}
	}
   	
   	function doExport(){
   		if(confirm("Are you sure?")) {
			var urlParams="doExport=true";
			
			refreshDiffList("control-panel","view_control_panel", urlParams);
   		}
	}
   	
   	function refresh(){
		refreshDiffList("control-panel","view_control_panel", "refresh=true");
	}
</script>


<div style="
display: block;
width: 100%;
text-align: right;">
	<button  dojoType="dijit.form.Button" onClick="refresh();">
	<%= LanguageUtil.get(pageContext, "data-integrity-refresh") %> 
	</button> 
</div>


<fieldset>
<legend><%=LanguageUtil.get(pageContext, "data-integrity-export-fieldset") %></legend>
<%if(UtilMethods.isSet(messageExport)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "data-integrity-message") %> </dt>
			<dd><%=messageExport %></dd>
		</dl>
<%} %>
<div style="float:left">
	<button  dojoType="dijit.form.Button" onClick="doExport();">
	<%= LanguageUtil.get(pageContext, "data-integrity-export") %> 
	</button> 
</div>
</fieldset>


<fieldset>
<legend><%=LanguageUtil.get(pageContext, "data-integrity-import-fieldset") %></legend>
<%if(UtilMethods.isSet(messageImport)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "data-integrity-message") %> </dt>
			<dd><%=messageImport %></dd>
		</dl>
<%} %>
<div style="float:left">
	<button  dojoType="dijit.form.Button" onClick="doImport();">
	<%= LanguageUtil.get(pageContext, "data-integrity-import") %> 
	</button> 
</div>
</fieldset>		


