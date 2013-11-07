<%@page import="com.dotmarketing.plugin.business.PluginAPI"%>
<%@page import="com.eng.dotcms.mostresearchedterms.bean.MostResearchedTerms"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.business.web.HostWebAPI"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.WebKeys"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.LanguageAPI"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="com.eng.dotcms.mostresearchedterms.MostResearchedTermsAPIImpl"%>
<%@page import="com.eng.dotcms.mostresearchedterms.MostResearchedTermsAPI"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	int termCountMin = 0, termCountMax = 0, rangeMinMax = 0, interval = 1;
	HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	LanguageAPI langAPI = APILocator.getLanguageAPI();
	long lang = 0;
	if(!UtilMethods.isSet((String)session.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)) && !UtilMethods.isSet(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE)))
		lang = langAPI.getDefaultLanguage().getId();
	else if(UtilMethods.isSet(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE)))
		lang = Long.parseLong(request.getParameter(WebKeys.HTMLPAGE_LANGUAGE));
	else 
		lang = Long.parseLong((String)session.getAttribute(WebKeys.HTMLPAGE_LANGUAGE));
	MostResearchedTermsAPI mrtAPI = new MostResearchedTermsAPIImpl();
	String _limit = request.getParameter("limit");
	int limit = 0;
	if(null ==_limit)
		limit = Integer.parseInt(APILocator.getPluginAPI().loadProperty("com.eng.dotcms.mostresearchedterms", "limit.terms"));
	else
		limit = Integer.parseInt(_limit);
	List<MostResearchedTerms> allTerms = mrtAPI.findAllTerms(lang,hostWebAPI.getCurrentHost(request).getIdentifier(),limit);
	
	for(MostResearchedTerms term: allTerms){
		int termCount = term.getOccur();
		if(termCountMin==0)
			termCountMin = termCount;
		if(termCountMin>termCount)
			termCountMin = termCount;
		if(termCountMax<termCount)
			termCountMax = termCount;		
	}
	
	rangeMinMax = termCountMax - termCountMin;
	
	if(rangeMinMax>5)
		interval = rangeMinMax / 5;
	
	int range1 = termCountMin;
	int range2 = range1 + interval;
	int range3 = range2 + interval;
	int range4 = range3 + interval;
	int range5 = range4 + interval;
%>
<style type="text/css" media="all">
	.tagCloud .xSmallTag{
		font-size: .65em;
	}
	.tagCloud .smallTag{
		font-size: .95em;
	}
	.tagCloud .mediumTag{
		font-size: 1.45em;
	}
	.tagCloud .largeTag{
		font-size: 2.2em;
	}
	.tagCloud .xLargeTag{
		font-size: 3.8em;
	}
</style>

<div class="tagCloud">
<%
	for(MostResearchedTerms term: allTerms){
		String _term = term.getTerm();
		int _termCount = term.getOccur();
		String _class = "xSmallTag";
		if(_termCount > range5)
			_class = "xLargeTag";
		if(_termCount > range4)
			_class = "largeTag";
		if(_termCount > range3)
			_class = "mediumTag";
		if(_termCount > range2)
			_class = "smallTag";
		
		if(UtilMethods.isSet(request.getParameter("URL")) && UtilMethods.isSet(request.getParameter("searchParam"))){
%>
			<a href="<%=request.getParameter("URL")%>?<%=request.getParameter("searchParam")%>=<%=_term%>" class="<%=_class%>" title="<%=_termCount%> occ"><%=_term%></a>
<%		
		}else{
%>
			<span class="<%=_class%>"><%=_term%></span>
<%			
		}
	}
%>
</div>