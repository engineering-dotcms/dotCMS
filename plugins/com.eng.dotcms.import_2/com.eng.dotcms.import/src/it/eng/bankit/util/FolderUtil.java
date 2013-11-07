package it.eng.bankit.util;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Treeable;
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
import com.dotmarketing.util.UtilMethods;

public class FolderUtil {
	private static Logger LOG = Logger.getLogger( FolderUtil.class );
	public static String INDEX_PAGE = null;
	public static String RSS_PAGE = null;
	private static List<Template> listaTemplates = null;
	private static boolean initialized = false;

	public static void init() {
		try {
			INDEX_PAGE = "index." + com.dotmarketing.util.Config.getStringProperty( "VELOCITY_PAGE_EXTENSION" );
			RSS_PAGE = "rss."+ com.dotmarketing.util.Config.getStringProperty( "VELOCITY_PAGE_EXTENSION" );
			listaTemplates = APILocator.getTemplateAPI().findTemplatesAssignedTo( ImportUtil.getHost() );
			initialized = true;
		} catch ( DotDataException e ) {
			LOG.error( "Error initializing", e );
		}
	}

	public static Template findTemplate( String templateName ) {
		Template template = null;
		if ( !initialized )
			init();
		for ( Template t : listaTemplates ) {
			if ( t.getTitle().equalsIgnoreCase( templateName ) ) {
				template = t;
				break;
			}
		}
		return template;
	}

	public static Folder findOrCreateFolder(String path) throws DotStateException, DotDataException, DotSecurityException{
		Folder folder = findFolder( path );
		if ( folder == null || !UtilMethods.isSet( folder.getInode() ) ) {
			folder = createFolder( path, 0, false );
		}
		return folder;
	}

	public static Folder findFolder( String path ) {
		String internalPath = path;
		try {
			if ( !internalPath.startsWith( File.separator ) ) {
				internalPath = File.separator + internalPath;
			}
			StringTokenizer pathTokenized = new StringTokenizer(internalPath, File.separator  );
			StringBuffer sbPath = new StringBuffer(File.separator );
			while (pathTokenized.hasMoreTokens()) {
				String curFolderName = pathTokenized.nextToken(); 	
				curFolderName = ImportUtil.getAlias( curFolderName );//search for alias
				sbPath.append(curFolderName);
				sbPath.append( File.separatorChar );
			}
			String resultPath = sbPath.toString(); 

			Folder folder = APILocator.getFolderAPI().findFolderByPath( resultPath, ImportUtil.getHost(), ImportUtil.getUser(), true );
			if ( folder == null || !InodeUtils.isSet( folder.getInode() ) ) {
				return null;
			} else {
				return folder;
			}
		} catch ( Exception e ) {
			LOG.error( "Error finding folder:"+path, e );
			return null;
		}
	}

	public static Folder createFolder( String path, Integer sortOrder, boolean showOnMenu ) throws DotStateException, DotDataException, DotSecurityException {
		return createFolder( path, sortOrder, showOnMenu, null, null );
	}

	public static Folder createFolder( String path, Integer sortOrder, boolean showOnMenu, Map<String, String> translations, Map<String, String> accesskeys )
	throws DotStateException, DotDataException, DotSecurityException {
		Folder f = null;
		Folder parent = null;
		StringTokenizer pathTokenizer = new StringTokenizer( path, "/" );
		StringBuffer pathBuffer = new StringBuffer( "/" );
		while ( pathTokenizer.hasMoreTokens() ) {
			String name = pathTokenizer.nextToken();
			// addI18NVariable(name , struct );
			pathBuffer.append( name + "/" );
			f = findFolder( pathBuffer.toString() );
			String pathKey = escapePath( path );
			if ( f == null ) {
				LOG.info( "FOLDER TO CREATE " + name );
				if ( !pathTokenizer.hasMoreTokens() ) {// Folder specific options
					f = createFolderObject(name,pathKey,parent,FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME,sortOrder,showOnMenu);
					if ( translations != null && !translations.isEmpty() ) {
						for ( String languageCode : translations.keySet() ) {
							String translation = translations.get( languageCode );
							if( UtilMethods.isSet(translation ) ){
								addLanguageVariable( ImportUtil.getLanguage( languageCode ), pathKey, translation );
							}
						}
					}
					if ( accesskeys != null && !accesskeys.isEmpty() ) {
						for ( String languageCode : accesskeys.keySet() ) {
							String translation = accesskeys.get( languageCode );
							if( UtilMethods.isSet( translation ) ){
								addLanguageVariable( ImportUtil.getLanguage( languageCode ), pathKey + "_ak",translation  );
							}
						}
					}
				} else {// Parent folder with default options
					f = createFolderObject(name,pathKey,parent,FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME,0,false);
				}
				APILocator.getFolderAPI().save( f, ImportUtil.getUser(), true );
			} else {
				LOG.debug( "La cartella " + name + " esiste sull'host " + ImportUtil.getHost().getHostname() );
			}
			parent = f;
		}
		return f;
	}

