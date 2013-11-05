package it.bankit.website.viewtool.navigation;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.velocity.VelocityServlet;
import com.dotmarketing.viewtools.navigation.NavResult;
import com.liferay.portal.model.User;

public class NavResultBankIT extends NavResult {

	private static final long serialVersionUID = 1L;

	private String href;
	private int order;
	private boolean hrefVelocity;
	private String parent;
	private String type;
	private String permissionId;
	private List<NavResult> children;
	private String hostId;
	private String folderId;
	private List<String> childrenFolderIds;
	private boolean checkPermissions;

	private User sysuser = null;

	public NavResultBankIT(String parent, String hostId, String folderId) {
		super(parent, hostId, folderId);
		this.hostId = hostId;
		this.folderId = folderId;
		this.parent = parent;
		hrefVelocity = false;
		href = "";
		order = 0;
		checkPermissions = Config.getBooleanProperty("ENABLE_NAV_PERMISSION_CHECK", false);
		try {
			sysuser = APILocator.getUserAPI().getSystemUser();

		} catch (DotDataException e) {
			Logger.warn(this, e.getMessage(), e);
		}

	}

	public NavResultBankIT(String parent, String host) {
		this(parent, host, null);
	}

	public void setChildren(List<NavResult> children) {
		this.children = children;
	}

	public void setChildrenFolderIds(List<String> childrenFolderIds) {
		this.childrenFolderIds = childrenFolderIds;
	}

	@Override
	public List<NavResult> getChildren() throws Exception {
		if (children == null && hostId != null && folderId != null) {
			// lazy loadinge children
			Host host = APILocator.getHostAPI().find(hostId, sysuser, true);
			Folder folder = APILocator.getFolderAPI().find(folderId, sysuser, true);
			Identifier ident = APILocator.getIdentifierAPI().find(folder);
			NavResult lazyMe = NavToolBankIT.getNav(host, ident.getPath());
			children = lazyMe.getChildren();
			childrenFolderIds = lazyMe.getChildrenFolderIds();
		}
		if (children != null) {
			ArrayList<NavResult> list = new ArrayList<NavResult>();
			for (NavResult nn : children) {
				if (nn instanceof NavResultBankIT) {
					if (nn.isFolder()) {
						// for folders we avoid returning the same instance
						// it could be changed elsewhere and we need it to
						// load its children lazily
						NavResult ff = new NavResultBankIT(folderId, ((NavResultBankIT) nn).hostId, ((NavResultBankIT) nn).folderId);
						ff.setTitle(nn.getTitle());
						ff.setHref(nn.getHref());
						ff.setOrder(nn.getOrder());
						ff.setType(nn.getType());
						ff.setPermissionId(nn.getPermissionId());
						list.add(ff);
					} else {
						list.add(nn);
					}
				}
			}

			if (checkPermissions) {
				// now filtering permissions
				List<NavResult> allow = new ArrayList<NavResult>(list.size());
				Context ctx = (VelocityContext) VelocityServlet.velocityCtx.get();
				HttpServletRequest req = (HttpServletRequest) ctx.get("request");
				User currentUser = WebAPILocator.getUserWebAPI().getLoggedInUser(req);
				if (currentUser == null)
					currentUser = APILocator.getUserAPI().getAnonymousUser();
				for (NavResult nv : list) {
					try {
						if (APILocator.getPermissionAPI().doesUserHavePermission(nv, PermissionAPI.PERMISSION_READ, currentUser)) {
							allow.add(nv);
						}
					} catch (Exception ex) {
						Logger.error(this, ex.getMessage(), ex);
					}
				}
				return allow;
			} else
				return list;
		} else {
			return new ArrayList<NavResult>();
		}

	}

}
