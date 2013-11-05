package it.bankit.website.viewtool.navigation;

import it.bankit.website.cache.BankitCache;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class NavigationUtil implements ViewTool{

	private static Logger LOG = Logger.getLogger( NavigationUtil.class );

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static FolderAPI folderAPI = APILocator.getFolderAPI();
	private HttpServletRequest request;
	private Context context;
	
	@Override
	public void init( Object initData ) {
		this.context = ( (ViewContext) initData ).getVelocityContext();
		this.request = ( (ViewContext) initData ).getRequest();
	}

	public int stringTokenizer( String str, String delim ) {
		StringTokenizer st = new StringTokenizer( str, delim );
		int count = st.countTokens();
		return count;
	}

	public Template findLiveTemplate( String id ) throws DotDataException, DotSecurityException {
		Template t = null;
		try {
			if ( id != null ) {
				t = (Template) APILocator.getVersionableAPI().findLiveVersion( id, APILocator.getUserAPI().getSystemUser(), false );
				if ( t != null && InodeUtils.isSet( t.getInode() ) ) {
					if ( !permissionAPI.doesUserHavePermission( t, PermissionAPI.PERMISSION_READ, APILocator.getUserAPI().getSystemUser(), true ) ) {
						throw new DotSecurityException( "You don't have permission to read the source file." );
					}
				}
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return t;
	}
	

	public Folder getParentFolder( Folder f ) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		Folder parent = folderAPI.findParentFolder( f, userAPI.getSystemUser(), false );
		if ( parent != null && InodeUtils.isSet(parent.getInode())) {
			return parent;
		} else{
			return null;
		}
	}
	
	public Folder getRootParent( Folder f ) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		Folder returnF = f;
		Folder parent = folderAPI.findParentFolder( f, userAPI.getSystemUser(), false );
		while ( parent != null ) {
			returnF = parent;
			parent = folderAPI.findParentFolder( parent, userAPI.getSystemUser(), false );
		}
		return returnF;
	}

	public String getFolderPathByIdentifier( String folderId ) {
		Identifier id = new Identifier();
		try {
			id = APILocator.getIdentifierAPI().findFromInode( folderId.toString() );
			if ( id == null ) {
			}
		} catch ( DotDataException e ) {
			e.printStackTrace();
		}
		return id.getPath();
	}

	/**
	 * @deprecated  non c'è necessità di questo metodo
	 * @use $ctx instance instead
	 */
	@Deprecated 
	public Context getVelocityContext( HttpServletRequest request, HttpServletResponse response ) throws PortalException, SystemException, DotDataException,
			DotSecurityException {
		Context context = VelocityUtil.getWebContext( request, response );
		return context;
	}

	
	public Folder[] getSubFolder( String currentPath, boolean showOnMenu ) {
		return getSubFolder(currentPath,request,showOnMenu);
	}
	
	/**
	 * @deprecated  non c'è bisogno di passare la request
	 * @use getSubFolder(String currentPath, boolean showOnMenu) instead
	 */
	@Deprecated
	public Folder[] getSubFolder( String currentPath, HttpServletRequest request, boolean showOnMenu ) {
		List<Folder> fList = null;
		try {
			Host host = WebAPILocator.getHostWebAPI().getCurrentHost( request );
			Folder f = APILocator.getFolderAPI().findFolderByPath( currentPath, host, APILocator.getUserAPI().getSystemUser(), false );
			fList = BankitCache.getInstance().findSubFolders( f );
			return fList.toArray( new Folder[fList.size()] );
		} catch ( Exception ex ) {
			LOG.fatal( ex.getStackTrace() );
			return null;
		}
	}
	
	public Folder[] getSubFolder( Folder currentFolder, boolean showOnMenu ) {
		return getSubFolder(currentFolder,request,showOnMenu);
	}
	
	/**
	 * @deprecated  non c'è bisogno di passare la request
	 * @use getSubFolder(Folder currentFolder, boolean showOnMenu) instead
	 */
	@Deprecated
	public Folder[] getSubFolder( Folder currentFolder, HttpServletRequest request, boolean showOnMenu ) {
		List<Folder> fList = new ArrayList<Folder>();
	
		try {
			fList = BankitCache.getInstance().findSubFolders( currentFolder );// indSubFoldersRecursively(
		
			return fList.toArray( new Folder[fList.size()] );
		} catch ( Exception ex ) {
			LOG.fatal( ex.getStackTrace() );
			return null;
		}
	}

	public Folder[] getSubFolderForListing( Folder currentFolder ) {

		List<Folder> fList = new ArrayList<Folder>();
		List<Folder> fListAnno = new ArrayList<Folder>();
		try {
			fList = BankitCache.getInstance().findSubFolders( currentFolder );
			for ( Folder fold : fList ) {
				String anno = fold.getName();
				try {
					Integer.parseInt( anno );
					fListAnno.add( fold );
				} catch ( Exception e ) {
				}
			}
			return fList.toArray( new Folder[fList.size()] );
		} catch ( Exception ex ) {
			LOG.fatal( ex.getStackTrace() );
			return null;
		}
	}

 
}
