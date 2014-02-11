package com.eng.dotcms.healthchecker.ajax;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.business.HealthCheckerAPI;
import com.eng.dotcms.healthchecker.util.HealthUtil;

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
		try{
			HealthCheckerAPI healthAPI = new HealthCheckerAPI();
			Map<String,String> pmap=getURIParams();
			String address = pmap.get("address");
			if(!healthAPI.nodeHasLeft(address)){
				String port = pmap.get("port");
				String protocol = pmap.get("protocol");
				HealthClusterViewStatus status = new HealthClusterViewStatus();
				status.setAddress(address);
				status.setPort(port);
				status.setProtocol(protocol);
				Map<String, String> params = new HashMap<String, String>();
				params.put("unlock", "KO");
		        String responseRest = HealthUtil.callRESTService(status,"/joinCluster");
		        response.getWriter().println(responseRest);
			}else
				response.getWriter().println("ALREADY_OOC");
		}catch(DotDataException e){
			response.getWriter().println("KO");
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	@Override
	public void action(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub

	}

}
