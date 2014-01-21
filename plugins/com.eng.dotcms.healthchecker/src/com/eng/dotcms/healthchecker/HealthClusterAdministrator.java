package com.eng.dotcms.healthchecker;

import java.util.Date;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import com.dotcms.publisher.util.TrustFactory;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.eng.dotcms.healthchecker.util.HealthUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

public class HealthClusterAdministrator extends ReceiverAdapter {
	
	private HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	private JChannel channel;
	
	public HealthClusterAdministrator() {
	}
	
	public void init(){
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if ((Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false) == false)
				&& Config.getBooleanProperty("DIST_INDEXATION_ENABLED", false)) {
			Logger.info(this, "***\t Starting JGroups Health Cluster Setup");
			try {
				String cacheFile = "cache-jgroups-" + Config.getStringProperty("CACHE_PROTOCOL", "tcp") + ".xml";
				Logger.debug(this, "***\t Going to load JGroups with this Classpath file " + cacheFile);
				String bindAddr = Config.getStringProperty("CACHE_BINDADDRESS", null);
				if (bindAddr != null) {
					Logger.debug(this, "***\t Using " + bindAddr + " as the bindaddress");
				} else {
					Logger.debug(this, "***\t bindaddress is not set");
				}
				if (UtilMethods.isSet(bindAddr)) {
					System.setProperty("jgroups.bind_addr", bindAddr);
				}
				String bindPort = Config.getStringProperty("CACHE_BINDPORT", null);
				if (bindPort != null) {
					Logger.debug(this, "***\t Using " + bindPort + " as the bindport");
				} else {
					Logger.debug(this, "***\t bindport is not set");
				}
				if (UtilMethods.isSet(bindPort)) {
					System.setProperty("jgroups.bind_port", bindPort);
				}
				String protocol = Config.getStringProperty("CACHE_PROTOCOL", "tcp");
				if (protocol.equals("tcp")) {
					Logger.info(this, "***\t Setting up TCP Prperties");
					System.setProperty("jgroups.tcpping.initial_hosts",
							Config.getStringProperty("CACHE_TCP_INITIAL_HOSTS", "localhost[7800]"));
				} else if (protocol.equals("udp")) {
					Logger.debug(this, "***\t Setting up UDP Prperties");
					System.setProperty("jgroups.udp.mcast_port", Config.getStringProperty("CACHE_MULTICAST_PORT", "45588"));
					System.setProperty("jgroups.udp.mcast_addr", Config.getStringProperty("CACHE_MULTICAST_ADDRESS", "228.10.10.10"));
				} else {
					Logger.info(this, "Not Setting up any Properties as no protocal was found");
				}
				System.setProperty("java.net.preferIPv4Stack", Config.getStringProperty("CACHE_FORCE_IPV4", "true"));
				Logger.info(this, "***\t Setting up JCannel for Health Checker");
				channel = new JChannel(classLoader.getResource(cacheFile));
				channel.setReceiver(this);
				channel.connect("dotCMSHealthCluster");
				channel.setOpt(JChannel.LOCAL, false);
				Logger.debug(this, "***\t " + channel.toString(true));
				Logger.info(this, "***\t Ending JGroups Health Cluster Setup");
			} catch (Exception e1) {
				Logger.info(this, "Error During JGroups Health Cluster Setup");
				Logger.fatal(this, e1.getMessage(), e1);
			}
		}
	}
	
//	public void testCluster(){
//		Message msg = new Message(null, null, "TESTINGHEALTHCLUSTER");
//		try {
//			channel.send(msg);
//			Logger.info(this, "Sending Ping to Cluster for Health " + new Date());
//		} catch (ChannelNotConnectedException e) {
//			Logger.error(this, e.getMessage(), e);
//		} catch (ChannelClosedException e) {
//			Logger.error(this, e.getMessage(), e);
//		}
//	}
	
