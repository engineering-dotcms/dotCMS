package com.eng.dotcms.healthchecker.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jgroups.Address;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.Health;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.util.HealthUtil;

import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_INSERT_HEALTH;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_DELETE_HEALTH;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CHECK_LEAVE;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_GET_NODE_LEAVE;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_INSERT_HEALTH_CLUSTER_VIEW;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_GET_HEALTH_CLUSTER_VIEW_STATUS;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_GET_SINGLE_CLUSTER_VIEW_STATUS;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_DELETE_HEALTH_CLUSTER_VIEW;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_UPDATE_HEALTH_CLUSTER_CREATOR;

public class HealthCheckerAPI {
	
	private DotConnect dc = null;
	
	public HealthCheckerAPI(){
		dc = new DotConnect();
	}
	
	/**
	 * Memorizza lo stato attuale su DB
	 * 
	 * @param health
	 * @throws DotDataException
	 */
	public void storeHealthStatus(Health health) throws DotDataException {
		dc.setSQL(ORACLE_INSERT_HEALTH);
		dc.addParam(HealthUtil.getStringAddress(health.getAddress()));
		dc.addParam(health.getClusterView().toString());
		dc.addParam(health.getStatus().toString());
		dc.addParam(HealthUtil.getStringAddress(health.getWrittenBy()));
		dc.loadResult();
	}
	
	/**
	 * Elimino lo stato dal DB
	 * 
	 * @param health
	 * @throws DotDataException
	 */
	public void deleteHealthStatus(Health health) throws DotDataException {
		dc.setSQL(ORACLE_DELETE_HEALTH);
		dc.addParam(HealthUtil.getStringAddress(health.getAddress()));
		dc.addParam(health.getStatus().toString());
		dc.loadResult();
	}
	
	public void deleteHealthStatus(Address address, AddressStatus status) throws DotDataException {
		dc.setSQL(ORACLE_DELETE_HEALTH);
		dc.addParam(HealthUtil.getStringAddress(address));
		dc.addParam(status.toString());
		dc.loadResult();
	}
	
	/**
	 * Restituisce il numero di nodi che hanno lasciato il cluster ad un certo momento.
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public int countLeave() {
		int count = 0;
		try{
			dc.setSQL(ORACLE_CHECK_LEAVE);
			List<Map<String, Object>> map = dc.loadObjectResults();
			if(map.size()>0)
				count = Integer.parseInt(map.get(0).get("num_leave").toString());
			return count;			
		}catch(DotDataException e) {
			Logger.error(getClass(), "ERRORE DOTDATA", e);
			return -1;
		}catch(Exception e){
			Logger.error(getClass(), "ERRORE GENERICO", e);
			return -1;
		}
	}
	
	/**
	 * Dato un nodo controlla se è etichettato come fuori dal cluster.
	 * 
	 * @param address
	 * @return
	 * @throws DotDataException
	 */
	public boolean isLeaveNode(Address address) {
		try{
			dc.setSQL(ORACLE_GET_NODE_LEAVE);
			dc.addParam(HealthUtil.getStringAddress(address));
			return dc.loadObjectResults().size()>0;
		}catch(DotDataException e){
			return false;
		}
	}
	
	/**
	 * Restituisce la data in millisecondi dell'ultimo evento di leave del nodo.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 27, 2014
	 */
	public Date getDateOfLastLeaveEvent(Address address) {
		try{
			dc.setSQL(ORACLE_GET_NODE_LEAVE);
			dc.addParam(HealthUtil.getStringAddress(address));
			List<Map<String, Object>> rs = dc.loadObjectResults();
			if(rs.size()>0){
				Map<String, Object> row = rs.get(0);
				return (Date)row.get("mod_date");
			}
			return null;
		}catch(DotDataException e){
			return null;
		}
	}
	
	/**
	 * Memorizza il nuovo nodo nel cluster.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	public void insertHealthClusterView(Address address, String port, String protocol, boolean isCreator, AddressStatus status) throws DotDataException {
		dc.setSQL(ORACLE_INSERT_HEALTH_CLUSTER_VIEW);
		dc.addParam(UUID.randomUUID().toString());
		dc.addParam(HealthUtil.getStringAddress(address));
		dc.addParam(port);
		dc.addParam(protocol);
		dc.addParam(status.toString());
		dc.addParam(isCreator?"Y":"N");
		dc.loadResult();			
	}
	
	/**
	 * Aggiorna il coordinator della cluster view.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * @date Jan 22, 2014
	 */
	public void updateHealthClusterViewCreator(Address address, boolean isCreator) throws DotDataException {
		dc.setSQL(ORACLE_UPDATE_HEALTH_CLUSTER_CREATOR);
		dc.addParam(isCreator?"Y":"N");
		dc.addParam(HealthUtil.getStringAddress(address));
		dc.loadResult();			
	}
	
	public List<HealthClusterViewStatus> clusterView() throws DotDataException {
		List<HealthClusterViewStatus> result = new ArrayList<HealthClusterViewStatus>();
		dc.setSQL(ORACLE_GET_HEALTH_CLUSTER_VIEW_STATUS);
		List<Map<String, Object>> map = dc.loadObjectResults();
		for(Map<String, Object> record:map){
			HealthClusterViewStatus status = new HealthClusterViewStatus();
			status.setId((String)record.get("id"));
			status.setAddress((String)record.get("address"));
			status.setPort((String)record.get("port"));
			status.setProtocol((String)record.get("protocol"));
			status.setStatus((String)record.get("status"));			
			String creator = (String)record.get("creator");
			status.setCreator(creator.equals("Y")?true:false);
			status.setModDate((Date)record.get("mod_date"));
			result.add(status);
		}
		return result;
	}
	
	public HealthClusterViewStatus singleClusterView(Address address) throws DotDataException {
		HealthClusterViewStatus status = new HealthClusterViewStatus();
		dc.setSQL(ORACLE_GET_SINGLE_CLUSTER_VIEW_STATUS);
		dc.addParam(HealthUtil.getStringAddress(address));
		List<Map<String, Object>> map = dc.loadObjectResults();
		if(map.size()>0){
			Map<String, Object> record = map.get(0);
			status.setId((String)record.get("id"));
			status.setAddress((String)record.get("address"));
			status.setPort((String)record.get("port"));
			status.setProtocol((String)record.get("protocol"));
			status.setStatus((String)record.get("status"));
			String creator = (String)record.get("creator");
			status.setCreator(creator.equals("Y")?true:false);
			status.setModDate((Date)record.get("mod_date"));
			return status;	
		}
		return null;	
	}
	
	public void deleteHealthClusterView(Address address) throws DotDataException {
		dc.setSQL(ORACLE_DELETE_HEALTH_CLUSTER_VIEW);
		dc.addParam(HealthUtil.getStringAddress(address));
		dc.loadResult();
	}
	
	public boolean isNodeInCluster(Address address) throws DotDataException {
		return !isLeaveNode(address);
	}
	
}