	public static Folder createFolderObject( String name, String title , Folder parent , String defaultFileType , int sortOrder , boolean isShowOnMenu) throws DotDataException {
		Folder folder= new Folder();
		try{
			Host host = ImportUtil.getHost();
			folder.setName( name );
			folder.setTitle( title );
			folder.setShowOnMenu( isShowOnMenu );
			folder.setSortOrder( sortOrder);
			folder.setHostId( host.getIdentifier() );
			if(UtilMethods.isSet( defaultFileType )){
				folder.setDefaultFileType( StructureCache.getStructureByVelocityVarName(defaultFileType).getInode());
			}else{
				folder.setDefaultFileType( StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("ALLEGATO_STRUCTURE") ).getInode());
			}
			Treeable pFolder;
			if(UtilMethods.isSet(parent )){
				pFolder=parent ;
			}
			else{
				pFolder=host ;
			}
			Identifier id=APILocator.getIdentifierAPI().createNew(folder, pFolder);
			folder.setIdentifier(id.getId());

		}catch (Exception e) {
			e.printStackTrace();
			LOG.error( "createFolder" + e.getMessage() );
		}
		return folder ;
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

			if( UtilMethods.isSet( value ) ) {
				Map<String, String> keysValue = new HashMap<String, String>();
				keysValue.put( key, value );
				try {
					String val = getStringKey( lang , key );
					if( !UtilMethods.isSet(val ) ){
						APILocator.getLanguageAPI().saveLanguageKeys(  lang, keysValue, keysValue  , null );
					}
				} catch ( Exception e ) {
					System.out.println("ERRORE in addLanguageVariable: Dati input metodo " + lang.getLanguage() + " key " + key + " value " + value  );
					LOG.error( e.getMessage(), e );
				}
			}
		}
	}

	public static String getStringKey(Language lang, String key) {
		String value = null;

		if ( lang != null && UtilMethods.isSet( key ) ) {
			List<LanguageKey> keys = APILocator.getLanguageAPI() .getLanguageKeys( lang  );
			LanguageKey searchKey = new LanguageKey( lang.getLanguageCode(), lang.getCountryCode(), key, null );
			int index = -1;
			if ((index = Collections.binarySearch(keys, searchKey, new LanguageKeyComparator())) >= 0) {
				value = keys.get(index).getValue();
			}
		}
		return (UtilMethods.isSet(value) ? value : "");
	}

	public static HTMLPage createIndexOnFolder( String title, String templateName, Folder folder ) throws DotDataException, DotStateException, DotSecurityException {
		return createPageOnFolder(INDEX_PAGE,title,templateName,folder,null);
	}

	public static HTMLPage createPageOnFolder( String pageUrl,String title, String templateName, Folder folder,Long cacheTTL ) throws DotDataException, DotStateException, DotSecurityException {
		HTMLPage pageCreated=null;
		if ( !initialized )
			init();
		Template template = findTemplate( templateName );
		if ( template != null ) {
			//	List<HTMLPage> livePages = APILocator.getHTMLPageAPI().findLiveHTMLPages( folder );
			HTMLPage workingPage = APILocator.getHTMLPageAPI().getWorkingHTMLPageByPageURL(pageUrl, folder );
			if (  workingPage == null ) {
				HTMLPage htmlPage = new HTMLPage();
				htmlPage.setParent( folder.getInode() );
				htmlPage.setFriendlyName( folder.getName() );
				htmlPage.setHttpsRequired( false );
				htmlPage.setIDate( new Date() );
				htmlPage.setMetadata( "" );
				htmlPage.setModDate( new Date() );
				htmlPage.setModUser( ImportUtil.getUser().getUserId() );
				htmlPage.setOwner( ImportUtil.getUser().getUserId() );
				htmlPage.setPageUrl( pageUrl );
				htmlPage.setRedirect( "" );
				htmlPage.setShowOnMenu( false );
				htmlPage.setSortOrder( 1 );
				htmlPage.setStartDate( new Date() );
				htmlPage.setTitle( title );
				htmlPage.setType( "htmlpage" );
				if(cacheTTL!=null){
					htmlPage.setCacheTTL( cacheTTL );
				}
				pageCreated=APILocator.getHTMLPageAPI().saveHTMLPage( htmlPage, template, folder, ImportUtil.getUser(), true );
			}
		}
		return pageCreated;
	}

	public static boolean isInitialized(){
		return initialized;
	}

}