//	@Override
//	public void receive(Message msg) {
//		super.receive(msg);
//		Logger.info(this, "Messaggio ricevuto...");
//		// controllo la view del canale e la memorizzo;
//		Logger.info(this, "View attuale: " + HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView());
//		int count = healthAPI.countLeave();
//		Logger.info(this, "Numero di istanze in LEAVE: " + count);
//		if(count>=0 || null==HealthChecker.INSTANCE.getLastView()){
//			HealthChecker.INSTANCE.setLastView(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView());
//			Logger.info(this, "Last View into receive method: " + HealthChecker.INSTANCE.getLastView());
//		}else
//			Logger.info(this, "La view e' cambiata (viewId memorizzato="+HealthChecker.INSTANCE.getLastView().getVid().getId()+"" +
//					") (viewId nuovo="+HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView().getVid().getId()+"): non la memorizzo...");		
//	}

	@SuppressWarnings("deprecation")
	@Override
	public void suspect(Address mbr) {		
		// memorizzo il suspect all'interno del singleton
		Logger.info(this, "Method suspect: 	There is a suspected member : " + mbr);
		Logger.info(this, "suspect + 		There is a suspected member : " + mbr);
		HealthChecker.INSTANCE.getHealth().setAddress(mbr);
		HealthChecker.INSTANCE.getHealth().setWrittenBy(channel.getLocalAddress());
	}

	@SuppressWarnings("deprecation")
	@Override
	public void viewAccepted(View new_view) {
		Logger.info(this, "Method view: 	Cluster View is : " + new_view);
		Logger.info(this, "viewAccepted + 	Cluster View is : " + new_view);
		
		// memorizzo la nuova view all'interno del singleton + eventuali controlli sull'azione da intraprendere
		HealthChecker.INSTANCE.getHealth().setClusterView(new_view);
		
		// controllo se vengo da un suspect...
		if(null!=HealthChecker.INSTANCE.getHealth().getAddress()){
			boolean contains = HealthUtil.containsMember(new_view,HealthChecker.INSTANCE.getHealth().getAddress());
			if(contains)
				HealthChecker.INSTANCE.getHealth().setStatus(AddressStatus.JOIN);
			else
				HealthChecker.INSTANCE.getHealth().setStatus(AddressStatus.LEAVE);
			
			// scrivo su DB
			try{
				healthAPI.storeHealthStatus(HealthChecker.INSTANCE.getHealth());
				HealthChecker.INSTANCE.flush();
			}catch(DotDataException e){
				Logger.error(getClass(), "Errore scatenato: " + e.getClass());
			}
		}else { 
			// recupero la lista degli indirizzi joinati
			List<Address> joined = HealthUtil.getJoined(new_view);
			Logger.info(getClass(), "Joined size: " + joined.size());
			if(joined.size()>0){
				for(Address newone:joined){
					HealthChecker.INSTANCE.getHealth().setAddress(newone);
					HealthChecker.INSTANCE.getHealth().setStatus(AddressStatus.JOIN);
					HealthChecker.INSTANCE.getHealth().setClusterView(new_view);
					HealthChecker.INSTANCE.getHealth().setWrittenBy(channel.getLocalAddress());
					try {
						healthAPI.storeHealthStatus(HealthChecker.INSTANCE.getHealth());						
						// controllo se nell'altro cluster lo stesso nodo è stato joinato
						boolean ctrl = true;
						while(ctrl){
							if(!HealthUtil.containsMember(CacheLocator.getCacheAdministrator().getJGroupsChannel().getView(),
									HealthChecker.INSTANCE.getHealth().getAddress())) {
								try {
									Logger.info(getClass(), "Il nodo " + HealthChecker.INSTANCE.getHealth().getAddress() + " non e' inserito completamente nel cluster...");
									Thread.sleep(3000);
								} catch (InterruptedException e) {
									Logger.error(getClass(), "Errore in wait");
									ctrl = false;
								}
							}else{
								Logger.info(getClass(), "Il nodo e' joinato nel cluster completamente...invoco il servizio per il flush della cache");
								//TODO Sviluppo servizio REST per flush cache...
								ClientConfig clientConfig = new DefaultClientConfig();
								Client client = Client.create(clientConfig);
						        WebResource webResource = client.resource("http://"+HealthChecker.INSTANCE.getHealth().getAddress().toString().split("[-]")[0]+"/api/health");
						        String response = webResource.path("/joinCluster").get(String.class);
						        Logger.info(getClass(), "RESPONSE: " + response);	
						        ctrl = false;
							}
								
						}
						HealthChecker.INSTANCE.flush();
						
					} catch (DotDataException e) {
						Logger.error(getClass(), "Errore scatenato da joined: " + e.getClass());						
					}
				}
			}
		}
	}	
	
	public JChannel getJGroupsHealthChannel(){
		return channel;
	}
	
}
