package it.eng.bankit.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

public class ImportConfig {

	private static final String fileConfig ="importHY.properties";
	private static Properties props ;

	private static Properties loadProperties () {
		InputStream is =  null;
		if( fileConfig != null ){
			try {
				is = ImportConfig.class.getClassLoader().getResourceAsStream( fileConfig );
				props = new Properties();
				props.load( is );
			} catch ( Exception e) {
				e.printStackTrace();
			}finally{
				if( is!= null )
					try {
						is.close();
					} catch (IOException e) {					
						e.printStackTrace();
					}
			}
		}
		return props;
	}


	public static String[] getProperties(String key ) {
		if( props == null ){
			props = loadProperties();
		}
		String val = (String) props.get(key);
		if(  !StringUtils.isEmpty(val) && val.indexOf(";")!= -1 ){
			return  val.split(";");
		}
		return new String[]{};
	}

	public static String getProperty(String key ) {
		if( props == null ){
			props = loadProperties();
		}
		return (String) props.get(key);
	}



}