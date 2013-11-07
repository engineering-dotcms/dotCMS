package it.eng.bankit.util;

import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

public class JobRepositoryInitializer {
	private DataSource dataSource;
	private String configFile;
	private JdbcTemplate jdbctemplate;

	/**
	 * This method reads the schema from an xml file and determines if it needs
	 * to issue DDL statements to create or modify tables already in the
	 * database.
	 * 
	 * @throws Exception
	 */
	public void checkDDL() throws Exception {

		jdbctemplate = new JdbcTemplate( dataSource );
		if ( !checkDB() ) {
			InputStreamReader reader = new InputStreamReader( getClass().getResourceAsStream( configFile ) );
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			int n;
			while ( ( n = reader.read( buffer ) ) != -1 ) {
				writer.write( buffer, 0, n );
			}
			String script = writer.toString();
			String[] statements = script.split( ";" );
			for ( String statement : statements ) {
				try {
					jdbctemplate.execute( statement );
				} catch ( Exception e ) {
					// Ignore
				}
			}
		}

	}

	private boolean checkDB() {
		boolean dbOk = false;
		List<Map<String,Object>> list=jdbctemplate.queryForList( "SELECT DISTINCT TABLE_NAME FROM INFORMATION_SCHEMA.SYSTEM_COLUMNS WHERE TABLE_NAME LIKE 'BATCH_%'" );
		//int numTable = jdbctemplate.queryForInt( "SELECT count(*) FROM INFORMATION_SCHEMA.SYSTEM_COLUMNS WHERE TABLE_NAME LIKE 'BATCH_%'" );
		dbOk = list.size() > 8;
		return dbOk;
	}

	public void shutdownDatabase() {
		jdbctemplate.update( "SHUTDOWN;" );
		System.out.println( "HSQLDB was gracefully shutdown. " );
	}

	public void setDataSource( DataSource dataSource ) {
		this.dataSource = dataSource;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setConfigFile( String configFile ) {
		this.configFile = configFile;
	}

	public String getConfigFile() {
		return configFile;
	}

}
