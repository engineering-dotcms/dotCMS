package com.dotmarketing.plugins.integrity.checker;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.PluginDeployer;
import com.dotmarketing.util.Logger;
// DOTCMS - 3800

public class IntegrityCheckerPluginDeployer implements PluginDeployer {

	public boolean deploy() {
		final String SQL_MYSQL_LOCAL = 
				"CREATE TABLE IF NOT EXISTS data_integrity_check_local ( "+
					"id varchar(36) primary key, "+
					"inode varchar(100),  "+
					"md5 varchar(700), "+
					"create_date DATETIME "+
				")";
		final String SQL_MYSQL_REMOTE = 
				"CREATE TABLE IF NOT EXISTS data_integrity_check_remote ( "+
				"id varchar(36) primary key, "+
				"inode varchar(100),  "+
				"md5 varchar(700), "+
				"create_date DATETIME "+
			")";
		
		final String SQL_ORACLE_LOCAL = 
				"CREATE TABLE data_integrity_check_local ( "+
					"id varchar(36) primary key, "+
					"inode varchar(100),  "+
					"md5 varchar(700), "+
					"create_date TIMESTAMP "+
				")";
		final String SQL_ORACLE_REMOTE = 
				"CREATE TABLE data_integrity_check_remote ( "+
				"id varchar(36) primary key, "+
				"inode varchar(100),  "+
				"md5 varchar(700), "+
				"create_date TIMESTAMP "+
			")";
		
		final String SQL_MSSQL_LOCAL = 
				"CREATE TABLE data_integrity_check_local ( "+
					"id varchar(36) primary key, "+
					"inode varchar(100),  "+
					"md5 varchar(700), "+
					"create_date DATETIME "+
				")";
		final String SQL_MSSQL_REMOTE = 
				"CREATE TABLE data_integrity_check_remote ( "+
				"id varchar(36) primary key, "+
				"inode varchar(100),  "+
				"md5 varchar(700), "+
				"create_date DATETIME "+
			")";
		
		
		final String SQL_PGRS_LOCAL = 
				"CREATE TABLE data_integrity_check_local ( "+
					"id varchar(36) primary key, "+
					"inode varchar(100),  "+
					"md5 varchar(700), "+
					"create_date TIMESTAMP "+
				")";
		final String SQL_PGRS_REMOTE = 
				"CREATE TABLE data_integrity_check_remote ( "+
				"id varchar(36) primary key, "+
				"inode varchar(100),  "+
				"md5 varchar(700), "+
				"create_date TIMESTAMP "+
			")";
		
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				try {
					dc.setSQL(SQL_PGRS_LOCAL);
					dc.loadResult();
					
					dc.setSQL(SQL_PGRS_REMOTE);
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				
				try {
					dc.setSQL(SQL_MYSQL_LOCAL);
					dc.loadResult();
					
					dc.setSQL(SQL_MYSQL_REMOTE);
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				try {
					dc.setSQL(SQL_MSSQL_LOCAL);
					dc.loadResult();
					
					dc.setSQL(SQL_MSSQL_REMOTE);
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}
			}else{
				try {
					dc.setSQL(SQL_ORACLE_LOCAL);
					dc.loadResult();
					
					dc.setSQL(SQL_ORACLE_REMOTE);
					dc.loadResult();
				} catch (DotDataException e) {
					Logger.error(this, e.getMessage(), e);
				}			
			}
		}finally{
			DbConnectionFactory.closeConnection();
		}
		
		return true;
	}

	public boolean redeploy(String version) {
		// TODO Auto-generated method stub
		return true;
	}

}
