package com.eng.dotcms.healthchecker.viewtool;

import java.util.Map;

import org.apache.velocity.tools.view.tools.ViewTool;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.jgroups.JChannel;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;

public class HealthCheckerTool implements ViewTool {
	
	private JChannel healthClusterChannel, cacheChannel;
	private HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	
	@Override
	public void init(Object initData) {
		healthClusterChannel = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel();
		cacheChannel = CacheLocator.getCacheAdministrator().getJGroupsChannel();
	}
	
	public boolean checkHealth(){
		return checkCacheStatus();
	}
	
	@SuppressWarnings("deprecation")
	public String getAddress(){
		return cacheChannel.getLocalAddress().toString();
	}
	
	/**
	 * Questo metodo controlla se il nodo in esame è nel cluster jGroups
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private boolean checkCacheStatus(){
		boolean cacheHealth = !healthAPI.isLeaveNode(healthClusterChannel.getLocalAddress());
		Logger.info(getClass(), "Cluster View  (Health): 	"+healthClusterChannel.getView().toString());
		Logger.info(getClass(), "Local Address (Health): 	"+healthClusterChannel.getLocalAddress().toString());
		Logger.info(getClass(), "Cluster View  (Cache): 	"+cacheChannel.getView().toString());
		Logger.info(getClass(), "Local Address (Cache): 	"+cacheChannel.getLocalAddress().toString());
		boolean esHealth = true;
		Map<String, ClusterIndexHealth> map = APILocator.getESIndexAPI().getClusterHealth();
		for(String indexName: APILocator.getESIndexAPI().listIndices()){
			ClusterIndexHealth health = map.get(indexName);
			if(health.getStatus().equals(ClusterHealthStatus.YELLOW)){
				esHealth = false;
				break;
			}
		}
		return esHealth&&cacheHealth;
	}

}
