<!-- 
  
  JSP for view all polls into the system.
  
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

<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.eng.dotcms.polls.util.PollsConstants"%>
<%@page import="com.dotmarketing.plugin.business.PluginAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@ include file="/html/common/init.jsp" %>

<style type="text/css">
#tools {
    text-align:center;
    width: 100%;
    margin: 0;
    display: block;
}
#links_table td {
    padding: 8px;
}

.expiredPoll {
	font-color: #DDDDDD;
	font-style: italic;	
	background-color: #E0E9F6;
}
</style>

<script type="text/javascript">
<%
String contentLayout="";
List<Layout> list=APILocator.getLayoutAPI().loadLayoutsForUser(user);
for(Layout ll : list) {
    for(String pid : ll.getPortletIds())
        if(pid.equals("EXT_11"))
            contentLayout=ll.getId();
}

int pageNumber=1;
if(request.getParameter("pageNumber")!=null) 
    pageNumber=Integer.parseInt(request.getParameter("pageNumber"));

// aggiunta del tasto per la pubblicazione remota di un sondaggio
PluginAPI pluginAPI = APILocator.getPluginAPI();
boolean remoteMode = Boolean.parseBoolean(pluginAPI.loadProperty(PollsConstants.PLUGIN_ID, PollsConstants.PROP_REMOTE_ENABLED));

List<Language> languages = APILocator.getLanguageAPI().getLanguages();

Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
String languageId = String.valueOf(defaultLang.getId());
%>

dojo.require("dotcms.dojo.push.PushHandler");
var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish")%>');

function movePage(x) {
	var cp=parseInt(dojo.byId('currentPage').textContent);
	dojo.byId('currentPage').textContent=cp+x;
	loadTable();
}

function disableButtons(x) {
	dijit.byId('refreshBtn').set('disabled',x);
	dijit.byId('runBtn').set('disabled',x);
	if(x=="") {
		var cp=parseInt(dojo.byId('currentPage').textContent);
		var tp=parseInt(dojo.byId('totalPages').textContent);
		if(cp>1)
			dijit.byId('prevBtn').set('disabled','');
		else
			dijit.byId('prevBtn').set('disabled','disabled');
		
		if(cp<tp)
			dijit.byId('nextBtn').set('disabled','');
		else
			dijit.byId('nextBtn').set('disabled','disabled');
	}
	else {
		dijit.byId('nextBtn').set('disabled',x);
	    dijit.byId('prevBtn').set('disabled',x);
	}
}

function goToAddPoll(){
	var el = dijit.byId('language_id');
	var dialog = new dijit.Dialog({
		id: 'addPoll',
        title: "<%= LanguageUtil.get(pageContext, "add-poll")%>",
        style: "width: 800px; ",
        content: new dojox.layout.ContentPane({
            href: "/html/plugins/com.eng.dotcms.polls/add_polls.jsp?el="+el
        }),
        onHide: function() {
        	var dialog=this;
        	setTimeout(function() {
        		dialog.destroyRecursive();
        	},200);
        },
        onLoad: function() {
        	
        }
    });
    dialog.show();	    
    dojo.style(dialog.domNode,'top','80px');
}

function goToViewPollVotes(pollId,languageId){
	var dialog = new dijit.Dialog({
		id: 'viewPollVotes',
        title: "<%= LanguageUtil.get(pageContext, "view-poll-votes")%>",
        style: "width: 800px; ",
        content: new dojox.layout.ContentPane({
            href: "/html/plugins/com.eng.dotcms.polls/view_poll_votes.jsp?pollId="+pollId+"&langId="+languageId
        }),
        onHide: function() {
        	var dialog=this;
        	setTimeout(function() {
        		dialog.destroyRecursive();
        	},200);
        },
        onLoad: function() {
        	
        }
    });
    dialog.show();	    
    dojo.style(dialog.domNode,'top','80px');
}

