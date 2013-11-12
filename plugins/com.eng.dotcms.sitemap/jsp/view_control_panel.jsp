<%@page import="it.eng.dotcms.sitemap.api.SitemapAPI"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="java.io.File"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
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
<%@ include file="/html/plugins/com.eng.dotcms.sitemap/init.jsp" %>

<%
SitemapAPI sitemapAPI = SitemapAPI.getInstance();
String messageInfo = null;

if(request.getParameter("createTree") !=null){
	try {
		if(request.getParameter("createTree").equals("true")) {
			sitemapAPI.createPlainTree();
			messageInfo = LanguageUtil.get(pageContext, "sitemap-builder-is-working");
		}
	} catch (Exception e) {
		messageInfo = e.toString();
	}
}

%>

<script type="text/javascript">
   	dojo.require("dijit.Tooltip");
   	
   	function createTreeMap(){
   		if(confirm("Are you sure?")) {
			var urlParams="createTree=true";
			
			refreshTool("control-panel","view_control_panel", urlParams);
   		}
	}
</script>



<fieldset>
<legend><%=LanguageUtil.get(pageContext, "sitemap-builder-fieldset") %></legend>
<%if(UtilMethods.isSet(messageInfo)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "sitemap-builder-message") %> </dt>
			<dd><%=messageInfo %></dd>
		</dl>
<%} %>

<div style="float:left">
	<button  dojoType="dijit.form.Button" onClick="createTreeMap();">
	<%= LanguageUtil.get(pageContext, "sitemap-builder-start") %> 
	</button> 
</div>

</fieldset>



