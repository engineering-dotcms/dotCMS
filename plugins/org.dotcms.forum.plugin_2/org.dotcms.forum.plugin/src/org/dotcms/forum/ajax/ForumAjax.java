package org.dotcms.forum.ajax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.directwebremoting.WebContextFactory;
import org.dotcms.forum.business.ManageForumLock;
import org.dotcms.forum.util.ForumUtils;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class ForumAjax {
	
	private UserAPI userAPI = APILocator.getUserAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private LanguageAPI langAPI = APILocator.getLanguageAPI();	
	
	public Map<String, Object> subscribeToForumContent(String userId, String contentIdentifier) {
    	List<String> subscribeError = new ArrayList<String>();
    	Map<String,Object> callbackData = new HashMap<String,Object>();
    	
		try{
			User subscribingUser = userAPI.loadUserById(userId, userAPI.getSystemUser(), true);
			Logger.debug(this, "Hitting the subscribing option with user: " + subscribingUser.getFullName());
			Contentlet content =  conAPI.findContentletByIdentifier(contentIdentifier, true, langAPI.getDefaultLanguage().getId(), userAPI.getSystemUser(), true);
			if(!UtilMethods.isSet(content.getInode())){
				subscribeError.add("There was an error. Please try again");
			}
			else if(UtilMethods.isSet(subscribingUser) && UtilMethods.isSet(contentIdentifier)){
				List<Contentlet> cons = ForumUtils.getUserSubscriptionsPerStructure(subscribingUser.getUserId(), content.getIdentifier());
				if(cons.size () == 0){
					ForumUtils.createSubscription(content,subscribingUser);
				} else{
					subscribeError.add("User is already subscribed to this content.");
				}
			}
		}
		catch(Exception e) {
			subscribeError.add("There was an error. User can't subscribe to this content. Please try again.");
		}
		finally{
			if(subscribeError.size()>0)
				callbackData.put("subscribeError", subscribeError);
		}
		return callbackData;

	}
	
	public Map<String, Object> unsubscribeToForumContent(String userId, String contentIdentifier) {
		List<String> subscribeError = new ArrayList<String>();
    	Map<String,Object> callbackData = new HashMap<String,Object>();
    	
		try{
			User unsubscribingUser = userAPI.loadUserById(userId, userAPI.getSystemUser(), true);
			Logger.debug(this, "Hitting the unsubscribing option with user: " + unsubscribingUser.getFullName());
			Contentlet content =  conAPI.findContentletByIdentifier(contentIdentifier, true, langAPI.getDefaultLanguage().getId(), userAPI.getSystemUser(), true);
			if(!UtilMethods.isSet(content.getInode())){
				subscribeError.add("There was an error. Please try again");
			}
			else if(UtilMethods.isSet(unsubscribingUser) && UtilMethods.isSet(content)){
				List<Contentlet> cons = ForumUtils.getUserSubscriptionsPerStructure(unsubscribingUser.getUserId(), content.getIdentifier());
				if(cons.size()>0){
					ForumUtils.deleteSubscription(cons.get(0));
				}
			}
		}
		catch(Exception e) {
			subscribeError.add("Unable to unsubscribe. Try again later");
			Logger.error(this, "Unable to unsubscribe. Try again later");
		}
		finally{
			if(subscribeError.size()>0)
				callbackData.put("subscribeError", subscribeError);
		}
		return callbackData;

	}
	
	public boolean isUserSubscribed (String userId, String contentIdentifier){
		try {
			List<Contentlet> subscriptions = new ArrayList<Contentlet>();
			if(UtilMethods.isSet(userId) && UtilMethods.isSet(contentIdentifier))
				subscriptions = ForumUtils.getUserSubscriptionsPerStructure (userId, contentIdentifier);
				if (subscriptions.size() > 0)
					return true;

		} catch (Exception e) {
			
			e.printStackTrace();
		} 
		return false;
	}
	
	// ######################################################################################################################################
	// BEGIN: METODI PER L'AMMINISTRAZIONE DA FRONTEND
	// ######################################################################################################################################
	
	public String closeThread(String threadId, boolean live, String languageId) {
		try {
			if(!ManageForumLock.INSTANCE.isLocked()){
				ManageForumLock.INSTANCE.setLocked(true);
				HttpSession session = WebContextFactory.get().getSession();
				User loggedIn = (User)session.getAttribute(WebKeys.CMS_USER);
				Contentlet thread = conAPI.findContentletByIdentifier(threadId, live, Long.parseLong(languageId), userAPI.getSystemUser(), true);
				thread.setStringProperty("threadStatus", "no");
				thread.setInode("");
				thread = conAPI.checkin(thread, loggedIn, true);
				if(live)
					conAPI.publish(thread, loggedIn, true);
				ManageForumLock.INSTANCE.setLocked(false);
				return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-close-thread-ok");
			}else
				return "LOCKED";
		} catch (DotContentletStateException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-close-thread-ko")+" ERROR: "+e.getMessage();
		} catch (NumberFormatException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-close-thread-ko")+" ERROR: "+e.getMessage();
		} catch (DotDataException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-close-thread-ko")+" ERROR: "+e.getMessage();
		} catch (DotSecurityException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-close-thread-ko")+" ERROR: "+e.getMessage();
		} catch (Exception e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-close-thread-ko")+" ERROR: "+e.getMessage();			
		}
	}
	
	
	public String openThread(String threadId, boolean live, String languageId) {
		try {
			if(!ManageForumLock.INSTANCE.isLocked()){
				ManageForumLock.INSTANCE.setLocked(true);
				HttpSession session = WebContextFactory.get().getSession();
				User loggedIn = (User)session.getAttribute(WebKeys.CMS_USER);
				Contentlet thread = conAPI.findContentletByIdentifier(threadId, live, Long.parseLong(languageId), userAPI.getSystemUser(), true);
				thread.setStringProperty("threadStatus", "yes");
				thread.setInode("");
				thread = conAPI.checkin(thread, loggedIn, true);
				if(live)
					conAPI.publish(thread, loggedIn, true);
				ManageForumLock.INSTANCE.setLocked(false);
				return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-open-thread-ok");
			}else
				return "LOCKED";				
		} catch (DotContentletStateException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-open-thread-ko")+" ERROR: "+e.getMessage();
		} catch (NumberFormatException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-open-thread-ko")+" ERROR: "+e.getMessage();
		} catch (DotDataException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-open-thread-ko")+" ERROR: "+e.getMessage();
		} catch (DotSecurityException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-open-thread-ko")+" ERROR: "+e.getMessage();
		} catch (Exception e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-open-thread-ok")+" ERROR: "+e.getMessage();			
		}
	}
	
	public String deleteThread(String threadId, boolean live, String languageId) {
		try {
			if(!ManageForumLock.INSTANCE.isLocked()){
				ManageForumLock.INSTANCE.setLocked(true);
				HttpSession session = WebContextFactory.get().getSession();
				User loggedIn = (User)session.getAttribute(WebKeys.CMS_USER);
				Contentlet thread = conAPI.findContentletByIdentifier(threadId, live, Long.parseLong(languageId), userAPI.getSystemUser(), true);
				conAPI.deleteRelatedContent(thread, RelationshipFactory.getRelationshipByRelationTypeValue("Parent_Thread-Child_Reply"), loggedIn, true);
				conAPI.delete(thread, loggedIn, true);
				ManageForumLock.INSTANCE.setLocked(false);
				return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-thread-ok");
			}else
				return "LOCKED";
		} catch (DotContentletStateException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-thread-ko")+" ERROR: "+e.getMessage();
		} catch (NumberFormatException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-thread-ko")+" ERROR: "+e.getMessage();
		} catch (DotDataException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-thread-ko")+" ERROR: "+e.getMessage();
		} catch (DotSecurityException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-thread-ko")+" ERROR: "+e.getMessage();
		} catch (Exception e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-thread-ko")+" ERROR: "+e.getMessage();			
		}
	}
	
	public String publishThread(String threadId, boolean live, String languageId) {
		try {
			if(!ManageForumLock.INSTANCE.isLocked()){
				ManageForumLock.INSTANCE.setLocked(true);
				HttpSession session = WebContextFactory.get().getSession();
				User loggedIn = (User)session.getAttribute(WebKeys.CMS_USER);
				Contentlet thread = conAPI.findContentletByIdentifier(threadId, live, Long.parseLong(languageId), userAPI.getSystemUser(), true);
				conAPI.publish(thread, loggedIn, true);
				ManageForumLock.INSTANCE.setLocked(false);
				return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-thread-ok");
			}else
				return "LOCKED";
		} catch (DotContentletStateException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-thread-ko")+" ERROR: "+e.getMessage();
		} catch (NumberFormatException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-thread-ko")+" ERROR: "+e.getMessage();
		} catch (DotDataException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-thread-ko")+" ERROR: "+e.getMessage();
		} catch (DotSecurityException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-thread-ko")+" ERROR: "+e.getMessage();
		} catch (Exception e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-thread-ko")+" ERROR: "+e.getMessage();			
		}
	}
	
	
	public String deleteReply(String replyId, boolean live, String languageId) {
		try {
			if(!ManageForumLock.INSTANCE.isLocked()){
				ManageForumLock.INSTANCE.setLocked(true);
				HttpSession session = WebContextFactory.get().getSession();
				User loggedIn = (User)session.getAttribute(WebKeys.CMS_USER);
				Contentlet reply = conAPI.findContentletByIdentifier(replyId, live, Long.parseLong(languageId), userAPI.getSystemUser(), true);
				conAPI.delete(reply, loggedIn, true);
				ManageForumLock.INSTANCE.setLocked(false);
				return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-reply-ok");
			}else
				return "LOCKED";
		} catch (DotContentletStateException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-reply-ko")+" ERROR: "+e.getMessage();
		} catch (NumberFormatException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-reply-ko")+" ERROR: "+e.getMessage();
		} catch (DotDataException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-reply-ko")+" ERROR: "+e.getMessage();
		} catch (DotSecurityException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-reply-ko")+" ERROR: "+e.getMessage();
		} catch (Exception e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-delete-reply-ko")+" ERROR: "+e.getMessage();			
		}
	}
	
	public String publishReply(String replyId, boolean live, String languageId) {
		try {
			if(!ManageForumLock.INSTANCE.isLocked()){
				ManageForumLock.INSTANCE.setLocked(true);
				HttpSession session = WebContextFactory.get().getSession();
				User loggedIn = (User)session.getAttribute(WebKeys.CMS_USER);
				Contentlet reply = conAPI.findContentletByIdentifier(replyId, live, Long.parseLong(languageId), userAPI.getSystemUser(), true);
				conAPI.publish(reply, loggedIn, true);
				ManageForumLock.INSTANCE.setLocked(false);
				return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-reply-ok");
			}else
				return "LOCKED";
		} catch (DotContentletStateException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-reply-ko")+" ERROR: "+e.getMessage();
		} catch (NumberFormatException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-reply-ko")+" ERROR: "+e.getMessage();
		} catch (DotDataException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-reply-ko")+" ERROR: "+e.getMessage();
		} catch (DotSecurityException e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-reply-ko")+" ERROR: "+e.getMessage();
		} catch (Exception e) {
			ManageForumLock.INSTANCE.setLocked(false);
			Logger.error(this, e.getMessage(), e);
			return langAPI.getStringKey(langAPI.getLanguage(languageId), "msg-publish-reply-ko")+" ERROR: "+e.getMessage();			
		}
	}	
	
	// ######################################################################################################################################
	// END: METODI PER L'AMMINISTRAZIONE DA FRONTEND
	// ######################################################################################################################################

	



	
	
	

	
}