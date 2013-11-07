package com.eng.dotcms.mostresearchedterms.util;

public class QueryBuilder {
	
	public static final String ORACLE_CHECK_TABLES 					=	"SELECT COUNT(*) as exist " +
																		"FROM user_tables " +
																		"WHERE table_name in ('MOST_RESEARCHED_TERMS', 'MOST_RESEARCHED_TERMS_TMP')";
	
	public static final String ORACLE_ADD_TERMS_TABLE				=	"CREATE TABLE MOST_RESEARCHED_TERMS ( " +
																		"	TERM	VARCHAR2(50) NOT NULL, " +
																		"	OCCUR	NUMBER(10,0) DEFAULT 1," +
																		"   LANGUAGE NUMBER(10,0), " +
																		"	HOST	VARCHAR2(36), " +
																		" 	CONSTRAINT mrt_pk PRIMARY KEY(TERM, LANGUAGE, HOST)" +
																		")";
	
	public static final String ORACLE_ADD_TEMP_TERMS_TABLE			=	"CREATE TABLE MOST_RESEARCHED_TERMS_TMP ( " +
																		"	ID		INTEGER NOT NULL, " +
																		"	QUERY	VARCHAR2(500), " +
																		"	LANGUAGE NUMBER(10,0), " +
																		"	HOST	VARCHAR2(36), " +
																		"	CONSTRAINT mrt_temp_pk PRIMARY KEY(ID) " +
																		")";
	
	public static final String ORACLE_ADD_TEMP_TERMS_SEQ			=	"CREATE SEQUENCE MOST_R_TERMS_TMP_SEQ START WITH 1 INCREMENT BY 1";
	
	public static final String ORACLE_INSERT_INTO_TEMP				=	"INSERT INTO MOST_RESEARCHED_TERMS_TMP VALUES " +
																		"( MOST_R_TERMS_TMP_SEQ.NEXTVAL, ?, ?, ?)";
	
	public static final String ORACLE_SELECT_FROM_TEMP				=	"SELECT ID, QUERY, LANGUAGE, HOST " +
																		"FROM MOST_RESEARCHED_TERMS_TMP";
	
	public static final String ORACLE_INSERT_NEW_TERM				=	"INSERT INTO MOST_RESEARCHED_TERMS (TERM, LANGUAGE, HOST) VALUES " +
																		"( ?, ?, ? )";
	
	public static final String ORACLE_UPDATE_TERM					=	"UPDATE MOST_RESEARCHED_TERMS " +
																		"SET OCCUR = OCCUR+1 " +
																		"WHERE lower(TERM) = ? " +
																		"AND LANGUAGE = ? " +
																		"AND HOST = ?";
	
	public static final String ORACLE_CLEAN_TEMP					=	"DELETE FROM MOST_RESEARCHED_TERMS_TMP WHERE ID = ?";
	
	public static final String ORACLE_SELECT_TERMS					=	"SELECT * " +
																		"FROM (" +
																		"	SELECT TERM, OCCUR " +
																		"	FROM MOST_RESEARCHED_TERMS " +
																		"	WHERE LANGUAGE = ? " +
																		"	AND HOST = ? " +
																		"	ORDER BY TERM ASC " +
																		") WHERE rownum<=?";
	
	public static final String ORACLE_SELECT_OCCUR_BY_TERM			=	"SELECT OCCUR " +
																		"FROM MOST_RESEARCHED_TERMS " +
																		"WHERE lower(TERM) = ? " +
																		"AND LANGUAGE = ? " +
																		"AND HOST = ?";
	
}
