package com.eng.dotcms.stat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.eng.dotcms.stat.bean.ClickSearchBean;
import com.eng.dotcms.stat.bean.ClickStepBean;
import com.eng.dotcms.stat.bean.SectionReport;
import com.eng.dotcms.stat.bean.SectionSearchBean;


public class ClickstreamStatisticsAPIImpl extends ClickstreamStatisticsAPI {
    
	private ClickStepResultMapper mapper = new ClickStepResultMapper();
	private SectionReportResultMapper sectionMapper = new SectionReportResultMapper();
	
    private static ClickstreamStatisticsAPIImpl instance = null;
    public static ClickstreamStatisticsAPIImpl getInstance() {
		if(instance==null)
			instance = new ClickstreamStatisticsAPIImpl();
		
		return instance;
	}
    
    public List<List<ClickStepBean>> findPageStream(ClickSearchBean searchBean) throws DotPublisherException {
    	try{
    		List<List<ClickStepBean>> pageStream = new ArrayList<List<ClickStepBean>>();
    		int counter = 0;
    		int steps = 5;
    		
    		if(searchBean.getSteps() != null && searchBean.getSteps() > 0)
    			steps = searchBean.getSteps();
    		
    		//Variable conditions
    		StringBuilder str = new StringBuilder();
    		if(searchBean.getDateFrom() != null) {
    			str.append("and cr2.TIMESTAMPPER >= to_date(?, 'YYYY-MM-DD') ");
    		}
    		if(searchBean.getDateTo() != null) {
    			str.append("and cr2.TIMESTAMPPER <= to_date(?, 'YYYY-MM-DD') ");
    		}
    		
			while(counter <= steps) {
	    		DotConnect dc = new DotConnect();
				
	    		if(searchBean.getPageType().equals(ClickSearchBean.PageType.LANDING))
	    			dc.setSQL(SQL_FIND_QUERY_LANDING.replace("{variable-conditions}", str.toString()));
	    		else
	    			dc.setSQL(SQL_FIND_QUERY_STARTING.replace("{variable-conditions}", str.toString()));
	    		
	    		dc.addParam(counter);
	    		
	    		//Load page
	    		HTMLPage page = APILocator.getHTMLPageAPI().loadLivePageById(
						searchBean.getPageId(),
						APILocator.getUserAPI().getSystemUser(), 
						false);
				if(page != null && UtilMethods.isSet(page.getInode())) {
					Identifier pageId = APILocator.getIdentifierAPI().find(page);
					dc.addParam(pageId.getParentPath()+page.getPageUrl());
				} else {
					page = APILocator.getHTMLPageAPI().loadWorkingPageById(
							searchBean.getPageId(),
							APILocator.getUserAPI().getSystemUser(), 
							false);
					if(page != null && UtilMethods.isSet(page.getInode())) {
						Identifier pageId = APILocator.getIdentifierAPI().find(page);
						dc.addParam(pageId.getParentPath()+page.getPageUrl());
					}
				}	
				
				if(searchBean.getDateFrom() != null)
	    			dc.addParam(new SimpleDateFormat("yyyy-MM-dd").format(searchBean.getDateFrom()));
	    		if(searchBean.getDateTo() != null)
	    			dc.addParam(new SimpleDateFormat("yyyy-MM-dd").format(searchBean.getDateTo()));
				
				
				List<ClickStepBean> step = mapper.mapRows(dc.loadObjectResults());
				List<ClickStepBean> stepToAdd = new ArrayList<ClickStepBean>();
				if(step != null && step.size() > 0) {
					long totalHints = 0;
					for (ClickStepBean clickStepBean : step) {
						totalHints += clickStepBean.getHintsCount();
					}
					for (ClickStepBean clickStepBean : step) {
						Integer perc = new Long((clickStepBean.getHintsCount() * 100) / totalHints).intValue();
						clickStepBean.setUserPercentage(perc);
						
						checkMinUserPercConstraint(searchBean, clickStepBean, stepToAdd);
						
						if(counter == 0 && searchBean.getPageType().equals(ClickSearchBean.PageType.LANDING)) {
							HTMLPage sPage = APILocator.getHTMLPageAPI().loadLivePageById(
									searchBean.getPageId(),
									APILocator.getUserAPI().getSystemUser(), 
									false);
							if(sPage != null && UtilMethods.isSet(sPage.getInode())) {
								clickStepBean.setPageName(sPage.getTitle());
							} else {
								sPage = APILocator.getHTMLPageAPI().loadWorkingPageById(
										searchBean.getPageId(),
										APILocator.getUserAPI().getSystemUser(), 
										false);
								if(sPage != null && UtilMethods.isSet(sPage.getInode()))
									clickStepBean.setPageName(sPage.getTitle());
							}
						}
					}
					
					stepToAdd = checkDepthConstraint(searchBean, stepToAdd);
					
					pageStream.add(stepToAdd);
				}
				
				counter++;
			}
			
			if(searchBean.getPageType().equals(ClickSearchBean.PageType.STARTING) && pageStream.size() > 0) {
				List<ClickStepBean> start = new ArrayList<ClickStepBean>();
				ClickStepBean startStep = new ClickStepBean();
				startStep.setUserPercentage(new Integer(100));
				
				HTMLPage sPage = APILocator.getHTMLPageAPI().loadLivePageById(
						searchBean.getPageId(),
						APILocator.getUserAPI().getSystemUser(), 
						false);
				if(sPage != null && UtilMethods.isSet(sPage.getInode())) {
					startStep.setPageName(sPage.getTitle());
				} else {
					sPage = APILocator.getHTMLPageAPI().loadWorkingPageById(
							searchBean.getPageId(),
							APILocator.getUserAPI().getSystemUser(), 
							false);
					if(sPage != null && UtilMethods.isSet(sPage.getInode()))
						startStep.setPageName(sPage.getTitle());
				}
					
				start.add(startStep);
				
				pageStream.remove(0);
				pageStream.add(0, start);
				pageStream.remove(pageStream.size()-1);
			}
			
			return pageStream;
			
		}catch(Exception e){
			Logger.debug(ClickstreamStatisticsAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
    }
    
    
    public Integer countPageDirectAccess(ClickSearchBean searchBean) throws DotDataException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SQL_COUNT_DIRECT_ACCESS);
			dc.addParam(searchBean.getPageId());
			
			return Integer.parseInt(dc.loadObjectResults().get(0).get("count").toString());
		}catch(Exception e){
			Logger.debug(this,e.getMessage(),e);
			throw new DotDataException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}
    
