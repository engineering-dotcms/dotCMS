package com.eng.dotcms.healthchecker.timer;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.dotcms.rest.HealthService;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.AddressStatus;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.Operation;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.eng.dotcms.healthchecker.util.HealthUtil;

public class HealthCheckerTimer extends TimerTask {

	private HealthCheckerAPI healthAPI = new HealthCheckerAPI(); 
	private long MAX_REJOIN_TIME = Config.getIntProperty("HEALTH_CHECKER_MAX_REJOIN_TIME", 60000);
	
	@Override
	public void run() {
		try {
			checkNodes();
		} catch (DotDataException e) {
			Logger.error(getClass(), "Error in TimerTask", e);
		}
	}
	
	private void checkNodes() throws DotDataException {
		Date now = new Date();
		List<String> servers = healthAPI.getAllServersInClusterExceptMe();
		for(String server:servers){			
			if(healthAPI.isLeaveNode(server)){
				Date lastLeave = healthAPI.getDateOfLastLeaveEvent(server);
				if(HealthUtil.getDateDiff(now, lastLeave, TimeUnit.MILLISECONDS)>MAX_REJOIN_TIME 
						&& !healthAPI.isHealthLock(server, Operation.RESTARTING)
						&& !healthAPI.isHealthLock(server, Operation.FLUSHING)
						&& !healthAPI.isHealthLock(server, Operation.JOINING)
						&& !healthAPI.isHealthLock(server, Operation.STARTING)) {
					try {
						HealthClusterViewStatus status = healthAPI.singleClusterView(server);
						long minutes = (MAX_REJOIN_TIME/1000/60);
						Logger.info(getClass(), "The node " + server + " exceeded the max wait time ("+minutes+" minute/s): call the restart...");
						if(HealthChecker.INSTANCE.getCountTimer()<5 && !status.isOutForTimer()){
							healthAPI.insertHealthLock(server, Operation.RESTARTING);
							Map<String, String> params = new HashMap<String, String>();
							params.put("lock", "OK");
							String response = HealthUtil.callRESTService(status, "/forceJoinCluster");
							if(HealthService.STATUS_OK.equals(response))					
								Logger.info(getClass(), "The node " + server + " was successful restarted...it will come back into the cluster as soon as possible.");
							else {
								Logger.info(getClass(), "I can't call the REST service on node " + server + ".Cause: "+response);
								healthAPI.deleteHealthLock(server, Operation.RESTARTING);
								HealthChecker.INSTANCE.setCountTimer(HealthChecker.INSTANCE.getCountTimer()+1);
							}
						}else{
							Logger.info(getClass(), "I can't restart the node "+ server +" after 5 attempts...delete it.");
							HealthChecker.INSTANCE.setCountTimer(0);
							try{
								HibernateUtil.startTransaction();
								healthAPI.deleteHealthStatus(server, AddressStatus.LEFT);
								healthAPI.deleteHealthStatus(server, AddressStatus.JOIN);
								healthAPI.updateHealthClusterView(status.getAddress(), now, true);
								healthAPI.deleteHealthLock(server, Operation.RESTARTING);
								healthAPI.deleteHealthLock(server, Operation.FLUSHING);
								healthAPI.deleteHealthLock(server, Operation.JOINING);
								HibernateUtil.commitTransaction();
							}catch(DotDataException e){
								HibernateUtil.rollbackTransaction();
							}
						}
					} catch (DotDataException e) {
						HealthChecker.INSTANCE.flush();
					}	
				}
			}
		}	
	}
}
