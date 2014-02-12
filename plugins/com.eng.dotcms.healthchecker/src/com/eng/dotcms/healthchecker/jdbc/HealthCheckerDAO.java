package com.eng.dotcms.healthchecker.jdbc;

import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CHECK_LEFT;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CHECK_NEW_CONTAINER;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CHECK_NEW_CONTENTLET;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CHECK_NEW_HTMLPAGE;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CHECK_NEW_TEMPLATE;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_DELETE_HEALTH;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_DELETE_HEALTH_CLUSTER_VIEW;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_DELETE_LOCK;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_GET_ALL_SERVERS_IN_CLUSTER;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_GET_HEALTH_CLUSTER_VIEW_STATUS;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_GET_NODE_LEFT;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_GET_SINGLE_CLUSTER_VIEW_STATUS;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_INSERT_HEALTH;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_INSERT_HEALTH_CLUSTER_VIEW;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_INSERT_LOCK;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_SELECT_LOCK;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_UPDATE_HEALTH_CLUSTER_CREATOR;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_UPDATE_HEALTH_CLUSTER_VIEW_TIMER;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jgroups.Address;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.HealthClusterView;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.HealthEvent;
import com.eng.dotcms.healthchecker.Operation;
import com.eng.dotcms.healthchecker.jdbc.mapper.HealthClusterViewMapper;
import com.eng.dotcms.healthchecker.jdbc.mapper.HealthClusterViewStatusMapper;
import com.eng.dotcms.healthchecker.util.HealthUtil;

