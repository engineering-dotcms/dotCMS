<%@page import="com.dotmarketing.plugins.emergency.sender.EmergencySenderAPI"%>
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
<%@ include file="/html/plugins/com.dotmarketing.plugins.emergency.sender/init.jsp" %>

<%
EmergencySenderAPI esAPI = EmergencySenderAPI.getInstance();
String messageInfo = null;

if(request.getParameter("enable") !=null){
	try {
		if(request.getParameter("enable").equals("true")) {
			esAPI.enableEmergencySender();
			messageInfo = LanguageUtil.get(pageContext, "emergency-sender-is-enabled");
		} else {
			esAPI.disableEmergencySender();
			messageInfo = LanguageUtil.get(pageContext, "emergency-sender-is-disabled");
		}
	} catch (Exception e) {
		messageInfo = e.toString();
	}
}

%>

<script type="text/javascript">
   	dojo.require("dijit.Tooltip");
   
   	function disableEmergency(){
   		if(confirm("Are you sure?")) {
			var urlParams="enable=false";
			
			refreshTool("control-panel","view_control_panel", urlParams);
   		}
	}
   	
   	function enableEmergency(){
   		if(confirm("Are you sure?")) {
			var urlParams="enable=true";
			
			refreshTool("control-panel","view_control_panel", urlParams);
   		}
	}
</script>



<fieldset>
<legend><%=LanguageUtil.get(pageContext, "emergency-sender-fieldset") %></legend>
<%if(UtilMethods.isSet(messageInfo)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "emergency-sender-message") %> </dt>
			<dd><%=messageInfo %></dd>
		</dl>
<%} %>
<%


if(!esAPI.isEnabledEmergencySender()){%>
<div style="float:left">
	<button  dojoType="dijit.form.Button" onClick="enableEmergency();">
	<%= LanguageUtil.get(pageContext, "emergency-sender-enable") %> 
	</button> 
</div>

<%} else { %>
<div style="float:left">
	<button  dojoType="dijit.form.Button" onClick="disableEmergency();">
	<%= LanguageUtil.get(pageContext, "emergency-sender-disable") %> 
	</button>
</div>
<%} %>
</fieldset>



