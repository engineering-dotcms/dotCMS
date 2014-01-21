package com.eng.dotcms.healthchecker.viewtool;

import org.apache.velocity.tools.view.tools.ViewTool;
import org.jgroups.JChannel;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.HealthChecker;

public class HealthCheckerTool implements ViewTool {
	
	private JChannel healthClusterChannel, cacheChannel;
	
	@Override
	public void init(Object initData) {
		healthClusterChannel = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel();
		cacheChannel = CacheLocator.getCacheAdministrator().getJGroupsChannel();
	}
	
	public boolean checkHealth(){
//		HealthChecker.INSTANCE.getClusterAdmin().testCluster();
		return checkCacheStatus();
	}
	
	/**
	 * Questo metodo controlla se il nodo in esame è nel cluster jGroups
	 * @return
	 */
	private boolean checkCacheStatus(){
		Logger.info(getClass(), "Cluster View  (Health): 	"+healthClusterChannel.getView().toString());
		Logger.info(getClass(), "Local Address (Health): 	"+healthClusterChannel.getLocalAddress().toString());
		Logger.info(getClass(), "Cluster View  (Cache): 	"+cacheChannel.getView().toString());
		Logger.info(getClass(), "Local Address (Cache): 	"+cacheChannel.getLocalAddress().toString());

		return healthClusterChannel.getView().containsMember(healthClusterChannel.getLocalAddress()) 
				&& cacheChannel.getView().containsMember(cacheChannel.getLocalAddress());
	}

}
