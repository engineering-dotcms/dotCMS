package org.dotcms.forum.util;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.model.Field.DataType;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;

public class ForumUtils {
	
	private static HostAPI hostAPI = APILocator.getHostAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static ContentletAPI conAPI = APILocator.getContentletAPI();
	private static LanguageAPI langAPI = APILocator.getLanguageAPI();
	private static PluginAPI pluginAPI = APILocator.getPluginAPI();
	
	public static List<Contentlet> getUserSubscriptionsPerStructure(String userId, String contentIdentifier){
    	List<Contentlet> cons = new ArrayList<Contentlet>();
		try{
			User userToCheck = userAPI.loadUserById(userId, userAPI.getSystemUser(), true);
			Contentlet content =  conAPI.findContentletByIdentifier(contentIdentifier, true, langAPI.getDefaultLanguage().getId(), userAPI.getSystemUser(), true);
			if(!UtilMethods.isSet(content.getInode())){
				Logger.error(ForumUtils.class,"There was an error. Please try again");
			}
			else if(UtilMethods.isSet(userToCheck) && UtilMethods.isSet(content)){
				Structure subscriptionStructure = getStructureToSubscribeOrUnsubscribe(content.getStructure());
				List <Field> fields = FieldsCache.getFieldsByStructureInode(subscriptionStructure.getInode());
				Field userIdField = null;
				Field contentIdentifierField = null;
				
				for (Field f : fields){
					if(f.getVelocityVarName().contains("userId")){
						userIdField = f;
					}
					if(f.getVelocityVarName().contains("topicId") || f.getVelocityVarName().contains("threadId")){
						contentIdentifierField = f;
					}
				}
				
				String luceneQuery = "+structureName:" + subscriptionStructure.getVelocityVarName() + 
				" +"+ subscriptionStructure.getVelocityVarName() + "." + userIdField.getVelocityVarName() + ":" + userToCheck.getUserId() +
				" +"+ subscriptionStructure.getVelocityVarName() + "." + contentIdentifierField.getVelocityVarName() + ":"+content.getIdentifier();
				
				cons = conAPI.search(luceneQuery, 0, -1, "modDate", userAPI.getSystemUser(), true);
			}
		}
		catch(Exception e) {
			Logger.error(ForumUtils.class, "Unable to subscribe to topic");
		}
		
		return cons;
	}
	
	public static void createSubscription (Contentlet content, User subscribingUser) throws DotHibernateException{
		
		Structure contentStructure = content.getStructure();
		Structure subscriptionStructure = getStructureToSubscribeOrUnsubscribe(contentStructure);
		List <Field> fields = FieldsCache.getFieldsByStructureInode(subscriptionStructure.getInode());
		Field userIdField = null;
		Field contentIdentifierField = null;
		Contentlet newCont = new Contentlet();
		for (Field f : fields){
			if(f.getVelocityVarName().contains("userId")){
				userIdField = f;
			}
			if(f.getVelocityVarName().contains("topicId") || f.getVelocityVarName().equalsIgnoreCase("threadId")){
				contentIdentifierField = f;
			}
		}
		
		try{
			if(UtilMethods.isSet(userIdField.getInode()) && UtilMethods.isSet(contentIdentifierField.getInode()) ){
				newCont.setStructureInode(subscriptionStructure.getInode());

				newCont.setModUser(subscribingUser.getUserId());
				newCont.setOwner(subscribingUser.getUserId());
				newCont.setLanguageId(langAPI.getDefaultLanguage().getId());
				newCont.setHost(hostAPI.findDefaultHost(userAPI.getSystemUser(), true).getIdentifier());
				conAPI.setContentletProperty(newCont, userIdField, subscribingUser.getUserId());
				conAPI.setContentletProperty(newCont, contentIdentifierField, content.getIdentifier());
			}
			
			newCont = conAPI.checkin(newCont, new ArrayList<Category>(), new ArrayList<Permission>(), 
					userAPI.getSystemUser(), true);
			
			APILocator.getVersionableAPI().setLive(newCont);
			APILocator.getVersionableAPI().setDeleted(newCont,false);
			APILocator.getVersionableAPI().setWorking(newCont);
			
		}
		catch(Exception e){
			Logger.error(ForumUtils.class, "There was an error. User can't subscribe to this content. Please try again." + e.getMessage());
		}
	}
	
