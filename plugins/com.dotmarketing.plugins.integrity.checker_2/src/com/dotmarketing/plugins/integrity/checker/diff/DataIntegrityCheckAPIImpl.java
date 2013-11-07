package com.dotmarketing.plugins.integrity.checker.diff;

import java.util.List;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;

public class DataIntegrityCheckAPIImpl extends DataIntegrityCheckAPI {
	
	private DataIntegrityResultMapper mapper = new DataIntegrityResultMapper();
	
	private static DataIntegrityCheckAPIImpl dicAPI = null;
	public static DataIntegrityCheckAPIImpl getInstance(){
		if(dicAPI == null){
			dicAPI = new DataIntegrityCheckAPIImpl();
		}
		return dicAPI;	
	}
	
	private final String SQL_GET_DIFF = 
		"select " +
		"local.inode as local_inode, local.md5 as local_md5, local.create_date as local_create_date, " +
		"remote.inode as remote_inode, remote.md5 as remote_md5, remote.create_date as remote_create_date "+
		"from data_integrity_check_local local, data_integrity_check_remote remote "+
		"where "+
		"local.inode = remote.inode "+
		"and  "+
		"local.md5 != remote.md5 ";
	
	@Override
	public List<DataIntegrityResultWrapper> getDiffList() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SQL_GET_DIFF);
			
			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.debug(DataIntegrityResultWrapper.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	
	private final String SQL_GET_LOCAL_ORPHAN = 
			"select " +
			"local.inode as local_inode, local.md5 as local_md5, local.create_date as local_create_date, " +
			"remote.inode as remote_inode, remote.md5 as remote_md5, remote.create_date as remote_create_date "+
			"FROM   data_integrity_check_local local "+
			"LEFT OUTER JOIN  data_integrity_check_remote remote "+
			"ON local.inode = remote.inode "+
			"where "+
			"remote.id is null ";
	@Override
	public List<DataIntegrityResultWrapper> getLocalOrphans() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SQL_GET_LOCAL_ORPHAN);
			
			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.debug(DataIntegrityResultWrapper.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	
	private final String SQL_GET_REMOTE_ORPHAN = 
		"select " +
		"local.inode as local_inode, local.md5 as local_md5, local.create_date as local_create_date, " +
		"remote.inode as remote_inode, remote.md5 as remote_md5, remote.create_date as remote_create_date "+
		"FROM   data_integrity_check_local local "+
		"RIGHT OUTER JOIN  data_integrity_check_remote remote "+
		"ON local.inode = remote.inode "+
		"where "+
		"local.id is null ";
				
	@Override
	public List<DataIntegrityResultWrapper> getRemoteOrphans() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SQL_GET_REMOTE_ORPHAN);
			
			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.debug(DataIntegrityResultWrapper.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
}
