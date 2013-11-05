<%@page import="com.dotmarketing.portlets.folders.business.FolderAPI"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
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
	
	PluginAPI pluginAPI = APILocator.getPluginAPI();
	boolean remoteMode = Boolean.parseBoolean(pluginAPI.loadProperty(PollsConstants.PLUGIN_ID, PollsConstants.PROP_REMOTE_ENABLED));
	
	List<Language> languages = APILocator.getLanguageAPI().getLanguages();
	
	Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
	String languageId = String.valueOf(defaultLang.getId());
%>

<script type="text/javascript">
	dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");
	
	function updateHostFolderValues(field){
	  if(!isInodeSet(dijit.byId('HostSelector').attr('value'))){
		 dojo.byId(field).value = "";
		 dojo.byId('hostId').value = "";
		 dojo.byId('folderInode').value = "";
	  }else{
		 var data = dijit.byId('HostSelector').attr('selectedItem');
		 if(data["type"]== "host"){
			alert('Non è possibile cancellare un Host!');
			return;
		 }else if(data["type"]== "folder"){
			dojo.byId(field).value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('folderInode').value =  dijit.byId('HostSelector').attr('value');
			dojo.byId('hostId').value = "";
		}
	  }
	}
	
	function deleteFolder(folderInode){
		var el = dijit.byId('language_id');
		var divResponse = dojo.byId("responseDeleteFolder");
		dijit.byId("deleteFolderBtn").setAttribute('disabled',true);
		var body = document.getElementsByTagName("body")[0];
		body.setAttribute("style","cursor: wait !important");
		var xhrArgs = {
			url:'/DotAjaxDirector/com.eng.dotcms.additions.deleteFolder.ajax.DeleteFolderAjaxAction/cmd/deleteFolder/folderInode/'+folderInode,
			handleAs: 'text',
			load: function(data){
				if(data.indexOf("FAILURE") > -1){
					dojo.style(divResponse, {color:'#CC3333'});
					divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "msg.error.deleteFolder") %><br />'+data
					dijit.byId("deleteFolderBtn").setAttribute('disabled',false);
					body.removeAttribute("style");
				}else{
					dojo.style(divResponse, {color:'#009933'});
					divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "msg.success.deleteFolder") %>'	
					dijit.byId("deleteFolderBtn").setAttribute('disabled',false);
					body.removeAttribute("style");
				}
			},
			error: function(err) {
				dojo.style(divResponse, {color:'#CC3333'});
				divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "msg.error.deleteFolder") %><br />'+error
				dijit.byId("deleteFolderBtn").setAttribute('disabled',false);
				body.removeAttribute("style");
			}
		}	
		dojo.style(divResponse, {color:'#FFCC33'});
		divResponse.innerHTML = '<%= LanguageUtil.get(pageContext, "msg.wait.deleteFolder") %>'
		var deferred = dojo.xhrPost(xhrArgs);	
	}	
	
	function resized() {
	    var viewport = dijit.getViewport();
	    var viewport_height = viewport.h;
	    
	    var  e =  dojo.byId("borderContainer");
	    dojo.style(e, "height", viewport_height -150+ "px");
	    
	    dijit.byId("borderContainer").resize();
	}
	
	dojo.ready(function(){
	    dojo.connect(window,"onresize",resized);
	    resized();
	});
</script>

<div class="portlet-wrapper">
	<div class="subNavCrumbTrail">
		<ul id="subNavCrumbUl">        
			<li><%=LanguageUtil.get(pageContext, "javax.portlet.title.EXT_REMOTE_DELETE_FOLDER")%></li>
			<li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "javax.portlet.title.EXT_REMOTE_DELETE_FOLDER_VIEW")%></span></li>
		</ul>
		<div class="clear"></div>
	</div>
	
	<div id="brokenLinkMain">
        <div id="borderContainer" dojoType="dijit.layout.BorderContainer" style="width:100%;">
            <div dojoType="dijit.layout.ContentPane" region="top">
              <div id="tools">
              	<span style="float: left; width: 60%">
<%
				String host = (String)(request.getAttribute("host") != null?request.getAttribute("host"):"");
				String folder = (String)(request.getAttribute("folder") != null?request.getAttribute("folder"):"");
				String selectorValue = UtilMethods.isSet(folder) && !folder.equals(FolderAPI.SYSTEM_FOLDER)?folder:host;
%>              	
					<div style="float: left; margin-right: 5px; margin-left: 8px;"><%=LanguageUtil.get(pageContext, "select-folder-to-delete")%>:</div>					
	      			
	     			
	     			<div style="float: right; margin-left: 8px">
	     				<button id="deleteFolderBtn" dojoType="dijit.form.Button" iconClass="sServerIcon" onClick="deleteFolder(document.getElementById('folderInode').value)">
							<%= LanguageUtil.get(pageContext, "delete-remote-folder")%>
						</button>
	     			</div>
	     			<div style="float: right; margin-left: 1px;" id="HostSelector" dojoType="dotcms.dijit.form.HostFolderFilteringSelect" onChange="updateHostFolderValues('fieldValue');"
	            		value="<%= selectorValue %>"></div>	  
					<input type="hidden" name="fieldValue" id="fieldValue"
            			value="<%= selectorValue %>"/>	            		   			
	     			<input type="hidden" name="hostId" id="hostId" value="<%=host%>"/>
	     			<input type="hidden" name="folderInode" id="folderInode" value="<%=folder%>"/>
              	</span>                                            
              </div>
              <div class="clear"></div>
            </div> 
            <div dojoType="dijit.layout.ContentPane" region="center" style="height: 20px;">
            	<div style="float: left; margin-right: 5px; margin-left: 8px; margin-top: 10px; font-weight: bold" id="responseDeleteFolder"></div>
            </div>
        </div>
    </div>
</div>