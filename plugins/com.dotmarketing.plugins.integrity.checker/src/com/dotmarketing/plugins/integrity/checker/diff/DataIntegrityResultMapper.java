package com.dotmarketing.plugins.integrity.checker.diff;

import java.util.Date;
import java.util.Map;

import com.dotcms.publisher.mapper.CommonRowMapper;

public class DataIntegrityResultMapper extends CommonRowMapper<DataIntegrityResultWrapper> {

	@Override
	public DataIntegrityResultWrapper mapObject(Map<String, Object> row) {
		DataIntegrityResultWrapper objToReturn = new DataIntegrityResultWrapper();
		
		objToReturn.setLocalInode((String) row.get("local_inode"));
		objToReturn.setLocalMD5((String) row.get("local_md5"));
		objToReturn.setLocalCheckDate((Date) row.get("local_create_date"));
		
		objToReturn.setRemoteInode((String) row.get("remote_inode"));
		objToReturn.setRemoteMD5((String) row.get("remote_md5"));
		objToReturn.setRemoteCheckDate((Date) row.get("remote_create_date"));
		
		return objToReturn;
	}

}
