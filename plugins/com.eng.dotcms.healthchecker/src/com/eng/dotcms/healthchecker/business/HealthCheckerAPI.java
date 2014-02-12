package com.eng.dotcms.healthchecker.business;

import java.util.Date;
import java.util.List;
import org.jgroups.Address;
import org.springframework.transaction.annotation.Transactional;

import com.dotmarketing.util.Config;
import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.HealthEvent;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.Operation;
import com.eng.dotcms.healthchecker.jdbc.HealthCheckerDAO;
import com.eng.dotcms.healthchecker.util.HealthUtil;

public class HealthCheckerAPI {
	
	private HealthCheckerDAO healthDAO;
	
	public HealthCheckerAPI(){}
	
	/**
	 * Memorizza lo stato attuale su DB
	 * 
	 * @param health
	 * @throws DotDataException
	 */
	public void storeHealthStatus(HealthEvent health) {
		healthDAO.storeHealthStatus(health);
	}
	
	/**
	 * Elimino lo stato dal DB
	 * 
	 * @param health
	 * @throws DotDataException
	 */
	public void deleteHealthStatus(HealthEvent health) {
		healthDAO.deleteHealthStatus(health);
	}
	
	public void deleteHealthStatus(Address address, AddressStatus status) {
		healthDAO.deleteHealthStatus(HealthUtil.getStringAddress(address), status);
	}
	
	public void deleteHealthStatus(String address, AddressStatus status) {
		healthDAO.deleteHealthStatus(address, status);
	}
	
	/**
	 * Restituisce il numero di nodi che hanno lasciato il cluster ad un certo momento.
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public int countLeft() {
		return healthDAO.countLeft();
	}
	
	/**
	 * Dato un nodo controlla se è etichettato come fuori dal cluster.
	 * 
	 * @param address
	 * @return
	 * @throws DotDataException
	 */
	public boolean nodeHasLeft(Address address) {
		return healthDAO.nodeHasLeft(HealthUtil.getStringAddress(address));
	}
	
	/**
	 * Dato un nodo controlla se è etichettato come fuori dal cluster.
	 * 
	 * @param address
	 * @return
	 * @throws DotDataException
	 */
	public boolean nodeHasLeft(String address) {
		return healthDAO.nodeHasLeft(address);
	}
	
	/**
	 * Restituisce la data in millisecondi dell'ultimo evento di leave del nodo.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 27, 2014
	 */
	public Date getDateOfLastLeaveEvent(Address address) {
		return healthDAO.getDateOfLastLeaveEvent(HealthUtil.getStringAddress(address));
	}
	
	/**
	 * Restituisce la data in millisecondi dell'ultimo evento di leave del nodo.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 27, 2014
	 */
	public Date getDateOfLastLeaveEvent(String address) {
		return healthDAO.getDateOfLastLeaveEvent(address);	
	}	
	
	/**
	 * Memorizza il nuovo nodo nel cluster.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	public void insertHealthClusterView(Address address, String port, String protocol, boolean isCreator, AddressStatus status, Date now, boolean isOutForTimer) {
		healthDAO.insertHealthClusterView(address, port, protocol, isCreator, status, now, isOutForTimer);
	}
	
	/**
	 * Aggiorno il campo out_for_timer.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	public void updateHealthClusterView(Address address, Date now, boolean isOutForTimer) {
		healthDAO.updateHealthClusterView(HealthUtil.getStringAddress(address), now, isOutForTimer);
	}
	
	/**
	 * Aggiorno il campo out_for_timer.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	public void updateHealthClusterView(String address, Date now, boolean isOutForTimer) {
		healthDAO.updateHealthClusterView(address, now, isOutForTimer);			
	}
	
	/**
	 * Aggiorna il coordinator della cluster view.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	public void updateHealthClusterViewCreator(Address address, boolean isCreator) {
		healthDAO.updateHealthClusterViewCreator(address, isCreator);		
	}
	
	public List<HealthClusterViewStatus> clusterView() {
		return healthDAO.clusterView();
	}
	
	public HealthClusterViewStatus singleClusterView(Address address) {
		return healthDAO.singleClusterView(HealthUtil.getStringAddress(address));
	}
	
	public HealthClusterViewStatus singleClusterView(String address) {
		return healthDAO.singleClusterView(address);
	}
	
	
	/**
	 * Dato un Address elimina tutte le righe nella tabella HEALTH_CLUSTER_VIEW
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 28, 2014
	 */
	public void deleteHealthClusterView(Address address) {
		healthDAO.deleteHealthClusterView(HealthUtil.getStringAddress(address));
	}
	
