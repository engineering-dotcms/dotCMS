package com.eng.dotcms.polls;

/**
 * Factory for retrieve the PollsManAPI
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
 * Mar 7, 2013 - 4:18:53 PM
 */
public class PollsManFactory {
	
	public static PollsManAPI getPollsManAPI(){
		return new PollsManAPIImpl();
	}
}
