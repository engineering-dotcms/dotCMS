package com.dotcms.publisher.pusher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.publishing.remote.bundler.CategoryBundler;
import com.dotcms.enterprise.publishing.remote.bundler.ContainerBundler;
import com.dotcms.enterprise.publishing.remote.bundler.ContentBundler;
import com.dotcms.enterprise.publishing.remote.bundler.DependencyBundler;
import com.dotcms.enterprise.publishing.remote.bundler.FolderBundler;
import com.dotcms.enterprise.publishing.remote.bundler.HTMLPageBundler;
import com.dotcms.enterprise.publishing.remote.bundler.HostBundler;
import com.dotcms.enterprise.publishing.remote.bundler.LanguageBundler;
import com.dotcms.enterprise.publishing.remote.bundler.LinkBundler;
import com.dotcms.enterprise.publishing.remote.bundler.OSGIBundler;
import com.dotcms.enterprise.publishing.remote.bundler.RelationshipBundler;
import com.dotcms.enterprise.publishing.remote.bundler.StructureBundler;
import com.dotcms.enterprise.publishing.remote.bundler.TemplateBundler;
import com.dotcms.enterprise.publishing.remote.bundler.UserBundler;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.util.TrustFactory;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

public class PushPublisher extends Publisher {
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	private TrustFactory tFactory;

	@Override
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
		if(LicenseUtil.getLevel()<300)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

		this.config = super.init(config);
		tFactory = new TrustFactory();

