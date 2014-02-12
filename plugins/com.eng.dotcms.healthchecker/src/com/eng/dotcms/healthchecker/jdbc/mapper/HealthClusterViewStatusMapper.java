package com.eng.dotcms.healthchecker.jdbc.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import com.eng.dotcms.healthchecker.HealthClusterViewStatus;
import com.eng.dotcms.healthchecker.Operation;

public class HealthClusterViewStatusMapper implements RowMapper<HealthClusterViewStatus> {

	@Override
	public HealthClusterViewStatus mapRow(ResultSet rs, int arg1) throws SQLException {
		HealthClusterViewStatus status = new HealthClusterViewStatus();
		status.setAddress(rs.getString("address"));
		status.setCreator("Y".equals(rs.getString("creator")));
		status.setId(rs.getString("id"));
		status.setModDate(rs.getTimestamp("mod_date"));
		status.setOutForTimer("Y".equals(rs.getString("out_for_timer")));
		status.setPort(rs.getString("port"));
		status.setProtocol(rs.getString("protocol"));
		status.setStatus(rs.getString("status"));	
		status.setOperation(null==rs.getString("operation")?Operation.NOONE:Operation.fromString(rs.getString("operation")));
		return status;
	}

}
