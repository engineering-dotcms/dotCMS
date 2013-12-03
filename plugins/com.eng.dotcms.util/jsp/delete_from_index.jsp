<%@page import="org.elasticsearch.ElasticSearchException"%>
<%@page import="org.elasticsearch.action.bulk.BulkRequestBuilder"%>
<%@page import="com.dotcms.content.elasticsearch.util.ESClient"%>
<%@page import="org.elasticsearch.client.Client"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="java.util.List"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESContentletIndexAPI"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	String[] ids = request.getParameter("id").split("[,]");
	ESContentletIndexAPI indexAPI = new ESContentletIndexAPI();
	Logger.info(this.getClass(), "Start deleting from indicies.");
	for(String id : ids){
		// remove from indicies
		try {
			String id_it=id+"_1";
	        String id_en=id+"_103";
	        Client client=new ESClient().getClient();
	        BulkRequestBuilder bulk=client.prepareBulk();
	        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

	        bulk.add(client.prepareDelete(info.live, "content", id_it));
	        bulk.add(client.prepareDelete(info.working, "content", id_it));
	        bulk.add(client.prepareDelete(info.live, "content", id_en));
	        bulk.add(client.prepareDelete(info.working, "content", id_en));	        
            bulk.execute().actionGet();
	    }catch(Exception ex) {
	    	out.println(ex.getMessage());
	    }
		Logger.info(this.getClass(), "Content " + id + " deleted from indicies");
	}
	Logger.info(this.getClass(), "End deleting from indicies.");
%>

Done !!