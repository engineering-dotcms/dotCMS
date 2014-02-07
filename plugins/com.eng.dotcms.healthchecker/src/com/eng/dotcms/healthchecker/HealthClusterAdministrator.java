package com.eng.dotcms.healthchecker;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import com.dotcms.rest.HealthService;
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
	private static int 	MAX_COUNT_SUSPECT = Config.getIntProperty("HEALTH_CHECKER_MAX_COUNT_SUSPECT", 5);
	private static long MAX_REJOIN_TIME = Config.getIntProperty("HEALTH_CHECKER_MAX_REJOIN_TIME", 60000);
	private static int 	MAX_COUNT_COMPLETELY_REJOIN = Config.getIntProperty("HEALTH_CHECKER_MAX_COUNT_COMPLETELY_REJOIN", 60);
	
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
		try{
			boolean amIOut = healthAPI.isLeaveNode(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress());
			HealthClusterViewStatus status = healthAPI.singleClusterView(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress());
			String resp = HealthUtil.callRESTService(status, "/checkNetwork");
			if(!amIOut && !HealthService.SOCKET_EXC.equals(resp)) {
				boolean isInLock = healthAPI.isHealthLock(mbr, Operation.FLUSHING) || healthAPI.isHealthLock(mbr, Operation.JOINING);
				if(!isInLock){
					boolean isAlreadyLeave = healthAPI.isLeaveNode(mbr);
					if(isAlreadyLeave) {
						Logger.info(getClass(), "Method suspect:  The node " + mbr + " is already out of cluster and I can't re-join. Try to force restart...");
						boolean isInRestart = healthAPI.isHealthLock(mbr, Operation.RESTARTING);
						if(!isInRestart){
							status = healthAPI.singleClusterView(mbr);
							healthAPI.insertHealthLock(mbr, Operation.RESTARTING);
							Map<String, String> params = new HashMap<String, String>();
							params.put("lock", "OK");
							String response = HealthUtil.callRESTService(status, "/forceJoinCluster");
							if(HealthService.STATUS_OK.equals(response))					
								Logger.info(getClass(), "Method suspect:  The node " + mbr + " was successful restarted...it will come back into the cluster as soon as possible.");
							else {
								Logger.info(getClass(), "Method suspect:  I can't call the REST service on node " + mbr + ".Cause: "+response);
								healthAPI.deleteHealthLock(mbr, Operation.RESTARTING);
							}
								
						}else
							Logger.info(getClass(), "Method suspect:  The node " + mbr + " is already in restart. Waiting the end of the operation.");
					}else{			
						Date now = new GregorianCalendar().getTime();
						int countSuspect = HealthChecker.INSTANCE.getCountSuspect();
						if(countSuspect<=MAX_COUNT_SUSPECT) {
							countSuspect++;
							// memorizzo il suspect all'interno del singleton
							Logger.info(this, "Method suspect: 	There is a suspected member : " + mbr);
							Logger.info(this, "suspect + 		There is a suspected member : " + mbr);
							HealthChecker.INSTANCE.getHealthEvent().setAddress(mbr);
							HealthChecker.INSTANCE.getHealthEvent().setWrittenBy(channel.getLocalAddress());
							HealthChecker.INSTANCE.getHealthEvent().setModDate(now);
							HealthChecker.INSTANCE.setCountSuspect(countSuspect);
						}else{ // raggiunto il limite di suspect accettate senza ricevere una view...il nodo è dichiarato fuori dal cluster.
							HealthChecker.INSTANCE.setCountSuspect(0);
							Logger.warn(getClass(), "Max number of suspect calls exceeded ("+MAX_COUNT_SUSPECT+"): the node is out of cluster.");
							// resetto il count per evitare continue chiamate al suspect senza un viewAccepted.
							HealthChecker.INSTANCE.getHealthEvent().setStatus(AddressStatus.LEFT);
							// scrivo su DB
							try{
								boolean isCreator = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView().getCreator().equals(HealthChecker.INSTANCE.getHealthEvent().getAddress());
								HibernateUtil.startTransaction();
								healthAPI.storeHealthStatus(HealthChecker.INSTANCE.getHealthEvent());
								healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getHealthEvent().getAddress(),AddressStatus.JOIN);
								healthAPI.insertHealthClusterView(HealthChecker.INSTANCE.getHealthEvent().getAddress(),
										Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),isCreator, 
										HealthChecker.INSTANCE.getHealthEvent().getStatus(), HealthChecker.INSTANCE.getHealthEvent().getModDate(), false);
								HealthChecker.INSTANCE.flush();
								HibernateUtil.commitTransaction();
							}catch(DotDataException e){
								try {
									HibernateUtil.rollbackTransaction();
								} catch (DotHibernateException e1) {
									Logger.fatal(getClass(), "DotHibernateException: " + e1.getMessage());
								}
//									Logger.error(getClass(), "Error: " + e.getClass()+": "+e.getMessage());			
							}
						}	
					}
				}else
					Logger.info(getClass(), "Method suspect:  The node "+ mbr +" is in flushing due a previous suspect.");
			}			
		}catch(DotDataException e){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.fatal(getClass(), "DotHibernateException: " + e1.getMessage());
			}
