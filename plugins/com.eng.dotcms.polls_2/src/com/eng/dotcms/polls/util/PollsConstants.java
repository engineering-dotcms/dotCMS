package com.eng.dotcms.polls.util;

/**
 * Constants belong from plugin.properties.
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
 * Mar 7, 2013 - 4:28:43 PM
 */
public class PollsConstants {
	
	// plugin id
	public static final String PLUGIN_ID 									= 	"com.eng.dotcms.polls";
	
	
	// global
	public static final String PROP_AUTO_CREATE_STRUCTURES 					= 	"polls.autoCreateStructures";
	public static final String PROP_POLLS_JOB_GROUP		 					= 	"quartz.job.group";
	public static final String PROP_POLL_VOTES_FILENAME						=	"quartz.csv.filename";
	public static final String PROP_POLL_STATUS_FILENAME					=	"quartz.csv.status.filename";
	public static final String PROP_POLL_STATUS_BUSY						=	"quartz.csv.status.busy";
	public static final String PROP_POLL_STATUS_FREE						=	"quartz.csv.status.free";
	public static final String PROP_REMOTE_ENABLED							=	"plugin.remote.enabled";
	public static final String PROP_FOLDER_PATH								=	"plugin.folder.path";
	
	
	// scheduled jobs
	public static final String PROP_ENABLE_EXPIRED_JOB						= 	"quartz.expired.job.enable";
	public static final String PROP_EXPIRED_JOB_NAME	 					= 	"quartz.expired.job.name";
	public static final String PROP_EXPIRED_JOB_DESCRIPTION					= 	"quartz.expired.job.description";
	public static final String PROP_EXPIRED_JOB_CLASS						= 	"quartz.expired.job.java.classname";
	public static final String PROP_EXPIRED_JOB_CRON_EXP					= 	"quartz.expired.job.cron.expression";
	
	public static final String PROP_ENABLE_PUT_CSV_JOB 						= 	"quartz.putCSV.enable";
	public static final String PROP_PUT_CSV_JOB_NAME	 					= 	"quartz.putCSV.job.name";
	public static final String PROP_PUT_CSV_JOB_DESCRIPTION					= 	"quartz.putCSV.job.description";
	public static final String PROP_PUT_CSV_JOB_CLASS						= 	"quartz.putCSV.job.java.classname";
	public static final String PROP_PUT_CSV_JOB_CRON_EXP					= 	"quartz.putCSV.job.cron.expression";
	public static final String PROP_PUT_CSV_JOB_DEST_PATH 					= 	"quartz.putCSV.destinationPath";

	public static final String PROP_ENABLE_GET_CSV_JOB 						= 	"quartz.getCSV.enable";
	public static final String PROP_GET_CSV_JOB_NAME	 					= 	"quartz.getCSV.job.name";
	public static final String PROP_GET_CSV_JOB_DESCRIPTION					= 	"quartz.getCSV.job.description";
	public static final String PROP_GET_CSV_JOB_CLASS						= 	"quartz.getCSV.job.java.classname";
	public static final String PROP_GET_CSV_JOB_CRON_EXP					= 	"quartz.getCSV.job.cron.expression";
	public static final String PROP_GET_CSV_JOB_SRC_PATH 					= 	"quartz.getCSV.sourcePath";
	
	
	// deployer	
	public static final String POLL_STRUCTURE_NAME 							= 	"Poll";
	public static final String CHOICE_STRUCTURE_NAME 						= 	"PollChoice";
	public static final String VOTE_STRUCTURE_NAME 							= 	"PollVote";
	public static final String RELATIONSHIP_NAME 							= 	"Parent_Poll-Child_PollChoice";

	
	
}
