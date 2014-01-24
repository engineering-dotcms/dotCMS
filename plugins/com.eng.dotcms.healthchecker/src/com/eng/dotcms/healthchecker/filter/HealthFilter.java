package com.eng.dotcms.healthchecker.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.jgroups.Address;

import com.eng.dotcms.healthchecker.HealthChecker;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;

public class HealthFilter implements Filter {
	
	private HealthCheckerAPI healthAPI = new HealthCheckerAPI();
	
	@Override
	public void destroy() {
		
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletResponse res = (HttpServletResponse) response;
		Address localAddress = HealthChecker.INSTANCE.getClusterAdmin().getJGroupsHealthChannel().getLocalAddress();
		if(healthAPI.isLeaveNode(localAddress)){
			res.sendRedirect("/html/plugins/com.eng.dotcms.healthchecker/406.jsp");
			return;
		}else
			chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}
	

}
