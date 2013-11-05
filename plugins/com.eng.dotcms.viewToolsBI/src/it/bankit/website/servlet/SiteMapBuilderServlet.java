package it.bankit.website.servlet;

import it.bankit.website.viewtool.navigation.NavToolBankIT;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.LogFactory;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.viewtools.navigation.NavResult;

public class SiteMapBuilderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private String bankitHostName ;
	private NavToolBankIT navTool = null;
	private Host currenthost = null;


	@Override
	public void init(ServletConfig config) throws ServletException {
		LogFactory.getLog( this.getClass()).info("SiteMapBuilderServlet servlet initializing ...");
		LogFactory.getLog( this.getClass()).info("[Init] SITEMAPBUILDERSERVLET");
		bankitHostName = getInitParameter("HOST_NAME");		
		getBankitHost();
		if (currenthost != null ) {
			navTool = new NavToolBankIT(currenthost);
			try {
				buildSiteMap();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {		 
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
	}


	private void buildSiteMap() throws DotDataException, Exception {
		NavResult rootNav = navTool.getNav("/");
		for (NavResult elem : rootNav) {
			navigateMap(elem.getChildren(), 1);
		}

	}
	private void navigateMap(List<NavResult> navs, int level) throws Exception {
		if (level <= 5) {
			for (NavResult elem : navs) {
				LogFactory.getLog(this.getClass()).info(">>>>>>> SiteMapBuilderServlet processo elemento : "+ elem.getTitle());
				if(elem.isFolder()){
					navigateMap (elem.getChildren(), level + 1);
				}
			}
		}
	}

	private boolean getBankitHost() {		

		List<Host> hosts = null;
		if( currenthost != null ){
			return true;
		}
		try {
			hosts = WebAPILocator.getHostWebAPI().getHostsWithPermission(
					PermissionAPI.PERMISSION_READ,
					APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (Host host : hosts) {
			if (bankitHostName.equals(host.getHostname().trim())) {
				currenthost = host;
				return true;
			}
		}

		return false;
	}

}
