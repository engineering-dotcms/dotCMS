package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

public class TaskBundle implements Runnable {
	
	private Map<String,Object> bundle;
	private PushPublisherConfig pconf;
	private PublisherAPI pubAPI;
	private PublishAuditAPI pubAuditAPI;
	private String name;
	
	public TaskBundle(Map<String,Object> bundle, PushPublisherConfig pconf, String name){
		this.bundle = bundle;
		this.pconf = pconf;
		this.name = name;
		pubAPI = PublisherAPI.getInstance();
		pubAuditAPI = PublishAuditAPI.getInstance();
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void run() {
		Logger.info(getClass(), name+": "+"Starting publishing bundle");
		List<PublishQueueElement> tempBundleContents = null;
		PublishAuditStatus status = null;
		PublishAuditHistory historyPojo = null;
		String tempBundleId = null;
		
		List<Class> clazz = new ArrayList<Class>();
		clazz.add(PushPublisher.class);
		try {
			tempBundleId = (String)bundle.get("bundle_id");
			tempBundleContents = pubAPI.getQueueElementsByBundleId(tempBundleId);
	
			//Setting Audit objects
			//History
			historyPojo = new PublishAuditHistory();
			//Retriving assets
			Map<String, String> assets = new HashMap<String, String>();
			List<PublishQueueElement> assetsToPublish = new ArrayList<PublishQueueElement>(); // all assets but contentlets
	
			for(PublishQueueElement c : tempBundleContents) {
				assets.put((String) c.getAsset(), c.getType());
				if(!c.getType().equals("contentlet"))
					assetsToPublish.add(c);
			}
			historyPojo.setAssets(assets);
	
			// all types of assets in the queue but contentlets are passed here, which are passed through lucene queries
			pconf.setAssets(assetsToPublish);
	
			//Status
			status =  new PublishAuditStatus(tempBundleId);
			status.setStatusPojo(historyPojo);
	
			//Insert in Audit table
			pubAuditAPI.insertPublishAuditStatus(status);
			//Queries creation
			pconf.setLuceneQueries(PublisherUtil.prepareQueries(tempBundleContents));
			pconf.setId(tempBundleId);
			pconf.setUser(APILocator.getUserAPI().getSystemUser());
			pconf.setStartDate(new Date());
			pconf.runNow();
			pconf.setPublishers(clazz);
			if ( Integer.parseInt( bundle.get( "operation" ).toString() ) == PublisherAPI.ADD_OR_UPDATE_ELEMENT ) {
				pconf.setOperation( PushPublisherConfig.Operation.PUBLISH );
			} else {
	        	pconf.setOperation( PushPublisherConfig.Operation.UNPUBLISH );
			}
			try {
				APILocator.getPublisherAPI().publish( pconf );
			} catch ( DotPublishingException e ) {
				/*
	            	If we are getting errors creating the bundle we should stop trying to publish it, this is not just a connection error,
	                there is something wrong with a bundler or creating the bundle.
				 */
				Logger.error(getClass(), name+": "+"Unable to publish Bundle: " + e.getMessage(), e );
				pubAuditAPI.updatePublishAuditStatus( pconf.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE, historyPojo );
	            pubAPI.deleteElementsFromPublishQueueTable( pconf.getId() );
			} 
			Logger.info(getClass(), name+": "+"End publishing bundle");
		} catch (NumberFormatException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (DotPublisherException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (Exception e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		}
	}

}
