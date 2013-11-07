package com.eng.dotcms.polls.ajax;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.util.TrustFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.eng.dotcms.polls.velocity.PollVote;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

import static com.eng.dotcms.polls.util.PollsConstants.*;

/**
 * Ajax class used into backend portlet for add and get polls
 * 
 * This file is part of Poll Management for dotCMS.
 * Poll Management for dotCMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Poll Management for dotCMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Poll Management for dotCMS.  If not, see <http://www.gnu.org/licenses/> 
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Mar 7, 2013 - 4:19:22 PM
 */
public class PollsAjaxAction extends AjaxAction {
	
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
	
	@SuppressWarnings({ "rawtypes"})
    public void getPolls(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String,String> pmap=getURIParams();
        int offset=Integer.parseInt(pmap.get("offset"));
        int pageSize=Integer.parseInt(pmap.get("pageSize"));
        String language = pmap.get("el");
        
        Map<String,Object> result=new HashMap<String,Object>();
        List<Map> list=new ArrayList<Map>();
        SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd hh:mm");
        try {
        	if(null==language)
        		language = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
        	Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
        	StringBuffer luceneQuery = new StringBuffer();
        	luceneQuery.append("+structureName:");
        	luceneQuery.append(StructureCache.getStructureByVelocityVarName(POLL_STRUCTURE_NAME).getName());
        	luceneQuery.append(" +conhost:");
        	luceneQuery.append(host.getIdentifier());
        	if(!"0".equals(language)){
	        	luceneQuery.append(" +languageId:");
	        	luceneQuery.append(language);
        	}
        	luceneQuery.append(" +live:true");
        	List<Contentlet> polls = APILocator.getContentletAPI().search(luceneQuery.toString(), pageSize, offset, null, WebAPILocator.getUserWebAPI().getLoggedInUser(request), true);
            for(Contentlet poll : polls) {
            	Language lang = APILocator.getLanguageAPI().getLanguage(poll.getLanguageId());
                User modUser=APILocator.getUserAPI().loadUserById(poll.getModUser());                
                Map<String,String> mm=new HashMap<String,String>();
                mm.put("inode", poll.getInode());
                mm.put("identifier", poll.getIdentifier());
                mm.put("title", poll.getTitle());
                mm.put("languageId", String.valueOf(poll.getLanguageId()));
                mm.put("question", (String)poll.getMap().get("question"));
                if(Boolean.parseBoolean((String)poll.getMap().get("expired")))
                	mm.put("flag", "/html/images/languages/"+lang.getLanguageCode()+"_"+lang.getCountryCode()+"_gray.gif");
                else
                	mm.put("flag", "/html/images/languages/"+lang.getLanguageCode()+"_"+lang.getCountryCode()+".gif");
                mm.put("date", df.format((Date)poll.getMap().get("expiration_date")));
                mm.put("expired", (String)poll.getMap().get("expired"));
                mm.put("user", modUser.getFullName()+"<"+modUser.getEmailAddress()+">");
                list.add(mm);
            }
                            
            result.put("list", list);
            result.put("total", list.size());
            
            response.setContentType("application/json");
            new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValue(response.getOutputStream(), result);
            
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

	public void addPoll(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String,String> pmap=getURIParams();
		try {        	
			String language = pmap.get("el");
			if(null==language)
				language = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);			
			User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T'HH:mm:ss");
			String expireDate = request.getParameter("pollExpireDate");
			String expireTime = request.getParameter("pollExpireTime");
			Date expirationDate =  sdf.parse(expireDate + " " + expireTime);
			GregorianCalendar gc = new GregorianCalendar();
			if(expirationDate.after(gc.getTime())){
				Structure pollStructure = StructureCache.getStructureByVelocityVarName(POLL_STRUCTURE_NAME);
				Contentlet poll = new Contentlet();
				List<Category> categories = APILocator.getCategoryAPI().findTopLevelCategories( APILocator.getUserAPI().getSystemUser(), false );
				List<Permission> structurePermissions = APILocator.getPermissionAPI().getPermissions(StructureCache.getStructureByVelocityVarName(CHOICE_STRUCTURE_NAME));				
				poll.setStructureInode(pollStructure.getInode());
				poll.setStringProperty("title", request.getParameter("pollTitle"));
				poll.setStringProperty("question", request.getParameter("pollQuestion"));
				poll.setDateProperty("expiration_date",expirationDate);
				poll.setStringProperty("expired", "false");
				poll.setHost(WebAPILocator.getHostWebAPI().getCurrentHost(request).getIdentifier());
				poll.setLanguageId(Long.parseLong(language));
				poll.setModUser(user.getUserId());
				poll.setModDate(new GregorianCalendar().getTime());				
				if(UtilMethods.isSet(pollStructure.getFolder())){	
					poll.setFolder(pollStructure.getFolder());
					poll.setStringProperty("pollpath", pollStructure.getFolder());
				}
				
				//add choices
				String _choices = request.getParameter("pollChoice");
				String[] choices = _choices.split("[|]");
				List<Contentlet> contentRelationships = new ArrayList<Contentlet>();
				List<Contentlet> contentSavedRelationships = new ArrayList<Contentlet>();
				Structure choiseStructure = StructureCache.getStructureByVelocityVarName(CHOICE_STRUCTURE_NAME);
				for(String c:choices){
					Contentlet choice = new Contentlet();
					choice.setStructureInode(choiseStructure.getInode());
					choice.setStringProperty("id", UUID.randomUUID().toString());
					choice.setStringProperty("text", c);
					choice.setHost(WebAPILocator.getHostWebAPI().getCurrentHost(request).getIdentifier());
					choice.setLanguageId(Long.parseLong(language));
					choice.setModUser(user.getUserId());
					choice.setModDate(new GregorianCalendar().getTime());	
					if(UtilMethods.isSet(choiseStructure.getFolder())){
						choice.setFolder(choiseStructure.getFolder());
						choice.setStringProperty("choicepath", choiseStructure.getFolder());
					}
					// add relationship
					contentRelationships.add(choice);
				}
				
				// save all choice
				
				for(Contentlet c : contentRelationships){
					APILocator.getContentletAPI().validateContentlet( c, categories );
				}
				
				for(Contentlet c : contentRelationships){
					c = APILocator.getContentletAPI().checkin(c, categories, structurePermissions, user, false);
					APILocator.getContentletAPI().publish(c, user, false);
					contentSavedRelationships.add(c);
				}
				
				// save poll
				structurePermissions = APILocator.getPermissionAPI().getPermissions(StructureCache.getStructureByVelocityVarName(POLL_STRUCTURE_NAME));
				
				APILocator.getContentletAPI().validateContentlet( poll, categories );
				poll = APILocator.getContentletAPI().checkin( poll, categories, structurePermissions, user, false );
				APILocator.getContentletAPI().publish(poll, user, false);
				// relate the contents
				APILocator.getContentletAPI().relateContent( poll, RelationshipFactory.getRelationshipByRelationTypeValue(RELATIONSHIP_NAME), contentSavedRelationships, user, false );
			}else
				throw new Exception("The expiration date must be after the actual time.");
		} catch (Exception e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		}
		
    }
	
	public void updateVotes(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			Map<String,String> pmap=getURIParams();
			String language = pmap.get("el");
			String identifier = pmap.get("identifier");
			if(null==language)
				language = (String) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
			Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
			User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
			List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(),true);

			Set<Environment> environments = new HashSet<Environment>();
			for(Role r: roles)
			     environments.addAll(APILocator.getEnvironmentAPI().findEnvironmentsByRole(r.getId()));

			
			TrustFactory tFactory = new TrustFactory();		
			ClientConfig cc = new DefaultClientConfig();
			
			if(Config.getStringProperty("TRUSTSTORE_PATH") != null && !Config.getStringProperty("TRUSTSTORE_PATH").trim().equals(""))
				cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, 
						new HTTPSProperties(tFactory.getHostnameVerifier(), tFactory.getSSLContext()));
			
