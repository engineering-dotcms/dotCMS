<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@ include file="/html/plugins/com.eng.dotcms.stat/init.jsp" %>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
	String portletId1 = "PLUGIN_CLICKSTREAM_STAT_BANKIT";
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
	function loadList (idTab, page) {
		refreshTabList(idTab, page, urlParams);
	}	
	
	function refreshTabList(idTab, page, urlParams){
		
		var url = "/html/plugins/com.eng.dotcms.stat/"+page+".jsp?"+ urlParams;		
		
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
			 		
			 	if(selectedTab.id =="search-stat-panel"){
			  		loadList("search-stat-panel","view_stat_form");
			  	} else if(selectedTab.id =="search-stat-sezione-panel") {
			  		loadList("search-stat-sezione-panel","view_stat_sezione_form");
				} else {
					loadList("search-stat-panel","view_stat_form");
				}
				 	
			});

	});
</script>



<div class="portlet-wrapper">
	
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
		<div id="search-stat-panel" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "search-stat-panel") %>" >
  			<div id="search-stat-panel_results">
			</div>

  		</div>
  		
  		<div id="search-stat-sezione-panel" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "search-stat-sezione-panel") %>" >
  			<div id="search-stat-sezione-panel_results">
			</div>

  		</div>
	
	</div>
</div>

<script type="text/javascript">
dojo.addOnLoad(function () {dijit.byId('mainTabContainer').selectChild('search-stat-panel');});
</script>

