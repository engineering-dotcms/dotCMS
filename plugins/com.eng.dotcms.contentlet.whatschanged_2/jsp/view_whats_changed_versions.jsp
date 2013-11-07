<%@page contentType="text/html;charset=UTF-8"%>
<%@page import="com.dotmarketing.portlets.common.bean.CrumbTrailEntry"%>
<%@page import="com.liferay.util.ParamUtil"%>
<%@page import="com.liferay.portal.ejb.PortletManagerUtil"%>
<%@page import="com.liferay.portal.model.Portlet"%>
<%@page import="org.w3c.dom.Document"%>
<%@page import="com.eng.dotcms.contentlet.whatschanged.WhatsChangedDaisyDiff"%>
<%@page import="com.eng.dotcms.contentlet.whatschanged.util.WhatsChangedUtil"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.beans.Identifier"%>
<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>

<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.portlets.languagesmanager.model.Language" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.dotmarketing.portlets.languagesmanager.business.*" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@ page import="com.dotmarketing.cache.StructureCache"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotmarketing.business.Role"%>
<%@ page import="com.dotmarketing.business.RoleAPI"%>
<%@ page import="com.dotmarketing.business.RoleAPIImpl"%>
<%@ page import="com.dotmarketing.portlets.folders.model.Folder"%>
<%@ page import="com.dotmarketing.beans.Host"%>
<%@ page import="com.dotmarketing.cache.FieldsCache"%>
<%@ page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@ page import="com.dotmarketing.business.PermissionAPI"%>

<%
	String portletId1 = "EXT_CONTENTLET_WHATS_CHANGED_VER";
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	
	if (!com.dotmarketing.util.UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);
%>

<style type="text/css">
	@import url("/html/plugins/com.eng.dotcms.contentlet.whatschanged/diff.css");
</style>

<script type="text/javascript">
	
	var checkboxCount = 0;
	checkboxValues = new Array();
	
	function addVersion(aCheckbox) {
		if(aCheckbox.checked==true){ //aggiungo
			if(checkboxCount==2){
				alert('Possono essere confrontate SOLO due versioni.');
				aCheckbox.checked=false;
				return;
			} else {				
				checkboxValues[checkboxCount] = aCheckbox.value;
				checkboxCount++;
				if(checkboxCount==2)
					dijit.byId("compareVersionsButton").attr("disabled", false);
				else
					dijit.byId("compareVersionsButton").attr("disabled", true);
			}		
		}else{ //elimino
			dijit.byId("compareVersionsButton").attr("disabled", true);
			for(var i=0; i<checkboxValues.length; i++){
				if(checkboxValues[i]==aCheckbox.value)
					checkboxValues.splice(i,1);
			}
			checkboxCount--;
		}
	}
	
	function compareVersions(identifier){
		var inodes_list='';
		for(var i=0; i<checkboxValues.length; i++){
			if(i>0){
				inodes_list+="|"+checkboxValues[i];				
			}else
				inodes_list+=checkboxValues[i];
		}	
		
		document.getElementById("inodes_list").value=inodes_list;
		document.getElementById("selected_contentlet").value=identifier;
		var form = document.getElementById("compare_versions_form");
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/view_whats_changed_versions" /><portlet:param name="cmd" value="compare_versions" /></portlet:actionURL>';
		submitForm(form);		
	}

</script>
<%
	
		String id=request.getParameter("selected_contentlet");
		Identifier ident = APILocator.getIdentifierAPI().find(id);
		boolean isImage = UtilMethods.isImage(ident.getAssetName());
		List<Contentlet> versions = APILocator.getContentletAPI().findAllVersions(ident, user, false);
		boolean canEdit  = false;
	
		if(versions.size() > 0){
			canEdit  = APILocator.getPermissionAPI().doesUserHavePermission(versions.get(0), PermissionAPI.PERMISSION_EDIT, user);
		}

%>

