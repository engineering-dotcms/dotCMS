package com.eng.dotcms.polls.quartz.job;

import static com.eng.dotcms.polls.util.PollsConstants.PLUGIN_ID;
import static com.eng.dotcms.polls.util.PollsConstants.PROP_POLL_STATUS_BUSY;
import static com.eng.dotcms.polls.util.PollsConstants.PROP_POLL_STATUS_FILENAME;
import static com.eng.dotcms.polls.util.PollsConstants.PROP_POLL_STATUS_FREE;
import static com.eng.dotcms.polls.util.PollsConstants.PROP_POLL_VOTES_FILENAME;
import static com.eng.dotcms.polls.util.PollsConstants.PROP_PUT_CSV_JOB_DEST_PATH;
import static com.eng.dotcms.polls.util.PollsConstants.VOTE_STRUCTURE_NAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.polls.quartz.job.util.PollsVotesFilenameFilter;

/**
 * Put votes into a CSV file and than send this file to the senders via REST service.
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
 * Jan 17, 2013 - 10:22:50 AM
 */
public class PutVotesIntoCSVJob implements StatefulJob {
	
	private ContentletAPI conAPI;
	private UserAPI userAPI;
	private PluginAPI pluginAPI;
	private SimpleDateFormat sdf;
	private String _destinationFolder; 
	private SyncCSVHandler syncHandler;
	
	public PutVotesIntoCSVJob(){
		conAPI = APILocator.getContentletAPI();
		userAPI = APILocator.getUserAPI();
		pluginAPI = APILocator.getPluginAPI();
		sdf = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");
		syncHandler = new SyncCSVHandler();
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		FileOutputStream fos = null;
		try {
			Logger.debug(this, "BEGIN: putting votes into CSV");			
			
			// load the destination folder and clean the content except the logFile.
			Logger.debug(this, "1. Load the destination folder...");
			_destinationFolder = pluginAPI.loadProperty(PLUGIN_ID, PROP_PUT_CSV_JOB_DEST_PATH);
			File destinationFolder = getDestinationFolder(_destinationFolder);
			Logger.debug(this, "1. ...destination folder load successful: " + _destinationFolder);
						
			// load all the votes
			Logger.debug(this, "2. Load all the votes...");
			StringBuffer luceneQuery = new StringBuffer();
			luceneQuery.append("+structureName:");
			luceneQuery.append(VOTE_STRUCTURE_NAME);
			luceneQuery.append(" +");
			luceneQuery.append(VOTE_STRUCTURE_NAME);
			luceneQuery.append(".sent_to_sender:0");
			luceneQuery.append(" +live:true");
			
			List<Contentlet> votes = conAPI.search(luceneQuery.toString(), 0, 0, null, userAPI.getSystemUser(), true);
			Logger.debug(this, "2. ...number of votes: " + votes.size());
			if(votes.size()>0){
				// lock the file: write in no append mode into the status file.
				lock(destinationFolder);
				
				// open a stream on file
				fos = new FileOutputStream(getDestinationFile(destinationFolder), true);
				// write each one into the file
				Logger.debug(this, "3. Write the single vote...");
				int i = 0;
				for(Contentlet vote:votes){
					String line = getCSVFormatFromContentlet(vote).toString();
					fos.write(line.getBytes());
					if(i<votes.size()-1)
						fos.write("\n".getBytes());
					i++;
					vote.setBoolProperty("sent_to_sender", true);
					vote.setInode("");
					vote = conAPI.checkin(vote, userAPI.getSystemUser(), true);
					conAPI.publish(vote, userAPI.getSystemUser(), true);
				}
				fos.flush();
				Logger.debug(this, "3. Write successful.");
				
				// unlock
				unlock(destinationFolder);
				
				//sync csv with staging
				syncHandler.sync();
			}
			
			Logger.debug(this, "END: putting votes into CSV. Number of votes: " + votes.size());
		} catch (DotDataException e) {
			Logger.error(this, "Error", e);
		} catch (DotSecurityException e) {
			Logger.error(this, "Error", e);
		} catch (FileNotFoundException e) {
			Logger.error(this, "Error", e);
		} catch (IOException e) {
			Logger.error(this, "Error", e);
		} finally {
			try {
				if(null!=fos)
					fos.close();				
			} catch (IOException e) {
				Logger.error(this, "Error during the FileOutputStream close", e);
			}
		}
		
	}
	
	private File getDestinationFolder(String pathname) throws DotDataException{
		File destinationFolder = new File(pathname);
		if(!destinationFolder.exists())
			destinationFolder.mkdirs();
		else
			cleanFolder(destinationFolder);
		return destinationFolder;
	}
	
	private File getDestinationFile(File parent) throws IOException, DotDataException{
		
		File csv = new File(parent,pluginAPI.loadProperty(PLUGIN_ID, PROP_POLL_VOTES_FILENAME)+"_"+sdf.format(new GregorianCalendar().getTime())+".txt");
		if(!csv.exists())
			csv.createNewFile();
		return csv;
	}
	
	private File getStatusFile(File parent) throws IOException, DotDataException{
		File csv = new File(parent,pluginAPI.loadProperty(PLUGIN_ID, PROP_POLL_STATUS_FILENAME));
		if(!csv.exists())
			csv.createNewFile();
		return csv;
	}
	
	private boolean cleanFolder(File destFolder) throws DotDataException{
		boolean ret = true;
		File[] files = destFolder.listFiles(new PollsVotesFilenameFilter(pluginAPI.loadProperty(PLUGIN_ID, PROP_POLL_VOTES_FILENAME)));
		for(File f:files){
			ret = f.delete();
		}
		return ret;
	}
	
	private void lock(File destination) throws IOException, DotDataException {
		FileOutputStream fos = null;
		try{
			fos = new FileOutputStream(getStatusFile(destination), false);
			fos.write(pluginAPI.loadProperty(PLUGIN_ID, PROP_POLL_STATUS_BUSY).getBytes());
			fos.flush();
			fos.close();
		}finally{
			if(null!=fos)
				fos.close();
		}
	}
	
	private void unlock(File destination) throws IOException, DotDataException{
		FileOutputStream fos = null;
		try{
			fos = new FileOutputStream(getStatusFile(destination), false);
			fos.write(pluginAPI.loadProperty(PLUGIN_ID, PROP_POLL_STATUS_FREE).getBytes());
			fos.flush();
			fos.close();
		}finally{
			if(null!=fos)
				fos.close();
		}
	}
	
	private StringBuilder getCSVFormatFromContentlet(Contentlet vote){
		StringBuilder csvVote = new StringBuilder();
		csvVote.append(vote.getIdentifier());
		csvVote.append("|");
		csvVote.append(vote.getMap().get("poll").toString());
		csvVote.append("|");
		csvVote.append(vote.getMap().get("choice").toString());
		csvVote.append("|");
		csvVote.append(vote.getMap().get("user").toString());
		csvVote.append("|");
		csvVote.append(vote.getLanguageId());
		return csvVote;				
	}
}	