//			Logger.error(getClass(), "Error: " + e.getClass()+": "+e.getMessage());			
		}

	}

	@SuppressWarnings("deprecation")
	@Override
	/**
	 * Nuova view del cluster.
	 */
	public void viewAccepted(View new_view) {
		boolean amIOut = false;
		HealthClusterViewStatus _status = new HealthClusterViewStatus();
		try {
			amIOut = healthAPI.isLeaveNode(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress());
			_status = healthAPI.singleClusterView(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress());
		} catch (DotDataException e2) {}
		String resp = HealthUtil.callRESTService(_status, "/checkNetwork");
		if(!amIOut && !HealthService.SOCKET_EXC.equals(resp)) {
			Date now = new GregorianCalendar().getTime();
			Logger.info(this, "Method view: 	Cluster View is : " + new_view);
			Logger.info(this, "viewAccepted + 	Cluster View is : " + new_view);
			
			// memorizzo la nuova view all'interno del singleton + eventuali controlli sull'azione da intraprendere
			HealthChecker.INSTANCE.getHealthEvent().setClusterView(new_view);
			
			// controllo se vengo da un suspect...
			if(null!=HealthChecker.INSTANCE.getHealthEvent().getAddress()){
				
				// resetto il count per evitare continue chiamate al suspect senza un viewAccepted.
				HealthChecker.INSTANCE.setCountSuspect(0);
				HealthChecker.INSTANCE.getHealthEvent().setStatus(AddressStatus.LEFT);
				// scrivo su DB
				try{
					HealthClusterViewStatus status = healthAPI.singleClusterView(HealthChecker.INSTANCE.getHealthEvent().getAddress());
					boolean isCreator = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getView().getCreator().equals(HealthChecker.INSTANCE.getHealthEvent().getAddress());
					HibernateUtil.startTransaction();
					healthAPI.storeHealthStatus(HealthChecker.INSTANCE.getHealthEvent());
					healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getHealthEvent().getAddress(),AddressStatus.JOIN);
					healthAPI.insertHealthClusterView(HealthChecker.INSTANCE.getHealthEvent().getAddress(),
							Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),isCreator, 
							HealthChecker.INSTANCE.getHealthEvent().getStatus(), now, false);
					HibernateUtil.commitTransaction();
					if(Config.getBooleanProperty("HEALTH_CHECKER_LOCK_REMOTE_PUBLISH",false)){
						Logger.info(getClass(), "The node "+HealthChecker.INSTANCE.getHealthEvent().getAddress()+" is an endpoint for remote publish: lock it");
						String response = HealthUtil.callRESTService(status, "/lockRemotePublish");
						if(HealthService.STATUS_OK.equals(response))
							Logger.info(getClass(), HealthChecker.INSTANCE.getHealthEvent().getAddress()+" Locked.");
						else{
							Logger.info(getClass(), "I can't call the REST service on node " + HealthChecker.INSTANCE.getHealthEvent().getAddress() + ".Cause: "+response);
							HealthChecker.INSTANCE.flush();
						}
					}
					HealthChecker.INSTANCE.flush();
				}catch(DotDataException e){
					HealthChecker.INSTANCE.flush();
					try {
						HibernateUtil.rollbackTransaction();
					} catch (DotHibernateException e1) {
						Logger.fatal(getClass(), "DotHibernateException: " + e1.getMessage());
					}
//					Logger.error(getClass(), "Error: " + e.getClass()+": "+e.getMessage());				
				}
			}else { 
				// recupero la lista degli indirizzi joinati
				List<Address> joined = HealthUtil.getJoined(new_view);
				Logger.debug(getClass(), "Joined size: " + joined.size());
				if(joined.size()>0){
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
							boolean isInLock = healthAPI.isHealthLock(newone, Operation.FLUSHING) || healthAPI.isHealthLock(newone, Operation.STARTING) || healthAPI.isHealthLock(newone, Operation.JOINING);						
							if(!isInLock){
								Date lastLeave = healthAPI.getDateOfLastLeaveEvent(newone);
								long diffInMilliseconds = HealthUtil.getDateDiff(now, lastLeave, TimeUnit.MILLISECONDS);							
								if(diffInMilliseconds<=MAX_REJOIN_TIME && diffInMilliseconds>0) {
									Logger.info(getClass(), "The node " + newone + " is back in time...I can rejoin into the cluster.");
									HealthChecker.INSTANCE.getHealthEvent().setAddress(newone);
									HealthChecker.INSTANCE.getHealthEvent().setStatus(AddressStatus.JOIN);
									HealthChecker.INSTANCE.getHealthEvent().setClusterView(new_view);
									HealthChecker.INSTANCE.getHealthEvent().setWrittenBy(channel.getLocalAddress());
									HealthChecker.INSTANCE.getHealthEvent().setModDate(now);
									
									// controllo se nell'altro cluster lo stesso nodo è stato joinato
									boolean ctrl = true;
									int limit = 0;
									healthAPI.insertHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.JOINING);
									while(ctrl){
										if(limit>=MAX_COUNT_COMPLETELY_REJOIN){
											healthAPI.deleteHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.JOINING);
											Logger.info(getClass(), "Exceeded the max number of times (10) which I can wait until the node rejoins with all the JGroups channels. Try to restart it...");
											boolean isInRestart = healthAPI.isHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.RESTARTING);
											if(!isInRestart){
												HealthClusterViewStatus status = healthAPI.singleClusterView(HealthChecker.INSTANCE.getHealthEvent().getAddress());
												healthAPI.insertHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.RESTARTING);
												Map<String, String> params = new HashMap<String, String>();
												params.put("lock", "OK");
												String response = HealthUtil.callRESTService(status, "/forceJoinCluster");
												if(HealthService.STATUS_OK.equals(response))					
													Logger.info(getClass(), "The node " + HealthChecker.INSTANCE.getHealthEvent().getAddress() + " was successful restarted...it will come back into the cluster as soon as possible.");
												else{
													Logger.info(getClass(), "I can't call the REST service on node " + HealthChecker.INSTANCE.getHealthEvent().getAddress() + ".Cause: "+response);
													healthAPI.deleteHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.RESTARTING);
												}
											}
											break;										
										}else{
											if(!HealthUtil.containsMember(CacheLocator.getCacheAdministrator().getJGroupsChannel().getView(),
													HealthChecker.INSTANCE.getHealthEvent().getAddress())) {										
												try {
													Logger.info(getClass(), "The node " + HealthChecker.INSTANCE.getHealthEvent().getAddress() + " is not yet into the cluster completely...");
													Thread.sleep(2000);
												} catch (InterruptedException e) {
													Logger.error(getClass(), "Errore in wait");
													ctrl = false;
												}
											}else{							
										        try{
										        	Logger.info(getClass(), "Ready to rejoin the node.");
										        	healthAPI.deleteHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.JOINING);
										        	/**
										        	 * In questo caso il nodo è rientrato anche nel canale principale e quindi posso procedere a:
										        	 *   
										        	 *  1. Chiamata al suo servizio REST per il flush della cache e quindi per il riallineamento.
										        	 *  2. Recupero della riga contenente lo stato;
										        	 *  3. Inserire la riga contenente il nuovo stato di JOIN;
										        	 *  4. Inserire la riga nella tabella dello status per l'amministrazione da backend;
										        	 *  5. Eliminazione delle precedenti righe contenenti lo stato LEAVE (in questo modo il nodo sa di essere nel
										        	 *     cluster nuovamente;   
										        	 */
										        	HealthClusterViewStatus status = healthAPI.singleClusterView(HealthChecker.INSTANCE.getHealthEvent().getAddress());
										        	if(!Config.getBooleanProperty("HEALTH_CHECKER_ALWAYS_FLUSH_CACHE", true) && HealthUtil.needFlushCache(lastLeave, now)){
														Logger.info(getClass(), "Node "+HealthChecker.INSTANCE.getHealthEvent().getAddress()+" back into the cluster: flushing cache...");											
														healthAPI.insertHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.FLUSHING);
														Map<String, String> params = new HashMap<String, String>();
														params.put("unlock", "OK");
												        String response = HealthUtil.callRESTService(status,"/joinCluster");
												        if(HealthService.STATUS_OK.equals(response)){
												        	Logger.info(getClass(), "Cache on node "+HealthChecker.INSTANCE.getHealthEvent().getAddress()+" successful flushed!");								        	
												        	HibernateUtil.startTransaction();					        	
												        	healthAPI.deleteHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.FLUSHING);
															healthAPI.storeHealthStatus(HealthChecker.INSTANCE.getHealthEvent());
															healthAPI.insertHealthClusterView(HealthChecker.INSTANCE.getHealthEvent().getAddress(),
																	Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),status.isCreator(),
																	HealthChecker.INSTANCE.getHealthEvent().getStatus(),HealthChecker.INSTANCE.getHealthEvent().getModDate(),false);
															healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getHealthEvent().getAddress(), AddressStatus.LEFT);
															HibernateUtil.commitTransaction();	
												        }else{
												        	Logger.info(getClass(), "I can't call the REST service on node " + HealthChecker.INSTANCE.getHealthEvent().getAddress() + ".Cause: "+response);
												        	healthAPI.deleteHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.FLUSHING);
												        }
										        	}else{
										        		Logger.info(getClass(), "Node "+HealthChecker.INSTANCE.getHealthEvent().getAddress()+" back into the cluster but no update were made on cache. I don't flush it.");
										    			if(Config.getBooleanProperty("HEALTH_CHECKER_LOCK_REMOTE_PUBLISH",false)){
										    				Logger.info(getClass(), "The node "+HealthChecker.INSTANCE.getHealthEvent().getAddress()+" is an endpoint for remote publish: unlock it");
										    				String response = HealthUtil.callRESTService(status,"/unlockRemotePublish");
										    				if(HealthService.STATUS_OK.equals(response))
										    					Logger.info(getClass(), HealthChecker.INSTANCE.getHealthEvent().getAddress()+" Unlocked.");
										    			}
										        		HibernateUtil.startTransaction();					        	
											        	healthAPI.deleteHealthLock(HealthChecker.INSTANCE.getHealthEvent().getAddress(), Operation.FLUSHING);
														healthAPI.storeHealthStatus(HealthChecker.INSTANCE.getHealthEvent());
														healthAPI.insertHealthClusterView(HealthChecker.INSTANCE.getHealthEvent().getAddress(),
																Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),status.isCreator(),
																HealthChecker.INSTANCE.getHealthEvent().getStatus(),HealthChecker.INSTANCE.getHealthEvent().getModDate(),false);
														healthAPI.deleteHealthStatus(HealthChecker.INSTANCE.getHealthEvent().getAddress(), AddressStatus.LEFT);
														HibernateUtil.commitTransaction();	
										        	}
												}catch(DotDataException e){
													try {
														HibernateUtil.rollbackTransaction();
													} catch (DotHibernateException e1) {
														Logger.fatal(getClass(), "DotHibernateException: " + e1.getMessage());
													}
//													Logger.error(getClass(), "Error: " + e.getClass()+": "+e.getMessage());
												}						      
										        ctrl = false;
											}										
										}
										limit++;
									}
								}else{
									try {
										HealthClusterViewStatus status = healthAPI.singleClusterView(newone);
										long minutes = (MAX_REJOIN_TIME/1000/60);
										Logger.info(getClass(), "The node " + newone + " is trying to rejoin the cluster after it exceeded the max wait time ("+minutes+" minute/s): call the restart...");
										healthAPI.insertHealthLock(newone, Operation.RESTARTING);
										Map<String, String> params = new HashMap<String, String>();
										params.put("lock", "OK");
										String response = HealthUtil.callRESTService(status, "/forceJoinCluster");
										if(HealthService.STATUS_OK.equals(response))					
											Logger.info(getClass(), "The node " + newone + " was successful restarted...it will come back into the cluster as soon as possible.");
										else{
											Logger.info(getClass(), "I can't call the REST service on node " + newone + ".Cause: "+response);
											healthAPI.deleteHealthLock(newone, Operation.RESTARTING);
										}
									} catch (DotDataException e) {
//										Logger.error(getClass(), "Error: " + e.getClass()+": "+e.getMessage());
										HealthChecker.INSTANCE.flush();
									}
								}
								HealthChecker.INSTANCE.flush();
							}else
								Logger.info(getClass(), "Method view: 	The node "+ newone +" is in flushing due a previous suspect.");
						}catch(Exception e){
							e.printStackTrace();
						}
					}				
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
