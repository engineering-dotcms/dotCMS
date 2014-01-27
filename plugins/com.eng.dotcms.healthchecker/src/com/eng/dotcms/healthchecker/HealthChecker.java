package com.eng.dotcms.healthchecker;

import org.jgroups.View;

public class HealthChecker {
	
	private Health health;
	public static HealthChecker INSTANCE = new HealthChecker();
	private HealthClusterAdministrator clusterAdmin;
	private View lastView = null; 
	private int countSuspect = 0;
	
	private HealthChecker(){
		health = new Health();
		setClusterAdmin(new HealthClusterAdministrator());
	}

	public Health getHealth() {
		return health;
	}

	public void setHealth(Health health) {
		this.health = health;
	}

	public HealthClusterAdministrator getClusterAdmin() {
		return clusterAdmin;
	}

	public void setClusterAdmin(HealthClusterAdministrator clusterAdmin) {
		this.clusterAdmin = clusterAdmin;
	}
		
	public View getLastView() {
		return lastView;
	}

	public void setLastView(View lastView) {
		this.lastView = lastView;
	}

	public int getCountSuspect() {
		return countSuspect;
	}

	public void setCountSuspect(int countSuspect) {
		this.countSuspect = countSuspect;
	}

	public void flush(){
		health = new Health();
	}

}
