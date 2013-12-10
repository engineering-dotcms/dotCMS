package it.eng.bankit.app.util;

import it.eng.bankit.deploy.IDeployConst;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageKeyComparator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class DotFolderUtil {
	private static String INDEX_PAGE;
	private static List<Template> listaTemplates = null;
	private static boolean initialized = false;
	private static Host defaultHost;

	public static void init() {
		try {
			INDEX_PAGE = "index." + com.dotmarketing.util.Config.getStringProperty( "VELOCITY_PAGE_EXTENSION" );
			User user = APILocator.getUserAPI().getSystemUser();
			String hostId =	APILocator.getPluginAPI().loadProperty( IDeployConst.PLUGIN_ID , "bankit.host");			
			defaultHost = APILocator.getHostAPI().find(hostId , user, true ); 
			listaTemplates = APILocator.getTemplateAPI().findTemplatesAssignedTo( defaultHost );
			initialized = true;
		} catch ( Exception e ) {
			Logger.error( DotFolderUtil.class, "Error initializing",e );
		}
	}
	public static Folder findOrCreateFolder( String path, boolean showOnMenu, Host host, User user ) {
		String internalPath = path;
		try {
			if ( !internalPath.startsWith( File.separator ) ) {
				internalPath = File.separator + internalPath;
			}
			StringTokenizer pathTokenized = new StringTokenizer( internalPath, File.separator );
			StringBuffer sbPath = new StringBuffer( File.separator );
			while ( pathTokenized.hasMoreTokens() ) {
				String curFolderName = pathTokenized.nextToken();

				sbPath.append( curFolderName );
				sbPath.append( File.separatorChar );
			}
			String resultPath = sbPath.toString();
			Folder folder = APILocator.getFolderAPI().findFolderByPath( resultPath, host, user, true );
			if ( folder == null || !InodeUtils.isSet( folder.getInode() ) ) {
				folder = createFolder( path, host, user, 0, showOnMenu );
			}
			return folder;
		} catch ( Exception e ) {
			Logger.error(DotFolderUtil.class, "Error finding folder:" + path, e );
			return null;
		}
	}

	public static Folder createFolder( String path, Host host, User user, Integer sortOrder, boolean showOnMenu) throws DotStateException, DotDataException, DotSecurityException {
		Folder f = null;
		Folder parent = null;
		StringTokenizer pathTokenizer = new StringTokenizer( path, "/" );
		StringBuffer pathBuffer = new StringBuffer( "/" );
		while ( pathTokenizer.hasMoreTokens() ) {
			String name = pathTokenizer.nextToken();
			pathBuffer.append( name + "/" );
			f = APILocator.getFolderAPI().findFolderByPath( pathBuffer.toString(), host, user, true );
			if ( f == null || !UtilMethods.isSet( f.getInode() )) {
				Logger.info(DotFolderUtil.class,  "FOLDER TO CREATE " + name );
				String pathKey = escapePath( pathBuffer.toString() );
				if ( !pathTokenizer.hasMoreTokens() ) {// Folder specific options
					f = createFolderObject( name, pathKey, host, parent, null, sortOrder, showOnMenu );
				} else {// Parent folder with default options
					f = createFolderObject( name, pathKey,host, parent, null, 0, false );
				}
				APILocator.getFolderAPI().save( f, user, true );
				Folder folderCheck = APILocator.getFolderAPI().findFolderByPath( pathBuffer.toString(), host, user, true );
				if (folderCheck==null || !UtilMethods.isSet( folderCheck.getInode() )|| !UtilMethods.isSet( folderCheck.getIdentifier() )){
					throw new DotDataException("Errore nel salvataggio del folder:"+pathBuffer.toString() +" host:"+host.getHostname());
				}else{
					f=folderCheck;
				}
			} else {
				Logger.debug(DotFolderUtil.class,  "La cartella " + name + " esiste sull'host " + host.getHostname() );
			}
			parent = f;
		}
		return f;
	}

	public static String escapePath( String path ) {
		String pathProcessed = path.trim().replace( "/", "." ).replace( " ", "-" );
		if ( pathProcessed.startsWith( "." ) ) {
			pathProcessed = pathProcessed.substring( 1 );
		}
		if ( pathProcessed.endsWith( "." ) ) {
			pathProcessed = pathProcessed.substring( 0, pathProcessed.length() - 1 );
		}
		return pathProcessed;
	}

	public static void addLanguageVariable( Language lang, String key, String value ) {
		String valueKey = APILocator.getLanguageAPI().getStringKey( lang, key );
		if ( !UtilMethods.isSet( valueKey ) || valueKey.equals( key ) ) {
			Map<String, String> keysValue = new HashMap<String, String>();
			keysValue.put( key, value );
			try {
				String val = getStringKey( lang, key );
				if ( !UtilMethods.isSet( val ) ) {
					APILocator.getLanguageAPI().saveLanguageKeys( lang, keysValue, keysValue, null );
				}
			} catch ( Exception e ) {
				// e.printStackTrace();
				Logger.error(DotFolderUtil.class, e.getMessage(), e );
			}
		}
	}

	public static String getStringKey( Language lang, String key ) {
		// User user = getRequestUser();
		String value = null;

		if ( lang != null && UtilMethods.isSet( key ) ) {
			List<LanguageKey> keys = APILocator.getLanguageAPI().getLanguageKeys( lang );
			LanguageKey searchKey = new LanguageKey( lang.getLanguageCode(), lang.getCountryCode(), key, null );
			int index = -1;
			if ( ( index = Collections.binarySearch( keys, searchKey, new LanguageKeyComparator() ) ) >= 0 ) {
				value = keys.get( index ).getValue();
			}
		}
		return ( UtilMethods.isSet( value ) ? value : "" );
	}
	
	public static HTMLPage createIndexOnFolder( String title, String templateName, Folder folder ) throws DotDataException, DotStateException, DotSecurityException {
		
		return createPageOnFolder(INDEX_PAGE,title,templateName,folder ,APILocator.getUserAPI().getSystemUser()  );
	}
	public static HTMLPage createPageOnFolder( String pageUrl,String title, String templateName, Folder folder , User user ) throws DotDataException, DotStateException, DotSecurityException {
		HTMLPage pageCreated=null;
		Template template = findTemplate( templateName );
		if ( template != null ) {
			if(pageUrl==null){
				pageUrl=INDEX_PAGE;
			}
			if(!pageUrl.endsWith( com.dotmarketing.util.Config.getStringProperty( "VELOCITY_PAGE_EXTENSION" ) )){
				throw new DotDataException("Invalid Page URL:"+pageUrl+" page extensiopn allowed:."+com.dotmarketing.util.Config.getStringProperty( "VELOCITY_PAGE_EXTENSION" ));
			}
			HTMLPage workingPage = APILocator.getHTMLPageAPI().getWorkingHTMLPageByPageURL(pageUrl, folder );
			if (  workingPage == null ) {
				//User user = APILocator.getUserAPI().getSystemUser();
				HTMLPage htmlPage = new HTMLPage();
				htmlPage.setParent( folder.getInode() );
				htmlPage.setFriendlyName( title );
				htmlPage.setHttpsRequired( false );
				htmlPage.setIDate( new Date() );
				htmlPage.setMetadata( "" );
				htmlPage.setModDate( new Date() );
				htmlPage.setModUser( user.getUserId() );
				htmlPage.setOwner( user.getUserId() );
				htmlPage.setPageUrl( pageUrl );
				htmlPage.setRedirect( "" );
				htmlPage.setShowOnMenu( false );
				htmlPage.setSortOrder( 1 );
				htmlPage.setStartDate( new Date() );
				htmlPage.setTitle( title );
				htmlPage.setType( "htmlpage" );
				pageCreated=APILocator.getHTMLPageAPI().saveHTMLPage( htmlPage, template, folder, user, true );
			}
		}else{
			throw new DotDataException("Impossibile creare la pagina:"+pageUrl+" template:"+templateName+" non trovato");
		}
		return pageCreated;
	}
	
	public static boolean publishPage(HTMLPage page) throws DotStateException, DotDataException, DotSecurityException{
		Identifier identifier = APILocator.getIdentifierAPI().find(page);
		// gets the current working asset

		WebAsset workingwebasset = null;

		// gets the current working asset
		workingwebasset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);
		if (workingwebasset==null||!InodeUtils.isSet(workingwebasset.getInode())) {
			workingwebasset = page;
		}

		if(workingwebasset.isDeleted()){
			throw new DotSecurityException("You may not publish deleted assets!!!");
		}
		
		WebAsset livewebasset = null;
		// gets the current working asset
		livewebasset = (WebAsset) APILocator.getVersionableAPI().findLiveVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);
		if (livewebasset==null||!InodeUtils.isSet(livewebasset.getInode())) {
			 APILocator.getVersionableAPI().setLive(workingwebasset);
			return true;
		}else{
			return false;
		}
		
	}

	private static Folder createFolderObject( String name, String title, Host host, Folder parent, String defaultFileType, int sortOrder, boolean isShowOnMenu )
			throws DotDataException {
		Folder folder = new Folder();
		try {
			folder.setName( name );
			folder.setTitle( title );
			folder.setShowOnMenu( isShowOnMenu );
			folder.setSortOrder( sortOrder );
			folder.setHostId( host.getIdentifier() );
			if ( UtilMethods.isSet( defaultFileType ) ) {
				folder.setDefaultFileType( StructureCache.getStructureByVelocityVarName( defaultFileType ).getInode() );
			} else {
				folder.setDefaultFileType( StructureCache.getStructureByVelocityVarName( FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME )
						.getInode() );
			}
			Identifier newIdentifier = null;
			if ( !UtilMethods.isSet( parent ) ) {
				newIdentifier = APILocator.getIdentifierAPI().createNew( folder, host );
			} else {

				try {// APILocator.getIdentifierAPI().loadFromCache(asset)
					newIdentifier = APILocator.getIdentifierAPI().createNew( folder, parent );
				} catch ( Exception e ) {
					// TODO: handle exception

				}
			}
			if ( newIdentifier != null )
				folder.setIdentifier( newIdentifier.getId() );
		} catch ( Exception e ) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return folder;
	}
	
	private static Template findTemplate( String templateName ) {
		Template template = null;
		if ( !initialized ) init();
		for ( Template t : listaTemplates ) {
			if ( t.getTitle().equalsIgnoreCase( templateName ) ) {
				template = t;
				break;
			}
		}
		return template;
	}

}
