package it.bankit.website.viewtool.navigation;

import it.bankit.website.viewtool.ContentUtil;
import it.bankit.website.viewtool.WebUtil;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class MenuNav implements ViewTool {

	protected static String MENU_VTL_PATH;
	protected static String SHORT_MENU_VTL_PATH;
	protected ContentletAPI conAPI;
	protected HttpServletRequest request;
	protected User user = null;
	protected LanguageAPI langAPI;
	protected ContentUtil cUtil;
	protected boolean EDIT_MODE;
	protected boolean ADMIN_MODE;
	protected boolean PREVIEW_MODE;
	protected boolean EDIT_OR_PREVIEW_MODE;
	
	protected WebUtil wUtil;

	protected Structure s;

	public int formCount = 0;

	static {
		String velocityRootPath = ConfigUtils.getDynamicVelocityPath() + java.io.File.separator;
		MENU_VTL_PATH = velocityRootPath + "menus" + java.io.File.separator;
		SHORT_MENU_VTL_PATH = ConfigUtils.getDynamicContentPath() + java.io.File.separator + "velocity" + java.io.File.separator;
	}

	public void init(Object obj) {
		
		s = StructureCache.getStructureByVelocityVarName("Link");
		
		cUtil = new ContentUtil();
		wUtil = new WebUtil();

		
		conAPI = APILocator.getContentletAPI();
		langAPI = APILocator.getLanguageAPI();
		
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		HttpSession ses = request.getSession(false);
		

		ADMIN_MODE = (ses.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		PREVIEW_MODE = ((ses.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
		EDIT_MODE = ((ses.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
		if(EDIT_MODE || PREVIEW_MODE){
			EDIT_OR_PREVIEW_MODE = true;
		}
		
		if (ses != null)
			user = (User) ses.getAttribute(WebKeys.CMS_USER);

		java.io.File fileFolder = new java.io.File(MENU_VTL_PATH);
		if (!fileFolder.exists()) {
			fileFolder.mkdirs();
		}
	}
	
	public List mergeList(List a, List b) {
		for (int i = 0; i < b.size(); i++) {

			Object[] list = (Object[]) b.get(i);

			if (list != null && list.length > 0) {

				for (int j = 0; j < list.length; j++) {
					if (list[j] instanceof com.dotmarketing.portlets.contentlet.business.Contentlet) {
						a.add((com.dotmarketing.portlets.contentlet.business.Contentlet) list[j]);
					}
				}

			}

		}
		return a;
	}


}