			Client client = Client.create(cc);
			for (Environment environment : environments){
				PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI().findSendingEndPointsByEnvironment(environment.getId()).get(0);
				WebResource resource = client.resource(endpoint.toURL()+"/api/polls/getVotes/"+identifier+"/"+language+"/"+user.getUserId()+"/"+host.getIdentifier());
        		ClientResponse clResponse = resource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        		String _votes = clResponse.getEntity(String.class);
        		if(!"FAILURE".equals(_votes)){
	        		Gson gson = new Gson();        		
					Map<String, PollVote> votes = gson.fromJson(_votes, new TypeToken<HashMap<String, PollVote>>(){}.getType());
					if(votes.size()>0){
						for(String key : votes.keySet()){
							PollVote vote = votes.get(key);
							Contentlet _vote = new Contentlet();
							_vote.setStringProperty("title", vote.getTitle());
							_vote.setStringProperty("choice", vote.getChoice());
							_vote.setStringProperty("poll", vote.getPoll());
							_vote.setStringProperty("user", vote.getUser());
							_vote.setBoolProperty("sent_to_sender", true);
							_vote.setStructureInode(StructureCache.getStructureByVelocityVarName(VOTE_STRUCTURE_NAME).getInode());
							_vote.setModDate(vote.getModDate());
							_vote.setModUser(vote.getModUser());
							_vote = APILocator.getContentletAPI().checkin(_vote, user, false);
							APILocator.getContentletAPI().publish(_vote, user, false);
						}
					}else
						response.getWriter().println("NORES");
        		}else
        			throw new DotRuntimeException("Error on call the getVotes from remote server.");
			}
				
		} catch (DotRuntimeException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (PortalException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (SystemException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (DotDataException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		} catch (DotSecurityException e) {
			response.getWriter().println("FAILURE: " + e.getMessage());
		}
		
		
	}
	
	@Override
	public void action(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub

	}

}