function updateVotes(pollId){
	var el = dijit.byId('language_id');
	var divResponse = dojo.byId("responseVotes");
	dijit.byId("updateVotesBtn").setAttribute('disabled',true);
	var body = document.getElementsByTagName("body")[0];
	body.setAttribute("style","cursor: wait !important");
	var xhrArgs = {
		url:'/DotAjaxDirector/com.eng.dotcms.polls.ajax.PollsAjaxAction/cmd/updateVotes/identifier/'+pollId+'/el/'+el,
		handleAs: 'text',
		load: function(data){
			if(data.indexOf("FAILURE") > -1){
				dojo.style(divResponse, {color:'#CC3333'});
				divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "not-updated-votes") %><br />'+data
				dijit.byId("updateVotesBtn").setAttribute('disabled',false);
				body.removeAttribute("style");
			}
			else if(data.indexOf("NORES") > -1){
				dojo.style(divResponse, {color:'#009933'});
				divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "no-votes") %>'	
				dijit.byId("updateVotesBtn").setAttribute('disabled',false);
				body.removeAttribute("style");
			}else{
				dojo.style(divResponse, {color:'#009933'});
				divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "votes-updated") %>'	
				dijit.byId("updateVotesBtn").setAttribute('disabled',false);
				body.removeAttribute("style");
			}
		},
		error: function(err) {
			dojo.style(divResponse, {color:'#CC3333'});
			divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "not-updated-votes") %><br />'+error
			dijit.byId("updateVotesBtn").setAttribute('disabled',false);
			body.removeAttribute("style");
		}
	}	
	dojo.style(divResponse, {color:'#FFCC33'});
	divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "updating-votes") %>'
	var deferred = dojo.xhrPost(xhrArgs);	
}

function backToPollsList(id, reload){
	dijit.byId(id).hide();
	if(reload)
		loadTable();
}

function loadTable() {
	var el = dijit.byId('language_id');
	var currentUser="<%=user.getUserId()%>";
	var lid="<%=contentLayout%>";
	var lidBL="<%=layout.getId()%>";	
	dojo.empty('table_body');
	var pageSize=10;
	var page=(parseInt(dojo.byId('currentPage').textContent)-1)*pageSize;
	dojo.xhr('GET',{
		url:'/DotAjaxDirector/com.eng.dotcms.polls.ajax.PollsAjaxAction/cmd/getPolls/offset/'+page+'/pageSize/'+pageSize+'/el/'+el,
		handleAs: 'json',
		load: function(data) {
			dojo.empty('table_body');
			for(var i=0;i<data.list.length;i++) {
				var identifier=data.list[i].identifier;
				var title=data.list[i].title;
				var question=data.list[i].question;
				var moduser=data.list[i].user;
				var expdate=data.list[i].date;
				var expired=data.list[i].expired;
				var languageId=data.list[i].languageId;
				var flag=data.list[i].flag;
				if(expired=="true"){
					var row="<tr class=\"expiredPoll\">"+
					  "<td style=\"width: 5%\"><strong><%=LanguageUtil.get(pageContext, "Expired")%></strong></td>"+	
					  "<td style=\"width: 15%\"><img src=\""+flag+"\" alt=\"flag\" />&nbsp;&nbsp;"+title+"</td>"+
			          "<td style=\"width: 60%\">"+question+"</td>"+
			          "<td style=\"width: 11%\">"+expdate+"</td>"+
			          "<td style=\"width: 9%\"><a style=\"cursor: pointer\" onclick=\"goToViewPollVotes('"+identifier+"','"+languageId+"')\" title=\"<%=LanguageUtil.get(pageContext,"view-poll-votes")%>\"><span class='previewIcon'></span></a></td>"+
			         "</tr>";					
				}else{
					var row="<tr>"+ 
					  "<td style=\"width: 5%\"></td>"+
					  "<td style=\"width: 15%\"><img src=\""+flag+"\" alt=\"flag\" />&nbsp;&nbsp;"+title+"</td>"+
			          "<td style=\"width: 60%\">"+question+"</td>"+
			          "<td style=\"width: 11%\">"+expdate+"</td>"+
			          <%
			          	if(!remoteMode){
			          %>
			          "<td style=\"width: 9%\"><a style=\"cursor: pointer\" onclick=\"goToViewPollVotes('"+identifier+"','"+languageId+"')\" title=\"<%=LanguageUtil.get(pageContext,"view-poll-votes")%>\"><span class='previewIcon'></span></a>&nbsp;&nbsp;&nbsp;
			          "<a style=\"cursor: pointer\" onclick=\"updateVotes('"+identifier+"')\" title=\"<%=LanguageUtil.get(pageContext,"update-votes")%>\"><span class='backupIcon'></span></a></td>"			          
			          <%
			          	}else{
			          %>
			          "<td style=\"width: 9%\"><a style=\"cursor: pointer\" onclick=\"goToViewPollVotes('"+identifier+"','"+languageId+"')\" title=\"<%=LanguageUtil.get(pageContext,"view-poll-votes")%>\"><span class='previewIcon'></span></a>&nbsp;&nbsp;&nbsp;"+
			          "<a style=\"cursor: pointer\" onclick=\"updateVotes('"+identifier+"')\" title=\"<%=LanguageUtil.get(pageContext,"update-votes")%>\"><span class='backupIcon'></span></a>&nbsp;&nbsp;&nbsp;"+
			          "<a style=\"cursor: pointer\" onclick=\"remotePollPublish('"+identifier+"')\" title=\"<%=LanguageUtil.get(pageContext,"Remote-Publish")%>\"><span class='pushIcon'></span></a></td>"+
			          <%
			          	}
			          %>
			         "</tr>";					
				}
				dojo.place(dojo.toDom(row),'table_body');				
			}
			dojo.byId('totalPages').textContent=Math.ceil(data.total/pageSize);
            disableButtons('');
		},
		error: function(err) {
			console.log(err);
		}
	});
}

