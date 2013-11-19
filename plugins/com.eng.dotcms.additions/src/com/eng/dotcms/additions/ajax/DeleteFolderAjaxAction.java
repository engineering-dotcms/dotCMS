package com.eng.dotcms.additions.ajax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class DeleteFolderAjaxAction extends AjaxAction {
	
	@SuppressWarnings("rawtypes")
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String cmd = getURIParams().get("cmd");
        java.lang.reflect.Method meth = null;
        Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
        Object arglist[] = new Object[] { request, response };
        try {
            if (getUser() == null ) {
                response.sendError(401);
                return;
            }
            meth = this.getClass().getMethod(cmd, partypes);
            meth.invoke(this, arglist);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Trying to run method:" + cmd);
            Logger.error(this.getClass(), e.getMessage(), e.getCause());
            throw new RuntimeException(e.getMessage(),e);
        }
    }
	
    public void deleteFolder(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    	
		Map<String,String> pmap=getURIParams();
		String folderInode = pmap.get("folderInode");
		Logger.info(getClass(), "Inizio processo di cancellazione remota della cartella " + folderInode);
		List<String> folders = new ArrayList<String>();
		folders.add(folderInode);
		GregorianCalendar now = new GregorianCalendar();
		boolean done = false;
		boolean error = false;
		try{
			Logger.info(getClass(), "Preparo il bundle da inviare in cancellazione...");
			User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
			// Preparo il bundle in cancellazione
			List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(),true);
	
			Set<Environment> environments = new HashSet<Environment>();
			for(Role r: roles)
				environments.addAll(APILocator.getEnvironmentAPI().findEnvironmentsByRole(r.getId()));
	
			List<Environment> envsToSendTo = new ArrayList<Environment>();
	
			for (Environment environment : environments)
				envsToSendTo.add(environment);
	
			Bundle bundle = new Bundle(null, now.getTime(), null, user.getUserId());			
			APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);
			            
			PublisherAPI.getInstance().addContentsToUnpublish(folders, bundle.getId(), now.getTime(), user);
			
			Logger.info(getClass(), "...bundle inserito correttamente. BundleID: " + bundle.getId());
			// creo un meccanismo per controllare quando il bundle è stato pubblicato sul delivery
			
			while(true){
				Logger.info(getClass(), "Controllo l'esito della cancellazione remota: " + bundle.getId());
				PublishAuditStatus status = PublishAuditAPI.getInstance().getPublishAuditStatus(bundle.getId());
				// se il bundle non si trova in uno stato intermedio
				if(null!=status && null!=status.getStatus()){
					if(status.getStatus().getCode()!=PublishAuditStatus.Status.BUNDLING.getCode()
							&& status.getStatus().getCode()!=PublishAuditStatus.Status.BUNDLE_REQUESTED.getCode()
							&& status.getStatus().getCode()!=PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY.getCode()
							&& status.getStatus().getCode()!=PublishAuditStatus.Status.PUBLISHING_BUNDLE.getCode()
							&& status.getStatus().getCode()!=PublishAuditStatus.Status.RECEIVED_BUNDLE.getCode()
							&& status.getStatus().getCode()!=PublishAuditStatus.Status.WAITING_FOR_PUBLISHING.getCode()
							&& status.getStatus().getCode()!=PublishAuditStatus.Status.SENDING_TO_ENDPOINTS.getCode()){
						Logger.info(getClass(), "Il bundle non si trova più in uno stato intermedio...");
						// controllo se è andata a buon fine la cancellazione
						if(status.getStatus().getCode()==PublishAuditStatus.Status.SUCCESS.getCode()){
							Logger.info(getClass(), "La cartella è stata correttamente cancellata sul receiver.");
							done = true;
							break;
						}else{
							Logger.info(getClass(), "Errore durante la cancellazione della cartella sul receiver.");
							error = true;
							break;
						}
					}else
						Thread.sleep(60000);	
				}else
					Thread.sleep(60000);				
			}
			
			// controllo quale è stata la condizione di uscita...
			if(error)
				response.getWriter().println("FAILURE: La cancellazione della cartella selezionata non è andata a buon fine.");
			else {
				// procedo alla cancellazione locale della cartella...
				Logger.info(getClass(), "Cancellazione della cartella sul sender in corso...");
				Folder deletedFolder = APILocator.getFolderAPI().find(folderInode, user, false); 
				APILocator.getFolderAPI().delete(deletedFolder, user, false);
				Logger.info(getClass(), "Cancellazione della cartella sul sender terminata con successo!");
			}
		} catch (DotRuntimeException e) {			
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (PortalException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (SystemException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (DotDataException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (DotPublisherException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (DotSecurityException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (InterruptedException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		}
		
	}
	
	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub

	}

}
