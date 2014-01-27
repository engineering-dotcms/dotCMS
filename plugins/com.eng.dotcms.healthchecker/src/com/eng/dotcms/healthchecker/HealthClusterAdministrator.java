package com.eng.dotcms.healthchecker;

import java.util.GregorianCalendar;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.eng.dotcms.healthchecker.util.HealthUtil;

/**
 * Classe dedicata alla gestione del canale jGroups utilizzato per monitorare la salute del cluster.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * @date Jan 22, 2014
 */
public class HealthClusterAdministrator extends ReceiverAdapter {
	
	private HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	private JChannel channel;
	private boolean cluster = false;
	private static int MAX_COUNT_SUSPECT = Config.getIntProperty("HEALTH_CHECKER_MAX_COUNT_SUSPECT", 5);
	private static long MAX_REJOIN_TIME = Config.getIntProperty("HEALTH_CHECKER_MAX_REJOIN_TIME", 60000);
	
	public HealthClusterAdministrator() {}
	
	/**
	 * Inizializzazione canale. Viene utilizzata la stessa logica e lo stesso file di configurazione utilizzato per il canale principale.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
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
				cluster = true;
				Logger.debug(this, "***\t " + channel.toString(true));
				Logger.info(this, "***\t Ending JGroups Health Cluster Setup");
			} catch (Exception e1) {
				Logger.info(this, "Error During JGroups Health Cluster Setup");
				Logger.fatal(this, e1.getMessage(), e1);
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	/**
	 * Intercetto il metodo suspect, scatenato quando un nodo è uscito dal cluster.
	 */
	public void suspect(Address mbr) {
		int countSuspect = HealthChecker.INSTANCE.getCountSuspect();
		if(countSuspect<MAX_COUNT_SUSPECT) {
			countSuspect++;
			// memorizzo il suspect all'interno del singleton
			Logger.info(this, "Method suspect: 	There is a suspected member : " + mbr);
			Logger.info(this, "suspect + 		There is a suspected member : " + mbr);
			HealthChecker.INSTANCE.getHealth().setAddress(mbr);
			HealthChecker.INSTANCE.getHealth().setWrittenBy(channel.getLocalAddress());
			HealthChecker.INSTANCE.setCountSuspect(countSuspect);
		}else{ // raggiunto il limite di suspect accettate senza ricevere una view...il nodo è dichiarato fuori dal cluster.
			Logger.warn(getClass(), "Raggiunto il limite massimo di chiamate ("+MAX_COUNT_SUSPECT+") al metodo suspect senza chiamare il viewAccepted: il nodo è fouri dal cluster.");
			// resetto il count per evitare continue chiamate al suspect senza un viewAccepted.
			HealthChecker.INSTANCE.getHealth().setStatus(AddressStatus.LEAVE);
			// scrivo su DB
			try{
				boolean isCreator = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView().getCreator().equals(HealthChecker.INSTANCE.getHealth().getAddress());
				HibernateUtil.startTransaction();
				healthAPI.storeHealthStatus(HealthChecker.INSTANCE.getHealth());
				healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getHealth().getAddress(),AddressStatus.JOIN);
				healthAPI.insertHealthClusterView(HealthChecker.INSTANCE.getHealth().getAddress(),
						Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),isCreator, 
						HealthChecker.INSTANCE.getHealth().getStatus());
				HealthChecker.INSTANCE.flush();
				HibernateUtil.commitTransaction();
			}catch(DotDataException e){
				try {
					HibernateUtil.rollbackTransaction();
				} catch (DotHibernateException e1) {
					Logger.fatal(getClass(), "DotHibernateException: " + e1.getMessage());
				}
					Logger.error(getClass(), "Errore scatenato: " + e.getClass());				
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	/**
	 * Nuova view del cluster.
	 */
	public void viewAccepted(View new_view) {
		Logger.info(this, "Method view: 	Cluster View is : " + new_view);
		Logger.info(this, "viewAccepted + 	Cluster View is : " + new_view);
		
		// memorizzo la nuova view all'interno del singleton + eventuali controlli sull'azione da intraprendere
		HealthChecker.INSTANCE.getHealth().setClusterView(new_view);
		
		// controllo se vengo da un suspect...
		if(null!=HealthChecker.INSTANCE.getHealth().getAddress()){
			// resetto il count per evitare continue chiamate al suspect senza un viewAccepted.
			HealthChecker.INSTANCE.setCountSuspect(0);
			HealthChecker.INSTANCE.getHealth().setStatus(AddressStatus.LEAVE);
			// scrivo su DB
			try{
				boolean isCreator = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView().getCreator().equals(HealthChecker.INSTANCE.getHealth().getAddress());
				HibernateUtil.startTransaction();
				healthAPI.storeHealthStatus(HealthChecker.INSTANCE.getHealth());
				healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getHealth().getAddress(),AddressStatus.JOIN);
				healthAPI.insertHealthClusterView(HealthChecker.INSTANCE.getHealth().getAddress(),
						Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),isCreator, 
						HealthChecker.INSTANCE.getHealth().getStatus());
				HealthChecker.INSTANCE.flush();
				HibernateUtil.commitTransaction();
			}catch(DotDataException e){
				try {
					HibernateUtil.rollbackTransaction();
				} catch (DotHibernateException e1) {
					Logger.fatal(getClass(), "DotHibernateException: " + e1.getMessage());
				}
				Logger.error(getClass(), "Errore scatenato: " + e.getClass());				
			}
		}else { 
			// recupero la lista degli indirizzi joinati
			List<Address> joined = HealthUtil.getJoined(new_view);
			Logger.info(getClass(), "Joined size: " + joined.size());
			if(joined.size()>0){
				long now = new GregorianCalendar().getTimeInMillis();
				/**
				 * Il controllo sugli indirizzi "joinati" viene fatto solo ed esclusivamente partendo dal presupposto che questi indirizzi
				 * siano presenti in tabella nello stato "LEAVE". 
				 * 
				 * Per gli indirizzi che si joinano per la prima volta questo non è il punto in cui vengono inseriti, ma nella HealthServlet
				 * che in init inserisce la nuova riga.
				 * 
				 * Viene effettuato anche un controllo sul tempo di rientro del nodo...se è maggiore di 5 minuti allora non viene fatto rientrare
				 * nel cluster ma viene riavviato.
				 * 
				 */
				for(Address newone:joined){
					try{
						
						Logger.info(getClass(), "Data corrente: " + now);
						
						long lastLeaveTime = healthAPI.getDateOfLastLeaveEvent(newone);
						Logger.info(getClass(), "Last Leave Time: " + lastLeaveTime);
						if(now-lastLeaveTime<=MAX_REJOIN_TIME) {
							Logger.info(getClass(), "Posso procedere con il re-inserimento nel cluster...");
							HealthChecker.INSTANCE.getHealth().setAddress(newone);
							HealthChecker.INSTANCE.getHealth().setStatus(AddressStatus.JOIN);
							HealthChecker.INSTANCE.getHealth().setClusterView(new_view);
							HealthChecker.INSTANCE.getHealth().setWrittenBy(channel.getLocalAddress());
							// controllo se nell'altro cluster lo stesso nodo è stato joinato
							boolean ctrl = true;
							while(ctrl){
								if(!HealthUtil.containsMember(CacheLocator.getCacheAdministrator().getJGroupsChannel().getView(),
										HealthChecker.INSTANCE.getHealth().getAddress())) {
									try {
										Logger.info(getClass(), "Il nodo " + HealthChecker.INSTANCE.getHealth().getAddress() + " non e' inserito completamente nel cluster...");
										Thread.sleep(2000);
									} catch (InterruptedException e) {
										Logger.error(getClass(), "Errore in wait");
										ctrl = false;
									}
								}else{							
							        try{
							        	Logger.info(getClass(), "Pronto per reinserire il nodo nel cluster.");
							        	/**
							        	 * In questo caso il nodo è rientrato anche nel canale principale e quindi posso procedere a:
							        	 * 
							        	 *  1. Recupero della riga contenente lo stato;
							        	 *  2. Inserire la riga contenente il nuovo stato di JOIN;
							        	 *  3. Inserire la riga nella tabella dello status per l'amministrazione da backend;
							        	 *  4. Eliminazione delle precedenti righe contenenti lo stato LEAVE (in questo modo il nodo sa di essere nel
							        	 *     cluster nuovamente;
							        	 *  5. Chiamata al suo servizio REST per il flush della cache e quindi per il riallineamento.   
							        	 */
							        	HealthClusterViewStatus status = healthAPI.singleClusterView(HealthChecker.INSTANCE.getHealth().getAddress());
							        	HibernateUtil.startTransaction();					        	
										healthAPI.storeHealthStatus(HealthChecker.INSTANCE.getHealth());
										healthAPI.insertHealthClusterView(HealthChecker.INSTANCE.getHealth().getAddress(),
												Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),status.isCreator(),
												HealthChecker.INSTANCE.getHealth().getStatus());
										healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getHealth().getAddress(), AddressStatus.LEAVE);
										HibernateUtil.commitTransaction();					        						        	
										Logger.info(getClass(), "Node "+HealthChecker.INSTANCE.getHealth().getAddress()+" back into the cluster: flushing cache...");
								        String response = HealthUtil.callRESTService(status,"/joinCluster");
								        if("OK".equals(response))
								        	Logger.info(getClass(), "Cache on node "+HealthChecker.INSTANCE.getHealth().getAddress()+" successful flushed!");								
									}catch(DotDataException e){
										try {
											HibernateUtil.rollbackTransaction();
										} catch (DotHibernateException e1) {
											Logger.fatal(getClass(), "DotHibernateException: " + e1.getMessage());
										}
										Logger.error(getClass(), "Errore scatenato: " + e.getClass(),e);
									}						      
							        ctrl = false;
								}
							}						
						}else{
							try {
								HealthClusterViewStatus status = healthAPI.singleClusterView(HealthChecker.INSTANCE.getHealth().getAddress());
								Logger.info(getClass(), "Il nodo " + newone + " sta tentando di rientrare nel cluster dopo aver superato il tempo massimo di attesa (5 minuti): chiamo il restart...");
								HealthUtil.callRESTService(status, "/forceJoinCluster");
							} catch (DotDataException e) {
								Logger.error(getClass(), "Errore scatenato: " + e.getClass(),e);
								HealthChecker.INSTANCE.flush();
							}
						}
						HealthChecker.INSTANCE.flush();
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}	
	
	public JChannel getJGroupsHealthChannel(){
		return channel;
	}

	public boolean isCluster() {
		return cluster;
	}

	public void setCluster(boolean cluster) {
		this.cluster = cluster;
	}
	
}