function resized() {
    var viewport = dijit.getViewport();
    var viewport_height = viewport.h;
    
    var  e =  dojo.byId("borderContainer");
    dojo.style(e, "height", viewport_height -150+ "px");
    
    dijit.byId("borderContainer").resize();
}

function remotePollPublish(pollIdentifier) {
	pushHandler.showDialog(pollIdentifier);
}

dojo.ready(function(){
    dojo.connect(window,"onresize",resized);
    resized();
    loadTable();
});
</script>

<div class="portlet-wrapper">
	<div class="subNavCrumbTrail">
		<ul id="subNavCrumbUl">        
			<li><%=LanguageUtil.get(pageContext, "javax.portlet.title.EXT_POLLS_MANAGEMENT")%></li>
			<li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "javax.portlet.title.EXT_POLLS_MANAGEMENT_VIEW")%></span></li>
		</ul>
		<div class="clear"></div>
	</div>
	
	<div id="brokenLinkMain">
        <div id="borderContainer" dojoType="dijit.layout.BorderContainer" style="width:100%;">
            <div dojoType="dijit.layout.ContentPane" region="top">
              <div id="tools">
              <span style="float: left">
					<dl>
                			<%if (languages.size() > 1) { %>
                                                <dt><%= LanguageUtil.get(pageContext, "Language") %>:</dt>
                                                <dd>
                                                    <div id="combo_zone2" style="width:215px; height:20px;">
                                                        <input id="language_id"/>
                                                    </div>
                                                    <script>
														<%StringBuffer buff = new StringBuffer();
														  // http://jira.dotmarketing.net/browse/DOTCMS-6148
														  buff.append("{identifier:'id',imageurl:'imageurl',label:'label',items:[");

														  String imageURL="/html/images/languages/all.gif";
														  String style="background-image:url(URLHERE);width:16px;height:11px;display:inline-block;vertical-align:middle;margin:3px 5px 3px 2px;";
														  buff.append("{id:'0',value:'',lang:'All',imageurl:'"+imageURL+"',label:'<span style=\""+style.replaceAll("URLHERE",imageURL)+"\"></span>All'}");
														  for (Language lang : languages) {
															  imageURL="/html/images/languages/" + lang.getLanguageCode()  + "_" + lang.getCountryCode() +".gif";
															  final String display=lang.getLanguage() + " - " + lang.getCountry().trim();
															  buff.append(",{id:'"+lang.getId()+"',");
															  buff.append("value:'"+lang.getId()+"',");
															  buff.append("imageurl:'"+imageURL+"',");
															  buff.append("lang:'"+display+"',");
															  buff.append("label:'<span style=\""+style.replaceAll("URLHERE",imageURL)+"\"></span>"+display+"'}");
														  }
														  buff.append("]}");%>

														function updateSelectBoxImage(myselect) {
															var imagestyle = "url('" + myselect.item.imageurl + "')";
															var selField = dojo.query('#combo_zone2 div.dijitInputField')[0];
															dojo.style(selField, "backgroundImage", imagestyle);
															dojo.style(selField, "backgroundRepeat", "no-repeat");
															dojo.style(selField, "padding", "0px 0px 0px 25px");
															dojo.style(selField, "backgroundColor", "transparent");
															dojo.style(selField, "backgroundPosition", "3px 6px");
														}

															var storeData=<%=buff.toString()%>;
															var langStore = new dojo.data.ItemFileReadStore({data: storeData});
															var myselect = new dijit.form.FilteringSelect({
																	 id: "language_id",
																	 name: "language_id",
																	 value: '',
																	 required: true,
																	 store: langStore,
																	 searchAttr: "lang",
																	 labelAttr: "label",
																	 labelType: "html",
																	 onChange: function() {
																		 var el=dijit.byId('language_id');
																		 updateSelectBoxImage(el);
																		 loadTable();
																	 },
																	 labelFunc: function(item, store) { return store.getValue(item, "label"); }
																},
																dojo.byId("language_id"));

																<%if(languageId.equals("0")) {%>
																	myselect.setValue('<%=languages.get(0).getId()%>');
																<%} else {%>
																	myselect.setValue('<%=languageId%>');
																<%}%>

													</script>
                                                </dd>
                                        <%} else { %>
                                                <% long langId = languages.get(0).getId(); %>
                                                <input type="hidden" name="language_id" id="language_id" value="<%= langId %>">
                                        <% } %>
                                        </dl>  
                 </span>                    
                <span style="float: right; margin-top: 0.5%; margin-right: 5%">                                
                <button id="addPollBtn" type="button" dojoType="dijit.form.Button" onClick="goToAddPoll()">
                   <span class="plusIcon"></span>
                   <%=LanguageUtil.get(pageContext,"add-poll")%>
                </button>
                </span>  
                <span id="responseVotes" style="font-weight: bold; margin-top: 0.5%"></span>
				<span style="float: right; margin-top: 0.5%;">                                
                <button id="updateVotesBtn" type="button" dojoType="dijit.form.Button" onClick="updateVotes('all')">
                   <span class="backupIcon"></span>
                   <%=LanguageUtil.get(pageContext,"update-votes")%>
                </button>
                </span>                               
              </div>
              <div class="clear"></div>
            </div>
            <div dojoType="dijit.layout.ContentPane" region="center">
                <table id="links_table" class="listingTable" border=1>
                <thead>
                    <tr>
                    	<th style="width: 5%"><%=LanguageUtil.get(pageContext, "Status")%></th>
                        <th style="width: 15%"><%=LanguageUtil.get(pageContext, "Title")%></th>
                        <th style="width: 60%"><%=LanguageUtil.get(pageContext, "Question")%></th>
                        <th style="width: 11%"><%=LanguageUtil.get(pageContext, "Expiration-Date")%></th>                        
                        <th style="width: 9%"><%=LanguageUtil.get(pageContext, "Action")%></th>
                                               
                    </tr>
                </thead>
                <tbody id="table_body">
                </tbody>
                </table>
            </div>
            <div dojoType="dijit.layout.ContentPane" region="bottom">
                <span id="tools">
                   <button id="prevBtn" type="button" dojoType="dijit.form.Button" onClick="movePage(-1)">
	                   <span class="previousIcon"></span>
	               </button>
	               
	               <span id="currentPage"><%=pageNumber %></span> / <span id="totalPages"></span>
	               
	               <button id="nextBtn" type="button" dojoType="dijit.form.Button" onClick="movePage(1)">
                       <span class="nextIcon"></span>
                   </button>
                </span>
            </div>
        </div>
    </div>
</div>

<form id="remotePublishForm">
	<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="">
	<input name="remotePublishDate" id="remotePublishDate" type="hidden" value="">
	<input name="remotePublishTime" id="remotePublishTime" type="hidden" value="">
	<input name="remotePublishExpireDate" id="remotePublishExpireDate" type="hidden" value="">
	<input name="remotePublishExpireTime" id="remotePublishExpireTime" type="hidden" value="">
	<input name="remotePublishNeverExpire" id="remotePublishNeverExpire" type="hidden" value="">
	<input name="iWantTo" id=iWantTo type="hidden" value="">
	<input name="whoToSend" id=whoToSend type="hidden" value="">
	<input name="newBundle" id=newBundle type="hidden" value="">
	<input name="bundleName" id=bundleName type="hidden" value="">
	<input name="bundleSelect" id=bundleSelect type="hidden" value="">	
</form>