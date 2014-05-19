package com.eng.bankit.cachemanager.viewtool;

import java.util.Map;

import org.apache.velocity.tools.view.tools.ViewTool;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.cluster.node.DiscoveryNode;

import com.dotcms.content.elasticsearch.util.ESClient;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class CacheManagerTool implements ViewTool {
	
	private String nodeName;
	
	@Override
	public void init(Object initData) {
		nodeName = "dotCMS_" + Config.getStringProperty("DIST_INDEXATION_SERVER_ID");
	}
	
	public boolean checkESHealth(){
		try{
			return checkESStatus()&&checkNode();
		}catch(Exception e){
			return false;
		}
	}
	
	private boolean checkESStatus() throws Exception {
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
		return esHealth;		
	}
	
	private ClusterHealthStatus getClusterStatus() throws Exception {
		AdminClient client=new ESClient().getClient().admin();
		ActionFuture<ClusterHealthResponse> nir = client.cluster().health(new ClusterHealthRequest());
		ClusterHealthResponse res  = nir.actionGet();		
		return res.getStatus();
	}
	
	private boolean checkNode() throws Exception{
		boolean nodeHealth = false;
		AdminClient client=new ESClient().getClient().admin();		
		ActionFuture<NodesStatsResponse> nir = client.cluster().nodesStats(new NodesStatsRequest());		
		NodesStatsResponse res  = nir.actionGet();
		NodeStats[] stats = res.getNodes();
		
		// controllo il nodo se è quello corrente
		for(NodeStats singleNode:stats){
			DiscoveryNode n = singleNode.getNode();
			Logger.info(getClass(), "Node name in ElasticSearch: " + n.getName());
			Logger.info(getClass(), "Node name in DotMarketing: " + nodeName);
			if(n.getName().equals(nodeName)){
				nodeHealth = true;
				break;
			}
		}		
		return nodeHealth;
	}
}
