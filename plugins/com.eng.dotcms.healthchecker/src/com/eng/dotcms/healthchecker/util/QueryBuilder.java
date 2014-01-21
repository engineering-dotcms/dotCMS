package com.eng.dotcms.healthchecker.util;

public class QueryBuilder {
	
	public static final String ORACLE_CHECK_TABLE 						=	"SELECT COUNT(*) as exist " +
																			"FROM user_tables " +
																			"WHERE table_name = 'HEALTH'";

	public static final String ORACLE_ADD_HEALTH_TABLE					=	"CREATE TABLE HEALTH ( " +
																			"	ADDRESS			VARCHAR2(30)  NOT NULL, " +
																			"	CLUSTER_VIEW	VARCHAR2(400) NOT NULL, " +
																			"	STATUS			VARCHAR2(10)  NOT NULL, " +
																			"	WRITTEN_BY		VARCHAR2(30)  NOT NULL, " +
																			"	MOD_DATE		TIMESTAMP	  NOT NULL, " +
																			" 	CONSTRAINT health_checker_pk PRIMARY KEY(ADDRESS,STATUS)" +
																			")";
	
	public static final String ORACLE_INSERT_HEALTH						=	"INSERT INTO HEALTH (ADDRESS, CLUSTER_VIEW, STATUS, WRITTEN_BY, MOD_DATE) VALUES (?,?,?,?,SYSDATE)";
	
	public static final String ORACLE_DELETE_HEALTH						=	"DELETE FROM HEALTH WHERE ADDRESS = ? AND STATUS = ?";
	
	public static final String ORACLE_CHECK_LEAVE						=	"SELECT count(*) AS NUM_LEAVE FROM HEALTH WHERE STATUS='LEAVE'";
	
	public static final String ORACLE_GET_NODE_LEAVE					=	"SELECT * FROM HEALTH WHERE ADDRESS = ? AND STATUS='LEAVE'";
}
