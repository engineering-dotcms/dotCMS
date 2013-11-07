package com.eng.dotcms.polls;

import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
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
 * Mar 7, 2013 - 4:16:57 PM
 */
public interface PollsManAPI {
	
	/**
	 * Checks if a user has already voted the given Poll
	 * 
	 * Jan 16, 2013 - 10:43:40 AM
	 */
	boolean hasVoted(String userId, String pollIdentifier, long languageId) throws DotContentletStateException, DotDataException, DotSecurityException;

	/**
	 * Insert a new vote
	 * 
	 * Jan 16, 2013 - 10:43:40 AM
	 */	
	void vote(String userId, String pollIdentifier, String choiceIdentifier, User user) throws DotDataException, DotContentletValidationException, DotContentletStateException, IllegalArgumentException, DotSecurityException;
	
	/**
	 * Return count of votes for a specific poll
	 * 
	 * Jan 16, 2013 - 10:43:40 AM
	 */	
	int getTotalPollVotes(String pollIdentifier) throws DotDataException, DotSecurityException;
	
	/**
	 * Return all choices for a specific poll
	 * 
	 * Jan 16, 2013 - 10:43:40 AM
	 */	
	List<Contentlet> getChoiceByPoll(String pollIdentifier, long languageId) throws DotDataException, DotSecurityException;
	
	/**
	 * Return vote list given a choice
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * Jul 12, 2013 - 10:54:30 AM
	 */
	List<Contentlet> getVotesFromChoice(String choiceIdentifier, String pollIdentifier, User user) throws DotDataException, DotSecurityException;
	
	/**
	 * Return count of votes for a specific choice of a specific poll
	 * 
	 * Jan 16, 2013 - 10:43:40 AM
	 */	
	int getChoiceVotes(String pollIdentifier, String choiceIdentifier) throws DotDataException, DotSecurityException;
	
	/**
	 * Updates the vote when we sent to sender.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * Jul 16, 2013 - 9:25:00 AM
	 */
	void updateVoteSent(String voteIdentifier, long languageId, User user) throws DotDataException, DotSecurityException;
}
