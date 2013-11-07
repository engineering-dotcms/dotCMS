package it.eng.bankit.converter;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.FolderWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;

public abstract class GenericMultilanguageConverter extends GenericConverter {
	private static Logger logger = Logger.getLogger( GenericMultilanguageConverter.class );
	protected String TITOLO;
	protected String NOTE;
	protected String LINKS;

	protected FolderWrapper folder;
	protected Map<String, Contentlet> multilangContentlet = new HashMap<String, Contentlet>( 2 );
	protected List<String> languages = new LinkedList<String>();
	protected Map<String, String> titleMap = new HashMap<String, String>();
	protected Map<String, String> noteMap = new HashMap<String, String>();
	protected Map<String, Map<String, String>> linksMap = new HashMap<String, Map<String, String>>();

	@Override
	protected void readStructureProperties() throws Exception {
		super.readStructureProperties();
		TITOLO = "titolo";
		NOTE = "note";
		LINKS = "links";
	}

	@Override
	public ContentletContainer parseContent() throws Exception {
		init();
		setValues();
		ContentletContainer rValue = new ContentletContainer();
		folder = readFolderProperties();
		rValue.setFolder( folder );
		for ( String languageCode : multilangContentlet.keySet() ) {
			rValue.add( populateWrapper( languageCode ) );
		}
		if ( !propertiesToProcess.isEmpty() ) {
			logger.warn( "Contentlet (" + titleMap.values() + ") in " + rValue.getFolder().getPath() + "\n Unprocessed properties:" + propertiesToProcess );
		}
		return rValue;
	}

	protected ContentletWrapper populateWrapper( String languageCode ) {
		ContentletWrapper wrapper = new ContentletWrapper();
		Contentlet contentlet = multilangContentlet.get( languageCode );
		wrapper.setContentlet( contentlet );
		wrapper.setQuery( getIdentityQuery( languageCode ) );
		if ( archived != null ) {
			wrapper.setArchived( archived );
		}
		return wrapper;
	}

	@Override
	protected void initContentlet() throws DotStateException, DotDataException, DotSecurityException {
		for ( String languageCode : languages ) {
			Contentlet contentlet = createContentlet( structure, languageCode );
			multilangContentlet.put( languageCode, contentlet );
		}
	}

	protected FolderWrapper readFolderProperties() throws DotStateException, DotDataException, DotSecurityException {
		FolderWrapper folderWrapper = new FolderWrapper();
		StringBuffer searchPath = new StringBuffer();

		Map<String, String> accessKeys = readMultilanguageProperty( HyperwaveKey.accessKey );

		if ( !getStruct().getFilePath().startsWith( "/" ) ) {
			searchPath.append( "/" );
		}
		searchPath.append( getStruct().getFilePath() );
		if ( !getStruct().getFilePath().endsWith( "/" ) ) {
			searchPath.append( "/" );
		}
		folderWrapper.setPath( searchPath.toString() );
		folderWrapper.setTemplateName( getTemplateName() );
		folderWrapper.setSortOrder( getSortOrder() );
		if ( archived == null || !archived ) {
			/*
			 * se Archiviato non vengono inserite le traduzioni per non
			 * mostrarlo sui menù
			 */
			folderWrapper.setShowOnMenu( isShowOnMenu() );
			folderWrapper.setTranslations( titleMap );
			if ( isShowOnMenu() ) {
				folderWrapper.setAccessKeys( accessKeys );
			}
		}

		return folderWrapper;
	}

	@Override
	protected void readProperties() throws Exception {
		super.readProperties();
		readLanguageAndTitle( properties );
		noteMap = readMultilanguageProperty( HyperwaveKey.Description );
		if ( hmiStructure.hasLinks() ) {
			for ( HmiStructure childrenLink : hmiStructure.getChildrenLinks() ) {
				readLink( childrenLink.getPropertiesHmi() );
			}
		}
	}

	protected void readLanguageAndTitle( Map<String, String> properties ) throws Exception {
		Map<String, String> title = readMultilanguageProperty( HyperwaveKey.Title, properties );

		if ( !title.isEmpty() ) {
			for ( String langCode : title.keySet() ) {
				titleMap.put( langCode, title.get( langCode ) );
			}

		}
		// IGNORE HW_Language
		propertiesToProcess.remove( HyperwaveKey.HW_Language );

		// Le lingue vengono determinate in base al titolo
		if ( titleMap.size() > languages.size() ) {
			for ( String lang : titleMap.keySet() ) {
				if ( !languages.contains( lang ) ) {
					languages.add( lang );
				}
			}
		}
	}

