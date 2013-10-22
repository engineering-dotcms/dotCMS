<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
	String[] ids = request.getParameter("id").split("[,]");
	IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();
	String current_live = info.live;
	String current_working = info.working;
	Logger.info(this.getClass(), "Start deleting from indicies.");
	for(String id : ids){
		// remove from indicies
		APILocator.getSiteSearchAPI().deleteFromIndex(current_live, id);
		APILocator.getSiteSearchAPI().deleteFromIndex(working_live, id);
		Logger.info(this.getClass(), "Content " + id + " deleted from indicies: " + current_live + ", " + current_working);
	}
	Logger.info(this.getClass(), "End deleting from indicies.");
%>

Done !!