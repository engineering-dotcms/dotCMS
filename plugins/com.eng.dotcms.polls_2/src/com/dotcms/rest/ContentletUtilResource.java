package com.dotcms.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;

/**
 * REST service for check if, on receiver server, exists a contentlet
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
 * Mar 7, 2013 - 4:12:50 PM
 */
@Path("/contentletUtil")
public class ContentletUtilResource extends WebResource {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private UserAPI userAPI = APILocator.getUserAPI();
	
	@GET
	@Path("/exist/{identifier}/{lang}")
	@Produces(MediaType.TEXT_PLAIN)
	public String existContentlet(@PathParam("identifier") String identifier, @PathParam("lang") long languageId) {
		try{
			String exist = "false";
			StringBuffer luceneQuery = new StringBuffer();
			luceneQuery.append("+identifier:");
			luceneQuery.append(identifier);
			luceneQuery.append(" +live:true");
			luceneQuery.append(" +languageId:");
			luceneQuery.append(languageId);
			List<Contentlet> con = conAPI.search(luceneQuery.toString(), 0, 0, null, userAPI.getSystemUser(), true);
			if(null!=con && con.size()>0)
				exist = "true";
			return exist;
		}catch(Exception e){
			Logger.error(this, e.getMessage(), e);
			return "false";
		}
	}
}