		return this.config;

	}

	@Override
	public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
		if(LicenseUtil.getLevel()<300)
	        throw new RuntimeException("need an enterprise pro license to run this bundler");

	    PublishAuditHistory currentStatusHistory = null;
		try {
			//Compressing bundle
			File bundleRoot = BundlerUtil.getBundleRoot(config);

			ArrayList<File> list = new ArrayList<File>(1);
			list.add(bundleRoot);
			File bundle = new File(bundleRoot+File.separator+".."+File.separator+config.getId()+".tar.gz");
			PushUtils.compressFiles(list, bundle, bundleRoot.getAbsolutePath());

			List<Environment> environments = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(config.getId());

			ClientConfig cc = new DefaultClientConfig();

			if(Config.getStringProperty("TRUSTSTORE_PATH") != null && !Config.getStringProperty("TRUSTSTORE_PATH").trim().equals(""))
				cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(tFactory.getHostnameVerifier(), tFactory.getSSLContext()));
			Client client = Client.create(cc);

			//Updating audit table
			currentStatusHistory = pubAuditAPI.getPublishAuditStatus(config.getId()).getStatusPojo();

			currentStatusHistory.setPublishStart(new Date());
			pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.SENDING_TO_ENDPOINTS, currentStatusHistory);
			//Increment numTries
			currentStatusHistory.addNumTries();

	        boolean hasError = false;
	        int errorCounter = 0;

			for (Environment environment : environments) {
				List<PublishingEndPoint> endpoints = APILocator.getPublisherEndPointAPI().findSendingEndPointsByEnvironment(environment.getId());

				boolean sent = false;

				if(!environment.getPushToAll()) {
					Collections.shuffle(endpoints);
					endpoints = endpoints.subList(0, 1);
				}

				for (PublishingEndPoint endpoint : endpoints) {
					EndpointDetail detail = new EndpointDetail();
	        		try {
	        			FormDataMultiPart form = new FormDataMultiPart();
	        			form.field("AUTH_TOKEN",
	        					retriveKeyString(
	        							PublicEncryptionFactory.decryptString(endpoint.getAuthKey().toString())));

	        			form.field("GROUP_ID", UtilMethods.isSet(endpoint.getGroupId()) ? endpoint.getGroupId() : endpoint.getId());

	        			form.field("ENDPOINT_ID", endpoint.getId());
	        			form.bodyPart(new FileDataBodyPart("bundle", bundle, MediaType.MULTIPART_FORM_DATA_TYPE));

	        			//Sending bundle to endpoint
	        			WebResource resource = client.resource(endpoint.toURL()+"/api/bundlePublisher/publish");
	        			synchronized(resource){
	        				ClientResponse response =
		        					resource.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class, form);


		        			if(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK)
		        			{
		        				detail.setStatus(PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY.getCode());
		        				detail.setInfo("Everything ok");
		        				sent = true;
		        			} else {
		        				detail.setStatus(PublishAuditStatus.Status.FAILED_TO_SENT.getCode());
		        				detail.setInfo(
		        						"Returned "+response.getClientResponseStatus().getStatusCode()+ " status code " +
		        								"for the endpoint "+endpoint.getId()+ "with address "+endpoint.getAddress());
		        			}
	        			}
	        			
	        		} catch(Exception e) {

	                    // if the bundle can't be sent after the total num of tries, delete the pushed assets for this bundle
	                    if(currentStatusHistory.getNumTries()==PublisherQueueJob.MAX_NUM_TRIES) {
	                      APILocator.getPushedAssetsAPI().deletePushedAssets(config.getId(), environment.getId());
	                    }

	        			hasError = true;
	        			detail.setStatus(PublishAuditStatus.Status.FAILED_TO_SENT.getCode());

	        			String error = 	"An error occured for the endpoint "+ endpoint.getId() + " with address "+ endpoint.getAddress() + ".  Error: " + e.getMessage();


	        			detail.setInfo(error);

	        			Logger.error(this.getClass(), error);
	        		}

	        		currentStatusHistory.addOrUpdateEndpoint(environment.getId(), endpoint.getId(), detail);
				}

				if(!sent) {
					hasError = true;
					errorCounter++;
				}


			}

			if(!hasError) {
				//Updating audit table
		        currentStatusHistory.setPublishEnd(new Date());
				pubAuditAPI.updatePublishAuditStatus(config.getId(),
						PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY, currentStatusHistory);

				//Deleting queue records
				//pubAPI.deleteElementsFromPublishQueueTable(config.getId());
			} else {
				if(errorCounter == environments.size()) {
					pubAuditAPI.updatePublishAuditStatus(config.getId(),
							PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS, currentStatusHistory);
				} else {
					pubAuditAPI.updatePublishAuditStatus(config.getId(),
							PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS, currentStatusHistory);
				}
			}

			return config;

		} catch (Exception e) {
			//Updating audit table
			try {
				pubAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.FAILED_TO_PUBLISH, currentStatusHistory);
			} catch (DotPublisherException e1) {
				throw new DotPublishingException(e.getMessage());
			}

			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotPublishingException(e.getMessage());

		}
	}






	private String retriveKeyString(String token) throws IOException {
		String key = null;
		if(token.contains(File.separator)) {
			File tokenFile = new File(token);
			if(tokenFile != null && tokenFile.exists())
				key = FileUtils.readFileToString(tokenFile, "UTF-8").trim();
		} else {
			key = token;
		}

		return PublicEncryptionFactory.encryptString(key);
	}

    /**
     * Returns the proper bundlers for each of the elements on the Publishing queue
     *
     * @return
     */
    @SuppressWarnings ("rawtypes")
    @Override
    public List<Class> getBundlers () {

        boolean buildUsers = false;
        boolean buildCategories = false;
        boolean buildOSGIBundle = false;
        for ( PublishQueueElement element : config.getAssets() ) {
            if ( element.getType().equals( "category" ) ) {
                buildCategories = true;
            } else if ( element.getType().equals( "osgi" ) ) {
                buildOSGIBundle = true;
            } else if ( element.getType().equals( "user" ) ) {
                buildUsers = true;
            }
        }

        List<Class> list = new ArrayList<Class>();

        //The order is important cause
        //I need to add all containers associated with templates

        /**
         * ISSUE #2244: https://github.com/dotCMS/dotCMS/issues/2244
         *
         */
        if ( buildUsers ) {
            list.add( UserBundler.class );
        } else if ( buildCategories ) {
            list.add( CategoryBundler.class );
        } else if ( buildOSGIBundle ) {
            list.add( OSGIBundler.class );
        } else {
            list.add( DependencyBundler.class );
            list.add( HostBundler.class );
            list.add( ContentBundler.class );
            list.add( FolderBundler.class );
            list.add( TemplateBundler.class );
            list.add( ContainerBundler.class );
            list.add( HTMLPageBundler.class );
            list.add( LinkBundler.class );

            if ( Config.getBooleanProperty( "PUSH_PUBLISHING_PUSH_STRUCTURES" ) ) {
                list.add( StructureBundler.class );
                /**
                 * ISSUE #2222: https://github.com/dotCMS/dotCMS/issues/2222
                 *
                 */
                list.add( RelationshipBundler.class );
            }
            list.add( LanguageBundler.class );
        }

        return list;
    }

}