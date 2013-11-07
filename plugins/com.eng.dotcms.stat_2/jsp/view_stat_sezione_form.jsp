<%@page import="com.dotmarketing.portlets.folders.model.Folder"%>
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
#searchSezioneForm {
	float: left;
	width: 24%;
	border-right: solid 1px #eee;
	padding: 1.3em;
	min-height: 150px ;
}

.result-panel {
	float:left;
	width: 74%;
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

	var message = dojo.byId("message-init");
	message.innerHTML = "";

	myCp = new dojox.layout.ContentPane({
		id : idTab+"-content-search"
	}).placeAt(idTab+"_results");

	myCp.attr("href", url);
	
	myCp.refresh();

}

function searchSection() {
	var urlParam = "";
	
	if(dijit.byId("monthFrom").value  != null && !isNaN(dijit.byId("monthFrom").value))
		urlParam += "&monthFrom="+dijit.byId("monthFrom").value;

	if(dijit.byId("monthTo").value  != null && !isNaN(dijit.byId("monthTo").value))
		urlParam += "&monthTo="+dijit.byId("monthTo").value;

	if(dijit.byId("year").value != null && !isNaN(dijit.byId("year").value))		
		urlParam+="&year="+dijit.byId("year").value;

	urlParam+="&section="+dojo.query("input[name=section]:checked")[0].value;
		
	refreshSearch("stat-sezione-search-result","view_stat_sezione_search", "refresh=true"+urlParam);
}
</script>

<%
//Load sections
ClickstreamStatisticsAPI cssAPI = ClickstreamStatisticsAPI.getInstance();
List<Folder> sections = cssAPI.findSections();
%>

<div id="searchSezioneForm">
	<div>
		<fieldset>
			<legend>Sezioni</legend>
			<ul>
				<%
				int counter = 0;
				for(Folder section: sections) {  %>
				<li>
					<%if(counter == 0) { %>
						<input dojoType="dijit.form.RadioButton" type="radio" 
						name="section" value="<%=section.getName()%>" 
						checked="checked"
						id="id<%=section.getName()%>" />
						
						<label for="id<%=section.getName()%>"><%=section.getName()%></label>
					<%} else {%>
						<input dojoType="dijit.form.RadioButton" type="radio" 
						name="section" value="<%=section.getName()%>" 
						id="id<%=section.getName()%>" />
						
						<label for="id<%=section.getName()%>"><%=section.getName()%></label>
					<%} %>
				</li>
				<% counter++;
				} %>
			</ul>
		</fieldset>
	</div>
	
	<div>
		<fieldset>
			<legend>Periodo riferimento</legend>
			
			<div style="width:100%; margin-top:0.5em;">
				<div  style="width: 30%"><label for="year">Anno</label></div>
				<select dojoType="dijit.form.ComboBox" id="year" name="year">
				    <%
					Calendar cal = Calendar.getInstance();
					cal.setTime(new Date());
					
				    for(int ii = cal.get(Calendar.YEAR); ii > 1999; ii--) {  
				    	if(ii == cal.get(Calendar.YEAR)) {
				    %>
				    	<option selected="selected"><%=ii%></option>
				    <%  } else {%>
				    	<option><%=ii%></option>
				    <%  }
				    } %>
				</select>
			</div>
			
			<div style="width:100%; margin-top:0.5em;">
				<div  style="width: 30%"><label for="monthFrom">Mese da</label></div>
				<select dojoType="dijit.form.Select" id="monthFrom" name="monthFrom">
				    <option selected="selected" value="1">Gennaio</option>
				    <option value="2">Febbraio</option>
				    <option value="3">Marzo</option>
				    <option value="4">Aprile</option>
				    <option value="5">Maggio</option>
				    <option value="6">Giugno</option>
				    <option value="7">Luglio</option>
				    <option value="8">Agosto</option>
				    <option value="9">Settembre</option>
				    <option value="10">Ottobre</option>
				    <option value="11">Novembre</option>
				    <option value="12">Dicembre</option>
				</select>
			</div>
			
			<div style="width:100%;margin-top:0.5em;">
				<div  style="width: 30%"><label for="monthTo">Mese a</label></div>
				<select dojoType="dijit.form.Select" id="monthTo" name="monthTo">
				    <option value="1">Gennaio</option>
				    <option value="2">Febbraio</option>
				    <option value="3">Marzo</option>
				    <option value="4">Aprile</option>
				    <option value="5">Maggio</option>
				    <option value="6">Giugno</option>
				    <option value="7">Luglio</option>
				    <option value="8">Agosto</option>
				    <option value="9">Settembre</option>
				    <option value="10">Ottobre</option>
				    <option value="11">Novembre</option>
				    <option selected="selected" value="12">Dicembre</option>
				</select>
			</div>
		</fieldset>
	</div>
	
	<div id="buttonForm" class="formField" style="padding-top: 1.5em;margin-left: 1em;">
		<button dojoType="dijit.form.Button" 
			onclick="searchSection();" 
			iconClass="searchIcon"><%= LanguageUtil.get(pageContext, "search-stat-search") %></button>
	</div>
</div>

<div id="stat-sezione-search-result-panel" class="result-panel" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "search-stat-sezione-panel") %>" >
	<div id="stat-sezione-search-result_results">
		<div id="message-init" style="text-align: center;margin-top: 1.4em;"><span>Effettua una ricerca</span></div>
	</div>
</div>

