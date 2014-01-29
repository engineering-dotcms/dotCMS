package com.eng.dotcms.healthchecker.util;

public class QueryBuilder {
	
	public static final String ORACLE_CHECK_TABLE 						=	"SELECT COUNT(*) as exist " +
																			"FROM user_tables " +
																			"WHERE table_name in ( 'HEALTH_EVENT', 'HEALTH_CLUSTER_VIEW' , 'HEALTH_LOCK' )";

	public static final String ORACLE_ADD_HEALTH_TABLE					=	"CREATE TABLE HEALTH_EVENT ( " +
																			"	ADDRESS			VARCHAR2(30)  										NOT NULL, " +
																			"	CLUSTER_VIEW	VARCHAR2(400) 										NOT NULL, " +
																			"	STATUS			VARCHAR2(10) check (STATUS in ('JOIN','LEAVE'))  	NOT NULL, " +
																			"	WRITTEN_BY		VARCHAR2(30)  										NOT NULL, " +
																			"	MOD_DATE		TIMESTAMP	  										NOT NULL, " +
																			" 	CONSTRAINT health_checker_pk PRIMARY KEY(ADDRESS,STATUS)" +
																			")";
	
	public static final String ORACLE_ADD_HEALTH_CLUSTER_TABLE			=	"CREATE TABLE HEALTH_CLUSTER_VIEW ( " +
																			"	ID				VARCHAR2(36)  										NOT NULL, " +			
																			"	ADDRESS			VARCHAR2(30)  										NOT NULL, " +
																			"	PORT			VARCHAR2(10)  										NOT NULL, " +
																			"	PROTOCOL		VARCHAR2(5)   										NOT NULL, " +
																			"	STATUS			VARCHAR2(10) check (STATUS in ('JOIN','LEAVE'))  	NOT NULL, " +
																			"	CREATOR			CHAR 		 check (CREATOR in ('N','Y'))   		NOT NULL, " +
																			"	MOD_DATE		TIMESTAMP	  										NOT NULL, " +																			
																			" 	CONSTRAINT health_cluster_pk PRIMARY KEY(ID)" +
																			")";
		
	public static final String ORACLE_ADD_HEALTH_LOCK_TABLE				=	"CREATE TABLE HEALTH_LOCK ( " +
																			"	ADDRESS			VARCHAR2(30)  																NOT NULL, " +
																			"	OPERATION		VARCHAR2(10) check (OPERATION in ('FLUSHING','RESTARTING','STARTING'))  	NOT NULL, " +
																			" 	CONSTRAINT health_lock_pk PRIMARY KEY(ADDRESS)" +
																			")";
	
	public static final String ORACLE_INSERT_LOCK						=	"INSERT INTO HEALTH_LOCK (ADDRESS, OPERATION) VALUES (?, ?)";
	
	public static final String ORACLE_DELETE_LOCK						=	"DELETE FROM HEALTH_LOCK WHERE ADDRESS = ? AND OPERATION = ?";
	
	public static final String ORACLE_SELECT_LOCK						=	"SELECT count(*) AS NUM_OP FROM HEALTH_LOCK WHERE ADDRESS = ? AND OPERATION = ?";
	
	public static final String ORACLE_CREATE_INDEX_ADDRESS_EVENT		=	"CREATE INDEX idx_health_event_addr ON HEALTH_EVENT (ADDRESS)";
	
	public static final String ORACLE_CREATE_INDEX_ADDRESS_VIEW			=	"CREATE INDEX idx_health_view_addr ON HEALTH_CLUSTER_VIEW (ADDRESS)";
	
	public static final String ORACLE_CREATE_INDEX_STATUS_VIEW			=	"CREATE INDEX idx_health_view_status ON HEALTH_CLUSTER_VIEW (STATUS)";
	
	public static final String ORACLE_CREATE_INDEX_OP_LOCK				=	"CREATE INDEX idx_health_lock_op ON HEALTH_LOCK (OPERATION)";	
	
	public static final String ORACLE_INSERT_HEALTH						=	"INSERT INTO HEALTH_EVENT (ADDRESS, CLUSTER_VIEW, STATUS, WRITTEN_BY, MOD_DATE) VALUES (?,?,?,?,?)";
	
	public static final String ORACLE_DELETE_HEALTH						=	"DELETE FROM HEALTH_EVENT WHERE ADDRESS = ? AND STATUS = ?";
	
	public static final String ORACLE_CHECK_LEAVE						=	"SELECT count(*) AS NUM_LEAVE FROM HEALTH_EVENT WHERE STATUS='LEAVE'";
	
	public static final String ORACLE_GET_NODE_LEAVE					=	"SELECT * FROM HEALTH_CLUSTER_VIEW WHERE ADDRESS = ? AND STATUS='LEAVE' ORDER BY MOD_DATE DESC";
	
	public static final String ORACLE_INSERT_HEALTH_CLUSTER_VIEW		=	"INSERT INTO HEALTH_CLUSTER_VIEW (ID, ADDRESS, PORT, PROTOCOL, STATUS, CREATOR, MOD_DATE) VALUES (?,?,?,?,?,?,?)";
	
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
	
	public static final String ORACLE_GET_HEALTH_CLUSTER_VIEW_STATUS	=	"SELECT hcv.ID, hcv.ADDRESS, hcv.PORT, hcv.PROTOCOL, hcv.STATUS, hcv.CREATOR, hcv.MOD_DATE, hl.OPERATION " +
																			"FROM HEALTH_CLUSTER_VIEW hcv LEFT JOIN HEALTH_LOCK hl ON hcv.ADDRESS = hl.ADDRESS " +
																			"ORDER BY hcv.MOD_DATE desc";
	
	public static final String ORACLE_GET_SINGLE_CLUSTER_VIEW_STATUS	=	"SELECT * FROM HEALTH_CLUSTER_VIEW WHERE ADDRESS = ?";
	
	public static final String ORACLE_CHECK_JOIN_AFTER_LEAVE			=	"SELECT count(*) AS NUM_JOINED_AFTER FROM HEALTH_CLUSTER_VIEW WHERE ADDRESS = ? AND STATUS = 'JOIN' AND MOD_DATE > ?";
	
	public static final String ORACLE_CHECK_NEW_CONTENTLET				=	"SELECT count(*) AS NUM_CONTENTLETS FROM CONTENTLET_VERSION_INFO WHERE VERSION_TS > ? AND VERSION_TS < ?";
	
	public static final String ORACLE_CHECK_NEW_HTMLPAGE				=	"SELECT count(*) AS NUM_PAGES FROM HTMLPAGE_VERSION_INFO WHERE VERSION_TS > ? AND VERSION_TS < ?";
	
	public static final String ORACLE_CHECK_NEW_CONTAINER				=	"SELECT count(*) AS NUM_CONTAINERS FROM CONTAINER_VERSION_INFO WHERE VERSION_TS > ? AND VERSION_TS < ?";
	
	public static final String ORACLE_CHECK_NEW_TEMPLATE				=	"SELECT count(*) AS NUM_TEMPLATES FROM TEMPLATE_VERSION_INFO WHERE VERSION_TS > ? AND VERSION_TS < ?";
	
}
