package com.eng.dotcms.additions.publisher;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publisher.util.TrustFactory;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.thoughtworks.xstream.converters.ConversionException;

/**
 * This class read the publishing_queue table and send bundles to some endpoints
 * @author Alberto
 *
 */
public class PublisherQueueJob implements StatefulJob {

	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	private PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
	private PublisherAPI pubAPI = PublisherAPI.getInstance();

    private static final Integer maxNumTries = Config.getIntProperty( "PUBLISHER_QUEUE_MAX_TRIES", 3 );

    @SuppressWarnings("rawtypes")
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
		    Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job - check for publish dates");
		    updatePublishExpireDates(arg0.getFireTime());
		    Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job - check for publish/expire dates");

			Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job - Audit update");
			updateAuditStatus();
			Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job - Audit update");


			List<PublishingEndPoint> endpoints = endpointAPI.getEnabledReceivingEndPoints();

			if(endpoints != null && endpoints.size() > 0)  {
				Logger.debug(PublisherQueueJob.class, "Started PublishQueue Job");
				PublisherAPI pubAPI = PublisherAPI.getInstance();

				PushPublisherConfig pconf = new PushPublisherConfig();
				List<Class> clazz = new ArrayList<Class>();
				clazz.add(PushPublisher.class);

				List<Map<String,Object>> bundles = pubAPI.getQueueBundleIdsToProcess();
				List<PublishQueueElement> tempBundleContents = null;
				PublishAuditStatus status = null;
				PublishAuditHistory historyPojo = null;
				String tempBundleId = null;
				int i=0;
				for(Map<String,Object> bundle: bundles) {
					if(i==1)
						throw new ConversionException("Prova di errore autoscatenato al secondo giro!!!!");
					Date publishDate = (Date) bundle.get("publish_date");

					if(publishDate.before(new Date())) {
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
//						pconf.setEndpoints(endpoints);

                        if ( Integer.parseInt( bundle.get( "operation" ).toString() ) == PublisherAPI.ADD_OR_UPDATE_ELEMENT ) {
                            pconf.setOperation( PushPublisherConfig.Operation.PUBLISH );
                        } else {
                            pconf.setOperation( PushPublisherConfig.Operation.UNPUBLISH );
                        }

                        try {
                        	Host bundleHost = PublisherHostUtil.getBundleHost(tempBundleContents);
                        	if(bundleHost != null)
                        		PushPublishLogger.log(this.getClass(), "Publishing bundle from host: "+bundleHost.getHostname());
                        	else
                        		PushPublishLogger.log(this.getClass(), "Publishing bundle from host: no host found");
                            APILocator.getPublisherAPI().publish( pconf );
                        } catch ( ConversionException e ) {
                            pubAuditAPI.updatePublishAuditStatus( pconf.getId(), PublishAuditStatus.Status.WAITING_FOR_BUNDLING, historyPojo );
                            PushPublishLogger.log(PublisherAPIImpl.class, "Completed Publishing Task in waiting for bundling", pconf.getId());
                            break;
//                            pubAPI.deleteElementsFromPublishQueueTable( pconf.getId() );
                        } catch ( DotPublishingException e ) {
                            /*
                            If we are getting errors creating the bundle we should stop trying to publish it, this is not just a connection error,
                            there is something wrong with a bundler or creating the bundle.
                             */
                            Logger.error( PublisherQueueJob.class, "Unable to publish Bundle: " + e.getMessage(), e );
                            pubAuditAPI.updatePublishAuditStatus( pconf.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE, historyPojo );
//                            pubAPI.deleteElementsFromPublishQueueTable( pconf.getId() );
                        } 
                    }
					i++;
				}

				Logger.debug(PublisherQueueJob.class, "Finished PublishQueue Job");
			}

		} catch (NumberFormatException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (DotPublisherException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (Exception e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}


	private void updateAuditStatus() throws DotPublisherException, DotDataException {
		try{				
			ClientConfig clientConfig = new DefaultClientConfig();
			TrustFactory tFactory = new TrustFactory();
	
			if(Config.getStringProperty("TRUSTSTORE_PATH") != null && !Config.getStringProperty("TRUSTSTORE_PATH").trim().equals("")) {
					clientConfig.getProperties()
					.put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(tFactory.getHostnameVerifier(), tFactory.getSSLContext()));
			}
	        Client client = Client.create(clientConfig);
	        WebResource webResource = null;
	
	        List<PublishAuditStatus> pendingAudits = pubAuditAPI.getPendingPublishAuditStatus();
	
	        //Foreach Bundle
	        for(PublishAuditStatus pendingAudit: pendingAudits) {
	        	//Gets groups list
	        	PublishAuditHistory localHistory = pendingAudit.getStatusPojo();
	
	        	Map<String, Map<String, EndpointDetail>> endpointsMap = localHistory.getEndpointsMap();
	        	Map<String, EndpointDetail> endpointsGroup = null;
	
	        	Map<String, Map<String, EndpointDetail>> bufferMap = new HashMap<String, Map<String, EndpointDetail>>();
	        	//Foreach Group
	        	for(String group : endpointsMap.keySet()) {
	        		endpointsGroup = endpointsMap.get(group);
	
		        	//Foreach endpoint
		        	for(String endpointId: endpointsGroup.keySet()) {
		        		EndpointDetail localDetail = endpointsGroup.get(endpointId);
	
		        		if(localDetail.getStatus() != PublishAuditStatus.Status.SUCCESS.getCode() &&
		        			localDetail.getStatus() != PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode())
		        		{
			        		PublishingEndPoint target = endpointAPI.findEndPointById(endpointId);
	
			        		if(target != null) {
				        		webResource = client.resource(target.toURL()+"/api/auditPublishing");
	
				        		try {
						        	PublishAuditHistory remoteHistory =
						        			PublishAuditHistory.getObjectFromString(
						        			webResource
									        .path("get")
									        .path(pendingAudit.getBundleId()).get(String.class));
	
						        	if(remoteHistory != null) {
						        		bufferMap.putAll(remoteHistory.getEndpointsMap());
							        	break;
						        	}
				        		} catch(Exception e) {
				        			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
				        		}
			        		}
		        		}
		        		else if(localDetail.getStatus() == PublishAuditStatus.Status.SUCCESS.getCode() ){
		        			Map<String, EndpointDetail> m = new HashMap<String, EndpointDetail>();
		        			m.put(endpointId, localDetail);
		        			bufferMap.put(group, m);
		        		}
			        }
		        }
	
	            int countOk = 0;
	            int countPublishing = 0;
	        	for(String groupId: bufferMap.keySet()) {
	        		Map<String, EndpointDetail> group = bufferMap.get(groupId);
	
	        		boolean isGroupOk = false;
	        		boolean isGroupPublishing = false;
		        	for(String endpoint: group.keySet()) {
		        		EndpointDetail detail = group.get(endpoint);
		        		localHistory.addOrUpdateEndpoint(groupId, endpoint, detail);
		        		if(detail.getStatus() == Status.SUCCESS.getCode())
		        			isGroupOk = true;
		        		else if(detail.getStatus() == Status.PUBLISHING_BUNDLE.getCode())
		        			isGroupPublishing = true;
	
		        	}
	
		        	if(isGroupOk)
		        		countOk++;
	
		        	if(isGroupPublishing)
		        		countPublishing++;
	        	}
	
	        	if(countOk == endpointsMap.size()) {
		        	pubAuditAPI.updatePublishAuditStatus(pendingAudit.getBundleId(),
		        			PublishAuditStatus.Status.SUCCESS,
		        			localHistory);
		        	pubAPI.deleteElementsFromPublishQueueTable(pendingAudit.getBundleId());
	        	} else if(localHistory.getNumTries() > maxNumTries) {
	        		pubAuditAPI.updatePublishAuditStatus(pendingAudit.getBundleId(),
		        			PublishAuditStatus.Status.FAILED_TO_PUBLISH,
		        			localHistory);
	        		pubAPI.deleteElementsFromPublishQueueTable(pendingAudit.getBundleId());
	        	} else if(countPublishing == endpointsMap.size()){
	        		pubAuditAPI.updatePublishAuditStatus(pendingAudit.getBundleId(),
	        				PublishAuditStatus.Status.PUBLISHING_BUNDLE,
		        			localHistory);
	        	} else {
	        		pubAuditAPI.updatePublishAuditStatus(pendingAudit.getBundleId(),
	        				PublishAuditStatus.Status.WAITING_FOR_PUBLISHING,
		        			localHistory);
	        	}
	        }
		}finally {
			DbConnectionFactory.closeConnection();
		}		
	}
	
	private void updatePublishExpireDates(Date fireTime) throws DotDataException, DotSecurityException {
		try{
				
		    if(LicenseUtil.getLevel()<300){
		        return;
		    }
		    List<String> expiredTitles = new ArrayList<String>();
		    
	        User systemU = APILocator.getUserAPI().getSystemUser();
	        String toPublish="select working_inode from identifier join contentlet_version_info " +
	                " on (identifier.id=contentlet_version_info.identifier) " +
	                " where syspublish_date is not null and syspublish_date<=? " +
	                " and (sysexpire_date is null or sysexpire_date >= ?) " + 
	                " and (live_inode is null or live_inode<>working_inode) "; 
	        
	        DotConnect dc=new DotConnect();
	        dc.setSQL(toPublish);
	        dc.addParam(fireTime);
	        dc.addParam(fireTime);
	        for(Map<String,Object> mm : (List<Map<String,Object>>)dc.loadResults()){
	            
	            try{
	                Contentlet c = APILocator.getContentletAPI().find( (String)mm.get("working_inode"), systemU, false);
	                APILocator.getContentletAPI().publish(c, APILocator.getUserAPI().loadUserById(c.getModUser(), systemU, false), false);
	            }
	            catch(Exception e){
	                Logger.debug(this, "content failed to publish: " +  e.getMessage());
	            }
	        }
	        String toExpire="select id,lang from identifier join contentlet_version_info " +
	                " on (identifier.id=contentlet_version_info.identifier) " +
	                " where sysexpire_date is not null and sysexpire_date<=? " +
	                " and live_inode is not null";
	        dc.setSQL(toExpire);
	        dc.addParam(fireTime);
	        for(Map<String,Object> mm : (List<Map<String,Object>>)dc.loadResults()) {
	            long lang=mm.get("lang") instanceof String ? Long.parseLong((String)mm.get("lang")) : ((Number)mm.get("lang")).longValue();
	            try{
	                Contentlet c = APILocator.getContentletAPI().findContentletByIdentifier((String)mm.get("id"), true, lang, systemU, false);
	                APILocator.getContentletAPI().unpublish(c, APILocator.getUserAPI().loadUserById(c.getModUser(), systemU, false), false);
	                expiredTitles.add(c.getTitle());
	            }
	            catch(Exception e){
	                Logger.debug(this, "content failed to publish: " +  e.getMessage());
	            }
	        }
	        if(Boolean.parseBoolean(APILocator.getPluginAPI().loadProperty("com.eng.dotcms.publisher", "email.enabled" )) && expiredTitles.size()>0){
			    MailUtil mailer = new MailUtil(APILocator.getPluginAPI().loadProperty("com.eng.dotcms.publisher", "email.from" ), 
			    		APILocator.getPluginAPI().loadProperty("com.eng.dotcms.publisher", "email.from.name" ), 
			    		APILocator.getPluginAPI().loadProperty("com.eng.dotcms.publisher", "email.to" ));        
		        mailer.sendMail(expiredTitles);
	        }
		}finally{
			DbConnectionFactory.closeConnection();
		}
    }
}