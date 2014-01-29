package com.eng.dotcms.healthchecker.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jgroups.Address;
import org.jgroups.View;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
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
		for(Address new_view_addr : new_view.getMembers()){
			if(!HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress().equals(new_view_addr)){
				if(healthAPI.isLeaveNode(new_view_addr)){
					joined.add(new_view_addr);
				}
			}
		}
		return joined;
	}
	
	public static String getStringAddress(Address address){
		String[] _address = address.toString().split("[-]");
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<_address.length-1; i++){
			sb.append(_address[i]);
			sb.append("-");
		}
		return sb.toString().substring(0, sb.toString().length()-1); 
	}
	
	
	public static String callRESTService(HealthClusterViewStatus status, String operation){
		ClientConfig clientConfig = new DefaultClientConfig();
		Client client = Client.create(clientConfig);
        WebResource webResource = client.resource(getRESTURL(status));
        return webResource.path(operation).get(String.class);
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
	
	private static String getRESTURL(HealthClusterViewStatus status){
		StringBuilder sb = new StringBuilder();
		sb.append(status);
		sb.append("/api/health");
		return sb.toString();
	}
	
	
	
}
