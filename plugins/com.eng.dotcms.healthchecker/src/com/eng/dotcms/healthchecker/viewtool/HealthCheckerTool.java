package com.eng.dotcms.healthchecker.viewtool;

import java.util.Map;

import org.apache.velocity.tools.view.tools.ViewTool;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.elasticsearch.client.AdminClient;
import org.jgroups.JChannel;

import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;

public class HealthCheckerTool implements ViewTool {
	
	private JChannel healthClusterChannel, cacheChannel;

	@Override
	public void init(Object initData) {
		healthClusterChannel = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel();
		cacheChannel = CacheLocator.getCacheAdministrator().getJGroupsChannel();
	}
	
	public boolean checkHealth(){
		try{
			return checkCacheStatus();
		}catch(Exception e){
//			Logger.error(getClass(), "Error in HealthCheckerTool.",e);
			return false;
		}
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
	private boolean checkCacheStatus() throws Exception {
		try{
			HealthCheckerAPI healthAPI = new HealthCheckerAPI();
			boolean cacheHealth = !healthAPI.nodeHasLeft(healthClusterChannel.getLocalAddress());
			Logger.debug(getClass(), "Is in cluster? " + cacheHealth);
			Logger.debug(getClass(), "Cluster View  (Health): 	"+healthClusterChannel.getView().toString());
			Logger.debug(getClass(), "Local Address (Health): 	"+healthClusterChannel.getLocalAddress().toString());
			Logger.debug(getClass(), "Cluster View  (Cache): 	"+cacheChannel.getView().toString());
			Logger.debug(getClass(), "Local Address (Cache): 	"+cacheChannel.getLocalAddress().toString());
			boolean esHealth = true;
			ClusterHealthStatus status = getClusterStatus();
			if(!status.equals(ClusterHealthStatus.RED)){
				Map<String, ClusterIndexHealth> map = APILocator.getESIndexAPI().getClusterHealth();
				
				for(String indexName: APILocator.getESIndexAPI().listIndices()){
					ClusterIndexHealth health = map.get(indexName);
					if(health.getStatus().equals(ClusterHealthStatus.RED)){
						esHealth = false;
						break;
					}
				}
			}else
				esHealth = false;
			return esHealth&&cacheHealth;
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	private ClusterHealthStatus getClusterStatus() throws Exception {
		AdminClient client=new ESClient().getClient().admin();
		ActionFuture<ClusterHealthResponse> nir = client.cluster().health(new ClusterHealthRequest());
		ClusterHealthResponse res  = nir.actionGet();		
		return res.getStatus();
	}
}
