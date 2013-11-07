<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.eng.dotcms.stat.bean.SectionReport"%>
<%@page import="com.eng.dotcms.stat.bean.SectionSearchBean"%>
<%@page import="com.eng.dotcms.stat.bean.ClickSearchBean.PageType"%>
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
.totalRow {
	text-align: right;
	font-size: medium;
	font-weight: bold;
	padding-right: 6.2em !important;
}
</style>
<%
String nastyError = "";
PluginAPI pAPI = APILocator.getPluginAPI();
String pluginId = "com.eng.dotcms.stat";

ClickstreamStatisticsAPI cssAPI = ClickstreamStatisticsAPI.getInstance();
SectionSearchBean search = new SectionSearchBean();


//Get parameter from request
String section = request.getParameter("section");
String year = request.getParameter("year");
String monthFrom = request.getParameter("monthFrom");
String monthTo = request.getParameter("monthTo");
String urlParam = "";
//Validation
if(!UtilMethods.isSet(section))
	nastyError=LanguageUtil.get(pageContext, "search-stat-page-required");
else
	search.setSection(section);

try {
	if(UtilMethods.isSet(year))
		search.setYear(Integer.parseInt(year));
	else
		throw new Exception();
	
	if(UtilMethods.isSet(monthFrom))
		search.setMonthFrom(Integer.parseInt(monthFrom));
	else
		throw new Exception();
	
	if(UtilMethods.isSet(monthTo))
		search.setMonthTo(Integer.parseInt(monthTo));
	else
		throw new Exception();
	
	urlParam += "year="+year+"&monthFrom="+monthFrom+"&monthTo="+monthTo+"&section="+section;
} catch(Exception e) {
	nastyError = LanguageUtil.get(pageContext, "search-stat-numeric-validation");
}

List<SectionReport> reports = cssAPI.findSectionReport(search);
%>
<%if(!UtilMethods.isSet(nastyError)) {%>
	<%if(reports != null && reports.size() > 0) {%>		
		<div style="margin-right: 0.5em;text-align: right;">
		<a href="/html/plugins/com.eng.dotcms.stat/export_xls.jsp?<%=urlParam%>" target="_blank">Esporta dati</a>
		</div>	
		<table class="listingTable" style="margin:10px;margin-bottom:20px;">
			<tr>
				<th style="width:100px;text-align:center;">
					Data
				</th>
				<th style="text-align:center;">
					Pagina
				</th>
				<th style="width:100px;text-align:center;">
					Visite
				</th>
			</tr>
			<!-- Dati -->
			<% 	
				String firstDate = new SimpleDateFormat("dd/MM/yyyy").format(reports.get(0).getDate());
				int hintsCounter = 0;
				for(SectionReport report : reports) { 
					String date = new SimpleDateFormat("dd/MM/yyyy").format(report.getDate());%>
					
					<%if(!firstDate.equals(date)) {
						firstDate = date;
					%>
						<tr>
							<td colspan="3" class="totalRow">Totale <%=hintsCounter %></td>
						</tr>
						
					<%hintsCounter = 0;
					} %>
					
					<tr>				
						
						<td>
							<%=date%>
						</td>
						<td>
							<%=report.getPage()%>
						</td>
						<td>
							<%=report.getHints()%>
						</td>
						
					</tr>
				<%
					hintsCounter+=report.getHints();
				} %>
				<tr>
					<td colspan="3" class="totalRow">Totale <%=hintsCounter %></td>
				</tr>
		</table>
	<%} else { %>
		<div id="message-init" style="text-align: center;margin-top: 1.4em;"><span><%= LanguageUtil.get(pageContext, "search-stat-no-result") %></span></div>
	<%} %>
<%} else { %>
	<div id="message-init" style="text-align: center;margin-top: 1.4em;"><span style="color: red">Errore: <%=nastyError%></span></div>
	
<%} %>

