package com.eng.dotcms.stat;

import java.util.Map;

import com.dotcms.publisher.mapper.CommonRowMapper;
import com.eng.dotcms.stat.bean.ClickStepBean;

public class ClickStepResultMapper extends CommonRowMapper<ClickStepBean> {

	@Override
	public ClickStepBean mapObject(Map<String, Object> row) {
		ClickStepBean objToReturn = new ClickStepBean();
		
		objToReturn.setPageName((String) row.get("request_uri"));
		objToReturn.setHintsCount(getIntegerFromObj(row.get("hints")));
		
		return objToReturn;
	}
	
}
