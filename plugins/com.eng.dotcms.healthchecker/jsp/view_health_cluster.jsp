<%@page import="com.eng.dotcms.healthchecker.HealthChecker"%>
<%@page import="java.util.GregorianCalendar"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@ include file="/html/plugins/com.eng.dotcms.healthchecker/init.jsp" %>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
	String portletId1 = "EXT_HEALTH_CHECKER_CLUSTER_TOOL";
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",WebAPILocator.getUserWebAPI().getLoggedInUser(request).getLocale());
	
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
<style>

#clusterViewDetail {
	margin: 0 0 0 10px;
}
</style>
<script type="text/javascript">
	dojo.require("dijit.form.NumberTextBox");
    dojo.require("dojox.layout.ContentPane");

    function loadClusterView(){
		var url = "/html/plugins/com.eng.dotcms.healthchecker/view_cluster_view.jsp";

		var myCp = dijit.byId("clusterViewContent");

		if (myCp) {
			myCp.destroyRecursive(false);
		}
		myCp = new dojox.layout.ContentPane({
			id : "clusterViewContent"
		}).placeAt("listingClusterView");

		myCp.attr("href", url);
		myCp.refresh();
	}
    
    dojo.ready(function(){
    	loadClusterView();
		var tab =dijit.byId("mainTabContainer");
	   	dojo.connect(tab, 'selectChild',
			function (evt) {
			 	selectedTab = tab.selectedChildWidget;				  	
				  	if(selectedTab.id =="clusterView"){
				  		loadClusterView();
				  	}
			});

	});
    
</script>    
<div class="portlet-wrapper">
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

  		<div id="clusterView" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "health_checker_cluster_view") %>" >
  			<div id="clusterViewDetail">
				<%= LanguageUtil.get(pageContext, "health_Cluster_View") %> <%=df.format(new GregorianCalendar().getTime())%>: <br />
				<strong><%=HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView()%></strong>
				<br />
				<br />
				<%= LanguageUtil.get(pageContext, "health_Creator") %>:<br /> 
				<strong><%=HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView().getCreator()%></strong>
			</div>
			<hr>
			<div>&nbsp;</div>
			<div id="listingClusterView"></div>
		</div>
	</div>
</div>