<div class="portlet-wrapper">
	<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>
	<form method="Post" action="" id="compare_versions_form">
		<input type="hidden" name="inodes_list" id="inodes_list" value="">
		<input type="hidden" name="selected_contentlet" id="selected_contentlet" value="">		
				<div id="contentletVersionsDiv" style="height:auto;">
					<div class="buttonRow" style="text-align: left;padding-left:20px;">
					<i><%= LanguageUtil.get(pageContext, "Select-Versions-Description") %></i>
					</div>
					<div class="buttonRow" style="text-align: left;padding-left:20px;">
					<%= LanguageUtil.get(pageContext, "Identifier") %> : <%=ident.getId() %>
					</div>
					<table class="listingTable">
						<tr>
							<th width="5%" nowrap><%= LanguageUtil.get(pageContext, "Status") %></th>
							<th width="5%" nowrap ><%= LanguageUtil.get(pageContext, "Action") %></th>
							<th width="1%" nowrap>&nbsp;</th>
							<th width="50%"><%= LanguageUtil.get(pageContext, "Title") %></th>
							<th width="20%"><%= LanguageUtil.get(pageContext, "Author") %></th>
							<%if(isImage){ %>
								<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Image") %></th>
							<%} %>
							<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Modified-Date") %></th>
							<th width="20%" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Inode") %></th>
						</tr>
					<%
					    Iterator<Contentlet> versionsIt = versions.iterator();
						int kmod = 0;
						boolean isAlreadyLocked = false;
						while (versionsIt.hasNext()) {
							Contentlet ver = versionsIt.next();
							Contentlet c = (Contentlet) ver;
							ContentletVersionInfo verinfo=APILocator.getVersionableAPI().getContentletVersionInfo(id, c.getLanguageId());
							Language langV=APILocator.getLanguageAPI().getLanguage(c.getLanguageId());
							boolean working = c.getInode().equals(verinfo.getWorkingInode());
							boolean live = c.getInode().equals(verinfo.getLiveInode());
							String vinode = ver.getInode();
							String title = ver.getTitle();
							String modUser = ver.getModUser();
							Date modDate = ver.getModDate();
					
							String str_style = "";
							if ((kmod % 2) == 0) {
								str_style = "class='alternate_1'";
							}
							else{
								str_style = "class='alternate_2'";
							}
							kmod++;
					%>
						<tr  <%=str_style%>>
							<td nowrap="nowrap" width="50" align="center">
							<%if(ver.isWorking()){%>
								<img src="/html/images/icons/status-away.png" />
							<%} %>
							<%if(ver.isLive()){%>
								<img src="/html/images/icons/status.png" />
							<%} %>
							</td>
							<td nowrap="nowrap" style="text-align:center;">
								<input type="checkbox" name="version_<%=vinode%>" value="<%=vinode%>" onClick="javascript: addVersion(this)" />						
							</td>
							<td> <img src="/html/images/languages/<%=langV.getLanguageCode()+"_"+langV.getCountryCode() %>.gif"/></td>
							<td> <%= title %><%if(working){%>&nbsp;(<%= LanguageUtil.get(pageContext, "Working-Version") %>)<% }%></td>
					<% 
						String modUserName = "";
						if(UtilMethods.isSet(modUser)){
							try{
								modUserName = APILocator.getUserAPI().loadUserById(modUser,APILocator.getUserAPI().getSystemUser(),false) != null ? APILocator.getUserAPI().loadUserById(modUser,APILocator.getUserAPI().getSystemUser(),false).getFullName(): "";
							}catch(Exception e){Logger.debug(this,"No User Found");}
						}
					%>
							 <td><%= modUserName %></td>
							<!-- DOTCMS-3813  -->
							<!-- }  -->
							
							<!-- Timezone
							<%= Calendar.getInstance().getTimeZone().getID() %> 
							 -->
							 <%if(isImage){ %>
								 <td align="center">
								 	<% if (!working && canEdit && !live) { %>
								 		<a  href="javascript: selectVersion('<%= vinode %>');">
								 	<%} %>
								 	<img src="/contentAsset/image/<%=vinode %>/fileAsset/?byInode=1&filter=Thumbnail&thumbnail_h=125&thumbnail_w=125" style="width:150px;height:150px;border:1px solid silver;padding:3px;"></a>
									
								 </td>
							 <%} %>
							<td nowrap="nowrap" style="text-align:center;"><%= UtilMethods.dateToHTMLDate(modDate) %> - <%= UtilMethods.dateToHTMLTime(modDate) %></td>
							<td nowrap="nowrap"><%= vinode %></td>
						</tr>
						<% } if (versions.size() == 0) { %>
							<tr>
								<td colspan="5">
									<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Versions-Found") %></div>
								</td>
							</tr>
						<% } %>
					
					</table>
					<div style="width: 100%; text-align: center; margin: 10px 0 2px 0">- - -</div>
					<!-- START Buton Row -->
					<div class="buttonRow">
						
						
						<div id="unArchiveButtonDiv">
							<button dojoType="dijit.form.Button" id="compareVersionsButton"  onClick="compareVersions('<%=id%>')" iconClass="publishIconDis" disabled="true">
								<%= LanguageUtil.get(pageContext, "Whats-Changed") %>
							</button>
						</div>
					</div>
					<!-- END Buton Row --> 					
				</div>
	</form>
	<%
		if(null!=request.getAttribute("compare_versions")){
	%>
	<hr />
		<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" style="height:auto; padding: 0 5px 5px 5px" id="borderContainer" class="shadowBox headerBox">
			<div style="float: left; margin: 10px 5px 10px 5px; width: 30%; text-align: right;"><strong><%= LanguageUtil.get(pageContext, "Field-name") %></strong></div>
			<div style="float: right; margin: 10px 5px 10px 5px; text-align: left; width: 60%"><strong><%= LanguageUtil.get(pageContext, "Difference") %></strong></div>
			<div class="clear"></div>
			<div style="margin-top: 20px"></div>
	<%			
			//questa lista è di due elementi e il primo (in posizione 0) è considerato la versione SU CUI EFFETTUARE IL CONFRONTO.
			List<Contentlet> selectedVersions = WhatsChangedUtil.getSelectedVersions(versions, request.getParameter("inodes_list"));
			Contentlet newVersion = selectedVersions.get(0);
			Contentlet oldVersion = selectedVersions.get(1);
			List<Field> fields = WhatsChangedUtil.getTextFields(FieldsCache.getFieldsByStructureInode(newVersion.getStructureInode()));
			for(Field field:fields){
	%>
			<div style="float: left; margin: 10px 5px 10px 5px; width: 30%; text-align: right;"><strong><%=field.getFieldName()%>: </strong></div>
			<div style="float: right; margin: 10px 5px 10px 5px; text-align: left; width: 60%; background-color: #E0E9F6; border:1px solid #9AB3CE; padding: 5px 5px 5px 5px"><%=WhatsChangedDaisyDiff.diffByField(newVersion, oldVersion, field)%></div>
			<div class="clear"></div>
	<%
			}
	%>
		</div>
	<%		
		}
	%>	
	
</div>
