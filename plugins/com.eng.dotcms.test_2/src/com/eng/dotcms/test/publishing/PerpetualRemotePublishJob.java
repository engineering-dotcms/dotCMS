package com.eng.dotcms.test.publishing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.xml.transform.TransformerConfigurationException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.xml.sax.SAXException;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.test.publishing.util.Utils;
import com.liferay.portal.model.User;

public class PerpetualRemotePublishJob implements StatefulJob {
	
	private String pluginId = "com.eng.dotcms.test";
	private PluginAPI pluginAPI = APILocator.getPluginAPI();
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private PublisherAPI publisherAPI = PublisherAPI.getInstance();
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {		
		// recupero la query dalla configurazione
		Logger.info(this, "*************************************************************************************************************");
		Logger.info(this, "********************************************** START REMOTE PUBLISH *****************************************");
		try {
			User systemUser = APILocator.getUserAPI().getSystemUser();
			List<String> identifiers = new ArrayList<String>();
			String bundleId = UUID.randomUUID().toString();
			String luceneQuery = pluginAPI.loadProperty(pluginId, "remotepublishing.query");
			Logger.info(this, "************* query: " + luceneQuery);
			// recupero i contenuti dalla query
			List<Contentlet> contentlets = conAPI.search(luceneQuery, 0, 0, null, systemUser, false);
			Logger.info(this, "************* numero di contenuti: " + contentlets.size());
			// recupero gli identifiers
			for(Contentlet c:contentlets){
				Random randomGenerator = new Random();
				int randomInt = randomGenerator.nextInt(100);
				if(randomInt<=49){
					Logger.debug(this, "************* il contenuto " + c.getIdentifier() + " deve essere depubblicato...");
					conAPI.unpublish(c, systemUser, false);
				}else{
					Logger.debug(this, "************* il contenuto " + c.getIdentifier() + " deve essere pubblicato...");
					if(!c.isLive())
						conAPI.publish(c, systemUser, false);
				}
				identifiers.add(c.getIdentifier());
			}
			Logger.info(this, "************* numero di identificativi: " + identifiers.size());
			// aggiungo in pubblicazione
			publisherAPI.addContentsToPublish(identifiers, bundleId, new Date(), APILocator.getUserAPI().getSystemUser());
			Logger.info(this, "******************************************* END REMOTE PUBLISH *******************************************");
			Logger.info(this, "**********************************************************************************************************");
			String checkResult = pluginAPI.loadProperty(pluginId, "remotepublishing.checkresult");
			if(Boolean.valueOf(checkResult)){
				Logger.info(this, "************* controllo il risultato su i server...");
				List<String> paths = Utils.getQueryPaths();
				if(null!=paths){
					Logger.info(this, "************* numero di path da controllare (senza i relativi sottopath): " + paths.size());
					// mi fermo per cinque minuti...
					Thread.sleep(300000);
					File parent = Utils.getCurrentExecutionRootPathWithCSS();
					for(String path:paths)				
						process(parent, path);					
				}
			}
		} catch (DotDataException e) {
			e.printStackTrace();
		} catch (DotSecurityException e) {
			e.printStackTrace();
		} catch (DotPublisherException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	private void process(File parent, String path) throws IOException,
			TransformerConfigurationException, SAXException, DotDataException {
		Logger.info(this, "************* path da processare: " + path);
		String uuid = UUID.randomUUID().toString();
		Connection.Response responseSender = Jsoup.connect(Utils.getSenderURL(path))
				.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
	            .timeout(10000)
	            .execute();
		Connection.Response responseReceiver = Jsoup.connect(Utils.getReceiverURL(path))
				.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
	            .timeout(10000)
	            .execute(); 

		Document sender = responseSender.parse();
		Document receiver = responseReceiver.parse();
		Utils.storeDifference(parent, 
				Utils.storeFile(parent, "sender_"+uuid+".html", sender), 
				Utils.storeFile(parent, "receiver_"+uuid+".html", receiver), uuid);	
		processLinks(parent,Utils.getSenderURL(path),sender,path);
	}
	
	private void processLinks(File parent, String relativePath, Document doc, String queryPath) throws DotDataException, IOException, TransformerConfigurationException, SAXException {
		String path = Utils.getSenderURL(null);
		// recupero i links
		Elements links = doc.select("a[href*="+queryPath+"]");
		if(links.size()==0) { //il documento non ha link...
			String uuid = UUID.randomUUID().toString();
			Connection.Response responseSender = Jsoup.connect(Utils.getSenderURL(relativePath))
					.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
		            .timeout(10000)
		            .execute();
			Connection.Response responseReceiver = Jsoup.connect(Utils.getReceiverURL(relativePath))
					.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
		            .timeout(10000)
		            .execute(); 

			Document sender = responseSender.parse();
			Document receiver = responseReceiver.parse();
			Utils.storeDifference(parent, 
					Utils.storeFile(parent, "sender_"+uuid+".html", sender), 
					Utils.storeFile(parent, "receiver_"+uuid+".html", receiver), uuid);	
		}else {
			 for (Element link : links) {
		            String url_link = link.attr("abs:href");
		            if(url_link.indexOf(path)>=0){
		            	Connection.Response aLink = Jsoup.connect(Utils.getSenderURL(url_link))
		    					.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
		    		            .timeout(10000)
		    		            .execute(); 
		            	processLinks(parent, Utils.getCleanPath(url_link), aLink.parse(), queryPath);
		            }
		        }
		}
	}
}
