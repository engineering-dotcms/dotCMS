package com.eng.dotcms.polls.velocity;

import static com.eng.dotcms.polls.util.PollsConstants.RELATIONSHIP_NAME;

import java.util.ArrayList;
import java.util.List;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.polls.util.PollsUtil;

/**
 * ViewTool for frontend vote form.
 * 
 * The usage of this mechanism make sense only if, by specific requirements, the receiver servers are only in frontend mode and all the 
 * activity are doing from the sender servers. 
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
 * Mar 7, 2013 - 4:30:18 PM
 */
public class PollsViewTool implements ViewTool {
	
	public Poll getPollByTitle(String title) throws DotDataException, DotSecurityException{
		Poll result = new Poll();
		List<PollChoice> choicesList = new ArrayList<PollChoice>();
		List<Contentlet> polls = APILocator.getContentletAPI().findByStructure(StructureCache.getStructureByVelocityVarName("Poll"), APILocator.getUserAPI().getSystemUser() , false, 0, 0);
		for(Contentlet poll : polls){
			String _title = (String)poll.getMap().get("title");
			if(_title.equals(title)){
				String expired = (String)poll.getMap().get("expired");
				if(!Boolean.parseBoolean(expired)){
					result.setIdentifier(poll.getIdentifier());
					result.setQuestion(new StringBuilder((String)poll.getMap().get("question")));
					List<Contentlet> choices = APILocator.getContentletAPI().getRelatedContent(poll, RelationshipFactory.getRelationshipByRelationTypeValue(RELATIONSHIP_NAME), APILocator.getUserAPI().getSystemUser(), true);
					for(Contentlet c : choices){
						PollChoice aChoice = new PollChoice();
						aChoice.setIdentifier(c.getIdentifier());
						aChoice.setText((String)c.getMap().get("text"));
						choicesList.add(aChoice);
					}
					result.setChoices(choicesList);
					return result;					
				}else{
					result.setExpired(true);
					return result;
				}
			}
		}
		result.setExpired(true);
		return result;
	}
	
	public String getVotesHtmlCode(String pollIdentifier, String language) {
		String htmlCode = "";
		try {
			htmlCode = PollsUtil.getVotesHtmlCode(pollIdentifier, Long.parseLong(language));
		} catch (NumberFormatException e) {
			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(),e);
		}
		return htmlCode;
	}
	
	@Override
	public void init(Object initData) {
		// TODO Auto-generated method stub

	}

}
