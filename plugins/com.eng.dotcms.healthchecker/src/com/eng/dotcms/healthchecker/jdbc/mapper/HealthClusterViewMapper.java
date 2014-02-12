package com.eng.dotcms.healthchecker.jdbc.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import com.eng.dotcms.healthchecker.HealthClusterView;

public class HealthClusterViewMapper implements RowMapper<HealthClusterView> {

	@Override
	public HealthClusterView mapRow(ResultSet rs, int arg1) throws SQLException {
		HealthClusterView status = new HealthClusterView();
		status.setAddress(rs.getString("address"));
		status.setCreator("Y".equals(rs.getString("creator")));
		status.setId(rs.getString("id"));
		status.setModDate(rs.getDate("mod_date"));
		status.setOutForTimer("Y".equals(rs.getString("out_for_timer")));
		status.setPort(rs.getString("port"));
		status.setProtocol(rs.getString("protocol"));
		status.setStatus(rs.getString("status"));		
		return status;
	}

}