    public Integer countPageSearchEngineAccess(ClickSearchBean searchBean) throws DotDataException {
    	try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SQL_COUNT_SEARCH_ENGINE_ACCESS);
			dc.addParam(searchBean.getPageId());
			
			return Integer.parseInt(dc.loadObjectResults().get(0).get("count").toString());
		}catch(Exception e){
			Logger.debug(this,e.getMessage(),e);
			throw new DotDataException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
    }
    
    //Constraints check
    private boolean checkMinUserPercConstraint(ClickSearchBean searchBean, 
    		ClickStepBean clickStepBean, 
    		List<ClickStepBean> stepToAdd) 
    {	
    	boolean inserted = false;
    	if(searchBean.getMinUserPercentage() != null && searchBean.getMinUserPercentage().intValue() < 100) {
			if(clickStepBean.getUserPercentage().intValue() >= searchBean.getMinUserPercentage().intValue()) {
				stepToAdd.add(clickStepBean);
				inserted = true;
			}
		} else {
			stepToAdd.add(clickStepBean);
			inserted = true;
		}
    	
    	return inserted;
    }
   
    private List<ClickStepBean> checkDepthConstraint(ClickSearchBean searchBean, List<ClickStepBean> stepToAdd) {
    	Integer depth = searchBean.getDepth();
    	if(depth == null)
    		depth = 4;
    	
    	if(depth != null && depth.intValue() > 0) {
			if(depth.intValue() < stepToAdd.size())
				return stepToAdd.subList(0, depth);
		}
		
		return stepToAdd;
    }
    
    
    public List<Folder> findSections() {
    	List<Folder> folderToReturn = new ArrayList<Folder>();
    	FolderAPI fAPI = APILocator.getFolderAPI();
    	HostAPI hAPI = APILocator.getHostAPI();
    	UserAPI uAPI = APILocator.getUserAPI();
    	
    	try {
			List<Folder> folders = 
					fAPI.findFoldersByHost(hAPI.findDefaultHost(uAPI.getSystemUser(), false),uAPI.getSystemUser(),false);
			
			for (Folder folder : folders) {
				if(folder.isShowOnMenu())
					folderToReturn.add(folder);
			}
		} catch (DotHibernateException e) {
			e.printStackTrace();
		} catch (DotSecurityException e) {
			e.printStackTrace();
		} catch (DotDataException e) {
			e.printStackTrace();
		}
    	
    	return folderToReturn;
    }
    
    public List<SectionReport> findSectionReport(SectionSearchBean searchBean) throws DotDataException {
    	DotConnect dc = new DotConnect();
		dc.setSQL(SQL_FIND_SECTION_REPORT);
		
		dc.addParam("/"+searchBean.getSection()+"/%");
		
		searchBean.getMonthFrom();
		searchBean.getMonthTo();
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, searchBean.getYear());
		cal.set(Calendar.MONTH, searchBean.getMonthFrom());
		cal.set(Calendar.DAY_OF_MONTH, 1);
		dc.addParam(cal.getTime());
		
		cal.set(Calendar.MONTH, searchBean.getMonthTo());
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		dc.addParam(cal.getTime());
		
		return sectionMapper.mapRows(dc.loadObjectResults());
    }
}