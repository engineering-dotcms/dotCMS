<%@page import="java.text.DateFormat"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.eng.dotcms.healthchecker.HealthClusterViewStatus"%>
<%@page import="com.eng.dotcms.healthchecker.business.HealthCheckerAPI"%>
<%@ include file="/html/plugins/com.eng.dotcms.healthchecker/init.jsp" %>
<%@ page import="java.util.List"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>

<%
	HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	List<HealthClusterViewStatus> view = healthAPI.clusterView();
	SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",WebAPILocator.getUserWebAPI().getLoggedInUser(request).getLocale());
	
%>

<style type="text/css">

.viewCreator {
	font-style: italic;	
	background-color: #E0E9F6;
}
</style>

<script type="text/javascript">

function refreshCache(address,port,protocol,id){
	var divResponse = dojo.byId("responseRefresh_"+id);
	dijit.byId("refreshCacheBtn_"+id).setAttribute('disabled',true);
	var body = document.getElementsByTagName("body")[0];
	body.setAttribute("style","cursor: wait !important");
	var xhrArgs = {
		url:'/DotAjaxDirector/com.eng.dotcms.healthchecker.ajax.HealthCheckerAjax/cmd/refreshCache/address/'+address+'/port/'+port+'/protocol/'+protocol,
		handleAs: 'text',
		load: function(data){
			if(data.indexOf("OK") > -1){
				dojo.style(divResponse, {color:'#009933'});
				divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "health-refreshing-cache-ok") %>'
				dijit.byId("refreshCacheBtn_"+id).setAttribute('disabled',false);
				body.removeAttribute("style");
			}else{
				dojo.style(divResponse, {color:'#CC3333'});
				divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "health-refreshing-cache-ko") %><br />'+data	
				dijit.byId("refreshCacheBtn_"+id).setAttribute('disabled',false);
				body.removeAttribute("style");
			}
		},
		error: function(err) {
			dojo.style(divResponse, {color:'#CC3333'});
			divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "health-refreshing-cache-ko") %><br />'+error
			dijit.byId("refreshCacheBtn_"+id).setAttribute('disabled',false);
			body.removeAttribute("style");
		}
	}	
	dojo.style(divResponse, {color:'#FFCC33'});
	divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "health-refreshing-cache") %>'
	var deferred = dojo.xhrPost(xhrArgs);	
}
</script>

<div style="padding-top: 5px">
			
			<table  class="listingTable" style="width:75%; float:left; margin: 0 0 0 10px">
				<tr style="line-height:20px; padding-bottom: 15px">					
					<th nowrap="nowrap" style="padding-left: 10px; width: 20%">
						<%= LanguageUtil.get(pageContext, "health_Server") %>
					</th>
					<th align="center" style="padding-left: 10px; font-size: 12px; width: 5%" >
						<%= LanguageUtil.get(pageContext, "health_Port") %>
					</th>
					<th align="center" style="padding-left: 10px; font-size: 12px; width: 5%" >
						<%= LanguageUtil.get(pageContext, "health_Protocol") %>
					</th>					
					<th align="center" style="padding-left: 10px; width: 5%">
						<%= LanguageUtil.get(pageContext, "health_Status") %>
					</th>
					<th align="right" style="padding-left: 10px; width: 25%">
						<%= LanguageUtil.get(pageContext, "health_Date") %>
					</th>
					<th align="center" style="padding-left: 10px; width: 5%">
					</th>	
					<th align="center" style="padding-left: 10px; width: 35%">
					</th>				
				</tr>
	<%
			boolean hasServers = view.size()>0;
			for(HealthClusterViewStatus singleView : view) {
	%>

				<tr style="line-height:20px; padding-bottom: 15px">
					<td style="padding-left: 10px; font-size: 12px;" >
						<%=singleView.getAddress()%>
					</td>
					<td style="padding-left: 10px; font-size: 12px" align="center" >
						<%=singleView.getPort()%>
					</td>
					<td style="padding-left: 10px; font-size: 12px" align="center" >
						<%=singleView.getProtocol()%>
					</td>					
					<td style="padding-left: 10px; font-size: 12px" >
						<%=singleView.getStatus()%>
					</td>
					<td style="padding-left: 10px; font-size: 12px" >
						<%=df.format(singleView.getModDate())%>
					</td>
					<td style="padding-left: 10px; font-size: 12px" >
						<%if("JOIN".equals(singleView.getStatus())) { %>
						<button id="refreshCacheBtn_<%=singleView.getId()%>" type="button" dojoType="dijit.form.Button" onClick="refreshCache('<%=singleView.getAddress()%>','<%=singleView.getPort()%>','<%=singleView.getProtocol()%>','<%=singleView.getId()%>')">
                   			<span class="refreshIcon"></span>
                   			<%=LanguageUtil.get(pageContext,"refresh")%>
                		</button>						
                		<%}%>
					</td>
					<td id="responseRefresh_<%=singleView.getId()%>" style="padding-left: 10px; font-size: 12px" >
					</td>
				</tr>


		<%}%>
			</table><br>			

		<%if(!hasServers){ %>
			<table style="width: 99%; border: 1px solid #D0D0D0">
				<tr>
					<td colspan="100" align="center"><%= LanguageUtil.get(pageContext, "health_No_Results") %></td>
				</tr>
			</table>
		<%}%>
</div>



