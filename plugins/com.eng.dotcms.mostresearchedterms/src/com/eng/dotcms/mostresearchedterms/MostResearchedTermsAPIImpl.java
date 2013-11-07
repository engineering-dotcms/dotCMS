package com.eng.dotcms.mostresearchedterms;

import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_CLEAN_TEMP;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_INSERT_INTO_TEMP;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_INSERT_NEW_TERM;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_SELECT_FROM_TEMP;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_SELECT_TERMS;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_SELECT_OCCUR_BY_TERM;
import static com.eng.dotcms.mostresearchedterms.util.QueryBuilder.ORACLE_UPDATE_TERM;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.eng.dotcms.mostresearchedterms.bean.MostResearchedTerms;
import com.eng.dotcms.mostresearchedterms.bean.MostResearchedTermsTemp;

public class MostResearchedTermsAPIImpl implements MostResearchedTermsAPI {	
	
	private DotConnect dc = null;
	
	public MostResearchedTermsAPIImpl(){
		dc = new DotConnect();
	}
	
	@Override
	public void insertTempQuery(StringBuilder aQuery, long language, String host) throws DotDataException {
		dc.setSQL(ORACLE_INSERT_INTO_TEMP);
		dc.addParam(aQuery.toString());		
		dc.addParam(language);
		dc.addParam(host);
		dc.loadResult();
	}

	@Override
	public List<MostResearchedTermsTemp> findAllTempQuery() throws DotDataException {
		List<MostResearchedTermsTemp> mrtTemp = new ArrayList<MostResearchedTermsTemp>();
		dc.setSQL(ORACLE_SELECT_FROM_TEMP);
		List<Map<String, Object>> resultset = dc.loadObjectResults();
		for(Map<String, Object> row:resultset){
			MostResearchedTermsTemp mrt = new MostResearchedTermsTemp();
			mrt.setHost(row.get("host").toString());
			mrt.setId(Integer.parseInt(row.get("id").toString()));
			mrt.setLanguage(APILocator.getLanguageAPI().getLanguage(row.get("language").toString()));
			mrt.setQuery(new StringBuilder(row.get("query").toString()));
			mrtTemp.add(mrt);
		}
		return mrtTemp;
	}

	@Override
	public void insertTerm(String term, long language, String host) throws DotDataException {
		dc.setSQL(ORACLE_INSERT_NEW_TERM);
		dc.addParam(term.toLowerCase());
		dc.addParam(language);
		dc.addParam(host);
		dc.loadResult();
	}

	@Override
	public void updateTerm(String term, long language, String host) throws DotDataException {
		dc.setSQL(ORACLE_UPDATE_TERM);
		dc.addParam(term.toLowerCase());
		dc.addParam(language);
		dc.addParam(host);
		dc.loadResult();
	}

	@Override
	public void deleteTempQuery(int id) throws DotDataException {
		dc.setSQL(ORACLE_CLEAN_TEMP);
		dc.addParam(id);
		dc.loadResult();
	}

	@Override
	public List<MostResearchedTerms> findAllTerms(long language, String host, int limit) throws DotDataException {
		List<MostResearchedTerms> mostResTerms = new ArrayList<MostResearchedTerms>();
		dc.setSQL(ORACLE_SELECT_TERMS);
		dc.addParam(language);
		dc.addParam(host);
		dc.addParam(limit);
		List<Map<String, Object>> resultset = dc.loadObjectResults();
		for(Map<String, Object> row:resultset){
			MostResearchedTerms mrt = new MostResearchedTerms();
			mrt.setHost(host);
			mrt.setLanguage(APILocator.getLanguageAPI().getLanguage(language));
			mrt.setOccur(Integer.parseInt(row.get("occur").toString()));
			mrt.setTerm(row.get("term").toString());
			mostResTerms.add(mrt);
		}
		return mostResTerms;
	}

	@Override
	public Integer findOccurByTerm(String term, long language, String host) throws DotDataException {
		dc.setSQL(ORACLE_SELECT_OCCUR_BY_TERM);
		dc.addParam(term.toLowerCase());
		dc.addParam(language);
		dc.addParam(host);		
		return Integer.parseInt(dc.loadObjectResults().get(0).get("occur").toString());
	}


}
