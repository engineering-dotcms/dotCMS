package com.eng.dotcms.healthchecker.util;

public class QueryBuilder {
	
	public static final String ORACLE_CHECK_TABLE 						=	"SELECT COUNT(*) as exist " +
																			"FROM user_tables " +
																			"WHERE table_name in ( 'HEALTH_EVENT', 'HEALTH_CLUSTER_VIEW' )";

	public static final String ORACLE_ADD_HEALTH_TABLE					=	"CREATE TABLE HEALTH_EVENT ( " +
																			"	ADDRESS			VARCHAR2(30)  									NOT NULL, " +
																			"	CLUSTER_VIEW	VARCHAR2(400) 									NOT NULL, " +
																			"	STATUS			VARCHAR2(10) check (STATUS in ('JOIN','LEAVE'))  	NOT NULL, " +
																			"	WRITTEN_BY		VARCHAR2(30)  									NOT NULL, " +
																			"	MOD_DATE		TIMESTAMP	  									NOT NULL, " +
																			" 	CONSTRAINT health_checker_pk PRIMARY KEY(ADDRESS,STATUS)" +
																			")";
	
	public static final String ORACLE_ADD_HEALTH_CLUSTER_TABLE			=	"CREATE TABLE HEALTH_CLUSTER_VIEW ( " +
																			"	ID				VARCHAR2(36)  									NOT NULL, " +			
																			"	ADDRESS			VARCHAR2(30)  									NOT NULL, " +
																			"	PORT			VARCHAR2(10)  									NOT NULL, " +
																			"	PROTOCOL		VARCHAR2(5)   									NOT NULL, " +
																			"	STATUS			VARCHAR2(10) check (STATUS in ('JOIN','LEAVE'))  	NOT NULL, " +
																			"	CREATOR			CHAR 		 check (CREATOR in ('N','Y'))   		NOT NULL, " +
																			"	MOD_DATE		TIMESTAMP	  									NOT NULL, " +																			
																			" 	CONSTRAINT health_cluster_pk PRIMARY KEY(ID)" +
																			")";
	
	
	public static final String ORACLE_INSERT_HEALTH						=	"INSERT INTO HEALTH_EVENT (ADDRESS, CLUSTER_VIEW, STATUS, WRITTEN_BY, MOD_DATE) VALUES (?,?,?,?,SYSDATE)";
	
	public static final String ORACLE_DELETE_HEALTH						=	"DELETE FROM HEALTH_EVENT WHERE ADDRESS = ? AND STATUS = ?";
	
	public static final String ORACLE_CHECK_LEAVE						=	"SELECT count(*) AS NUM_LEAVE FROM HEALTH_EVENT WHERE STATUS='LEAVE'";
	
	public static final String ORACLE_GET_NODE_LEAVE					=	"SELECT * FROM HEALTH_EVENT WHERE ADDRESS = ? AND STATUS='LEAVE'";
	
	public static final String ORACLE_INSERT_HEALTH_CLUSTER_VIEW		=	"INSERT INTO HEALTH_CLUSTER_VIEW (ID, ADDRESS, PORT, PROTOCOL, STATUS, CREATOR, MOD_DATE) VALUES (?,?,?,?,?,?,SYSDATE)";
	
	public static final String ORACLE_UPDATE_HEALTH_CLUSTER_CREATOR 	= 	"UPDATE HEALTH_CLUSTER_VIEW SET CREATOR = ? " +
																			"WHERE ID = ( " +
																			"	SELECT t.ID " +
																			"	FROM ( " +
																			"		SELECT * " +
																			"		FROM HEALTH_CLUSTER_VIEW " +
																			"		WHERE ADDRESS = ? " +
																			"		ORDER BY MOD_DATE DESC " +
																			"	) t " +
																			"	WHERE t.ROWNUM<=1 " +
																			")";
	
	public static final String ORACLE_DELETE_HEALTH_CLUSTER_VIEW		=	"DELETE FROM HEALTH_CLUSTER_VIEW WHERE ADDRESS = ?";
	
	public static final String ORACLE_GET_HEALTH_CLUSTER_VIEW_STATUS	=	"SELECT hcv.ID, hcv.ADDRESS, hcv.PORT, hcv.PROTOCOL, hcv.STATUS, hcv.CREATOR, hcv.MOD_DATE " +
																			"FROM HEALTH_CLUSTER_VIEW hcv " +
																			"ORDER BY hcv.MOD_DATE desc";
	
	public static final String ORACLE_GET_SINGLE_CLUSTER_VIEW_STATUS	=	"SELECT * FROM HEALTH_CLUSTER_VIEW WHERE ADDRESS = ?";
}
