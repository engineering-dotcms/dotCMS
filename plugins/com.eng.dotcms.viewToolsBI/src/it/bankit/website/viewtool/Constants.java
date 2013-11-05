package it.bankit.website.viewtool;

import java.io.IOException;
import java.util.Properties;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

public class Constants implements ViewTool {
	private static final String PROPERTIES_FILENAME = "VelocityConstants.properties";
	private static Properties properties = new Properties();
	private static boolean initialized=false;
	
	private void init(){
		try {
			properties.load( Constants.class.getClassLoader().getResourceAsStream( PROPERTIES_FILENAME ) );
			initialized=true;
		} catch ( IOException e ) {
			e.printStackTrace( System.err );
		}
	}

	@Override
	public void init( Object initData ) {
		if (!initialized) init();
		
		Context context = ( (ViewContext) initData ).getVelocityContext();
		for ( Object keyObj : properties.keySet() ) {
			String key = (String) keyObj;
			context.put( key, properties.getProperty( key ) );
		}
	}
}