	public static void deleteSubscription(Contentlet content) {
		try{
			conAPI.unpublish(content, userAPI.getSystemUser(), true);
			conAPI.archive(content, userAPI.getSystemUser(), true);
			conAPI.delete(content, userAPI.getSystemUser(), true);
		}
		catch (Exception e){
			
		}	
	}
	
	public static Structure getSubscriptionStructureToSendEmails(Structure contentStructure){
		Structure subscriptionStructure = null;
		//if content is a new thread, send email to topic subscribers
		if (contentStructure.getVelocityVarName().toLowerCase().contains("thread"))
			subscriptionStructure = StructureFactory.getStructureByVelocityVarName("TopicSubscription");
		//if content is a new reply, send email to thread subscribers
		else if (contentStructure.getVelocityVarName().toLowerCase().contains("reply"))
			subscriptionStructure = StructureFactory.getStructureByVelocityVarName("ThreadSubscription");
		return subscriptionStructure;
	}
	
	public static Structure getStructureToSubscribeOrUnsubscribe(Structure contentStructure){
		Structure subscriptionStructure = null;
		//if content is a new thread, send email to topic subscribers
		if (contentStructure.getVelocityVarName().toLowerCase().contains("topic"))
			subscriptionStructure = StructureFactory.getStructureByVelocityVarName("TopicSubscription");
		//if content is a new reply, send email to thread subscribers
		else if (contentStructure.getVelocityVarName().toLowerCase().contains("thread"))
			subscriptionStructure = StructureFactory.getStructureByVelocityVarName("ThreadSubscription");
		return subscriptionStructure;
	}
	
	public static Structure createStructure(String name, String description, Host host, int type) throws DotHibernateException {
		Structure aStructure = new Structure();
		aStructure.setName(name);
		aStructure.setHost(host.getIdentifier());
		aStructure.setDescription(description);
		aStructure.setStructureType(type);
		String structureVelocityName = VelocityUtil.convertToVelocityVariable(aStructure.getName(), true);
		List<String> velocityvarnames = StructureFactory.getAllVelocityVariablesNames();
		int found = 0;
		if (VelocityUtil.isNotAllowedVelocityVariableName(structureVelocityName)) {
			found++;
		}

		for (String velvar : velocityvarnames) {
			if (velvar != null) {
				if (structureVelocityName.equalsIgnoreCase(velvar)) {
					found++;
				} else if (velvar.toLowerCase().contains(structureVelocityName.toLowerCase())) {
					String number = velvar.substring(structureVelocityName.length());
					if (RegEX.contains(number, "^[0-9]+$")) {
						found++;
					}
				}
			}
		}
		if (found > 0) {
			structureVelocityName = structureVelocityName + Integer.toString(found);
		}
		aStructure.setVelocityVarName(structureVelocityName);
		
		StructureFactory.saveStructure(aStructure);
		StructureCache.removeStructure(aStructure);
		StructureCache.addStructure(aStructure);
		return aStructure;
	}

	public static Structure createStructure(String name, String description, Folder folder, int type) throws DotDataException, DotSecurityException {
		Structure aStructure = new Structure();
		aStructure.setName(name);
		aStructure.setHost(APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false).getIdentifier());
		aStructure.setFolder(folder.getInode());
		aStructure.setDescription(description);
		aStructure.setStructureType(type);
		String structureVelocityName = VelocityUtil.convertToVelocityVariable(aStructure.getName(), true);
		List<String> velocityvarnames = StructureFactory.getAllVelocityVariablesNames();
		int found = 0;
		if (VelocityUtil.isNotAllowedVelocityVariableName(structureVelocityName)) {
			found++;
		}

		for (String velvar : velocityvarnames) {
			if (velvar != null) {
				if (structureVelocityName.equalsIgnoreCase(velvar)) {
					found++;
				} else if (velvar.toLowerCase().contains(structureVelocityName.toLowerCase())) {
					String number = velvar.substring(structureVelocityName.length());
					if (RegEX.contains(number, "^[0-9]+$")) {
						found++;
					}
				}
			}
		}
		if (found > 0) {
			structureVelocityName = structureVelocityName + Integer.toString(found);
		}
		aStructure.setVelocityVarName(structureVelocityName);
		
