package com.eng.dotcms.healthchecker.util;

import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.View;

import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class HealthUtil {
	
	public static boolean containsMember(View clusterView, Address address) {
		String address_str = address.toString().split("[-]")[0];
		for(Address a:clusterView.getMembers()){
			String _address = a.toString();
			if(address_str.equalsIgnoreCase(_address.split("[-]")[0]))
				return true;
		}
		return false;
	}
	
	public static boolean isMerge(View view){
		int indexOf = view.toString().indexOf("MergeView");
		Logger.info(HealthUtil.class, "IndexOf: " + indexOf);
		return indexOf>=0;
	}
	
	/**
	 * La nuova View deve contenere per forza più dati della vecchia
	 * @param new_view
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static List<Address> getJoined(View new_view) {
		HealthCheckerAPI healthAPI = new HealthCheckerAPI();
		List<Address> joined = new ArrayList<Address>();
		for(Address new_view_addr : new_view.getMembers()){
			Logger.debug(HealthUtil.class, "Address: " + new_view_addr);
			Logger.debug(HealthUtil.class, "Local Address: " + HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress());
			if(!HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress().equals(new_view_addr)){
				Logger.debug(HealthUtil.class, "Provo a vedere se e' LEAVE");
				if(healthAPI.isLeaveNode(new_view_addr)){
					Logger.debug(HealthUtil.class, "E' LEAVE...");
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
	
	
	public static String callRESTService(HealthClusterViewStatus status){
		ClientConfig clientConfig = new DefaultClientConfig();
		Client client = Client.create(clientConfig);
        WebResource webResource = client.resource(getRESTURL(status));
        return webResource.path("/joinCluster").get(String.class);
	}
	

	private static String getRESTURL(HealthClusterViewStatus status){
		StringBuilder sb = new StringBuilder();
		sb.append(status);
		sb.append("/api/health");
		return sb.toString();
	}
}
