package com.eng.dotcms.stat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import com.dotcms.publisher.mapper.CommonRowMapper;
import com.eng.dotcms.stat.bean.SectionReport;

public class SectionReportResultMapper extends CommonRowMapper<SectionReport> {

	@Override
	public SectionReport mapObject(Map<String, Object> row) {
		SectionReport objToReturn = new SectionReport();
		
		try {
			objToReturn.setDate(new SimpleDateFormat("dd-MM-yyyy").parse((String) row.get("day_req")));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		objToReturn.setPage((String) row.get("request_uri"));
		objToReturn.setHints(getIntegerFromObj(row.get("hints")));
		
		return objToReturn;
	}
	
}
