package com.eng.dotcms.mostresearchedterms;

import java.util.List;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.mostresearchedterms.bean.MostResearchedTermsTemp;
import com.eng.dotcms.mostresearchedterms.util.Util;

/**
 * Cron job for the Most researched terms. Read from the temp table and than group by into the new table.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Jan 3, 2013 - 2:34:49 PM
 */
public class MostResearchedTermsJob implements StatefulJob {

	private MostResearchedTermsAPI mrtAPI = new MostResearchedTermsAPIImpl();
	
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		Logger.debug(this, "Start filling the most researched terms table...");
		try{			
			// gets all the query that the users typed... 
			List<MostResearchedTermsTemp> queries = mrtAPI.findAllTempQuery();
			
			// for every row...
			for(MostResearchedTermsTemp row : queries){
				List<String> terms = Util.getQueryTermsList(row.getQuery().toString());
				HibernateUtil.startTransaction();
				for(String term:terms){
					try{
						mrtAPI.insertTerm(term,row.getLanguage().getId(),row.getHost());
						Logger.info(this, "New term...insert");						
					}catch(Exception e){
						Logger.info(this, "Term already exists...update count");
						mrtAPI.updateTerm(term,row.getLanguage().getId(),row.getHost());					
					}
				}
				HibernateUtil.commitTransaction();
				Logger.info(this, "...delete this record from the temporary table.");
				mrtAPI.deleteTempQuery(row.getId());
			}
			Logger.debug(this, "End filling the most researched terms table...");
		}catch(DotDataException e){
			Logger.error(this, e.getMessage(), e);
			return;
		}
	}

}
