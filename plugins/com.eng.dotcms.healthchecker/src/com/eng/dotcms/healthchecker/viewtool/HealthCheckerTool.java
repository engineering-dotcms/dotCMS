package com.eng.dotcms.healthchecker.viewtool;

import java.util.Map;

import org.apache.velocity.tools.view.tools.ViewTool;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.AdminClient;
import org.jgroups.JChannel;

import com.dotcms.content.elasticsearch.util.ESClient;
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
		Logger.debug(getClass(), "Cluster View  (Health): 	"+healthClusterChannel.getView().toString());
		Logger.debug(getClass(), "Local Address (Health): 	"+healthClusterChannel.getLocalAddress().toString());
		Logger.debug(getClass(), "Cluster View  (Cache): 	"+cacheChannel.getView().toString());
		Logger.debug(getClass(), "Local Address (Cache): 	"+cacheChannel.getLocalAddress().toString());
		boolean esHealth = true;
		Map<String, ClusterIndexHealth> map = APILocator.getESIndexAPI().getClusterHealth();
		
		NodeInfo[] nodesInCluster = getNodesInfo();
		for(NodeInfo n:nodesInCluster){
			Logger.info(getClass(), "Node: "+n.getHttp().address().publishAddress().toString());
		}
		for(String indexName: APILocator.getESIndexAPI().listIndices()){
			ClusterIndexHealth health = map.get(indexName);
			if(health.getStatus().equals(ClusterHealthStatus.YELLOW)){
				esHealth = false;
				break;
			}
		}
		return esHealth&&cacheHealth;
	}
	
	private NodeInfo[] getNodesInfo(){
		AdminClient client=new ESClient().getClient().admin();
		NodesInfoRequest req = new NodesInfoRequest();
		ActionFuture<NodesInfoResponse> nir = client.cluster().nodesInfo(req);
		NodesInfoResponse res  = nir.actionGet();
		return res.getNodes();
	}

}
