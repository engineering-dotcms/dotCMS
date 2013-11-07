<%@page import="com.dotmarketing.plugins.integrity.checker.diff.DataIntegrityResultWrapper"%>
<%@page import="com.dotmarketing.plugins.integrity.checker.diff.DataIntegrityCheckAPI"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.dotcms.publisher.business.PublishAuditAPI"%>
<%@page import="com.dotcms.publisher.business.PublishAuditStatus"%>
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
<%@ include file="/html/plugins/com.dotmarketing.plugins.integrity.checker/init.jsp" %>
<%

    DataIntegrityCheckAPI dicAPI = DataIntegrityCheckAPI.getInstance();

    String nastyError = null;
    
    int offset = 0;
    try{offset = Integer.parseInt(request.getParameter("offset"));}catch(Exception e){}
    if(offset <0) offset=0;
    int limit = 50;
    try{limit = Integer.parseInt(request.getParameter("limit"));}catch(Exception e){}
    if(limit <0 || limit > 500) limit=50;
    


    List<DataIntegrityResultWrapper> iresults =  null;
    int counter =  0;

    try{
   		iresults =  dicAPI.getDiffList();
   		counter =   iresults.size();
    }catch(DotPublisherException e){
    	iresults = new ArrayList<DataIntegrityResultWrapper>();
    	nastyError = e.toString();
    }catch(Exception pe){
    	iresults = new ArrayList<DataIntegrityResultWrapper>();
    	nastyError = pe.toString();
    }
    
  
    
	long begin=offset;
	long end = offset+limit;
	long total = counter;
	long previous=(begin-limit);
	if(previous < 0){previous=0;} 
    
  %>
  

<script type="text/javascript">
   dojo.require("dijit.Tooltip");
</script> 

<%if(UtilMethods.isSet(nastyError)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "data-integrity-query-error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
<%} %>



<%if(iresults.size() >0){ %>




				
	<table class="listingTable ">
		<tr>		
			<th  nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "data-integrity-identifier") %></strong></th>	
			<th style="width:100%" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "data-integrity-content") %></strong></th>	
			<th style="width:100px" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "data-integrity-date-check") %></strong></th>
		</tr>
		<% for(DataIntegrityResultWrapper c : iresults) {
			String errorclass="";
		%>
			<tr <%=errorclass%>>			
			
				<td valign="top" nowrap="nowrap">
					<a href="/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=c.getLocalInode() %>&referer=<%=referer %>"><%=c.getLocalInode()%></a>
				</td>
				<td valign="top" style="cursor: pointer">
					<%try{ %>
						<span class="contentIncSpan">
							<%	String title = "";
								List<Contentlet> contents = APILocator.getContentletAPI().search(
									"+inode:"+c.getLocalInode(),
									limit, offset, "modDate", APILocator.getUserAPI().getSystemUser(), false);
								if(contents.size() > 0)
									title = contents.get(0).getTitle();
							%>
							<a href="/c/portal/layout?p_l_id=<%=layoutId %>&p_p_id=EXT_11&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=/ext/contentlet/edit_contentlet&_EXT_11_cmd=edit&inode=<%=c.getLocalInode() %>&referer=<%=referer %>"><%=title%></a>
						</span>
					<%}catch(Exception e) {%>
					
					<%} %>
				</td>
			    <td valign="top" nowrap="nowrap"><%=UtilMethods.dateToHTMLDate(c.getLocalCheckDate(),"MM/dd/yyyy hh:mma") %></td>
			</tr>
		<%}%>
<table width="97%" style="margin:10px;" >
	<tr>
		<%
		if(begin > 0){ %>
			<td width="33%" ><button dojoType="dijit.form.Button" onClick="refreshDiffList('diff', 'view_file_diff_list','offset=<%=previous%>&limit=<%=limit%>');return false;" iconClass="previousIcon"><%= LanguageUtil.get(pageContext, "data-integrity-previous") %></button></td>
		<%}else{ %>
			<td  width="33%" >&nbsp;</td>
		<%} %>
			<td  width="34%"  colspan="2" align="center"><strong> <%=begin+1%> - <%=end < total?end:total%> <%= LanguageUtil.get(pageContext, "data-integrity-of") %> <%=total%> </strong></td>
		<%if(end < total){ 
			long next=(end < total?end:total);
		%>
			<td align="right" width="33%" ><button class="solr_right" dojoType="dijit.form.Button" onClick="refreshDiffList('diff', 'view_file_diff_list','offset=<%=next%>&limit=<%=limit%>');return false;" iconClass="nextIcon"><%= LanguageUtil.get(pageContext, "data-integrity-next") %></button></td>
		<%}else{ %>
			<td  width="33%" >&nbsp;</td>
		<%} %>
	</tr>
</table>
<%
}else{ 
%>
	<table class="listingTable ">
		<tr>		
			<th  nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "data-integrity-identifier") %></strong></th>	
			<th style="width:100%" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "data-integrity-content") %></strong></th>	
			<th style="width:100px" nowrap="nowrap" ><strong><%= LanguageUtil.get(pageContext, "data-integrity-date-check") %></strong></th>
		</tr>
		<tr>
			<td colspan="4" align="center"><%= LanguageUtil.get(pageContext, "data-integrity-no-results") %></td>
		</tr>
	</table>
<%} %>

