package com.eng.dotcms.polls.quartz.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.util.TrustFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

/**
 * Handler used for call a REST service on the receivers server for check if a poll exists or not on the other side.
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
 * Mar 7, 2013 - 4:21:11 PM
 */
public class ContentletUtilHandler {
	
	private TrustFactory tFactory;
	private PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
	
	public ContentletUtilHandler() {
		tFactory = new TrustFactory();
	}
	
	public boolean existOnReceivers(String identifier, long languageId) {
		try {
			List<PublishingEndPoint> receivers = endpointAPI.getReceivingEndPoints();
			Map<String, List<PublishingEndPoint>> endpointsMap = new HashMap<String, List<PublishingEndPoint>>();
			List<PublishingEndPoint> buffer = null;
			
			for (PublishingEndPoint pEndPoint : receivers) {				
				String gid = UtilMethods.isSet(pEndPoint.getGroupId()) ? pEndPoint.getGroupId() : pEndPoint.getId();
				if(endpointsMap.get(gid) == null)
					buffer = new ArrayList<PublishingEndPoint>();
				else 
					buffer = endpointsMap.get(gid);
				buffer.add(pEndPoint);
				endpointsMap.put(gid, buffer);				
			}
			
			ClientConfig cc = new DefaultClientConfig();			
			if(Config.getStringProperty("TRUSTSTORE_PATH") != null && !Config.getStringProperty("TRUSTSTORE_PATH").trim().equals(""))
				cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(tFactory.getHostnameVerifier(), tFactory.getSSLContext()));
			Client client = Client.create(cc);
			boolean exists = false;
			for(String group : endpointsMap.keySet()) {
	        	List<PublishingEndPoint> groupList = endpointsMap.get(group);
	        	for (PublishingEndPoint endpoint : groupList) {
	        		WebResource resource = client.resource(endpoint.toURL()+"/api/contentletUtil/exist/"+identifier+"/"+languageId);
	        		ClientResponse response = resource.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
	        		exists = Boolean.parseBoolean(response.getEntity(String.class));	        		
	        	}
			}
			return exists;
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			return false;
		}
	}
}
