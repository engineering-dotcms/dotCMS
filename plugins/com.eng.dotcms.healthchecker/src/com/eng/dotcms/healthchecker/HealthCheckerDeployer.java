package com.eng.dotcms.healthchecker;

import java.util.Map;

import com.dotmarketing.cms.content.submit.PluginDeployer;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_ADD_HEALTH_TABLE;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CHECK_TABLE;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_ADD_HEALTH_CLUSTER_TABLE;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CREATE_INDEX_ADDRESS_EVENT;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CREATE_INDEX_ADDRESS_VIEW;
import static com.eng.dotcms.healthchecker.util.QueryBuilder.ORACLE_CREATE_INDEX_STATUS_VIEW;

public class HealthCheckerDeployer extends PluginDeployer {
	
	@Override
	public boolean deploy() {
		try{
			DotConnect dc = new DotConnect();			
			try {
				if(!existsTables(dc)){
					// create tables
					dc.setSQL(ORACLE_ADD_HEALTH_TABLE);
					dc.loadResult();
					dc.setSQL(ORACLE_ADD_HEALTH_CLUSTER_TABLE);
					dc.loadResult();
					dc.setSQL(ORACLE_CREATE_INDEX_ADDRESS_EVENT);
					dc.loadResult();
					dc.setSQL(ORACLE_CREATE_INDEX_ADDRESS_VIEW);
					dc.loadResult();
					dc.setSQL(ORACLE_CREATE_INDEX_STATUS_VIEW);
					dc.loadResult();					
				}
				return true;
			}catch(DotDataException e){
				Logger.error(this, e.getMessage(), e);
				return false;
			}
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

	protected boolean existsTables(DotConnect dc) {
		try{
			dc.setSQL(ORACLE_CHECK_TABLE);		
			Map<String, Object> row = dc.loadObjectResults().get(0); 
			if(Integer.parseInt(row.get("exist").toString())==2)
				return true;
			else
				return false;
		}catch(DotDataException e){
			return false;
		}
	}
	
}