	protected void readLink( Map<String, String> linkProperties ) {
		String protocol = linkProperties.get( HyperwaveKey.Protocol );
		String host = linkProperties.get( HyperwaveKey.Host );
		String port = linkProperties.get( HyperwaveKey.Port );
		String path = linkProperties.get( HyperwaveKey.Path );
		Map<String, String> title = readMultilanguageProperty( HyperwaveKey.Title, linkProperties );

		StringBuilder url = new StringBuilder();
		if ( protocol != null ) {
			url.append( protocol );
		} else {
			url.append( "http" );
		}
		url.append( "://" );
		url.append( host );
		if ( port != null && !port.equals( "80" ) ) {
			url.append( ":" + port );
		}
		if ( path != null ) {
			if ( !path.startsWith( "/" ) ) {
				url.append( '/' );
			}
			url.append( path );
		}
		for ( String lang : title.keySet() ) {
			Map<String, String> links = linksMap.get( lang );
			if ( links == null ) {
				links = new HashMap<String, String>();
				linksMap.put( lang, links );
			}
			links.put( title.get( lang ), url.toString() );
		}
	}

	protected void setValues() throws Exception {
		for ( String language : multilangContentlet.keySet() ) {
			Contentlet contentlet = multilangContentlet.get( language );
			internalSetValues( contentlet );
			internalSetLanguageValues( contentlet, language );
		}
	}

	/* To extend */
	protected void internalSetLanguageValues( Contentlet contentlet, String language ) throws Exception {
		if ( titleMap.containsKey( language ) ) {
			String curTitle = titleMap.get( language );
			if ( curTitle.length() < 255 ) {
				contentlet.setProperty( TITOLO, curTitle );
			} else {
				contentlet.setProperty( TITOLO, curTitle.substring( 0, 255 ) );
			}
		}
		if ( noteMap.containsKey( language ) ) {
			String nota = noteMap.get( language );
			if ( nota.length() < 255 ) {
				contentlet.setProperty( NOTE, nota );
			} else {
				contentlet.setProperty( NOTE, nota.substring( 0, 255 ) );
			}
			contentlet.setProperty( NOTE, nota );
		}
		if ( linksMap.containsKey( language ) ) {
			JSONArray ja = new JSONArray();
			Map<String, String> links = linksMap.get( language );
			StringBuilder sb = new StringBuilder();
			sb.append( '{' );
			for ( String key : links.keySet() ) {
				JSONObject jo = new JSONObject();
				jo.put( key, links.get( key ) );
				ja.put( jo );
				if ( sb.length() > 1 ) {
					sb.append( ',' );
				}
				sb.append( '"' );
				sb.append( key );
				sb.append( '"' );
				sb.append( ':' );
				sb.append( '"' );
				sb.append( links.get( key ) );
				sb.append( '"' );
			}
			sb.append( '}' );

			contentlet.setProperty( LINKS, sb.toString() );
		}
	}

	// Convenient method for reading multilanguage properties with standard
	// ln:text...
	protected Map<String, String> readMultilanguageProperty( String propertyName ) {
		return readMultilanguageProperty( propertyName, properties );
	}

	protected Map<String, String> readMultilanguageProperty( String propertyName, Map<String, String> origin ) {
		Map<String, String> rValue = new HashMap<String, String>( 2 );
		for ( String curProperty : origin.keySet() ) {
			if ( curProperty.startsWith( propertyName ) ) {
				String[] languageProperty = curProperty.split( ":" );
				rValue.put( languageProperty[1], origin.get( curProperty ) );
				if ( origin == properties ) {
					propertiesToProcess.remove( curProperty );
				}
			}
		}
		return rValue;
	}

	protected void addFile( Contentlet contentlet, String propertyName, File file ) throws Exception {
		try {// Copy file in temp directory preventing after checkin deletion
			File tempFile = FileUtil.createTempFile( file );
			if ( !tempFile.isDirectory() ) {
				contentlet.setBinary( propertyName, tempFile );
			} else {
				logger.error( "Error in delete or create temp folder:" + tempFile.getName() );
			}

		} catch ( IOException e ) {
			logger.error( e.getMessage(), e );
		}
	}

	@Override
	public String getIdentityQuery( String languageCode ) {
		if ( structureName != null && folder != null && languageCode != null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "+structureName:" );
			sb.append( structureName );
			if ( UtilMethods.isSet( goId ) ) {
				sb.append( " +" );
				sb.append( structureName );
				sb.append( '.' );
				sb.append( GOID );
				sb.append( ":\"" );
				sb.append( goId );
				sb.append( '\"' );
			}
			sb.append(" +conhost:");
			sb.append( ImportUtil.getHost().getIdentifier());
			sb.append( " +path:*" );
			sb.append( ImportUtil.sanitizeLuceneParameter( folder.getPath() ) );
			sb.append( "* +languageId:" );
			sb.append( findLanguageId( languageCode ) );
			sb.append( " +deleted:false +working:true" );
			return sb.toString();
		} else
			return null;
	}
}
