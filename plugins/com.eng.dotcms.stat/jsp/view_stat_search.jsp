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
.customStat ul li {
	margin: 0;
	padding: 0;
	padding-left: 0.5em;
	padding-top: 0.4em;
	padding-bottom: 0.4em;
}

.customStat td {
	margin:0;
	padding:0;
}

.customStat ul {
	margin-left:0
}

.counters {
	text-align: right;
	font-size: medium;
	font-weight: bold;
	margin-top: 1em;
	margin-right: 0.6em;
}
</style>
<%
String nastyError = "";
PluginAPI pAPI = APILocator.getPluginAPI();
String pluginId = "com.eng.dotcms.stat";

ClickstreamStatisticsAPI cssAPI = ClickstreamStatisticsAPI.getInstance();
ClickSearchBean search = new ClickSearchBean();


//Get parameter from request
String pageId = request.getParameter("pageId");
String pageType = request.getParameter("pageType");
String dateFrom = request.getParameter("dateFrom");
String dateTo = request.getParameter("dateTo");
String depth = request.getParameter("depth");
String maxStep = request.getParameter("maxStep");
String minPerc = request.getParameter("minPerc");

//Validation
if(!UtilMethods.isSet(pageId))
	nastyError=LanguageUtil.get(pageContext, "search-stat-page-required");
else
	search.setPageId(pageId);

if(!UtilMethods.isSet(pageType))
	nastyError=LanguageUtil.get(pageContext, "search-stat-pageType-required");
else
	search.setPageType(PageType.valueOf(pageType));

Date dateFromObj = null;
if(UtilMethods.isSet(dateFrom)) {
	dateFromObj = new SimpleDateFormat("yyyy-MM-dd").parse(dateFrom);
	search.setDateFrom(dateFromObj);
}

Date dateToObj = null;
if(UtilMethods.isSet(dateTo)) {
	dateToObj = new SimpleDateFormat("yyyy-MM-dd").parse(dateTo);
	search.setDateTo(dateToObj);
}

if(UtilMethods.isSet(dateFromObj) && UtilMethods.isSet(dateToObj)) {
	if(dateFromObj.after(dateToObj))
		nastyError=LanguageUtil.get(pageContext, "search-stat-date-validation");
}

try {
if(UtilMethods.isSet(depth))
	search.setDepth(Integer.parseInt(depth));

if(UtilMethods.isSet(maxStep))
	search.setSteps(Integer.parseInt(maxStep));

if(UtilMethods.isSet(minPerc))
	search.setMinUserPercentage(Integer.parseInt(minPerc));
} catch(Exception e) {
	nastyError = LanguageUtil.get(pageContext, "search-stat-numeric-validation");
}

List<List<ClickStepBean>> steps = cssAPI.findPageStream(search);

int countDirectAccess = cssAPI.countPageDirectAccess(search);
int countSearchEngineAccess = cssAPI.countPageSearchEngineAccess(search);

if(nastyError.equals("") && PageType.valueOf(pageType).equals(ClickSearchBean.PageType.LANDING))
	Collections.reverse(steps);

%>

<%if(!UtilMethods.isSet(nastyError)) {%>
	<%if(steps != null && steps.size() > 0) {%>			
		<table class="listingTable customStat" style="margin:10px;margin-bottom:20px;">
			<tr>
				<%if(PageType.valueOf(pageType).equals(ClickSearchBean.PageType.STARTING)) {%>
				<th style="width:30px;text-align:center; background-color: lightgreen">
				<%= LanguageUtil.get(pageContext, "search-stat-starting-page") %> 
				</th>
				<%} %>
				
				<% 
				for(int ii = 1; ii <= steps.size()-1; ii++) { %>
				<th style="width:30px;text-align:center;">
					Step <%=ii %>
				</th>
				<%} %>
				
				<%if(PageType.valueOf(pageType).equals(ClickSearchBean.PageType.LANDING)) { %>
				<th style="width:30px;text-align:center; background-color: lightgreen">
				<%= LanguageUtil.get(pageContext, "search-stat-landing-page") %>
				</th>
				<%} %>
			</tr>
			<!-- Data -->
			<tr>				
				<% 
				for(List<ClickStepBean> step : steps) { %>
				<td>
					<ul>
						<%int tempCounter = 0;
						  for(ClickStepBean csb : step) { %>
							<%if(tempCounter < step.size()-1) {%>
							<li style="border-bottom: solid 1px #eee;">
								<%=csb.getPageName() %>&nbsp;<strong><%=csb.getUserPercentage()%>%</strong>
							</li>
							<%} else {%>
							<li>
								<%=csb.getPageName() %>&nbsp;<strong><%=csb.getUserPercentage()%>%</strong>
							</li>
							<%} tempCounter++; %>
						<%} %>
					</ul>
				</td>
				<%} %>
			</tr>		
		</table>
		
		<div class="counters">
			<span><%= LanguageUtil.get(pageContext, "search-stat-direct-access") %> <%=countDirectAccess %></span>
		</div>
		<div class="counters">
			<span><%= LanguageUtil.get(pageContext, "search-stat-searchEngine-access") %> <%=countSearchEngineAccess %></span>
		</div>
	<%} else { %>
		<%= LanguageUtil.get(pageContext, "search-stat-no-result") %>
	<%} %>
<%} else { %>
	<span style="color: red">Errore: <%=nastyError%></span>
<%} %>

