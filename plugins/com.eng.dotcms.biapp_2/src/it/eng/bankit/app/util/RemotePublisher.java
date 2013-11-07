package it.eng.bankit.app.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.ArrayUtils;

import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class RemotePublisher {
	public static long POOLING_INTERVAL = 60000;// 1 Minuto
	public static long POOLING_WARNING_TIME = 300000;// 5 Minuti
	public static long POOLING_MAX_TIME = 900000;// 15 Minuti
	public static Status[] FAILTURE_STATUS = { Status.FAILED_TO_BUNDLE, Status.FAILED_TO_PUBLISH, Status.FAILED_TO_SEND_TO_ALL_GROUPS, Status.FAILED_TO_SEND_TO_SOME_GROUPS, Status.FAILED_TO_SENT };
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	private PublisherAPI publisherAPI = PublisherAPI.getInstance();
	private PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
	private User user;
	private Map<String, Date> publicationQue = new HashMap<String, Date>();
	private boolean running = true;

	public RemotePublisher( User userPubblication ) {
		this.user = userPubblication;
	}

	public boolean remoteServerAvaiable() {
		boolean avaiability = false;
		try {
			List<PublishingEndPoint> endpoints = endpointAPI.getEnabledReceivingEndPoints();
			avaiability = !endpoints.isEmpty();
		} catch ( DotDataException e ) {
			Logger.error( RemotePublisher.class, "Errore nel check della disponibilità di endpoint per Remote Publish" );
		}
		return avaiability;
	}

	public String publish( Set<String> contents ) throws Exception {
		String bundleId = null;
		if ( contents != null && !contents.isEmpty() ) {
			bundleId = UUID.randomUUID().toString();
			List<String> contentToPublish = new ArrayList<String>( contents );
			publisherAPI.addContentsToPublish( contentToPublish, bundleId, new Date(), user );
			// waitPubblication(bundleId);
		}
		return bundleId;
	}

	public String unPublish( Set<String> contents ) throws Exception {
		String bundleId = null;
		if ( contents != null && !contents.isEmpty() ) {
			bundleId = UUID.randomUUID().toString();
			List<String> contentToUnPublish = new ArrayList<String>( contents );
			publisherAPI.addContentsToUnpublish( contentToUnPublish, bundleId, new Date(), user );
			// waitPubblication(bundleId);
		}
		return bundleId;
	}

	public void abortPubblications() throws Exception {
		running = false;
		for ( String bundleId : publicationQue.keySet() ) {
			publisherAPI.deleteElementsFromPublishQueueTable( bundleId );
		}
	}

	public void waitPubblication( String bundleId ) throws Exception {
		publicationQue.put( bundleId, new Date() );
		do {
			Thread.sleep( POOLING_INTERVAL );
		} while ( running && checkPublishStatus( bundleId ) );
	}

	public boolean checkPublishStatus( String bundleId ) throws Exception {
		PublishAuditStatus auditStatus = pubAuditAPI.getPublishAuditStatus( bundleId );
		Date startDate = publicationQue.get( bundleId );
		long diff = System.currentTimeMillis() - startDate.getTime();
		if ( diff > POOLING_MAX_TIME ) {
			throw new TimeoutException( "Errore di pubblicazione remota BundleId:" + bundleId + " time expired" );
		} else if ( diff > POOLING_WARNING_TIME && diff < ( POOLING_WARNING_TIME + ( POOLING_INTERVAL * 2 ) ) ) {
			Logger.warn( RemotePublisher.class, "Pubblicazione remota lenta BundleId:" + bundleId );
		}
		if ( auditStatus != null && auditStatus.getStatus().equals( Status.SUCCESS ) ) {
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
