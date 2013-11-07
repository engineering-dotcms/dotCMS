<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@ include file="/html/plugins/com.dotmarketing.plugins.emergency.sender/init.jsp" %>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
	String portletId1 = "EXT_INTEGRITY_CHECK_TOOL";
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	
	if (!com.dotmarketing.util.UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

	
%>
<div class="portlet-wrapper">
	<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>
</div>

<script type="text/javascript">
	dojo.require("dijit.form.NumberTextBox");
    dojo.require("dojox.layout.ContentPane");
	
    var urlParams = "";
	function loadTool (idTab, page) {
		refreshTool(idTab, page, urlParams);
	}	
	
	function refreshTool(idTab, page, urlParams){

		var url = "/html/plugins/com.dotmarketing.plugins.emergency.sender/"+page+".jsp?"+ urlParams;		
		
		var myCp = dijit.byId(idTab+"-content");	
		
		if (myCp) {
			myCp.destroyRecursive(false);
		}
		myCp = new dojox.layout.ContentPane({
			id : idTab+"-content"
		}).placeAt(idTab+"_results");

		myCp.attr("href", url);
		
		myCp.refresh();

	}
	
	
	dojo.ready(function(){
		
		var tab =dijit.byId("mainTabContainer");
		
	   	dojo.connect(tab, "selectChild",
			function (evt) {
	   		
			 	selectedTab = tab.selectedChildWidget;
			 		
				 	if(selectedTab.id =="control-panel-tab"){
				 		loadTool("control-panel","view_control_panel");
				  	}
			});

	});
</script>



<div class="portlet-wrapper">
	
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
		<div id="control-panel-tab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "emergency-sender-control-panel") %>" >
  			<div id="control-panel_results">
			</div>

  		</div>
	
	</div>
</div>

<script type="text/javascript">
dojo.addOnLoad(function () {dijit.byId('mainTabContainer').selectChild('control-panel-tab');});
</script>


