package com.eng.dotcms.additions.workflow.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;

/**
 * Questa servlet prende in input un path di pagina a cui puntare e la visualizza in modalità edit
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 */
public class WorkflowPreviewHTMLPageServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5686159026515504966L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(req,resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getParameter("p");
		String userId = req.getParameter("uid");
		
		req.getSession().setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
		req.getSession().setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
		req.getSession().setAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION, "true");
		
		User user;
		try {
//			user = UserLocalManagerUtil.getUserById(userId);
//			req.setAttribute(WebKeys.USER, user);
			req.getRequestDispatcher(path).forward(req, resp);
		} catch (Exception e) {} 
//		catch (SystemException e) {
//			e.printStackTrace();
//		}
		
	}
	
	
}
