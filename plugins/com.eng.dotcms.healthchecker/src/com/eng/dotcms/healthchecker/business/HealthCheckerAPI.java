package com.eng.dotcms.healthchecker.business;

import java.util.List;
import java.util.Map;

import org.jgroups.Address;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.Health;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_INSERT_HEALTH;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_DELETE_HEALTH;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CHECK_LEAVE;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_GET_NODE_LEAVE;

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
		try{
			dc.setSQL(ORACLE_INSERT_HEALTH);
			dc.addParam(health.getAddress().toString().split("[-]")[0]);
			dc.addParam(health.getClusterView().toString());
			dc.addParam(health.getStatus().toString());
			dc.addParam(health.getWrittenBy().toString().split("[-]")[0]);
			dc.loadResult();
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	/**
	 * Elimino lo stato dal DB
	 * 
	 * @param health
	 * @throws DotDataException
	 */
	public void deleteHealthStatus(Health health) throws DotDataException {
//		try{
			dc.setSQL(ORACLE_DELETE_HEALTH);
			dc.addParam(health.getAddress().toString().split("[-]")[0]);
			dc.addParam(health.getStatus().toString());
			dc.loadResult();
//		}finally{
//			DbConnectionFactory.closeConnection();
//		}
	}
	
	public void deleteHealthStatus(Address address, AddressStatus status) throws DotDataException {
//		try{
			dc.setSQL(ORACLE_DELETE_HEALTH);
			dc.addParam(address.toString().split("[-]")[0]);
			dc.addParam(status.toString());
			dc.loadResult();
//		}finally{
//			DbConnectionFactory.closeConnection();
//		}
	}
	
	/**
	 * Restituisce il numero di nodi che hanno lasciato il cluster ad un certo momento.
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public int countLeave() {
		Logger.debug(getClass(), "Sono nel Leave");
		int count = 0;
		try{
			dc.setSQL(ORACLE_CHECK_LEAVE);
			List<Map<String, Object>> map = dc.loadObjectResults();
			if(map.size()>0){
				Logger.debug(getClass(), "Numero di righe: " + map.size());
				Logger.debug(getClass(), "MAPPA: " + map.get(0));
				Logger.debug(getClass(), "VALORE NUM_LEAVE: " + map.get(0).get("num_leave"));
				count = Integer.parseInt(map.get(0).get("num_leave").toString());
				Logger.debug(getClass(), "Numero di istanze in LEAVE nel DB: " + count);
			}
			return count;			
		}catch(DotDataException e) {
			Logger.error(getClass(), "ERRORE DOTDATA", e);
			return -1;
		}catch(Exception e){
			Logger.error(getClass(), "ERRORE GENERICO", e);
			return -1;
		}
//		finally{
//			DbConnectionFactory.closeConnection();
//		}
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
			dc.addParam(address.toString().split("[-]")[0]);
			return dc.loadObjectResults().size()>0;
		}catch(DotDataException e){
			return false;
		}
	}
	
}
