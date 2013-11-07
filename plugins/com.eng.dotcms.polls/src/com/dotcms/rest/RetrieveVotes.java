package com.dotcms.rest;

import static com.eng.dotcms.polls.util.PollsConstants.POLL_STRUCTURE_NAME;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.polls.PollsManAPI;
import com.eng.dotcms.polls.PollsManFactory;
import com.eng.dotcms.polls.velocity.PollVote;
import com.google.gson.Gson;
import com.liferay.portal.model.User;

/**
 * 
 * REST service for retrieve the votes from receiver
 * 
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
 * Jul 12, 2013 - 10:00:49 AM
 */
@Path("/polls")
public class RetrieveVotes extends WebResource {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private UserAPI userAPI = APILocator.getUserAPI();
	private PollsManAPI pollsAPI = PollsManFactory.getPollsManAPI();
	
	@GET
	@Path("/getVotes/{identifier}/{language}/{userId}/{hostId}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getVotes(@PathParam("identifier") String identifier, @PathParam("language") long languageId, 
			@PathParam("userId") String userId, @PathParam("hostId") String hostId) {
		try {
			Map<String, PollVote> votes = new HashMap<String, PollVote>();
			Gson gson = new Gson();
			User user = userAPI.loadUserById(userId);
			StringBuffer sb = new StringBuffer();
			if("all".equals(identifier)) {
				String pollStr = StructureCache.getStructureByVelocityVarName(POLL_STRUCTURE_NAME).getName();
				sb.append("+structureName:");
				sb.append(pollStr);
				sb.append(" +conhost:");
				sb.append(hostId);
				sb.append(" +live:true");
			}else{
				sb.append("+identifier: ");
				sb.append(identifier);
				sb.append(" +languageId: ");
				sb.append(languageId);
			}
			List<Contentlet> polls = conAPI.search(sb.toString(), 0, -1, null, user, false);
			if(null!=polls){
				for(Contentlet poll:polls){
					List<Contentlet> choices = pollsAPI.getChoiceByPoll(poll.getIdentifier(), languageId);				
					for(Contentlet choice:choices){
						List<Contentlet> _votes = pollsAPI.getVotesFromChoice(choice.getIdentifier(), poll.getIdentifier(), user);
						for(Contentlet _vote:_votes){
							PollVote vote = new PollVote();
							vote.setIdentifier(_vote.getIdentifier());
							vote.setInode("");
							vote.setChoice(_vote.getStringProperty("choice"));
							vote.setPoll(_vote.getStringProperty("poll"));
							vote.setTitle(_vote.getTitle());
							vote.setUser(_vote.getStringProperty("user"));
							vote.setModDate(_vote.getModDate());
							vote.setModUser(_vote.getModUser());
							votes.put(choice.getIdentifier()+"_"+new GregorianCalendar().getTimeInMillis(), vote);
							pollsAPI.updateVoteSent(_vote.getIdentifier(), _vote.getLanguageId(), user);
						}
					}
				}
			}
			return gson.toJson(votes);
		}catch(Exception e){
			Logger.error(this, e.getMessage(), e);
			return "FAILURE";
		}
	}
}
