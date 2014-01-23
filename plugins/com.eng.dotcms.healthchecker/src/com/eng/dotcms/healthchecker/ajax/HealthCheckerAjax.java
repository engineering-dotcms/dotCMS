package com.eng.dotcms.healthchecker.ajax;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.util.HealthUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class HealthCheckerAjax extends AjaxAction {
	
	@SuppressWarnings("rawtypes")
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String cmd = getURIParams().get("cmd");
        java.lang.reflect.Method meth = null;
        Class partypes[] = new Class[] { HttpServletRequest.class, HttpServletResponse.class };
        Object arglist[] = new Object[] { request, response };
        try {
            if (getUser() == null ) {
                response.sendError(401);
                return;
            }
            meth = this.getClass().getMethod(cmd, partypes);
            meth.invoke(this, arglist);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Trying to run method:" + cmd);
            Logger.error(this.getClass(), e.getMessage(), e.getCause());
            throw new RuntimeException(e.getMessage(),e);
        }
    }
	
	public void refreshCache(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String,String> pmap=getURIParams();
		String address = pmap.get("address");
		String port = pmap.get("port");
		String protocol = pmap.get("protocol");
		HealthClusterViewStatus status = new HealthClusterViewStatus();
		status.setAddress(address);
		status.setPort(port);
		status.setProtocol(protocol);
		ClientConfig clientConfig = new DefaultClientConfig();
		Client client = Client.create(clientConfig);
        WebResource webResource = client.resource(HealthUtil.getRESTURL(status));
        String responseRest = webResource.path("/joinCluster").get(String.class);
        response.getWriter().println(responseRest);
	}
	
	@Override
	public void action(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub

	}

}
