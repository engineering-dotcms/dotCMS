package com.eng.dotcms.healthchecker.listener;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.Operation;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.eng.dotcms.healthchecker.util.HealthUtil;

public class HealthCheckerListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent sce) {

	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			Logger.info(getClass(), "Initializing Spring JDBC context...");
			ApplicationContext context = new ClassPathXmlApplicationContext("com/eng/dotcms/healthchecker/spring-jdbc.xml");
			HealthChecker.INSTANCE.setSpringContext(context);
			Logger.info(getClass(), "Spring JDBC context initialization done!");
			HealthCheckerAPI healthAPI = (HealthCheckerAPI)HealthChecker.INSTANCE.getSpringContext().getBean("healthCheckerAPI");
			Logger.info(getClass(), "Cleaning data before restart the cluster health...");		
			healthAPI.cleanNode(HealthUtil.getAddressFromHostname(InetAddress.getLocalHost().getHostName()));
			healthAPI.insertHealthLock(HealthUtil.getAddressFromHostname(InetAddress.getLocalHost().getHostName()), 
					Operation.STARTING);
			Logger.info(getClass(), "Starting the cluster health for this node.");
		} catch (UnknownHostException e) {
			Logger.warn(getClass(), "Can't clean the data for the cluster health. Reason: " + e.getMessage());
		} catch (Exception e) {
			Logger.warn(getClass(), "Can't clean the data for the cluster health. Reason: " + e.getMessage());
		}
	}

}
