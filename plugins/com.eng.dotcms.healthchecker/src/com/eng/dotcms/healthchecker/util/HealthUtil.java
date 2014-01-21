package com.eng.dotcms.healthchecker.util;

import java.util.ArrayList;
import java.util.List;

import org.jgroups.Address;
import org.jgroups.View;

import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;

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
}
