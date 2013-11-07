package com.eng.dotcms.polls.quartz.job;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.polls.quartz.job.util.PollsVotesFilenameFilter;

import static com.eng.dotcms.polls.util.PollsConstants.*;

/**
 * Get votes from a CSV file and import into the system. This job can be disabled by plugin configuration and can be enabled only if 
 * the plugin is deployed into a remote environment (there are configured endpoints).
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
 * Jan 17, 2013 - 2:26:27 PM
 */
public class GetVotesFromCSVJob implements StatefulJob {

	private ContentletAPI conAPI;
	private UserAPI userAPI;
	private PluginAPI pluginAPI;
	private String _sourceFolder;
	
	public GetVotesFromCSVJob() {
		conAPI = APILocator.getContentletAPI();
		userAPI = APILocator.getUserAPI();
		pluginAPI = APILocator.getPluginAPI();
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try{
			Logger.debug(this, "BEGIN: getting votes from CSV");
			
			// load the source folder with the single csv file and the status file.
			Logger.debug(this, "1. Load the source folder...");
			_sourceFolder = pluginAPI.loadProperty(PLUGIN_ID, PROP_GET_CSV_JOB_SRC_PATH);
			File sourceFolder = getSourceFolder(_sourceFolder);
			Logger.debug(this, "1. ...destination folder load successful: " + _sourceFolder);
			
			// load the status file and read the status at this time
			File statusFile = getStatusFile(sourceFolder);
			if(null!=statusFile){
				String status = getStatus(statusFile);
				
				if(pluginAPI.loadProperty(PLUGIN_ID, PROP_POLL_STATUS_FREE).equals(status)){
					Logger.info(this, "The Status is FREE: I can read the CSV file...");
					// get the votes to checkin
					File[] sourceFile = getSourceFiles(sourceFolder);
					if(null!=sourceFile){
						Logger.info(this, "Number of CSV file found: "+sourceFile.length);
						for(File f: sourceFile){
							List<Contentlet> votes = getVotesToCheckin(getVotesFromCSV(f));
							List<Category> categories = APILocator.getCategoryAPI().findTopLevelCategories( APILocator.getUserAPI().getSystemUser(), false );
							List<Permission> structurePermissions = APILocator.getPermissionAPI().getPermissions(StructureCache.getStructureByVelocityVarName(VOTE_STRUCTURE_NAME));
							for(Contentlet vote:votes){
								// validate and checkin...and publish
								conAPI.validateContentlet(vote, categories);
								vote = conAPI.checkin(vote, categories, structurePermissions, userAPI.getSystemUser(), true);
								conAPI.publish(vote, userAPI.getSystemUser(), true);
							}
							Logger.info(this, "The votes were inserted and published.");							
						}
						if(cleanFolder(sourceFolder))
							Logger.info(this, "The source folder is clean.");
					}
				}else{
					Logger.info(this, "The Status is BUSY: EXIT...");
				}			
			}
		} catch(IOException e){
			Logger.error(this, e.getMessage());
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage());
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage());
		}
	}
	
	private File[] getSourceFiles(File parent) throws DotDataException {
		File[] csv = parent.listFiles(new PollsVotesFilenameFilter(pluginAPI.loadProperty(PLUGIN_ID, PROP_POLL_VOTES_FILENAME)));
		if(csv.length==0){
			cleanFolder(parent);
			Logger.error(this,"Excepted 1 or more csv file(s), found 0");
			return null;
		}else
			return parent.listFiles(new PollsVotesFilenameFilter(pluginAPI.loadProperty(PLUGIN_ID, PROP_POLL_VOTES_FILENAME)));
		
	}	
	
	private File getStatusFile(File parent) throws DotDataException {
		File csv = new File(parent,pluginAPI.loadProperty(PLUGIN_ID, PROP_POLL_STATUS_FILENAME));
		if(!csv.exists()){
			cleanFolder(parent);
			Logger.debug(this, "The status file doesn't exists! I can't know if the csv file contains right data.");
			return null;
		}
		return csv;
	}
	
	private File getSourceFolder(String pathname) {
		File sourceFolder = new File(pathname);
		if(!sourceFolder.exists()){
			Logger.warn(this, "The source folder doesn't exists. This can be the first time that this job runs, otherwise there is something wrong.");
			sourceFolder.mkdirs();
		}
		return sourceFolder;
	}
	
	private boolean cleanFolder(File sourceFolder){
		boolean ret = true;
		File[] files = sourceFolder.listFiles();
		for(File f:files){
			if(f.isDirectory())
				ret = cleanFolder(f);
			else
				ret = f.delete();
		}
		return ret;
	}
	
	private String getStatus(File status) throws IOException {
		FileReader fileReader = new FileReader(status);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = bufferedReader.readLine();
		fileReader.close();
		return line;
	}
	
	private List<String> getVotesFromCSV(File csv) throws IOException{
		List<String> votesList = new ArrayList<String>();
		FileReader fileReader = new FileReader(csv);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			votesList.add(line);
		}
		fileReader.close();
		return votesList;
	}
	
	private List<Contentlet> getVotesToCheckin(List<String> votesString) throws DotDataException, DotSecurityException {
		List<Contentlet> votesToCheckin = new ArrayList<Contentlet>();
		for(String voteString:votesString){
			String[] voteArr = voteString.split("[|]");
			try{
				Contentlet vote = new Contentlet();					
				vote.setStructureInode(StructureCache.getStructureByVelocityVarName(VOTE_STRUCTURE_NAME).getInode());
				vote.setStringProperty("poll", voteArr[1]);
				vote.setStringProperty("choice", voteArr[2]);
				vote.setStringProperty("user", voteArr[3]);
				vote.setBoolProperty("sent_to_sender", true);
				vote.setLanguageId(Long.parseLong(voteArr[4]));
				votesToCheckin.add(vote);
			}catch(DotContentletStateException e){
				Logger.error(this, e.getMessage(), e);
			}
			
		}

		return votesToCheckin;
	}
}
