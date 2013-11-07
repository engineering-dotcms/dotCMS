package com.eng.dotcms.mostresearchedterms;

import java.util.List;
import com.dotmarketing.exception.DotDataException;
import com.eng.dotcms.mostresearchedterms.bean.MostResearchedTerms;
import com.eng.dotcms.mostresearchedterms.bean.MostResearchedTermsTemp;

public interface MostResearchedTermsAPI {
	
	void insertTempQuery(StringBuilder aQuery, long language, String host) throws DotDataException;
	
	List<MostResearchedTermsTemp> findAllTempQuery() throws DotDataException;
	
	void insertTerm(String term, long language, String host) throws DotDataException;
	
	void updateTerm(String term, long language, String host) throws DotDataException;
	
	void deleteTempQuery(int id) throws DotDataException;
	
	List<MostResearchedTerms> findAllTerms(long language, String host, int limit) throws DotDataException;
	
	Integer findOccurByTerm(String term, long language, String host) throws DotDataException;
}
