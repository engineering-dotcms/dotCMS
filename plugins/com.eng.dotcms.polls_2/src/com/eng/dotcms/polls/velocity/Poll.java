package com.eng.dotcms.polls.velocity;

import java.util.List;

/**
 * Bean that represents a single Poll, used by the velocity macro.
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
 * Mar 7, 2013 - 4:29:22 PM
 */
public class Poll {
	
	private String identifier;
	private StringBuilder question;
	private List<PollChoice> choices;
	private boolean expired;
	
	public StringBuilder getQuestion() {
		return question;
	}
	public void setQuestion(StringBuilder question) {
		this.question = question;
	}
	public List<PollChoice> getChoices() {
		return choices;
	}
	public void setChoices(List<PollChoice> choices) {
		this.choices = choices;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public boolean isExpired() {
		return expired;
	}
	public void setExpired(boolean expired) {
		this.expired = expired;
	}
	
}
