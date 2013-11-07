<!-- 
  
  JSP for view all Poll votes.
  
  The usage of this mechanism make sense only if, by specific requirements, the receiver servers are only in frontend mode and all the 
  activity are doing from the sender servers. 
  
  This file is part of Poll Management for dotCMS.
  Poll Management for dotCMS is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.
 
  Poll Management for dotCMS is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.
 
  You should have received a copy of the GNU Lesser General Public License
  along with Poll Management for dotCMS.  If not, see <http://www.gnu.org/licenses/>  

-->


<%@page import="com.eng.dotcms.polls.util.PollsUtil"%>
<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>

<style>
	.poll-result-table{width:99%;border:1px solid #d0d0d0;border-collapse:collapse;margin:0 auto;font-size:11px;}
	.poll-result-table tr{}
	.poll-result-table th, .poll-result-table td{padding:5px 8px;border:1px solid #d0d0d0;border-top: none;}
	.poll-result-table th{font-weight:bold;background:#ececec;}
	.poll-result-table td a{text-decoration:none;}
	.poll-result-table td table tr{border:none;}
	.poll-result-table td button, .poll-result-table table.dijitSelect, .poll-result-table td .dijitTextBox{font-size:12px;}
	
	.poll-responses { font-weight: bold;}
</style>

<div style="margin:auto;">
		<%
			String pollId = request.getParameter("pollId");
			String languageId = request.getParameter("langId");
			if(null==languageId)
				languageId = (String)session.getAttribute("com.dotmarketing.htmlpage.language");
		%>
		<br />
		<%=PollsUtil.getVotesHtmlCode(pollId, Long.parseLong(languageId)) %>
		<br />
		<table align="center">
			<tr>
				<td colspan="2" class="buttonRow" style="text-align: center;white-space: nowrap;">
					<button dojoType="dijit.form.Button" onClick="backToPollsList('viewPollVotes',false)" id="close" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "Close") %></button>
			    </td>
		    </tr>
	   </table>	
	</div>
</div>