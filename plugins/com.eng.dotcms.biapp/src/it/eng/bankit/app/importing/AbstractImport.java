package it.eng.bankit.app.importing;

import it.eng.bankit.app.util.DisplayUtil;
import it.eng.bankit.app.util.FileReaderUtil;
import it.eng.bankit.app.util.RemotePublisher;
import it.eng.bankit.deploy.IDeployConst;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public abstract class AbstractImport {
	protected static final SimpleDateFormat hwDateFormat = new SimpleDateFormat( "yyyy/MM/dd hh:mm:ss" );
	protected static final SimpleDateFormat simpleHwDateFormat = new SimpleDateFormat( "yyyy/MM/dd" );
	protected static final SimpleDateFormat luceneDateFormat = new SimpleDateFormat( "MM/dd/yyyy" );

	protected PluginAPI pluginAPI = APILocator.getPluginAPI();
	protected ContentletAPI contentletApi = APILocator.getContentletAPI();
	protected FolderAPI folderApi = APILocator.getFolderAPI();
	protected LanguageAPI languageApi = APILocator.getLanguageAPI();
	protected PermissionAPI permissionApi = APILocator.getPermissionAPI();
	protected IdentifierAPI identifierApi = APILocator.getIdentifierAPI();

	protected User user;
	protected Host host;
	protected Language langIt;
	protected Language langEn;
	protected Map<String, String> idMap = new HashMap<String, String>();
	protected RemotePublisher rPublisher;

	protected Set<String> contentToBackup = new TreeSet<String>();
	protected Set<String> contentToPublish = new TreeSet<String>();
	protected Set<String> contentToUnPublish = new TreeSet<String>();

	protected String backupBundleId = null;
	protected String publishBundleId = null;
	protected String unpublishBundleId = null;

	protected Date backupStartTime = null;
	protected Date publishStartTime = null;
	protected Date unpublishStartTime = null;

	protected boolean initialized = false;
	protected boolean remoteBackup = false;
	protected boolean remotePublished = false;
	protected boolean updateMode = false;
	protected String structureName;

	public AbstractImport( User userImport, Host host ) {
		this.user = userImport;
		this.host = host;
		rPublisher = new RemotePublisher( user );
	}

	protected void init() throws Exception {
		langIt = languageApi.getLanguage( "it", "IT" );
		langEn = languageApi.getLanguage( "en", "US" );
		initialized = true;
	}

	protected Contentlet createContentlet( Structure structure, Language lang ) throws Exception {
		Contentlet contentlet = null;
		contentlet = new Contentlet();
		Date data = new Date();
		contentlet.setStructureInode( structure.getInode() );
		contentlet.setLanguageId( lang.getId() );
		contentlet.setHost( host.getIdentifier() );
		contentlet.setLastReview( data );
		contentlet.setDateProperty( "timeCreated", data );
		return contentlet;
	}

	protected Contentlet setCommonFields( Contentlet contentlet, File file ) {
		String testHtml = FileReaderUtil.getHtmlBody( file );
		String titolo = FileReaderUtil.getHtmlTitle( file );
		Logger.debug( getClass(), "Contenuto titolo " + titolo );
		contentlet.setProperty( "titolo", titolo );
		contentlet.setProperty( "corpoNotizia", testHtml );
		contentlet.setDateProperty( "importDate", new Date() );
		return contentlet;
	}

	protected Contentlet setFileAssetFields( Contentlet contentlet, String title, File file ) throws Exception {
		String fileName = FilenameUtils.getName( file.getName() );
		String description = FilenameUtils.removeExtension( fileName );

		if ( title.length() >= 255 ) {
			contentlet.setProperty( "titoloLungo", title );
			title = title.substring( 0, 254 );
			contentlet.setProperty( FileAssetAPI.TITLE_FIELD, title );
		} else {
			contentlet.setProperty( FileAssetAPI.TITLE_FIELD, title );
		}

		contentlet.setProperty( FileAssetAPI.DESCRIPTION, description );
		addFile( contentlet, FileAssetAPI.BINARY_FIELD, file );// Do not delete
		// afther
		// checkin
		contentlet.setProperty( FileAssetAPI.FILE_NAME_FIELD, fileName );
		contentlet.setDateProperty( "importDate", new Date() );
		contentlet.setLongProperty( FileAssetAPI.SORT_ORDER, 10L );
		return contentlet;
	}

	protected Contentlet persistContentlet(   Contentlet contentlet, String contextIdentifier ) throws DotDataException, DotContentletValidationException, DotContentletStateException,
	IllegalArgumentException, DotSecurityException {
		return persistContentlet( contentlet, contextIdentifier, null );
	}

	protected Contentlet persistContentlet(  Contentlet contentlet, String contextIdentifier, Category category ) throws DotDataException, DotContentletValidationException,
	DotContentletStateException, IllegalArgumentException, DotSecurityException {
		Contentlet returnContentlet = null;
		User insertUser = user;
		String identifier = idMap.get( contextIdentifier );
		if ( UtilMethods.isSet( identifier ) ) {
			contentlet.setIdentifier( identifier );
			Logger.info( this.getClass(), "Inserisco la traduzione della contentlet. Struttura  " +  contentlet.getStructure().getVelocityVarName() );
		}
		List<Permission> permissionList = permissionApi.getPermissions( getStructure( getStructureName() ));
		Logger.info( this.getClass(), " permissionList  " +  permissionList  );
		if ( category != null ) {
			List<Category> categories = Collections.singletonList( category );
			Logger.info( this.getClass(), " category getKey  " +  category.getKey()   );				
			returnContentlet = contentletApi.checkin( contentlet, categories, permissionList, insertUser, true );
		} else {
			returnContentlet = contentletApi.checkin( contentlet, permissionList, insertUser, true );
		}

		if ( returnContentlet.isLocked() ) {
			contentletApi.unlock( returnContentlet, insertUser, true );
		}

//		if ( returnContentlet.getStructure().getVelocityVarName().equalsIgnoreCase( "Link" ) && returnContentlet.getLanguageId() != languageApi.getDefaultLanguage().getId() ) {
//			try {// Pezzotto per gestire errore pubblicazione eventuale x
//				// linguagio non di default
//				contentletApi.publish( returnContentlet, insertUser, true );
//			} catch ( Exception e ) {
//				Logger.warn( this.getClass(), "Pubblicazione Link con errore gestito in language non di default ", e );
//			}
//		} else {			
			contentletApi.publish( returnContentlet, insertUser , true );
//		}

		String newIdentifier = returnContentlet.getIdentifier();
		if ( !UtilMethods.isSet( identifier ) && UtilMethods.isSet( newIdentifier ) ) {
			idMap.put( contextIdentifier, newIdentifier );
		}

		if ( UtilMethods.isSet( newIdentifier ) ) {
			contentToPublish.add( newIdentifier );
		} else {
			Logger.warn( this.getClass(), "Error identifier null inode:" + returnContentlet.getInode() );
		}
		return returnContentlet;
	}

	protected boolean removeContentlet( Contentlet contentlet ) throws DotContentletStateException, DotDataException, DotSecurityException {
		boolean removed = false;
		if ( contentlet != null && contentlet.getInode() != null ) {
			if ( contentlet.isLive() ) {
				contentletApi.unpublish( contentlet, user, true );
			}
			if ( contentlet.isLocked() ) {
				contentletApi.unlock( contentlet, user, true );
			}
			if ( !contentlet.isArchived() ) {
				contentletApi.archive( contentlet, user, true );
			}
			contentletApi.delete( contentlet, user, true );
			removed = true;
		}
		return removed;
	}

	public void remotePublish( boolean wait ) throws Exception {
		if ( rPublisher.remoteServerAvaiable() ) {
			if ( !contentToBackup.isEmpty() ) {
				String backupBundleId = rPublisher.publish( contentToBackup );
				backupStartTime = new Date();
				Logger.info( this.getClass(), "Remote Backup avviato (Bundle-id:" + backupBundleId + ")" );
				if ( wait ) {
					waitRemotePublication( backupBundleId );
					remoteBackup = true;
					Logger.info( this.getClass(), "Remote Backup effettuato con successo " + DisplayUtil.printAuditStatusDescription( backupBundleId, null, backupStartTime, "success" ) );
				}

			}

			if ( !contentToPublish.isEmpty() ) {
				String publishBundleId = rPublisher.publish( contentToPublish );
				publishStartTime = new Date();
				Logger.info( this.getClass(), "Remote Pubblishing avviato (Bundle-id:" + publishBundleId + ")" );
				if ( wait ) {
					waitRemotePublication( publishBundleId );
					remotePublished = true;
					Logger.info( this.getClass(), "Remote Pubblishing effettuato con successo " + DisplayUtil.printAuditStatusDescription( publishBundleId, null, publishStartTime, "success" ) );
				}
			}

			if ( !contentToUnPublish.isEmpty() ) {
				String unpublishBundleId = rPublisher.unPublish( contentToUnPublish );
				unpublishStartTime = new Date();
				Logger.info( this.getClass(), "Cancellazione remota avviata (Bundle-id:" + unpublishBundleId + ")" );
				if ( wait ) {
					waitRemotePublication( unpublishBundleId );
					Logger.info( this.getClass(), "Cancellazione remota effettuata con successo " + DisplayUtil.printAuditStatusDescription( unpublishBundleId, null, unpublishStartTime, "success" ) );
				}
			}

			// pubblicazione remota effettuata clear delle liste
			clear();
		} else {
			throw new Exception( "nessun server remoto disponibile, impossibile effettuare pubblicazione remota" );
		}

	}

	public void waitRemotePublication( String bundleId ) throws Exception {
		rPublisher.waitPubblication( bundleId );
	}

	public void clear() {
		contentToBackup.clear();
		contentToUnPublish.clear();
		contentToPublish.clear();
	}

	protected void addFile( Contentlet contentlet, String propertyName, File file ) throws Exception {
		try {
			File tempFile = new File( APILocator.getFileAPI().getRealAssetPath() + File.separator + "tmp_banca" + File.separator + System.currentTimeMillis(), file.getName() );
			FileUtil.copyFile( file, tempFile, false );
			contentlet.setBinary( propertyName, tempFile );
		} catch ( IOException e ) {
			Logger.error( ConsulWebImport.class, "Error copy file " + file.getName(), e );
		}
	}

	public Structure getStructure( String structureName ) {
		Structure structure = null;
		if ( !UtilMethods.isSet( structureName ) ) {
			structure = StructureCache.getStructureByVelocityVarName( structureName );
		}
		return structure;
	}

	protected Contentlet getContentlet( Structure st, Language lang, Folder folder ) {
		Contentlet contentlet = null;
		try {

			List<Contentlet> results = getContentlets( st, lang, folder );
			if ( results != null && results.size() > 0 ) {
				contentlet = results.get( 0 );
			}
		} catch ( Exception e ) {
			Logger.warn( AbstractImport.class, "Error quering content", e );
		}
		return contentlet;
	}

	protected List<Contentlet> getContentlets( Structure st, Language lang, Folder folder ) {
		List<Contentlet> results = new ArrayList<Contentlet>();
		try {
			StringBuilder query = new StringBuilder();
			if ( st != null ) {
				query.append( "+structureName:" );
				query.append( st.getVelocityVarName() );
			}
			if ( folder != null ) {
				query.append( " +conFolder:" );
				query.append( folder.getInode() );
			}
			if ( lang != null ) {
				query.append( " +languageId:" );
				query.append( lang.getId() );
			}
			results.addAll( contentletApi.search( query.toString(), 100, 0, "modDate desc", user, true ) );
		} catch ( Exception e ) {
			Logger.warn( AbstractImport.class, "Error quering content", e );
		}
		return results;
	}

	protected List<Contentlet> checkoutContentlets( Structure st, Language lang, Folder folder ) {
		List<Contentlet> results = new ArrayList<Contentlet>();
		try {
			StringBuilder query = new StringBuilder();
			if ( st != null ) {
				query.append( "+structureName:" );
				query.append( st.getVelocityVarName() );
			}
			if ( folder != null ) {
				query.append( " +conFolder:" );
				query.append( folder.getInode() );
			}
			if ( lang != null ) {
				query.append( " +languageId:" );
				query.append( lang.getId() );
			}
			query.append( " +deleted:false +working:true" );
			results.addAll( contentletApi.checkoutWithQuery( query.toString(), user, true ) );
		} catch ( Exception e ) {
			Logger.warn( AbstractImport.class, "Error quering content", e );
		}
		return results;
	}

	protected void deleteOldVersions( String id ) throws DotDataException, DotStateException, DotSecurityException {
		Identifier identifier = identifierApi.find( id );

		List<Contentlet> versions = contentletApi.findAllVersions( identifier, user, false );
		for ( Contentlet curVersion : versions ) {
			try {
				if ( !curVersion.isWorking() ) {
					contentletApi.deleteVersion( curVersion, user, false );
				}
			} catch ( DotDataException e ) {
				Logger.warn( this.getClass(), "Error deleting old version, identifier:" + id + " inode:" + curVersion.getInode(), e );
			}
		}
	}

	protected boolean readBooleanProperty( String key ) throws DotDataException {
		String property = pluginAPI.loadProperty( IDeployConst.PLUGIN_ID, key );
		if ( UtilMethods.isSet( property ) ) {
			return Boolean.parseBoolean( property );
		} else {
			return false;
		}
	}

	public boolean isUpdateMode() {
		return updateMode;
	}

	public void setUpdateMode( boolean updateMode ) {
		this.updateMode = updateMode;
	}

	public User getUser() {
		return user;
	}

	public String getStructureName() {
		return structureName;
	}


}
