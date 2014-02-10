package com.eng.dotcms.healthchecker.util;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jgroups.Address;
import org.jgroups.View;

import com.dotcms.rest.HealthService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class HealthUtil {
	
	private static HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	
	public static boolean containsMember(View clusterView, Address address) {
		String address_str = address.toString().split("[-]")[0];
		for(Address a:clusterView.getMembers()){
			String _address = a.toString();
			if(address_str.equalsIgnoreCase(_address.split("[-]")[0]))
				return true;
		}
		return false;
	}

	/**
	 * La nuova View deve contenere per forza più dati della vecchia
	 * @param new_view
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static List<Address> getJoined(View new_view) {
		List<Address> joined = new ArrayList<Address>();
		try{
			for(Address new_view_addr : new_view.getMembers()){
				if(!HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress().equals(new_view_addr)){
					if(healthAPI.isLeaveNode(new_view_addr)){
						joined.add(new_view_addr);
					}
				}
			}
		}catch(DotDataException e){}
		return joined;
	}
	
	public static String getStringAddress(Address address){
		String[] _address = address.toString().split("[-]");
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<_address.length-1; i++){
			sb.append(_address[i]);
			sb.append("-");
		}
		if(UtilMethods.isSet(Config.getStringProperty("HEALTH_CHECKER_ADDRESS_SUFFIX")))
			return sb.toString().substring(0, sb.toString().length()-1).concat(Config.getStringProperty("HEALTH_CHECKER_ADDRESS_SUFFIX"));
		return sb.toString().substring(0, sb.toString().length()-1); 
	}
	
	public static String getStringAddress(String address){
		String[] _address = address.toString().split("[-]");
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<_address.length-1; i++){
			sb.append(_address[i]);
			sb.append("-");
		}
		if(UtilMethods.isSet(Config.getStringProperty("HEALTH_CHECKER_ADDRESS_SUFFIX")))
			return sb.toString().substring(0, sb.toString().length()-1).concat(Config.getStringProperty("HEALTH_CHECKER_ADDRESS_SUFFIX"));
		return sb.toString().substring(0, sb.toString().length()-1); 
	}
	
	
	public static String callRESTService(HealthClusterViewStatus status, String operation) {
		try{
			ClientConfig clientConfig = new DefaultClientConfig();
			Client client = Client.create(clientConfig);
			client.setConnectTimeout(20000);
			client.setReadTimeout(60000);
	        WebResource webResource = client.resource(getRESTURL(status));
	        webResource = webResource.path(operation);
	        return webResource.get(String.class);
		}catch(Exception e){
//			Logger.info(HealthUtil.class, "Exc: " + e.getCause().getClass());
			if(e.getCause() instanceof ConnectException)
				return HealthService.CONNECTION_EXC;
			else {
				return HealthService.SOCKET_EXC;
			}
		}
	}
	
	public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		if(null!=date2){
		    long diffInMillies = date1.getTime() - date2.getTime();
		    return timeUnit.convert(diffInMillies,timeUnit);
		}else
			return -1;
	}

	/**
	 * Controlla se sono stati inseriti dei contenuti nell'intervallo in cui il nodo è rimasto fuori dal cluster.
	 * 
	 * @param address
	 * @return
	 * @throws DotDataException
	 */
	public static boolean needFlushCache(Date leaveDate, Date joinDate) throws DotDataException {
		if(null!=leaveDate){
			int count = healthAPI.checkContentlet(leaveDate, joinDate);
			if(count>0)
				return true;
			else
				count = healthAPI.checkContainer(leaveDate, joinDate);
			if(count>0)
				return true;
			else
				count = healthAPI.checkHtmlPage(leaveDate, joinDate);
			if(count>0)
				return true;
			else
				count = healthAPI.checkTemplate(leaveDate, joinDate);
			
			return count > 0;
		}else
			return false;
		
	}
	
	public void runOSCommand(String...cmds) throws IOException{
		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.start();
	}
	
	private static String getRESTURL(HealthClusterViewStatus status){
		StringBuilder sb = new StringBuilder();
		sb.append(status);
		sb.append("/api/health");
		return sb.toString();
	}
	
	
	
}