	/**
	 * Dato un Address elimina tutte le righe nella tabella HEALTH_CLUSTER_VIEW
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 28, 2014
	 */
	public void deleteHealthClusterView(String address) {
		healthDAO.deleteHealthClusterView(address);
	}
	
	public void insertHealthLock(Address address, Operation op) {
		healthDAO.insertHealthLock(HealthUtil.getStringAddress(address), op);
	}
	
	public void insertHealthLock(String address, Operation op) {
		healthDAO.insertHealthLock(address, op);
	}
	
	public void deleteHealthLock(Address address, Operation op) {
		healthDAO.deleteHealthLock(HealthUtil.getStringAddress(address), op);
	}
	
	public void deleteHealthLock(String address, Operation op) {
		healthDAO.deleteHealthLock(address, op);
	}
	
	public boolean isHealthLock(Address address, Operation op) {
		return healthDAO.isHealthLock(HealthUtil.getStringAddress(address), op);		
	}
	
	public boolean isHealthLock(String address, Operation op) {
		return healthDAO.isHealthLock(address, op);		
	}
	
	public int checkContentlet(Date leaveDate, Date joinDate) {
		return healthDAO.checkContentlet(leaveDate, joinDate);
	}

	public int checkContainer(Date leaveDate, Date joinDate) {
		return healthDAO.checkContainer(leaveDate, joinDate);		
	}
	
	public int checkHtmlPage(Date leaveDate, Date joinDate) {
		return healthDAO.checkHtmlPage(leaveDate, joinDate);
	}

	public int checkTemplate(Date leaveDate, Date joinDate) {
		return healthDAO.checkTemplate(leaveDate, joinDate);
	}
	
	public List<String> getAllServersInClusterExceptMe() {
		return healthDAO.getAllServersInClusterExceptMe();	
	}
	
	@Transactional
	public void cleanNode(Address localAddress) {
		deleteHealthStatus(localAddress, AddressStatus.LEFT);
		deleteHealthStatus(localAddress, AddressStatus.JOIN);
		deleteHealthClusterView(localAddress);
		deleteHealthLock(localAddress, Operation.RESTARTING);
		deleteHealthLock(localAddress, Operation.FLUSHING);
		deleteHealthLock(localAddress, Operation.JOINING);
		deleteHealthLock(localAddress, Operation.STARTING);
	}
	
	@Transactional
	public void cleanNode(String localAddress) {
		deleteHealthStatus(localAddress, AddressStatus.LEFT);
		deleteHealthStatus(localAddress, AddressStatus.JOIN);
		deleteHealthClusterView(localAddress);
		deleteHealthLock(localAddress, Operation.RESTARTING);
		deleteHealthLock(localAddress, Operation.FLUSHING);
		deleteHealthLock(localAddress, Operation.JOINING);
		deleteHealthLock(localAddress, Operation.STARTING);
	
	}

	@Transactional
	public void markOutForTimer(String server, HealthClusterViewStatus status, Date now) {
		deleteHealthStatus(server, AddressStatus.LEFT);
		deleteHealthStatus(server, AddressStatus.JOIN);
		updateHealthClusterView(status.getAddress(), now, true);
		deleteHealthLock(server, Operation.RESTARTING);
		deleteHealthLock(server, Operation.FLUSHING);
		deleteHealthLock(server, Operation.JOINING);
	}
	
	@Transactional
	public void markOutOfCluster(HealthEvent event, boolean isCreator, Date date) {
		storeHealthStatus(event);
		deleteHealthStatus(event.getAddress(),AddressStatus.JOIN);
		insertHealthClusterView(event.getAddress(), Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),isCreator, 
				event.getStatus(), date, false);
	}
	
	@Transactional
	public void rejoin(HealthEvent event, boolean isCreator, Operation op, Date now) {
		deleteHealthLock(event.getAddress(), op);
		storeHealthStatus(event);
		insertHealthClusterView(event.getAddress(),
				Config.getStringProperty("HEALTH_CHECKER_REST_PORT","80"),Config.getStringProperty("HEALTH_CHECKER_REST_PROTOCOL","http"),isCreator,
				event.getStatus(), now,false);
		deleteHealthStatus(event.getAddress(), AddressStatus.LEFT);
	}
	
	public HealthCheckerDAO getHealthDAO() {
		return healthDAO;
	}

	public void setHealthDAO(HealthCheckerDAO healthDAO) {
		this.healthDAO = healthDAO;
	}
}
