package com.eng.dotcms.stat;

import java.util.List;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.eng.dotcms.stat.bean.ClickSearchBean;
import com.eng.dotcms.stat.bean.ClickStepBean;
import com.eng.dotcms.stat.bean.SectionReport;
import com.eng.dotcms.stat.bean.SectionSearchBean;


public abstract class ClickstreamStatisticsAPI {
 
    private static ClickstreamStatisticsAPI cssAPI = null;
	public static ClickstreamStatisticsAPI getInstance(){
		if(cssAPI == null){
			cssAPI = ClickstreamStatisticsAPIImpl.getInstance();
		}
		return cssAPI;	
	}

	
	protected String SQL_FIND_QUERY_LANDING = 
			"select cr1.REQUEST_URI as request_uri, count(*) as hints "+
			"from  "+
			"CLICKSTREAM_REQUEST cr1, "+
			"CLICKSTREAM_REQUEST cr2 "+
			"where "+
			"cr1.REQUEST_ORDER = (cr2.REQUEST_ORDER - ?) "+
			"and cr2.REQUEST_URI = ? "+
			"{variable-conditions} "+
			"and cr1.CLICKSTREAM_ID = cr2.CLICKSTREAM_ID "+
			"group by cr1.REQUEST_URI "+
			"order by hints desc ";
	
	
	protected String SQL_FIND_QUERY_STARTING = 
			"select cr1.REQUEST_URI as request_uri, count(*) as hints "+
			"from  "+
			"CLICKSTREAM_REQUEST cr1, "+
			"CLICKSTREAM_REQUEST cr2 "+
			"where "+
			"cr1.REQUEST_ORDER = (cr2.REQUEST_ORDER + ?) "+
			"and cr2.REQUEST_URI = ? "+
			"{variable-conditions} "+
			"and cr1.CLICKSTREAM_ID = cr2.CLICKSTREAM_ID "+
			"group by cr1.REQUEST_URI "+
			"order by hints desc ";
     
    public abstract List<List<ClickStepBean>> findPageStream(ClickSearchBean searchBean) throws DotPublisherException;
    
    
    protected String SQL_COUNT_DIRECT_ACCESS = 
    		"select count(*) as count "+
			"from clickstream c "+
			"where "+
			"c.NUMBER_OF_REQUESTS = 1 "+
			"and last_page_id = ?";
    
    public abstract Integer countPageDirectAccess(ClickSearchBean searchBean) throws DotDataException;
    
    
    protected String SQL_COUNT_SEARCH_ENGINE_ACCESS = 
    		"select count(*) as count "+
			"from clickstream c, CLICKSTREAM_REQUEST cr "+
			"where c.LAST_PAGE_ID = ? "+
			"and c.NUMBER_OF_REQUESTS = 2 "+
			"and c.CLICKSTREAM_ID = cr.CLICKSTREAM_ID "+
			"and cr.REQUEST_ORDER = 1 "+
			"and cr.REQUEST_URI = '/homepage/search/simplesearch.html' or "+
			"cr.REQUEST_URI = '/homepage/search/advancedsearchresult.html' ";
    
    public abstract Integer countPageSearchEngineAccess(ClickSearchBean searchBean) throws DotDataException;
    
    
    public abstract List<Folder> findSections(); 
    
    
    protected String SQL_FIND_SECTION_REPORT = 
    		"select TO_CHAR(TIMESTAMPPER, 'DD-MM-YYYY') as day_req, REQUEST_URI as request_uri, count(*) as hints "+
			"from CLICKSTREAM_REQUEST cr "+ 
			"where REQUEST_URI like ? "+
			"and TIMESTAMPPER between ? and ? "+
			"group by TO_CHAR(TIMESTAMPPER, 'DD-MM-YYYY'), REQUEST_URI "+
			"order by DAY_REQ ";
    
    public abstract List<SectionReport> findSectionReport(SectionSearchBean searchBean) throws DotDataException;
}