		StructureFactory.saveStructure(aStructure);
		StructureCache.removeStructure(aStructure);
		StructureCache.addStructure(aStructure);
		return aStructure;
	}

	
	/**
	 * Create a generic Field
	 * 
	 * Jan 14, 2013 - 12:16:41 PM
	 */
	public static void createField(String structureInode, String name, FieldType type, DataType dataType, boolean required, boolean searchable, boolean unique, boolean listed) throws DotHibernateException{
		Field aField = new Field();
		aField.setFieldName(name);
		aField.setFieldType(type.toString());
		aField.setSearchable(searchable);
		aField.setIndexed(searchable);
		aField.setUnique(unique);
		aField.setRequired(required);
		aField.setVelocityVarName(VelocityUtil.convertToVelocityVariable(name, false));
		aField.setStructureInode(structureInode);
		aField.setListed(listed);
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
		aField.setSortOrder(fields.size()+1);
		if(DataType.BOOL.equals(dataType)){
//			aField.setValues("True|1\nFalse|0");
			aField.setDefaultValue("0");
		}
		if(FieldType.RELATIONSHIPS_TAB.equals(type))
			aField.setFieldContentlet("");
		else
			aField.setFieldContentlet(FieldFactory.getNextAvaliableFieldNumber(dataType.toString(), "", structureInode));
		FieldFactory.saveField(aField);
		FieldsCache.removeField(aField);
		FieldsCache.addField(aField);
	}

	/**
	 * Create a radio Field
	 * 
	 * Jan 14, 2013 - 12:16:41 PM
	 */
	public static void createRadioField(String structureInode, String name, StringBuffer code, String defaultValue, DataType dataType, boolean required, boolean searchable, boolean unique, boolean listed) throws DotHibernateException{
		Field aField = new Field();
		aField.setFieldName(name);
		aField.setFieldType(FieldType.RADIO.toString());
		aField.setSearchable(searchable);
		aField.setIndexed(searchable);
		aField.setUnique(unique);
		aField.setRequired(required);
		aField.setVelocityVarName(VelocityUtil.convertToVelocityVariable(name, false));
		aField.setStructureInode(structureInode);
		aField.setListed(listed);
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
		aField.setSortOrder(fields.size()+1);		
		if(DataType.BOOL.equals(dataType))
			aField.setDefaultValue("0");
		else if(DataType.TEXT.equals(dataType)){
			aField.setValues(code.toString());
			aField.setDefaultValue(defaultValue);
		}
		aField.setFieldContentlet(FieldFactory.getNextAvaliableFieldNumber(dataType.toString(), "", structureInode));
		FieldFactory.saveField(aField);
		FieldsCache.removeField(aField);
		FieldsCache.addField(aField);
	}
	
	/**
	 * Create a custom Field
	 * 
	 * Jan 14, 2013 - 12:16:41 PM
	 */
	public static void createCustomField(String structureInode, String name, StringBuffer code, boolean required, boolean searchable, boolean unique, boolean listed) throws DotHibernateException{
		Field aField = new Field();
		aField.setFieldName(name);
		aField.setFieldType(FieldType.CUSTOM_FIELD.toString());
		aField.setSearchable(searchable);
		aField.setIndexed(searchable);
		aField.setUnique(unique);
		aField.setRequired(required);
		aField.setVelocityVarName(VelocityUtil.convertToVelocityVariable(name, false));
		aField.setStructureInode(structureInode);
		aField.setListed(listed);
		aField.setFieldContentlet(FieldFactory.getNextAvaliableFieldNumber(DataType.LONG_TEXT.toString(), "", structureInode));
		aField.setValues(code.toString());
		List<Field> fields = FieldsCache.getFieldsByStructureInode(structureInode);
		aField.setSortOrder(fields.size()+1);
		FieldFactory.saveField(aField);
		FieldsCache.removeField(aField);
		FieldsCache.addField(aField);
	}
	
	public static boolean existStructure(String structureName){
		Structure s = StructureCache.getStructureByVelocityVarName(structureName);
		if(null!=s.getName())
			return true;
		return false;
	}
	
	public static boolean existRelationship(String relationshipName, Structure parentStructure){
		Relationship rel = RelationshipFactory.getRelationshipByRelationTypeValue(relationshipName);
		if(null!=rel.getRelationTypeValue())
			return true;
		return false;
	}	

	public static  void  SendContentSubmitEmail (Contentlet content, Contentlet parentContent, Structure parentStructure,
			List<String> emails) throws NoSuchUserException, DotDataException, DotSecurityException {

		StringBuffer Body = new StringBuffer();
		String emailHeader = pluginAPI.loadProperty("org.dotcms.forum.plugin", "submit.content.notification.email.header");
		if(UtilMethods.isSet(emailHeader)){
			if(emailHeader.contains("{0}"))
				emailHeader = emailHeader.replace("{0}", content.getStructure().getName());
			if(emailHeader.contains("{1}"))
				emailHeader = emailHeader.replace("{1}", parentStructure.getName());
			if(emailHeader.contains("{2}"))
				emailHeader = emailHeader.replace("{2}", parentContent.getTitle());
			
		}else {
			emailHeader = "Notification of update on " +  parentStructure.getName() + " \"" + parentContent.getTitle() + "\"";
		}
		

		Body.append("<table border='0' cellpadding=3 cellspacing=1 bgcolor='#eeeeee'>");
		Body.append("<tr><td colspan=\"2\"><b> " + emailHeader  + "</b></td>");
		//Body.append("<td align='right'>" +new Date().toString() +"</td></tr>");
		Body.append("<tr bgcolor='#ffffff'>");
		Body.append("<td>");
		Body.append("Name:");
		Body.append("</td>");
		Body.append("<td>");
		Body.append(content.getTitle());
		Body.append("</td>");
		Body.append("</tr>");
		Body.append("<tr bgcolor='#ffffff'>");
		Body.append("<td>");
		Body.append("Description:");
		Body.append("</td>");
		Body.append("<td>");
		Body.append(content.getMap().get("description"));
		Body.append("</td>");
		Body.append("</tr>");
		Body.append("<tr bgcolor='#ffffff'>");
		Body.append("<td>");
		Body.append("Date Added");
		Body.append("</td>");
		Body.append("<td>");
		String parsedDate = UtilMethods.dateToJDBC(content.getModDate());
		Body.append(parsedDate);
		Body.append("</td>");
		Body.append("</tr>");
		Body.append("<tr bgcolor='#ffffff'>");
		Body.append("<td>");
		Body.append("Content Creator");
		Body.append("</td>");
		Body.append("<td>");
		User contentOwner = userAPI.loadUserById(content.getOwner(), userAPI.getSystemUser(), true);
		Body.append(contentOwner.getFullName());
		Body.append("</td>");
		Body.append("</tr>");
		Body.append("<tr bgcolor='#ffffff'>");
		Body.append("<td>");
		Body.append("URL");
		Body.append("</td>");
		Body.append("<td>");
		String serverName = APILocator.getHostAPI().findDefaultHost(userAPI.getSystemUser(), false).getHostname();
		String urlMapPattern=parentStructure.getUrlMapPattern();
		int temp = urlMapPattern.indexOf("{");
		urlMapPattern=urlMapPattern.substring(0, temp);
		if(!urlMapPattern.endsWith("/"))
			urlMapPattern = urlMapPattern + "/";
		String contentUrl="http://"+ serverName + urlMapPattern + parentContent.getMap().get("urlTitle");
		Body.append(contentUrl);
		Body.append("</td>");
		Body.append("</tr>");

		
		
		Body.append("</table>");
		for (String email : emails) {
			try{
				Mailer m = new Mailer();
				m.setFromEmail("Website");
				m.setFromEmail(CompanyUtils.getDefaultCompany().getEmailAddress());
				m.setToEmail(email);
				m.setSubject(emailHeader);
				m.setHTMLBody(Body.toString());
				m.setFromEmail(Config.getStringProperty("EMAIL_SYSTEM_ADDRESS"));
				m.sendMessage();
			}
			catch(Exception e){
				Logger.error(ForumUtils.class, "Unable to send email to subscriber: " + email);
			}
	

		}
	
	}

}
