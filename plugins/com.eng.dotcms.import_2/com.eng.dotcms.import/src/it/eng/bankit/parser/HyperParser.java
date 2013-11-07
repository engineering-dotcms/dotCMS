package it.eng.bankit.parser;

import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.ImportConfig;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;

public abstract class HyperParser {

	protected abstract List<HmiStructure>  readFile(  String path );
	
 	private User user = null;
	private HostAPI hostAPI = APILocator.getHostAPI();
	private Host host ;

	
	public void processHyperwave(  String path ){
		init();
		readFile( path );
	}

	private void init() {
		if( host  == null ){
			try {
				host =  hostAPI.findByName( ImportConfig.getProperty("HOST_NAME"), user, true );
				user = APILocator.getUserAPI().getSystemUser();
			}   catch ( Exception e) {
				e.printStackTrace();
			}		
		}
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}
	
	
	
}
