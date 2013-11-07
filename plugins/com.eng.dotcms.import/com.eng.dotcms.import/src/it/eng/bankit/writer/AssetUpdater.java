package it.eng.bankit.writer;

import it.eng.bankit.util.HyperwaveKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class AssetUpdater implements ItemWriter<Map<String, String>>, InitializingBean {
	protected static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat( "yyyy/MM/dd hh:mm:ss" );
	public static long updated=0;
	public static long missed=0;
	private ContentletAPI contentletAPI = null;
	private FolderAPI folderAPI = null;
	private User user = null;
	private Host host = null;
	private Map<String,Language> languages=new HashMap<String,Language>(2);
	private boolean initialized = false;

	@Override
	public void write( List<? extends Map<String, String>> listaContlets ) {
		if ( !initialized ) {
			init();
		}

		for ( Map<String, String> curContent : listaContlets ) {
			Folder curFolder = null;
			try {
				if ( curContent.get( "path" ) != null ) {
					curFolder = folderAPI.findFolderByPath( curContent.get( "path" ), host, user, true );
					// curFolder = FolderUtil.findFolder( curContent.get( "path"
					// ) );
					if ( curFolder != null && InodeUtils.isSet( curFolder.getInode() ) ) {
						String fileName = curContent.get( "fileName" );
						String langCode = curContent.get( "HW_Language" );
						Language language=languages.get( langCode );
						StringBuilder query = new StringBuilder();
						query.append( "+structureName:AllegatoDettaglio" );
						query.append( " +conFolder:" );
						query.append( curFolder.getInode() );
						query.append( " +AllegatoDettaglio.fileName:" );
						query.append( fileName );
						query.append( " +languageId:" );
						query.append( language.getId() );
						query.append( " +working:true +live:true +deleted:false" );
						List<Contentlet> assets = contentletAPI.checkout( query.toString(), user, false, -1, 2 );
						if ( !assets.isEmpty() ) {
							for ( Contentlet curAsset : assets ) {
								updateAsset( curAsset, curContent.get( HyperwaveKey.TimeModified ) );
							}
						} else {
							Logger.warn( this.getClass(), "No asset (" + fileName + ") found in folder:" + curContent.get( "path" ) );
							missed++;
						}

					} else {
						Logger.warn( this.getClass(), "No folder:" + curContent.get( "path" ) + " found" );
						missed++;
					}
				} else {
					Logger.warn( this.getClass(), "Null path for" + curContent.toString() );
				}
			} catch ( Exception e ) {
				Logger.error( this.getClass(), "ERRORE update asset" + curContent.get( "fileName" ), e );
			}
		}
	}

	private void updateAsset( Contentlet asset, String dateProperties ) throws Exception {
		Date date = dateTimeFormat.parse( dateProperties );
		Date originalTimeModified = asset.getDateProperty( "timeModified" );
		Date originalPublicationTime = asset.getDateProperty( "dataPubblicazione" );
		if ( originalTimeModified == null || originalTimeModified.compareTo( date ) != 0 || originalPublicationTime == null || originalPublicationTime.compareTo( date ) != 0 ) {
			asset.setDateProperty( "timeModified", date );
			asset.setDateProperty( "dataPubblicazione", date );
			Contentlet assetUpdated = contentletAPI.checkin( asset, user, false );
			contentletAPI.unlock( assetUpdated, user, false );
			contentletAPI.publish( assetUpdated, user, false );
			updated++;
			Logger.info( this.getClass(), "Update asset " +assetUpdated.get( "fileName" )+" "+ assetUpdated.getInode() + " OK" );
		} else {
			Logger.info( this.getClass(), "Update asset not needed " +asset.get( "fileName" )+" "+ asset.getInode() );
		}

	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	private void init() {
		updated=0;
		missed=0;
		contentletAPI = APILocator.getContentletAPI();
		folderAPI = APILocator.getFolderAPI();
		try {
			user = APILocator.getUserAPI().getSystemUser();
			host = APILocator.getHostAPI().findDefaultHost( user, false );
			languages.put( "en", APILocator.getLanguageAPI().getLanguage( "en", "US" ));
			languages.put( "it", APILocator.getLanguageAPI().getLanguage( "it", "IT" ));
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		initialized = true;
	}

}
