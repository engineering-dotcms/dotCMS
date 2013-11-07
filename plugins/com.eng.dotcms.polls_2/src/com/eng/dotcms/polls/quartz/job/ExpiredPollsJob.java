package com.eng.dotcms.polls.quartz.job;

import static com.eng.dotcms.polls.util.PollsConstants.POLL_STRUCTURE_NAME;
import static com.eng.dotcms.polls.util.PollsConstants.PLUGIN_ID;
import static com.eng.dotcms.polls.util.PollsConstants.PROP_ENABLE_EXPIRED_JOB;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * Check and update all the expired polls. If the plugin is deployed into a remote environment (there are configured endpoints) it tries to 
 * publish directly if the current expired poll exists on the receivers.
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
 * Mar 7, 2013 - 4:22:12 PM
 */
public class ExpiredPollsJob implements StatefulJob {
	
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {			
			boolean enabled = Boolean.parseBoolean(pluginAPI.loadProperty(PLUGIN_ID, PROP_ENABLE_EXPIRED_JOB));
			// check if it's enabled
			if(enabled){
				GregorianCalendar now = new GregorianCalendar();
				Logger.debug(this, "BEGIN: Check if some polls are expired...");
				int count = 0;
				List<Contentlet> polls = APILocator.getContentletAPI().findByStructure(StructureCache.getStructureByVelocityVarName(POLL_STRUCTURE_NAME), APILocator.getUserAPI().getSystemUser() , false, 0, 0);
				for(Contentlet poll : polls){
					User modUser = APILocator.getUserAPI().loadUserById(poll.getModUser());
					Date expirationDate  = (Date)poll.getMap().get("expiration_date");		
					String expired = (String)poll.getMap().get("expired");
					if(expirationDate.before(now.getTime()) && !Boolean.parseBoolean(expired)){ // the contentlet is expired...disable it
						Logger.debug(this, "the poll with identifier "+poll.getIdentifier()+" is expired...disable it");
						poll.setStringProperty("expired", "true");
						poll.setInode("0");
						List<Category> categories = APILocator.getCategoryAPI().findTopLevelCategories( APILocator.getUserAPI().getSystemUser(), false );
						APILocator.getContentletAPI().validateContentlet( poll, categories );
						poll = APILocator.getContentletAPI().checkin( poll, modUser, false );
						APILocator.getContentletAPI().publish(poll, modUser, true);
						count++;
					}						
				}
				Logger.debug(this, "END: Check if some polls are expired...");
				Logger.debug(this, "Number of expired Polls updated: " + count);
			}
		} catch (DotDataException e) {
			Logger.error(this, "Error...",e);
		} catch (DotSecurityException e) {
			Logger.error(this, "Error...",e);
		} 
	}

}
