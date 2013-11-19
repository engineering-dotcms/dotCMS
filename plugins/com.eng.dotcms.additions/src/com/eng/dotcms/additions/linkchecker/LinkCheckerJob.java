package com.eng.dotcms.additions.linkchecker;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ThreadSafeSimpleDateFormat;

public class LinkCheckerJob implements Job, StatefulJob {
    
    private static DateFormat luceneDateFormat=new ThreadSafeSimpleDateFormat("yyyyMMddHHmmss");
    
    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        if(LicenseUtil.getLevel()<200)
            return;
        try {
        	Logger.info(this, "Start check broken links on Structure: Link and Link Semplice");
        	processLinks(ctx);
        	processLinkSemplici(ctx);
        	Logger.info(this, "End check broken links on Structure: Link and Link Semplice");
		} catch (DotDataException e) {
			Logger.warn(this, e.getMessage(),e);
		} catch (DotSecurityException e) {
			Logger.warn(this, e.getMessage(),e);
		}finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.warn(this, e.getMessage(),e);
			}
		}
    }   
    
    /**
     * Questo metodo processa i contenuti di tipo Link
     * 
     * @param ctx
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void processLinks(JobExecutionContext ctx) throws DotDataException, DotSecurityException {
    	StringBuilder query=new StringBuilder();
        
    	if(ctx.getPreviousFireTime()!=null) {
    		query.append("+modDate:[")
    			.append(luceneDateFormat.format(ctx.getPreviousFireTime()))
    			.append(" TO ")
    			.append(luceneDateFormat.format(ctx.getFireTime()))
    			.append("] ");
        }
        
        query.append("+structureName:Link +Link.linkType:*E*");
    	List<Contentlet> contents=null;
        int offset=0;
        int processed=0;
        final int pageSize = 100;
        int badlinks=0;
        
        do {
        	contents=APILocator.getContentletAPI().search(
            	query.toString(), pageSize, offset, "modDate", 
                APILocator.getUserAPI().getSystemUser(), false);
                
        	processed+=contents.size();
        	for(Contentlet con : contents) {
        		List<InvalidLink> links=new ArrayList<InvalidLink>();
        		try {
        			APILocator.getLinkCheckerAPI().deleteInvalidLinks(con);
        			if(con.isArchived() == false) {
        				InvalidLink il = LinkUtil.findInvalidLink(con);
        				if(null!=il)
        					links.add(il);
        			}
        		}catch(Exception ex) {
        			Logger.warn(this, "error parsing html in content \""+con.getTitle()+"\" value. Detail: "+ex.getMessage());
        		}       
        		if(links!=null && links.size()>0) {
        			try {
        				HibernateUtil.startTransaction();
                        APILocator.getLinkCheckerAPI().saveInvalidLinks(con, links);
                        HibernateUtil.commitTransaction();
        			}catch(Exception ex) {
        				try {
        					HibernateUtil.rollbackTransaction();
        				} catch (DotHibernateException e) {
                        	Logger.warn(this, e.getMessage(),e);
        				}
        				Logger.warn(this, "error saving broken links: "+ex.getMessage());
        			}
        			badlinks+=links.size();
        		}
        	}
            offset+=pageSize;
        } while(contents.size()>0);
        Logger.info(this, "Finished checking for broken links for structure Link. Processed "+processed+" contentlets. Found "+badlinks+" broken links");
       
    }
    
    /**
     * Questo metodo processa i contenuti di tipo Link Semplice
     * 
     * @param ctx
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void processLinkSemplici(JobExecutionContext ctx) throws DotDataException, DotSecurityException {
    	StringBuilder query=new StringBuilder();
        
    	if(ctx.getPreviousFireTime()!=null) {
    		query.append("+modDate:[")
    			.append(luceneDateFormat.format(ctx.getPreviousFireTime()))
    			.append(" TO ")
    			.append(luceneDateFormat.format(ctx.getFireTime()))
    			.append("] ");
        }
        
        query.append("+structureName:LinkSemplice +LinkSemplice.linkType:*E*");
    	List<Contentlet> contents=null;
        int offset=0;
        int processed=0;
        final int pageSize = 100;
        int badlinks=0;
        
        do {
        	contents=APILocator.getContentletAPI().search(
            	query.toString(), pageSize, offset, "modDate", 
                APILocator.getUserAPI().getSystemUser(), false);
                
        	processed+=contents.size();
        	for(Contentlet con : contents) {
        		List<InvalidLink> links=new ArrayList<InvalidLink>();
        		try {
        			APILocator.getLinkCheckerAPI().deleteInvalidLinks(con);
        			if(con.isArchived() == false) {
        				InvalidLink il = LinkUtil.findInvalidLink(con);
        				if(null!=il)
        					links.add(il);
        			}
        		}catch(Exception ex) {
        			Logger.warn(this, "error parsing html in content \""+con.getTitle()+"\" value. Detail: "+ex.getMessage());
        		}       
        		if(links!=null && links.size()>0) {
        			try {
        				HibernateUtil.startTransaction();
                        APILocator.getLinkCheckerAPI().saveInvalidLinks(con, links);
                        HibernateUtil.commitTransaction();
        			}catch(Exception ex) {
        				try {
        					HibernateUtil.rollbackTransaction();
        				} catch (DotHibernateException e) {
                        	Logger.warn(this, e.getMessage(),e);
        				}
        				Logger.warn(this, "error saving broken links: "+ex.getMessage());
        			}
        			badlinks+=links.size();
        		}
        	}
            offset+=pageSize;
        } while(contents.size()>0);
        Logger.info(this, "Finished checking for broken links for structure Link Semplice. Processed "+processed+" contentlets. Found "+badlinks+" broken links");
       
    }
}