public class HealthCheckerDAO {
	
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private JdbcTemplate jdbcTemplate;
	
	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
		return namedParameterJdbcTemplate;
	}

	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}
	
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	/**
	 * Memorizza lo stato attuale su DB
	 * 
	 * @param health
	 * @throws DotDataException
	 */
	@Transactional
	public void storeHealthStatus(HealthEvent health) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", HealthUtil.getStringAddress(health.getAddress()));
		params.put("clusterView", health.getClusterView().toString());
		params.put("status", health.getStatus().toString());
		params.put("writtenBy", HealthUtil.getStringAddress(health.getWrittenBy()));
		params.put("modDate", health.getModDate());
		namedParameterJdbcTemplate.update(ORACLE_INSERT_HEALTH, params);
	}
	
	/**
	 * Elimino lo stato dal DB
	 * 
	 * @param health
	 * @throws DotDataException
	 */
	@Transactional
	public void deleteHealthStatus(HealthEvent health) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", HealthUtil.getStringAddress(health.getAddress()));
		params.put("status", health.getStatus().toString());
		namedParameterJdbcTemplate.update(ORACLE_DELETE_HEALTH, params);
	}
	
	@Transactional
	public void deleteHealthStatus(Address address, AddressStatus status) {
		deleteHealthStatus(HealthUtil.getStringAddress(address), status);
	}
	
	@Transactional
	public void deleteHealthStatus(String address, AddressStatus status) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", address);
		params.put("status", status.toString());
		namedParameterJdbcTemplate.update(ORACLE_DELETE_HEALTH, params);
	}
	
	/**
	 * Restituisce il numero di nodi che hanno lasciato il cluster in un certo momento.
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public int countLeft() {
		Map<String, Object> params = new HashMap<String, Object>();		
		params.put("status", AddressStatus.LEFT.toString());
		return namedParameterJdbcTemplate.queryForInt(ORACLE_CHECK_LEFT, params);
	}
	
	/**
	 * Dato un nodo controlla se è etichettato come fuori dal cluster.
	 * 
	 * @param address
	 * @return
	 * @throws DotDataException
	 */
	public boolean nodeHasLeft(Address address) {
		return nodeHasLeft(HealthUtil.getStringAddress(address));
	}
	
	/**
	 * Dato un nodo controlla se è etichettato come fuori dal cluster.
	 * 
	 * @param address
	 * @return
	 * @throws DotDataException
	 */
	public boolean nodeHasLeft(String address) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", address);
		List<HealthClusterView> views = namedParameterJdbcTemplate.query(ORACLE_GET_NODE_LEFT, params, new HealthClusterViewMapper());
		if(views.size()>0)
			return AddressStatus.LEFT.toString().equals(views.get(0).getStatus());				
		return false;
	}
	
	/**
	 * Restituisce la data in millisecondi dell'ultimo evento di leave del nodo.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 27, 2014
	 */
	public Date getDateOfLastLeaveEvent(Address address) {
		return getDateOfLastLeaveEvent(HealthUtil.getStringAddress(address));
	}
	
	/**
	 * Restituisce la data in millisecondi dell'ultimo evento di leave del nodo.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 27, 2014
	 */
	public Date getDateOfLastLeaveEvent(String address) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", address);
		List<HealthClusterView> views = namedParameterJdbcTemplate.query(ORACLE_GET_NODE_LEFT, params, new HealthClusterViewMapper());
		if(views.size()>0)
			return views.get(0).getModDate();
		return null;
	}	
	
	/**
	 * Memorizza il nuovo nodo nel cluster.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	@Transactional
	public void insertHealthClusterView(Address address, String port, String protocol, boolean isCreator, AddressStatus status, Date now, boolean isOutForTimer) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", UUID.randomUUID().toString());
		params.addValue("address", HealthUtil.getStringAddress(address));
		params.addValue("port", port);
		params.addValue("protocol", protocol);
		params.addValue("status", status.toString());
		params.addValue("creator", isCreator?"Y":"N");
		params.addValue("modDate", now);
		params.addValue("outForTimer", isOutForTimer?"Y":"N");
		namedParameterJdbcTemplate.update(ORACLE_INSERT_HEALTH_CLUSTER_VIEW, params);			
	}
	
	/**
	 * Aggiorno il campo out_for_timer.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	@Transactional
	public void updateHealthClusterView(Address address, Date now, boolean isOutForTimer) {
		updateHealthClusterView(HealthUtil.getStringAddress(address), now, isOutForTimer);
	}
	
	/**
	 * Aggiorno il campo out_for_timer.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	@Transactional
	public void updateHealthClusterView(String address, Date now, boolean isOutForTimer) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("outForTimer", isOutForTimer?"Y":"N");
		params.put("modDate", now);
		params.put("address", address);
		params.put("status", AddressStatus.LEFT.toString());
		namedParameterJdbcTemplate.update(ORACLE_UPDATE_HEALTH_CLUSTER_VIEW_TIMER, params);
	}
	
	/**
	 * Aggiorna il coordinator della cluster view.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	public void updateHealthClusterViewCreator(Address address, boolean isCreator) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("creator", isCreator?"Y":"N");
		params.put("address", HealthUtil.getStringAddress(address));
		namedParameterJdbcTemplate.update(ORACLE_UPDATE_HEALTH_CLUSTER_CREATOR, params);
	}
	
	public List<HealthClusterViewStatus> clusterView() {
		return jdbcTemplate.query(ORACLE_GET_HEALTH_CLUSTER_VIEW_STATUS, new HealthClusterViewStatusMapper());		
	}
	
	public HealthClusterViewStatus singleClusterView(Address address) {
		return singleClusterView(HealthUtil.getStringAddress(address));
	}
	
	public HealthClusterViewStatus singleClusterView(String address) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", address);
		List<HealthClusterViewStatus> status = namedParameterJdbcTemplate.query(ORACLE_GET_SINGLE_CLUSTER_VIEW_STATUS, params, new HealthClusterViewStatusMapper());
		return status.size()>0?status.get(0):null;
	}
	
	/**
	 * Dato un Address elimina tutte le righe nella tabella HEALTH_CLUSTER_VIEW
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 28, 2014
	 */
	public void deleteHealthClusterView(Address address) {
		deleteHealthClusterView(HealthUtil.getStringAddress(address));
	}
	
	/**
	 * Dato un Address elimina tutte le righe nella tabella HEALTH_CLUSTER_VIEW
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 28, 2014
	 */
	public void deleteHealthClusterView(String address) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", address);
		namedParameterJdbcTemplate.update(ORACLE_DELETE_HEALTH_CLUSTER_VIEW, params);
	}
	
	public void insertHealthLock(Address address, Operation op) {
		insertHealthLock(HealthUtil.getStringAddress(address), op);
	}
	
	public void insertHealthLock(String address, Operation op) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", address);
		params.put("operation", op.toString());
		namedParameterJdbcTemplate.update(ORACLE_INSERT_LOCK, params);
	}
	@Transactional
	public void deleteHealthLock(Address address, Operation op) {
		deleteHealthLock(HealthUtil.getStringAddress(address), op);
	}
	
	@Transactional
	public void deleteHealthLock(String address, Operation op) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", address);
		params.put("operation", op.toString());
		namedParameterJdbcTemplate.update(ORACLE_DELETE_LOCK, params);
	}
	
	public boolean isHealthLock(Address address, Operation op) {
		return isHealthLock(HealthUtil.getStringAddress(address), op);		
	}
	
	public boolean isHealthLock(String address, Operation op) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", address);
		params.put("operation", op.toString());
		return namedParameterJdbcTemplate.queryForInt(ORACLE_SELECT_LOCK, params)>0;
	}
	
	public int checkContentlet(Date leaveDate, Date joinDate) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("bottomDate", leaveDate);
		params.put("topDate", joinDate);
		return namedParameterJdbcTemplate.queryForInt(ORACLE_CHECK_NEW_CONTENTLET, params);
	}

	public int checkContainer(Date leaveDate, Date joinDate) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("bottomDate", leaveDate);
		params.put("topDate", joinDate);
		return namedParameterJdbcTemplate.queryForInt(ORACLE_CHECK_NEW_CONTAINER, params);
		
	}
	
	public int checkHtmlPage(Date leaveDate, Date joinDate) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("bottomDate", leaveDate);
		params.put("topDate", joinDate);
		return namedParameterJdbcTemplate.queryForInt(ORACLE_CHECK_NEW_HTMLPAGE, params);

	}

	public int checkTemplate(Date leaveDate, Date joinDate) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("bottomDate", leaveDate);
		params.put("topDate", joinDate);
		return namedParameterJdbcTemplate.queryForInt(ORACLE_CHECK_NEW_TEMPLATE, params);
	}
	
	@SuppressWarnings("deprecation")
	public List<String> getAllServersInClusterExceptMe() {
		String myself = HealthUtil.getStringAddress(HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("address", myself);
		return namedParameterJdbcTemplate.queryForList(ORACLE_GET_ALL_SERVERS_IN_CLUSTER, params, String.class);
	}
	
}
