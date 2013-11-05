package it.bankit.website.cache;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

public class LiveCacheProxy {
	private String htmlExtension;

	public LiveCacheProxy() {
		 htmlExtension = "." + Config.getStringProperty( "VELOCITY_PAGE_EXTENSION" );
	}

	public CacheBean getPathFromCache( String URI, Host host ) throws DotStateException, DotDataException, DotSecurityException {
		CacheBean bean = new CacheBean();
		bean.setURI( URI );
		try {
			bean.setPath( LiveCache.getPathFromCache( URI, host ) );
		} catch ( DotStateException e ) {
			bean.setPath( null );
		}
		if ( bean.getPath() == null && !URI.endsWith( "/" ) && !URI.contains( htmlExtension ) ) {
			FileAsset asset = searchAssetUrl( URI, host );
			if ( asset != null ) {
				bean.setInode( asset.getInode() );
				bean.setLanguageId( asset.getLanguageId() );
				bean.setPath( addToLiveAssetToCache( asset, URI ) );
			} else {
				int fileAssetIndex = bean.getPath().indexOf( "/fileAsset/" );
				int dotIndex = bean.getPath().lastIndexOf( '.' );
				String inodeCandidate = null;
				if ( fileAssetIndex > 5 ) {
					inodeCandidate = bean.getPath().substring( 5, fileAssetIndex );
				} else if ( dotIndex > 5 ) {
					inodeCandidate = bean.getPath().substring( 5, dotIndex );
				}
				if ( InodeUtils.isSet( inodeCandidate ) && inodeCandidate.indexOf( '.' ) == -1 ) {
					bean.setInode( inodeCandidate );
				}
			}
		}

		return bean;
	}

	public FileAsset searchAssetUrl( String URI, Host host ) {
		String path = URI.substring( 0, URI.lastIndexOf( '/' ) + 1 );
		String fileName = URI.substring( URI.lastIndexOf( '/' ) + 1 );
		Folder folder = null;
		try {
			folder = APILocator.getFolderAPI().findFolderByPath( path, host, APILocator.getUserAPI().getAnonymousUser(), true );
			if (folder!=null&&InodeUtils.isSet(folder.getInode())){
				List<FileAsset> fileAssets = APILocator.getFileAssetAPI().findFileAssetsByFolder( folder, APILocator.getUserAPI().getAnonymousUser(), true );
				for ( FileAsset curAsset : fileAssets ) {
					if ( curAsset.getFileName().equals( fileName ) ) {
						return curAsset;
					}
				}
			}
			
		} catch ( Exception e ) {
			if(Logger.isDebugEnabled( LiveCacheProxy.class)){
				Logger.debug( LiveCacheProxy.class, "Error searching for URI:"+URI,e );
			}
		} 

		/*
		 * StringBuilder query=new StringBuilder(); query.append( "+conhost:" );
		 * query.append( host.getIdentifier() ); query.append(
		 * " +structureType:"); query.append(
		 * Structure.STRUCTURE_TYPE_FILEASSET); query.append( " +conFolder:" );
		 * query.append( folder.getInode() );
		 * query.append(" +deleted:false +working:true +live:true"); try {
		 * List<Contentlet>
		 * contentlets=APILocator.getContentletAPI().search(query.toString(),
		 * 10, -1, "modDate", APILocator.getUserAPI().getSystemUser(), true);
		 * if(!contentlets.isEmpty()){ Contentlet contentlet=contentlets.get( 0
		 * ); return addToLiveAssetToCache( contentlet,URI); } } catch (
		 * DotDataException e ) { Logger.error( LiveCacheProxy.class,
		 * e.getMessage(),e ); } catch ( DotSecurityException e ) {
		 * Logger.error( LiveCacheProxy.class, e.getMessage(),e ); }
		 */
		return null;
	}

	public String addToLiveAssetToCache( Contentlet contentlet, String URI ) {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		String path = APILocator.getFileAssetAPI().getRelativeAssetPath( APILocator.getFileAssetAPI().fromContentlet( contentlet ) );
		// add the entry to the cache
		Logger.debug( LiveCacheProxy.class, "Mapping: " + URI + " to " + path );
		cache.put( LiveCache.getPrimaryGroup() + contentlet.getHost() + ":" + URI, path, LiveCache.getPrimaryGroup() + "_" + contentlet.getHost() );
		return path;
	}
}
