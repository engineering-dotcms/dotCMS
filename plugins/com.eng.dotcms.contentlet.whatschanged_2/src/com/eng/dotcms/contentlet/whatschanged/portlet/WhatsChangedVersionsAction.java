package com.eng.dotcms.contentlet.whatschanged.portlet;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.liferay.portal.util.Constants;

public class WhatsChangedVersionsAction extends DotPortletAction {
	
	@Override
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {
		String cmd = req.getParameter(Constants.CMD);
		if("compare_versions".equals(cmd)) { //sono nella fase di confronto delle versioni
			req.setAttribute("compare_versions", "OK");
		}
		setForward(req, "portlet.ext.contentlet.view_whats_changed_versions");
	}

}
