package org.dotcms.forum;

import static org.dotcms.forum.util.ForumConstants.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.dotcms.forum.util.ForumUtils;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cms.content.submit.PluginDeployer;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.model.Field.DataType;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.widget.business.WidgetAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class ForumDeployer extends PluginDeployer {
	
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	private PermissionAPI perAPI = APILocator.getPermissionAPI();
	
	@Override
	public boolean deploy() {
		try {
			if(Boolean.valueOf(pluginAPI.loadProperty(PLUGIN_ID, PROP_AUTO_CREATE_STRUCTURES))){				
				Folder rootPath = APILocator.getFolderAPI().findFolderByPath(pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_ROOT_PATH), 
						APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true), 
						APILocator.getUserAPI().getSystemUser(), true);
				Folder topicPath = APILocator.getFolderAPI().findFolderByPath(pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_ROOT_PATH)+pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_TOPIC_PATH), 
						APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true), 
						APILocator.getUserAPI().getSystemUser(), true);
				Folder threadPath = APILocator.getFolderAPI().findFolderByPath(pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_ROOT_PATH)+pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_THREAD_PATH), 
						APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true), 
						APILocator.getUserAPI().getSystemUser(), true);
				
				if(!UtilMethods.isSet(rootPath.getInode())){
					rootPath = APILocator.getFolderAPI().createFolders(pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_ROOT_PATH), 
							APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true), 
							APILocator.getUserAPI().getSystemUser(), true);
					topicPath = APILocator.getFolderAPI().createFolders(rootPath.getPath()+pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_TOPIC_PATH), 
							APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true), 
							APILocator.getUserAPI().getSystemUser(), true);
					threadPath = APILocator.getFolderAPI().createFolders(rootPath.getPath()+pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_THREAD_PATH), 
							APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true), 
							APILocator.getUserAPI().getSystemUser(), true);
				}else{
					if(!UtilMethods.isSet(topicPath.getInode())){
						topicPath = APILocator.getFolderAPI().createFolders(rootPath.getPath()+pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_TOPIC_PATH), 
							APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true), 
							APILocator.getUserAPI().getSystemUser(), true);
					}
					if(!UtilMethods.isSet(threadPath.getInode())){
						threadPath = APILocator.getFolderAPI().createFolders(rootPath.getPath()+pluginAPI.loadProperty(PLUGIN_ID, PROP_FOLDER_THREAD_PATH), 
							APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true), 
							APILocator.getUserAPI().getSystemUser(), true);
					}
				}
				
				// create Topic Structure
				if(!ForumUtils.existStructure(TOPIC_STRUCTURE_NAME)) {
					Logger.info(this, "The Structure Topic must be created");
					Structure topicStructure = ForumUtils.createStructure(TOPIC_STRUCTURE_NAME, TOPIC_STRUCTURE_NAME, topicPath, Structure.STRUCTURE_TYPE_CONTENT);					
					// start field creation
					ForumUtils.createField(topicStructure.getInode(), "Title", FieldType.TEXT, DataType.TEXT, true, true, true, true);
					StringBuffer sb = new StringBuffer();
					sb.append("#parse('/static/plugins/org.dotcms.forum.plugin/url_title_custom_field_code.vtl')");
					ForumUtils.createCustomField(topicStructure.getInode(), "URL Title", sb, true, true, false, false);
					ForumUtils.createField(topicStructure.getInode(), "Type", FieldType.CATEGORY, DataType.TEXT, true, true, false, false);
					ForumUtils.createField(topicStructure.getInode(), "Last Modified", FieldType.DATE_TIME, DataType.DATE, true, true, true, true);
					ForumUtils.createField(topicStructure.getInode(), "Description", FieldType.WYSIWYG, DataType.LONG_TEXT, false, false, false, false);
					ForumUtils.createField(topicStructure.getInode(), "Topic Path", FieldType.HOST_OR_FOLDER, DataType.TEXT, false, false, false, false);
				}
				
				// create Thread Structure
				if(!ForumUtils.existStructure(THREAD_STRUCTURE_NAME)) {
					Logger.info(this, "The Structure Thread must be created");
					Structure threadStructure = ForumUtils.createStructure(THREAD_STRUCTURE_NAME, THREAD_STRUCTURE_NAME, threadPath, Structure.STRUCTURE_TYPE_CONTENT);
					ForumUtils.createField(threadStructure.getInode(), "Title", FieldType.TEXT, DataType.TEXT, true, true, true, true);
					StringBuffer sb = new StringBuffer();
					sb.append("#parse('/static/plugins/org.dotcms.forum.plugin/url_title_custom_field_code.vtl')");
					ForumUtils.createCustomField(threadStructure.getInode(), "URL Title", sb, true, true, false, false);					
					ForumUtils.createField(threadStructure.getInode(), "Last Modified", FieldType.DATE_TIME, DataType.DATE, true, true, true, true);
					sb = new StringBuffer();
					sb.append("Open|yes");
					sb.append('\r');
					sb.append('\n');
					sb.append("Closed|no");
					ForumUtils.createRadioField(threadStructure.getInode(), "Thread Status", sb, "yes", DataType.TEXT, true, true, true, false);
					ForumUtils.createField(threadStructure.getInode(), "Description", FieldType.WYSIWYG, DataType.LONG_TEXT, false, false, false, false);
					ForumUtils.createField(threadStructure.getInode(), "Thread Path", FieldType.HOST_OR_FOLDER, DataType.TEXT, false, false, false, false);
					perAPI.permissionIndividually(APILocator.getHostAPI().find(threadStructure.getHost(), 
							APILocator.getUserAPI().getSystemUser(), false), 
							threadStructure, APILocator.getUserAPI().getSystemUser(), false);
					for(Permission p : addAnonymousPermissions(threadStructure))
						perAPI.save(p, threadStructure, APILocator.getUserAPI().getSystemUser(), false);
				}	
				
				// create Reply Structure
				if(!ForumUtils.existStructure(REPLY_STRUCTURE_NAME)) {
					Logger.info(this, "The Structure Reply must be created");
					Structure replyStructure = ForumUtils.createStructure(REPLY_STRUCTURE_NAME, REPLY_STRUCTURE_NAME, threadPath, Structure.STRUCTURE_TYPE_CONTENT);
					ForumUtils.createField(replyStructure.getInode(), "Title", FieldType.TEXT, DataType.TEXT, true, true, true, true);
					ForumUtils.createField(replyStructure.getInode(), "Description", FieldType.WYSIWYG, DataType.LONG_TEXT, true, false, false, false);
					perAPI.permissionIndividually(APILocator.getHostAPI().find(replyStructure.getHost(), 
							APILocator.getUserAPI().getSystemUser(), false), 
							replyStructure, APILocator.getUserAPI().getSystemUser(), false);
					for(Permission p : addAnonymousPermissions(replyStructure))
						perAPI.save(p, replyStructure, APILocator.getUserAPI().getSystemUser(), false);
				}
				
				// create TopicListingWidget Structure
				if(!ForumUtils.existStructure(TOPIC_LISTING_STRUCTURE_NAME)) {
					Logger.info(this, "The Structure TopicListingWidget must be created");
					Structure topicListingStructure = ForumUtils.createStructure(TOPIC_LISTING_STRUCTURE_NAME, TOPIC_LISTING_STRUCTURE_NAME, topicPath, Structure.STRUCTURE_TYPE_WIDGET);					
					// start field creation
					APILocator.getWidgetAPI().createBaseWidgetFields(topicListingStructure);
					ForumUtils.createField(topicListingStructure.getInode(), "Listing Header", FieldType.TEXT, DataType.TEXT, true, true, false, false);
					ForumUtils.createField(topicListingStructure.getInode(), "Forum Description", FieldType.WYSIWYG, DataType.LONG_TEXT, true, false, false, false);
					ForumUtils.createField(topicListingStructure.getInode(), "Max Topic Per Page", FieldType.TEXT, DataType.INTEGER, true, false, false, false);
					StringBuffer sb = new StringBuffer();
					sb.append("Ascendent|asc");
					sb.append('\r');
					sb.append('\n');
					sb.append("Descendent|desc");
					ForumUtils.createRadioField(topicListingStructure.getInode(), "Order By Date", sb, "desc", DataType.TEXT, true, false, false, false);
					ForumUtils.createField(topicListingStructure.getInode(), "Forum Category", FieldType.CATEGORY, DataType.TEXT, true, true, false, false);
					ForumUtils.createField(topicListingStructure.getInode(), "Thread Listing Page URL", FieldType.TEXT, DataType.TEXT, false, false, false, false);
					ForumUtils.createField(topicListingStructure.getInode(), "Widget Path", FieldType.HOST_OR_FOLDER, DataType.TEXT, false, false, false, false);
					
					// get the widgetCode for update the field
					List<Field> fields = FieldsCache.getFieldsByStructureInode(topicListingStructure.getInode());
					for(Field field : fields){
						if(field.getFieldName().equals(WidgetAPI.WIDGET_CODE_FIELD_NAME)){
							field.setValues("#parse('/static/plugins/org.dotcms.forum.plugin/topic-listing-widget-structure-widget-code.vtl')");
							FieldFactory.saveField(field);
							FieldsCache.removeField(field);
							FieldsCache.addField(field);

						}
					}
				}
				
				// create ThreadListingWidget Structure
				if(!ForumUtils.existStructure(THREAD_LISTING_STRUCTURE_NAME)) {
					Logger.info(this, "The Structure ThreadListingWidget must be created");
					Structure threadListingStructure = ForumUtils.createStructure(THREAD_LISTING_STRUCTURE_NAME, THREAD_LISTING_STRUCTURE_NAME, threadPath, Structure.STRUCTURE_TYPE_WIDGET);
					APILocator.getWidgetAPI().createBaseWidgetFields(threadListingStructure);
					// start field creation
					StringBuffer sb = new StringBuffer();
					sb.append("Ascendent|asc");
					sb.append('\r');
					sb.append('\n');
					sb.append("Descendent|desc");
					
					ForumUtils.createRadioField(threadListingStructure.getInode(), "Order By Date", sb, "desc", DataType.TEXT, true, false, false, false);
					ForumUtils.createField(threadListingStructure.getInode(), "Redirect Page", FieldType.TEXT, DataType.TEXT, true, false, false, false);
					ForumUtils.createField(threadListingStructure.getInode(), "Widget Path", FieldType.HOST_OR_FOLDER, DataType.TEXT, false, false, false, false);
					// get the widgetCode for update the field
					List<Field> fields = FieldsCache.getFieldsByStructureInode(threadListingStructure.getInode());
					for(Field field : fields){
						if(field.getFieldName().equals(WidgetAPI.WIDGET_CODE_FIELD_NAME)){
							field.setValues("#parse('/static/plugins/org.dotcms.forum.plugin/thread-listing-widget-structure-widget-code.vtl')");
							FieldFactory.saveField(field);
							FieldsCache.removeField(field);
							FieldsCache.addField(field);

						}
					}
				}
				
				// create ThreadDetailWidget Structure
				if(!ForumUtils.existStructure(THREAD_DETAIL_STRUCTURE_NAME)) {
					Logger.info(this, "The Structure ThreadDetailWidget must be created");
					Structure threadDetailStructure = ForumUtils.createStructure(THREAD_DETAIL_STRUCTURE_NAME, THREAD_DETAIL_STRUCTURE_NAME, threadPath, Structure.STRUCTURE_TYPE_WIDGET);
					APILocator.getWidgetAPI().createBaseWidgetFields(threadDetailStructure);
					// start field creation
					StringBuffer sb = new StringBuffer();
					sb.append("Ascendent|asc");
					sb.append('\r');
					sb.append('\n');
					sb.append("Descendent|desc");
					ForumUtils.createRadioField(threadDetailStructure.getInode(), "Order By Date", sb, "desc", DataType.TEXT, true, false, false, false);
					ForumUtils.createField(threadDetailStructure.getInode(), "Redirect Page", FieldType.TEXT, DataType.TEXT, true, false, false, false);
					ForumUtils.createField(threadDetailStructure.getInode(), "Widget Path", FieldType.HOST_OR_FOLDER, DataType.TEXT, false, false, false, false);
					// get the widgetCode and the preExecute for update the fields
					List<Field> fields = FieldsCache.getFieldsByStructureInode(threadDetailStructure.getInode());
					for(Field field : fields){
						if(field.getFieldName().equals(WidgetAPI.WIDGET_CODE_FIELD_NAME)){
							field.setValues("#parse('/static/plugins/org.dotcms.forum.plugin/thread-detail-widget-structure-widget-code.vtl')");
							FieldFactory.saveField(field);
							FieldsCache.removeField(field);
							FieldsCache.addField(field);
						}else if(field.getFieldName().equals(WidgetAPI.WIDGET_PRE_EXECUTE_FIELD_NAME)){
							field.setValues(getPreExecuteCode().toString());
							FieldFactory.saveField(field);
							FieldsCache.removeField(field);
							FieldsCache.addField(field);							
						}
					}
				}
				
				// create relationship between the Topic and Thread structures
				if(!ForumUtils.existRelationship(TOPIC_THREAD_RELATIONSHIP_NAME, StructureCache.getStructureByVelocityVarName(TOPIC_STRUCTURE_NAME))){
					Logger.info(this, "The Relationship "+TOPIC_THREAD_RELATIONSHIP_NAME+" must be created");
					Relationship relationshipTopicThread = new Relationship();
					relationshipTopicThread.setParentStructureInode(StructureCache.getStructureByVelocityVarName(TOPIC_STRUCTURE_NAME).getInode());
					relationshipTopicThread.setParentRelationName("Parent "+TOPIC_STRUCTURE_NAME);
					relationshipTopicThread.setParentRequired(false);
					relationshipTopicThread.setChildStructureInode(StructureCache.getStructureByVelocityVarName(THREAD_STRUCTURE_NAME).getInode());
					relationshipTopicThread.setChildRelationName("Child "+THREAD_STRUCTURE_NAME);
					relationshipTopicThread.setChildRequired(false);
					relationshipTopicThread.setCardinality(0);
					relationshipTopicThread.setRelationTypeValue(TOPIC_THREAD_RELATIONSHIP_NAME);
					RelationshipFactory.saveRelationship(relationshipTopicThread);					
				}	
				
				// create relationship between the Thread and Reply structures
				if(!ForumUtils.existRelationship(THREAD_REPLY_RELATIONSHIP_NAME, StructureCache.getStructureByVelocityVarName(THREAD_STRUCTURE_NAME))){
					Logger.info(this, "The Relationship "+THREAD_REPLY_RELATIONSHIP_NAME+" must be created");
					Relationship relationshipThreadReply = new Relationship();
					relationshipThreadReply.setParentStructureInode(StructureCache.getStructureByVelocityVarName(THREAD_STRUCTURE_NAME).getInode());
					relationshipThreadReply.setParentRelationName("Parent "+THREAD_STRUCTURE_NAME);
					relationshipThreadReply.setParentRequired(false);
					relationshipThreadReply.setChildStructureInode(StructureCache.getStructureByVelocityVarName(REPLY_STRUCTURE_NAME).getInode());
					relationshipThreadReply.setChildRelationName("Child "+REPLY_STRUCTURE_NAME);
					relationshipThreadReply.setChildRequired(false);
					relationshipThreadReply.setCardinality(0);
					relationshipThreadReply.setRelationTypeValue(THREAD_REPLY_RELATIONSHIP_NAME);
					RelationshipFactory.saveRelationship(relationshipThreadReply);					
				}				
				alertCreateDetailPages();
			}
			return true;
		}catch(Exception e){
			Logger.error(this, "Error in deploy plugin "+PLUGIN_ID, e);
			CacheLocator.getCacheAdministrator().flushAll();
			return false;
		}
	}

	private List<Permission> addAnonymousPermissions(Structure pollVoteStructure) throws DotDataException {
		List<Permission> votePermissions = new ArrayList<Permission>();
		Permission cmsAnonPublish = new Permission();
		cmsAnonPublish.setRoleId(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
		cmsAnonPublish.setPermission(PermissionAPI.PERMISSION_PUBLISH);
		cmsAnonPublish.setInode(pollVoteStructure.getInode());
		
		Permission cmsAnonEdit = new Permission();
		cmsAnonEdit.setRoleId(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
		cmsAnonEdit.setPermission(PermissionAPI.PERMISSION_EDIT);
		cmsAnonEdit.setInode(pollVoteStructure.getInode());
		
		Permission cmsAnonUse = new Permission();
		cmsAnonUse.setRoleId(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
		cmsAnonUse.setPermission(PermissionAPI.PERMISSION_USE);
		cmsAnonUse.setInode(pollVoteStructure.getInode());
		votePermissions.add(cmsAnonUse);
		votePermissions.add(cmsAnonEdit);
		votePermissions.add(cmsAnonPublish);
		return votePermissions;
	}
	
	private StringBuffer getPreExecuteCode() throws IOException {
		InputStream in = null;
		try{
			StringBuffer _code = new StringBuffer();
			in = this.getClass().getClassLoader().getResourceAsStream("code.txt");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				_code.append(line);
				_code.append('\r');
				_code.append('\n');
			}			
			return _code;
		}finally{
			if(null!=in)
				in.close();			
		}
	}
	
	private void alertCreateDetailPages(){
		Logger.info(this, "*****************************************************************************************");
		Logger.info(this, "*****************************************************************************************");
		Logger.info(this, APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(), "msg-deployer-detail-pages-pt1"));		
		Logger.info(this, APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(), "msg-deployer-detail-pages-pt2"));
		Logger.info(this, APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(), "msg-deployer-detail-pages-pt3"));
		Logger.info(this, APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(), "msg-deployer-detail-pages-pt4"));
		Logger.info(this, "*****************************************************************************************");
		Logger.info(this, "*****************************************************************************************");
	}
}
