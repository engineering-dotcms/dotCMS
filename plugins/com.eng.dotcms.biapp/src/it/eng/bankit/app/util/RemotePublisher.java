package it.eng.bankit.app.util;

import it.eng.bankit.deploy.IDeployConst;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.ArrayUtils;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class RemotePublisher {
	public static Status[] FAILTURE_STATUS = { Status.FAILED_TO_BUNDLE, Status.FAILED_TO_PUBLISH, Status.FAILED_TO_SEND_TO_ALL_GROUPS, Status.FAILED_TO_SEND_TO_SOME_GROUPS, Status.FAILED_TO_SENT };
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	private PublisherAPI publisherAPI = PublisherAPI.getInstance();
	private User user;
	private PluginAPI pAPI = APILocator.getPluginAPI();
	private Map<String, Date> publicationQue = new HashMap<String, Date>();
	private boolean running = true;
	private long 	poolingIntervalLong ; 
	private long 	poolingWarningLong ; 
	private long 	poolingMaxLong ; 
	

	public RemotePublisher( User userPubblication ) {
		this.user = userPubblication;
		init();
	}

	private void init() {		
		try{
			String poolingInterval = pAPI.loadProperty(IDeployConst.PLUGIN_ID, 
					                                      it.eng.bankit.deploy.IDeployConst.CAMBI_POOLING_INTERVAL  );
			String poolingWarning = pAPI.loadProperty(it.eng.bankit.deploy.IDeployConst.PLUGIN_ID, it.eng.bankit.deploy.IDeployConst.CAMBI_POOLING_WARNTIME );
			String  poolingMax = pAPI.loadProperty(it.eng.bankit.deploy.IDeployConst.PLUGIN_ID, it.eng.bankit.deploy.IDeployConst.CAMBI_POOLING_MAXTIME );
			poolingIntervalLong = Long.parseLong( poolingInterval ) ;
			poolingWarningLong = Long.parseLong( poolingWarning ) ;
			poolingMaxLong = Long.parseLong( poolingMax ) ;
		}catch (Exception e) {
			e.printStackTrace();
			poolingIntervalLong = 60000;
		    poolingWarningLong = 300000;
			poolingMaxLong = 1800000;
		}
		Logger.info( RemotePublisher.class,  "INIT : poolingIntervalLong:"+poolingIntervalLong+" - poolingWarningLong:"+poolingWarningLong+" - poolingMaxLong:"+poolingMaxLong) ;
	}

	public boolean remoteServerAvaiable() {
//		boolean avaiability = false;
		//			List<PublishingEndPoint> endpoints = endpointAPI.getEnabledReceivingEndPoints();
		//			avaiability = !endpoints.isEmpty();
		Logger.info( RemotePublisher.class,  "remoteServerAvaiable -> UTENTE " + user.getUserId() ) ;
		List<Environment> envsToSendTo = getEnvironments( user );
		return !envsToSendTo.isEmpty();
	}

	public String publish( Set<String> contents ) throws Exception {
		String bundleId = null;
		if ( contents != null && !contents.isEmpty() ) {
			Logger.info(this.getClass() , "contents " + contents );
			//bundleId = UUID.randomUUID().toString();
			List<Environment> envsToSendTo = getEnvironments( user );
			Logger.info(this.getClass() , "envsToSendTo " + envsToSendTo );
			Date pDate = java.util.Calendar.getInstance().getTime();
			Bundle bundle = new Bundle(null, pDate , null, user.getUserId());
			APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);
			List<String> contentToPublish = new ArrayList<String>( contents );
			publisherAPI.addContentsToPublish( contentToPublish, bundle.getId(), pDate, user );
			Logger.info(this.getClass() , "publish MANDO IN PUBBLICAZIONE BUNDLEID " + bundleId  );

			//	List<String> contentToPublish = new ArrayList<String>( contents );
			//	publisherAPI.addContentsToPublish( contentToPublish, bundleId, new Date(), user );
			// waitPubblication(bundleId);
			return bundle.getId();
		}
		return bundleId;
	}

	public String unPublish( Set<String> contents ) throws Exception {
		String bundleId = null;
		if ( contents != null && !contents.isEmpty() ) {
			bundleId = UUID.randomUUID().toString();
			List<Environment> envsToSendTo = getEnvironments( user );
			Date pDate = java.util.Calendar.getInstance().getTime();
			Bundle bundle = new Bundle(null,pDate   , null, user.getUserId());
			APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);
			List<String> contentToPublish = new ArrayList<String>( contents );
			publisherAPI.addContentsToPublish( contentToPublish, bundle.getId(), pDate, user );			

			//			List<String> contentToUnPublish = new ArrayList<String>( contents );
			//			publisherAPI.addContentsToUnpublish( contentToUnPublish, bundleId, new Date(), user );
			// waitPubblication(bundleId);
		}
		return bundleId;
	}



	private List<Environment> getEnvironments( User user ){
		List<Environment> envsToSendTo = new ArrayList<Environment>();
		try{
			Logger.info( RemotePublisher.class,  "UTENTE " + user.getUserId() ) ;
			List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId() ,true);
			Logger.info( RemotePublisher.class,  "roles " +roles ) ;
			String userIDSU = APILocator.getUserAPI().getSystemUser().getUserId();
			if( user.getUserId().equalsIgnoreCase(userIDSU ) ){
				Logger.info( RemotePublisher.class,  "UTENTE é SISTEM USER " + user.getUserId() ) ;
				envsToSendTo =	APILocator.getEnvironmentAPI().findAllEnvironments();
				Logger.info( RemotePublisher.class,  "SystemUser invia a  " + envsToSendTo  ) ;
			}else {
				Set<Environment> environments = new HashSet<Environment>();
				for(Role r: roles) {
					environments.addAll(APILocator.getEnvironmentAPI().findEnvironmentsByRole(r.getId()));
				}
				for (Environment environment : environments){
					envsToSendTo.add(environment);
				}
				Logger.info( RemotePublisher.class,  "USERCAMBI invia a  " + envsToSendTo  ) ;
			}
		}catch (Exception e) {
			Logger.error(this.getClass() ,"ERROR -> getEnvironments " + e.getMessage() );
		}
		return envsToSendTo;

	}
	public void abortPubblications() throws Exception {
		running = false;
		for ( String bundleId : publicationQue.keySet() ) {
			publisherAPI.deleteElementsFromPublishQueueTable( bundleId );
		}
	}

	public void waitPubblication( String bundleId ) throws Exception {
		Date d = new Date();
		publicationQue.put( bundleId, d);
		Logger.info( RemotePublisher.class, " ---------- "   );
		Logger.info( RemotePublisher.class, "bundleId:" + bundleId);
		Logger.info( RemotePublisher.class, "waitPubblication - Date:" + d);
		Logger.info( RemotePublisher.class, " ----------"   );
		do {
			Thread.sleep( poolingIntervalLong );
		} while ( running && checkPublishStatus( bundleId ) );
	}

	public boolean checkPublishStatus( String bundleId ) throws Exception {
		 
		PublishAuditStatus auditStatus = pubAuditAPI.getPublishAuditStatus( bundleId );
		Date startDate = publicationQue.get( bundleId );
		long diff = System.currentTimeMillis() - startDate.getTime();
		Logger.info( RemotePublisher.class, " ------ "   );
		Logger.info( RemotePublisher.class, "checkPublishStatus - BundleId:" + bundleId);
		Logger.info( RemotePublisher.class, "checkPublishStatus - Current:" + System.currentTimeMillis());
		Logger.info( RemotePublisher.class, "checkPublishStatus - StartDate.getTime:" + startDate.getTime());
		Logger.info( RemotePublisher.class, "checkPublishStatus - diff:" + diff);
		Logger.info( RemotePublisher.class, "checkPublishStatus - BundleId:" + bundleId);
		Logger.info( RemotePublisher.class, " ------ "   );
		
		if ( diff > poolingMaxLong ) {
			throw new TimeoutException( "Errore di pubblicazione remota BundleId:" + bundleId + " time expired" );
		} else if ( diff > poolingWarningLong && diff < ( poolingWarningLong + ( poolingIntervalLong * 2 ) ) ) {
			Logger.warn( RemotePublisher.class, "Pubblicazione remota lenta BundleId:" + bundleId );
		}
		Logger.info( RemotePublisher.class, " ------ "   );
		Logger.info( RemotePublisher.class, "AuditStatus " + ( auditStatus != null && auditStatus.getStatus()!= null ? auditStatus.getStatus().getCode() : "ancora nessun ack"));
		Logger.info( RemotePublisher.class, " ------ "   );
		if ( auditStatus != null && auditStatus.getStatus().equals( PublishAuditStatus.Status.SUCCESS ) ) {
			return false;// end
		} else if ( auditStatus != null && ArrayUtils.contains( FAILTURE_STATUS, auditStatus.getStatus() ) ) {
			List<PublishQueueElement> elements=publisherAPI.getQueueElementsByBundleId( bundleId );
			if (elements==null||elements.isEmpty()){
				throw new Exception( buildErrorMessage( auditStatus ) );
			}else{
				Logger.warn( RemotePublisher.class, "Pubblicazione remota("+bundleId+") in errore ma numero massimo di tentativi non superato" );
			}
		}
		return true;// continue checking
	}

	private String buildErrorMessage( PublishAuditStatus auditStatus ) {
		StringBuilder sb = new StringBuilder();
		sb.append( "Errore di pubblicazione remota [" );
		sb.append( auditStatus.getStatus().toString() );
		sb.append( "] BundleId:" );
		sb.append( auditStatus.getBundleId() );
		sb.append( "\n" );
		PublishAuditHistory history = auditStatus.getStatusPojo();
		if ( history != null ) {
			Map<String, Map<String, EndpointDetail>> endPoints = history.getEndpointsMap();
			for ( String key : endPoints.keySet() ) {
				Map<String, EndpointDetail> detailMap = endPoints.get( key );
				for ( String detailKey : detailMap.keySet() ) {
					EndpointDetail detail = detailMap.get( detailKey );
					sb.append( '[' );
					sb.append( detailKey );
					sb.append( "] " );
					sb.append( detail.getStatus() );
					sb.append( " - " );
					sb.append( detail.getInfo() );
					if ( UtilMethods.isSet( detail.getStackTrace() ) ) {
						sb.append( detail.getStackTrace() );
					}
				}
			}
		}
		return sb.toString();
	}
}
