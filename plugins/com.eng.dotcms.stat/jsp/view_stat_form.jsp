<%@page import="com.eng.dotcms.stat.bean.ClickStepBean"%>
<%@page import="com.eng.dotcms.stat.bean.ClickSearchBean"%>
<%@page import="com.eng.dotcms.stat.ClickstreamStatisticsAPI"%>
<%@page import="com.dotmarketing.plugin.business.PluginAPI"%>
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
<%@ include file="/html/plugins/com.eng.dotcms.stat/init.jsp" %>

<style type="text/css">
#searchForm {
	width: 100%;
	min-height: 150px ;
}

.formField {
	float: left;
	margin-right:1.5em;
}

.formField label {
	margin-left: 3px;
}
</style>

<script type="text/javascript">
dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
dojo.require('dotcms.dijit.form.FileSelector');
dojo.require("dotcms.dijit.FileBrowserDialog");
dojo.require("dijit.form.NumberTextBox");
dojo.require("dijit.Tooltip");

function refreshSearch(idTab, page, urlParams){
	
	var url = "/html/plugins/com.eng.dotcms.stat/"+page+".jsp?"+ urlParams;		
	
	var myCp = dijit.byId(idTab+"-content-search");	

	if (myCp) {
		myCp.destroyRecursive(false);
	}

	myCp = new dojox.layout.ContentPane({
		id : idTab+"-content-search"
	}).placeAt(idTab+"_results");

	myCp.attr("href", url);
	
	myCp.refresh();

}

function searchStep() {
	console.log(dijit.byId("internalLinkType"));
	var urlParam = "";

	if(dijit.byId("internalLinkType").value  != "")
		urlParam += "&pageId="+dijit.byId("internalLinkType").value;

	if(dijit.byId('dateFrom').get('value') != null) {
		var dateValue = 
				dojo.date.locale.format(dijit.byId('dateFrom').get('value'),
				{datePattern: "yyyy-MM-dd", selector: "date"});
		
		urlParam+="&dateFrom="+dateValue;
	}

	if(dijit.byId('dateTo').get('value') != null) {
		var dateValue = 
				dojo.date.locale.format(dijit.byId('dateTo').get('value'),
				{datePattern: "yyyy-MM-dd", selector: "date"});
		
		urlParam+="&dateTo="+dateValue;
	}

	if(!isNaN(dijit.byId("depth").value))
		urlParam += "&depth="+dijit.byId("depth").value;

	//if(!isNaN(dijit.byId("maxStep").value))
		//urlParam += "&maxStep="+dijit.byId("maxStep").value;

	if(!isNaN(dijit.byId("minPerc").value))
		urlParam += "&minPerc="+dijit.byId("minPerc").value;

	if(dijit.byId("landingPage").checked)
		urlParam += "&pageType="+dijit.byId("landingPage").value;
	else
		urlParam += "&pageType="+dijit.byId("startingPage").value;

	
	refreshSearch("stat-search-result","view_stat_search", "refresh=true"+urlParam);
}
</script>


<div id="searchForm">
	<div id="pageSelector" class="formField" style="width:100%; margin-bottom: 1em;">
		<input  id="internalLinkType"  
			type="text" name="internalLinkIdentifier" 
			dojoType="dotcms.dijit.form.FileSelector" 
			fileBrowserView="list"  showThumbnail="false"
			 />
			
		<label><%= LanguageUtil.get(pageContext, "search-stat-pageType") %></label>
		<input dojoType="dijit.form.RadioButton" type="radio" name="pageType" value="<%=ClickSearchBean.PageType.LANDING %>" checked="checked" id="landingPage" />
		<label for="landingPage"><%= LanguageUtil.get(pageContext, "search-stat-landing") %></label>	
		&nbsp;
		&nbsp;
		<input dojoType="dijit.form.RadioButton" type="radio" name="pageType" value="<%=ClickSearchBean.PageType.STARTING %>"  id="startingPage" />
		<label for="startingPage"><%= LanguageUtil.get(pageContext, "search-stat-starting") %></label>
	</div>
	
	
	<div id="dataFromSelector" class="formField" style="width:115px;">
		<label for="dateFrom"><%= LanguageUtil.get(pageContext, "search-stat-dateFrom") %></label>
		<input 
			type="text" 
			dojoType="dijit.form.DateTextBox" 
			validate="return false;" 
			invalidMessage=""  
			id="dateFrom"
			name="dateFrom" value=""
			style="width:100%">
			<!-- input type="text" name="publishDate" id="publishTime" value="now" style="width:100px;"
			  data-dojo-type="dijit.form.TimeTextBox"
			  onChange="dojo.byId('val').value=arguments[0].toString().replace(/.*1970\s(\S+).*/,'T$1')"
			  required="true" /> -->
	</div>
	
	<div id="dataToSelector" class="formField" style="width:115px;">
		<label for="dateTo"><%= LanguageUtil.get(pageContext, "search-stat-dateTo") %></label>
		<input 
			type="text" 
			dojoType="dijit.form.DateTextBox" 
			validate="return false;" 
			invalidMessage=""  
			id="dateTo"
			name="dateTo" value=""
			style="width:100%">
			<!-- input type="text" name="publishDate" id="publishTime" value="now" style="width:100px;"
			  data-dojo-type="dijit.form.TimeTextBox"
			  onChange="dojo.byId('val').value=arguments[0].toString().replace(/.*1970\s(\S+).*/,'T$1')"
			  required="true" /> -->
	</div>
	
	<div id="depthSelector" class="formField" style="width:110px">
		<label for="depth"><%= LanguageUtil.get(pageContext, "search-stat-depth") %></label>
		<input 
			type="text" 
			dojoType="dijit.form.NumberTextBox" 
			validate="return false;" 
			invalidMessage=""  
			id="depth"
			name="depth"
			style="width:100%">
	</div>
	
	<!-- div id="maxStepSelector" class="formField" style="width:50px">
		<label for="maxStep">Step</label>
		<input 
			type="text" 
			dojoType="dijit.form.NumberTextBox" 
			validate="return false;" 
			invalidMessage=""  
			id="maxStep"
			name="maxStep"
			style="width:100%">
	</div-->
	
	<div id="minPercSelector" class="formField" style="width:130px">
		<label for="minPerc"><%= LanguageUtil.get(pageContext, "search-stat-offset") %></label>
		<input 
			type="text" 
			dojoType="dijit.form.NumberTextBox" 
			validate="return false;" 
			invalidMessage=""  
			id="minPerc"
			name="minPerc"
			style="width:100%">
	</div>
	
	<div id="buttonForm" class="formField" style="padding-top: 1.5em;margin-left: 1em;">
		<button dojoType="dijit.form.Button" 
			onclick="searchStep();" 
			iconClass="searchIcon"><%= LanguageUtil.get(pageContext, "search-stat-search") %></button>
	</div>
</div>

<div id="stat-search-result-panel" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "search-stat-panel") %>" >
	<div id="stat-search-result_results">
	</div>
</div>

