package org.dotcms.forum.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;
import org.dotcms.forum.util.CleanSubscriptionsThread;
import org.dotcms.forum.util.EmailSubscribersThread;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cms.content.submit.util.CaptchaUtil;
import com.dotmarketing.cms.content.submit.util.SubmitContentUtil;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.DNSUtil;
import com.dotmarketing.util.EmailUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.URLEncoder;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.UploadServletRequest;

/**
 * This Action manage the submit content save procedure
 * @author Jose
 *
 */
public class SubmitForumContentAction extends DispatchAction{


	private CategoryAPI catAPI = APILocator.getCategoryAPI();
	private HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static ContentletAPI conAPI = APILocator.getContentletAPI();
	private static PluginAPI pluginAPI = APILocator.getPluginAPI();

	public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response) {
		ActionForward forward = new ActionForward("/");
		forward.setRedirect(false);
		return forward;
	}

	@SuppressWarnings("unchecked")
	public ActionForward submitContent(ActionMapping rMapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
	throws Exception {

		ActionErrors errors = new ActionErrors();
		ActionMessages message = new ActionMessages();
		HttpSession session = request.getSession();
		UploadServletRequest uploadReq = (UploadServletRequest) request;
		Host host = hostWebAPI.getCurrentHost(request);
		

		boolean useCaptcha = false;
		boolean useAudioCaptcha =false;
		boolean autoPublish = false;
		String moderatorRole="";
		List<Field> imageFields = new ArrayList<Field>();
		List<Field> fileFields = new ArrayList<Field>();


		/**
		 * Getting Referrer
		 */
		String referrer = request.getParameter("referrer");
		if(referrer.endsWith("?")){
			referrer = referrer.substring(0,referrer.length()-1);
		}
		ActionForward af = new ActionForward(referrer);
		af.setRedirect(true);
		
		int index = referrer.lastIndexOf('/');
		String htmlServlet = null;
		if (index < 0)
			htmlServlet = referrer;
		else
			htmlServlet = referrer.substring(index + 1);
		
		if (htmlServlet.indexOf('.') < 0) {
			//If is a servlet
			referrer += "/";
		}
		
		String params="";
		HibernateUtil.startTransaction();

		try {
			/**
			 * Getting user
			 */
			String userId = request.getParameter("userId");

			/**
			 * Getting content structure
			 */
			String structureInode = request.getParameter("structure");
			Structure st = StructureCache.getStructureByInode(structureInode);


			/**
			 * Getting options flags
			 */
			String options = request.getParameter("options");
			if(UtilMethods.isSet(options)){
				options = PublicEncryptionFactory.decryptString(options);
				String[] opt = options.split(";");
				for(String text: opt){
					if(text.indexOf("contentUseCaptcha") != -1){
						useCaptcha = Boolean.parseBoolean(text.substring(text.indexOf("=")+1));
					}else if(text.indexOf("contentUseAudioCaptcha") != -1){
						useAudioCaptcha = Boolean.parseBoolean(text.substring(text.indexOf("=")+1));
					}else if(text.indexOf("contentAutoPublish") != -1){
						autoPublish = Boolean.parseBoolean(text.substring(text.indexOf("=")+1));
					}else if(text.indexOf("contentModeration") != -1){
						moderatorRole = text.substring(text.indexOf("=")+1);
					}
				}
			}

			/**
			 * Setting content values
			 */
			URLEncoder encoder = new URLEncoder();
			StringBuilder paramsBuff=new StringBuilder();
			List<String> parametersName = new ArrayList<String>();
			List<String[]> values = new ArrayList<String[]>();
			List<String> parametersfileName = new ArrayList<String>();
			List<String[]> filevalues = new ArrayList<String[]>();
			java.util.Enumeration<String> parameterNames = request.getParameterNames();
			Map <String, String> parameters=  new HashMap <String, String> ();
			String parameterName;	
			String emailvalues;
			String []emailvaluessep;
			List <String> emails= new ArrayList <String>();
			if (st.getStructureType() == Structure.STRUCTURE_TYPE_FORM) {
                emailvalues = st.getFieldVar("formEmail").getValues();
                if(UtilMethods.isSet(emailvalues)){
                	emailvalues = emailvalues.trim().toLowerCase();
                }
				if (UtilMethods.isSet(emailvalues)) {
					if (emailvalues.contains(",")) {
						emailvaluessep = emailvalues.split(",");
						for (String email : emailvaluessep) {
							if(RegEX.contains(email.trim(),"^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*((\\.[A-Za-z]{2,}){1}$)"))
								emails.add(email.trim());
								else throw new Exception("The email list provided by the Form is incorrectly formmated, please enter on the Form properties a valid email addresses separated by a comma.");
						}
					}else
						if(RegEX.contains(emailvalues.trim(),"^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*((\\.[A-Za-z]{2,}){1}$)"))
						emails.add(emailvalues.trim());
						else throw new Exception("The email provided by the Form is incorrect, please enter on the Form properties valid email address. ");
				} 
				
				parameters.put("formTitle", st.getFieldVar("formTitle").getValues());
				parameters.put("formEmail", emailvalues);
				parameters.put("formReturnPage", st.getFieldVar("formReturnPage").getValues());	
				

			}
			
			String titleValue="";
			String urlTitleValue="";
			
			for (; parameterNames.hasMoreElements();) {
				parameterName = parameterNames.nextElement();
				Field  field = st.getFieldVar(parameterName);
				String fieldTypeStr = field!=null?field.getFieldType():"";
				Field.FieldType fieldType =  Field.FieldType.getFieldType(fieldTypeStr);
				String[] fieldValues = request.getParameterValues(parameterName);
				String value = "";
				
				/**
				 * Custom code for Forum functionality
				 * Set URL Title for submitted content
				 */
				if(parameterName.equalsIgnoreCase("title")){
					titleValue = fieldValues[0];
				}
				
				if(parameterName.equalsIgnoreCase("urlTitle")){
					urlTitleValue = titleValue.toLowerCase();
					urlTitleValue = urlTitleValue.replaceAll("[^a-zA-Z0-9]","-"); 
					urlTitleValue= urlTitleValue.replaceAll("[\\s]","-");
				    while(urlTitleValue.indexOf("--") > -1){
				    	urlTitleValue = urlTitleValue.replaceAll("--","-");  
				    }
				    if (urlTitleValue.charAt(urlTitleValue.length()-1)=='-') 
				    {
				    	urlTitleValue = urlTitleValue.substring(0, urlTitleValue.length()-1);
				    }
				    if (urlTitleValue.charAt(0)=='-') 
				    {
				    	urlTitleValue = urlTitleValue.substring(1, urlTitleValue.length());
				    }
				    fieldValues[0]=urlTitleValue;
				}
				
				/**
				 * end of custom code for Forum functionality
				 * Set URL Title for submitted content
				 */
				
				if(fieldValues.length>1){
					for(String val:fieldValues){
						value+=","+val;
					}
					parameters.put(parameterName,value.substring(1));
				}else{
					parameters.put(parameterName,fieldValues[0]);
				}
				
				/* 
				 * Update Graziano 26/02/2013
				 * Aggiunta campo HOST_OR_FOLDER per l'associazione automatica alla stessa cartella di appartenenza della struttura.	
				 * 
				 * N.B. per il publishing remoto TUTTI i contenuti devono avere un'associazione con una folder
				*/
				
				if(fieldType!=null && fieldType.equals(Field.FieldType.HOST_OR_FOLDER)){
					parametersName.add(parameterName);					
					String folder = st.getFolder();
					values.add(new String[]{folder});
				}else{
					
					if(fieldType == null || (fieldType!=null && !fieldType.equals(Field.FieldType.IMAGE) && !fieldType.equals(Field.FieldType.FILE)&& !fieldType.equals(Field.FieldType.BINARY))){
						parametersName.add(parameterName);
						String[] vals = request.getParameterValues(parameterName);
						/**
						 * Custom code for Forum functionality
						 * Add previous  URL Title value for submitted content
						 */
						if(parameterName.equalsIgnoreCase("urlTitle"))
							values.add(fieldValues);
						else
							values.add(vals);
						/**
						 * end of custom code for Forum functionality
						 * Add previous  URL Title value for submitted content
						 */
						if(!parameterName.equals("dispatch") && !parameterName.equals("captcha") && !parameterName.equals("options") && !parameterName.equals("structure") && !parameterName.equals("userId") && !parameterName.equals("referrer")){ 
							if(!SubmitContentUtil.imageOrFileParam(st, parameterName)){
								for(String val : vals){
									paramsBuff.append("&").append(parameterName).append("=").append(encoder.encode(val));
								}						
							}
						}
					}
					else {
						parametersfileName.add(parameterName);
						String[] vals = request.getParameterValues(parameterName);
						filevalues.add(vals);
					
						if(!parameterName.equals("dispatch") && !parameterName.equals("captcha") && !parameterName.equals("options") && !parameterName.equals("structure") && !parameterName.equals("userId") && !parameterName.equals("referrer")){ 
							if(!SubmitContentUtil.imageOrFileParam(st, parameterName)){
								for(String val : vals){
									paramsBuff.append("&").append(parameterName).append("=").append(encoder.encode(val));
								}						
							}
						}
					}
				}
			}
			
			params=paramsBuff.toString();
		
			/*
			 * Checking for captcha
			 */
			if(useCaptcha){

				if(!CaptchaUtil.isValidImageCaptcha(request)){
					errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required", "Validation Image"));
					saveMessages(session, errors);
					if(errors.size() > 0 && UtilMethods.isSet(params)){
						//referrer=referrer+"?"+params.substring(1);
						referrer = request.getRequestURL().toString();
						af = new ActionForward(referrer);
						af.setRedirect(true);
					}
					return af;
				}

			} 

			if(useAudioCaptcha){

				if(!CaptchaUtil.isValidAudioCaptcha(request)){
					errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.contentlet.required","Validation Sound"));
					saveMessages(session, errors);
					if(errors.size() > 0 && UtilMethods.isSet(params)){
						referrer=referrer+"?"+params.substring(1);
						af = new ActionForward(referrer);
						af.setRedirect(true);
					}
					return af;					
				}
			}
			
			/**
			 * Get Categories
			 */
			ArrayList<Category> cats = new ArrayList<Category>();
			String[] arr = request.getParameterValues("categories") == null?new String[0]:request.getParameterValues("categories");
			if (arr != null && arr.length > 0) {
				for (int i = 0; i < arr.length; i++) {
					cats.add(catAPI.find(arr[i], SubmitContentUtil.getUserFromId(userId), true));
				}
			}

			for (int i = 0; i < parametersName.size(); ++i) {
				Field field = st.getFieldVar(parametersName.get(i));
				String[] fieldValue = values.get(i);
				int size = 0;
                if(fieldValue!= null && !fieldValue[0].equals("") ){
                	size=1;
					if (fieldValue[0].contains(",")) {
						String[] fieldValueaux = fieldValue[0].split(",");
						fieldValue = fieldValueaux;
						size = fieldValue.length;
					}
			   }

				for (int j = 0; j < size; j++) {
					if ((field != null)&& (field.getFieldType() != null)&& (field.getFieldType().equals(Field.FieldType.CATEGORY.toString())))
						cats.add(catAPI.find(com.dotmarketing.util.VelocityUtil.cleanVelocity(fieldValue[j].trim()),SubmitContentUtil.getUserFromId(userId), true));
				}
			}

			List<Map<String,Object>> fileParameters = new ArrayList<Map<String,Object>>();
			
			DotContentletValidationException cve = new DotContentletValidationException("Contentlet's fields are not valid");
			boolean hasError = false;

			/**
			 * Get image fields
			 */
			imageFields = StructureFactory.getImagesFieldsList(st, parametersfileName, filevalues);
			if(imageFields.size() > 0){

				for(Field f : imageFields){
					java.io.File uploadedFile = uploadReq.getFile(f.getVelocityVarName());
					String title = uploadReq.getFileName(f.getVelocityVarName());
					if(f.isRequired() && !UtilMethods.isSet(title)){
						cve.addRequiredField(f);
						hasError = true;
						continue;
					}
					if(uploadedFile!=null && uploadedFile.length()> 0){
						String contentType = uploadReq.getContentType(f.getVelocityVarName());
						if(contentType.equals("image/png") || contentType.equals("image/gif") || contentType.equals("image/jpeg")){
							Map<String,Object> temp = new HashMap<String,Object>();
							temp.put("field", f);
							temp.put("title", title);
							temp.put("file", uploadedFile);
							temp.put("host", host);
							fileParameters.add(temp);	
						}else{
							cve.addBadTypeField(f);
							hasError = true;
							continue;
						}			
					}	
				}
			}

			/**
			 * Get file fields
			 */
			//http://jira.dotmarketing.net/browse/DOTCMS-3463
			Map <String, Object> tempBinaryValues= new HashMap <String, Object>();
			fileFields = StructureFactory.getFilesFieldsList(st, parametersfileName, filevalues);
			List <Field> catfields = FieldsCache.getFieldsByStructureInode(st.getInode());
			for(Field field:catfields){
				Map <String, Object> binaryvalues= new HashMap <String, Object>();
				if (field.getFieldType().equals("binary"))
				{
					tempBinaryValues=processBinaryTempFileUpload( field.getVelocityVarName(), request) ;
					binaryvalues.put("field", field);
					parametersName.add(tempBinaryValues.get("parameterName").toString());
					Object ob = tempBinaryValues.get("parameterValues");
					if(ob != null){
						File f = (File)ob;
						binaryvalues.put(field.getVelocityVarName(), f);
						values.add(new String []{f.getAbsolutePath()});
					}else{
					  binaryvalues.put(field.getVelocityVarName(), null);
					  values.add(new String []{""});
					}
					fileParameters.add(binaryvalues);
				}
				
			}
			if(fileFields.size() > 0){

				for(Field f : fileFields){
					java.io.File uploadedFile = uploadReq.getFile(f.getVelocityVarName());
					String title = uploadReq.getFileName(f.getVelocityVarName());
					if(f.isRequired() && !UtilMethods.isSet(title)){
						cve.addRequiredField(f);
						hasError = true;
						continue;
					}
					Map<String,Object> temp = new HashMap<String,Object>();
					temp.put("field", f);
					temp.put("title", title);
					temp.put("file", uploadedFile);
					temp.put("host", host);
					fileParameters.add(temp);					
				}
			}
			
			if(hasError){
				throw cve;
			}

			/**
			 * Save content
			 */
			Contentlet contentlet = SubmitContentUtil.createContent(st, cats, userId, parametersName, values, options, fileParameters, autoPublish, host);
			
			
			/**
			 * If the moderator field is set, a work flow task is created
			 */
			if(UtilMethods.isSet(moderatorRole)){
				SubmitContentUtil.createWorkFlowTask(contentlet, userId, moderatorRole);
			}

			message.add(Globals.MESSAGE_KEY, new ActionMessage("message.contentlet.save"));
			session.setAttribute(Globals.MESSAGE_KEY, message);

			HibernateUtil.commitTransaction();
			if(!APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode())){
				Logger.error(this, "Unable to index contentlet");
			}
			
			/**
			 * Custom Code for Forum Functionality
			 * Clean orphaned subscriptions per structure (on a separate thread)
			 * Send emails to subscribers (on a separate thread)
			 */
			
			CleanSubscriptionsThread subscriptionsThread= new CleanSubscriptionsThread(contentlet.getStructure(), contentlet);
			subscriptionsThread.start();

			EmailSubscribersThread emailThread = new EmailSubscribersThread(contentlet);
			emailThread.start(); 
			
			String contentStructureForForwardURL = "";
		
			List<Relationship> rels = RelationshipFactory.getRelationshipsByChild(contentlet.getStructure());
		
			List<Contentlet> parentContents = conAPI.getRelatedContent(contentlet, rels.get(0), userAPI.getSystemUser(), true);
			
			if(contentlet.getStructure().getVelocityVarName().toLowerCase().contains("thread")){
				contentStructureForForwardURL = "/" + pluginAPI.loadProperty("org.dotcms.forum.plugin", "submit.content.forum.folder.name").toLowerCase() +
				"/" + pluginAPI.loadProperty("org.dotcms.forum.plugin", "submit.content.topic.folder.name").toLowerCase() +
				"/" + parentContents.get(0).getMap().get(pluginAPI.loadProperty("org.dotcms.forum.plugin", "url.title.field.velocity.var.name"));
			}
			else {
				contentStructureForForwardURL = "/" + pluginAPI.loadProperty("org.dotcms.forum.plugin", "submit.content.forum.folder.name").toLowerCase() +
				"/" + pluginAPI.loadProperty("org.dotcms.forum.plugin", "submit.content.thread.folder.name").toLowerCase() +
				"/" + parentContents.get(0).getMap().get(pluginAPI.loadProperty("org.dotcms.forum.plugin", "url.title.field.velocity.var.name"));
			}
			

			session.setAttribute("forwardPageAfterSubmit",contentStructureForForwardURL);
			
			
			/**
			 * End Custom Code for Forum Functionality
			 */
			
			
			if (st.getStructureType()== Structure.STRUCTURE_TYPE_FORM ){
				if(st.getFieldVar("formReturnPage")!= null && UtilMethods.isSet(st.getFieldVar("formReturnPage").getValues())){
					af.setPath(st.getFieldVar("formReturnPage").getValues());
				}
				if(UtilMethods.isSet(request.getParameter("formReturnPageOverride"))){
					af.setPath(request.getParameter("formReturnPageOverride"));
				}
				parameters.put("returnUrl", af.getPath());
				parameters.put("IP:", request.getRemoteAddr());
				
				String reverseName = null;
				try{
					reverseName = DNSUtil.reverseDns(request.getRemoteAddr());
					if(!reverseName.equals(request.getRemoteAddr())){
						parameters.put("reverseName", reverseName);
					}
				}
				catch(Throwable t){

				}
				
				
				
				
				try{
					String referer = request.getHeader("Referer");
					if(referer.indexOf("?") > -1){
						referer = referer.substring(0,referer.indexOf("?"));
					}
					parameters.put("referrer", referer);
				}
				catch(Exception e){
					
				}
				

				

				Clickstream clickstream = (Clickstream) request.getSession().getAttribute("clickstream");
				if(clickstream != null &&  UtilMethods.isSet(clickstream.getInitialReferrer())){
					parameters.put("Initial Referer", clickstream.getInitialReferrer());
				}
				
				parameters.remove("options");
				parameters.remove("structure");
				parameters.remove("dispatch");
				parameters.remove("formEmail");
				parameters.remove("formTitle");
				parameters.remove("formReturnPage");
				EmailUtils.SendContentSubmitEmail(parameters, st,emails);
			}

		}catch (DotContentletValidationException ve) {
			HibernateUtil.rollbackTransaction();
			Logger.debug(this, ve.getMessage());

			if(ve.hasRequiredErrors()){ 
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED);
				for (Field errorField : reqs) {
					errors.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.required", errorField.getFieldName()));
				}
			}
			if(ve.hasLengthErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_MAXLENGTH);
				for (Field errorField : reqs) {
					errors.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.maxlength", errorField.getFieldName(),"255"));
				}
			}
			if(ve.hasPatternErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_PATTERN);
				for (Field errorField : reqs) {
					errors.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.format", errorField.getFieldName()));
				}
			}
			if(ve.hasRelationshipErrors()){
				//need to update message to support multiple relationship validation errors
				errors.add(Globals.ERROR_KEY, new ActionMessage("message.relationship.required", "relationships"));
			}

			if(ve.hasRelationshipErrors()){
				//need to update message to support multiple relationship validation errors
				errors.add(Globals.ERROR_KEY, new ActionMessage("message.relationship.required", "relationships"));
			}

			if(ve.hasUniqueErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_UNIQUE);
				for (Field errorField : reqs) {
					errors.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.unique", errorField.getFieldName()));
				}
			}
			if(ve.hasBadTypeErrors()){
				List<Field> reqs = ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_BADTYPE);
				for (Field errorField : reqs) {
					errors.add(Globals.ERROR_KEY, new ActionMessage("message.contentlet.invalid.image", errorField.getFieldName()));
				}
			}
			//errors.add(Globals.ERROR_KEY, new ActionMessage("error.invalid.field",e.getMessage()));
			session.setAttribute(Globals.ERROR_KEY, errors);
		}catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Logger.debug(this, e.getMessage());
			errors.add(Globals.ERROR_KEY, new ActionMessage("error.generic.message",e.getMessage()));
			session.setAttribute(Globals.ERROR_KEY, errors);
		}

		if(errors.size() > 0 && UtilMethods.isSet(params)){
			referrer=referrer+"?"+params.substring(1);
			af = new ActionForward(referrer);
			af.setRedirect(true);
		}

		return af;

	}

	public Map<String, Object> processBinaryTempFileUpload(String binaryFieldName,
			HttpServletRequest request) throws PortalException,
			SystemException, Exception {

		UploadServletRequest uploadRequest = (UploadServletRequest) request;
		File f = uploadRequest.getFile(binaryFieldName);
		Map<String, Object> values = new HashMap<String, Object>();
		boolean isEmptyFile = false;
		String fileName;
		String userId = request.getParameter("userId");
		User user = null;
		if (userId != null && !userId.equals("")) {
			user = UserLocalManagerUtil.getUserById(userId);
		} else {
			user = APILocator.getUserAPI().getAnonymousUser();
			userId = APILocator.getUserAPI().getAnonymousUser().getUserId();
		}

		if (f != null) {

			if (f.length() == 0)
				isEmptyFile = true;
			if (!isEmptyFile) {
				fileName = uploadRequest.getFileName(binaryFieldName);

				File tempUserFolder = new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary()
								+ java.io.File.separator + user.getUserId());
				if (!tempUserFolder.exists())
					tempUserFolder.mkdirs();

				File dest = new File(tempUserFolder.getAbsolutePath()
						+ File.separator + fileName);
				if (dest.exists())
					dest.delete();
				f.renameTo(dest);
               //http://jira.dotmarketing.net/browse/DOTCMS-3463
				/*SubmitContentUtil.saveTempFile(user, host, f, tempUserFolder
						.getAbsolutePath(), binaryFieldName);*/
				//String path = dest.getAbsolutePath();
				values.put("parameterName", binaryFieldName);
				values.put("parameterValues", dest);

			}

			else {
				values.put("parameterName", binaryFieldName);
				values.put("parameterValues", null);
			}

		}

		return values;
	}

}



