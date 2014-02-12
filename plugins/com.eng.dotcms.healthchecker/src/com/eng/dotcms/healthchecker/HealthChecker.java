package com.eng.dotcms.healthchecker;

import org.jgroups.View;
import org.springframework.context.ApplicationContext;

public class HealthChecker {
	
	private HealthEvent healthEvent;
	public static HealthChecker INSTANCE = new HealthChecker();
	private HealthClusterAdministrator clusterAdmin;
	private View lastView = null; 
	private int countSuspect = 0;
	private int countTimer = 0;
	private ApplicationContext springContext;
	
	private HealthChecker(){
		healthEvent = new HealthEvent();
		setClusterAdmin(new HealthClusterAdministrator());
	}

	public HealthEvent getHealthEvent() {
		return healthEvent;
	}

	public void setHealthEvent(HealthEvent health) {
		this.healthEvent = health;
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

	public int getCountTimer() {
		return countTimer;
	}

	public void setCountTimer(int countTimer) {
		this.countTimer = countTimer;
	}
	
	public ApplicationContext getSpringContext() {
		return springContext;
	}

	public void setSpringContext(ApplicationContext springContext) {
		this.springContext = springContext;
	}

	public void flush(){
		healthEvent = new HealthEvent();
	}

}
