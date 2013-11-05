/**
 * 
 */
package it.bankit.website.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageKeyComparator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author cesare
 * 
 */
public class BankitCache implements Cachable {

	private static String PRIMARY_GROUP = "Bankit";
	private static String SUB_FOLDERS_GROUP = PRIMARY_GROUP + "-SubFolderCache";
	private static String LANGUAGE_GROUP = PRIMARY_GROUP + "-LanguageCache";
	private static String[] groups = { SUB_FOLDERS_GROUP, LANGUAGE_GROUP };
	private static BankitCache instance = null;
	private LanguageAPI langAPI;
	private FolderAPI folderAPI;
	private long cacheTtl = -1;// Default unlimited
	private Collection<String> invalidInodes = new Vector<String>();

	public static BankitCache getInstance() {
		if ( instance == null )
			instance = new BankitCache();
		return instance;
	}

	public BankitCache() {
		langAPI = APILocator.getLanguageAPI();
		folderAPI = APILocator.getFolderAPI();
		int cacheTtlBankit = Config.getIntProperty( "cache.bankit.ttl", -404 );
		if ( cacheTtlBankit != -404 ) {
			cacheTtl = cacheTtlBankit*60000;//Cache in Minuti
		} else {
			int cacheTtlDefault = Config.getIntProperty( "cache.default.ttl" );
			cacheTtl = cacheTtlDefault*60000;//Cache in Minuti
		}

	}

	public String getStringKey( Language lang, String key ) {
		String value = null;
		if ( lang != null && UtilMethods.isSet( key ) ) {
			try {
				value = cacheGetLanguage( lang, key );
				if ( value == null ) {
					value = internalGetStringKey( lang, key );
					if ( value != null ) {
						cachePutLanguage( lang, key, value );
					}
				}
			} catch ( DotCacheException e ) {
				Logger.debug( BankitCache.class, "Language Cache Entry not found(Lang:" + lang.toString() + ", key:" + key + ")", e );
			}
		}
		return ( value == null ? "" : value );
	}

	public List<Folder> findSubFolders( Folder folder ) {
		List<Folder> subFolders = null;
		if ( folder != null && UtilMethods.isSet( folder.getInode() ) ) {
			try {
				subFolders = cacheGetSubFolders( folder );
				if ( subFolders == null ) {
					subFolders = internalGetSubFolders( folder );
					if ( subFolders != null ) {
						cachePutSubFolders( folder, subFolders );
					}
				}
			} catch ( DotCacheException e ) {
				Logger.debug( BankitCache.class, "Subfolder Cache Entry not found(Folder:" + folder.getTitle() + ")", e );
			}
		}
		return subFolders;
	}

	private String generateLanguageKey( Language lang, String key ) {
		return LANGUAGE_GROUP + "_" + ( lang.getCountryCode() != null ? lang.getLanguageCode() + "_" + lang.getCountryCode() : lang.getLanguageCode() ) + "_Key_" + key;
	}

	private String cacheGetLanguage( Language lang, String key ) throws DotCacheException {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		@SuppressWarnings( "unchecked" )
		CacheResult<String> result = (CacheResult<String>) cache.get( generateLanguageKey( lang, key ), LANGUAGE_GROUP );
		return ( result != null && result.isValidTimestampResult() ? result.getObject() : null );
	}// When AOP meccanism active disable control of validity

	private void cachePutLanguage( Language lang, String key, String value ) throws DotCacheException {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		CacheResult<String> result = new CacheResult<String>();
		result.setValidityCheck( String.valueOf( System.currentTimeMillis() ) );
		result.setObject( value );
		cache.put( generateLanguageKey( lang, key ), result, LANGUAGE_GROUP );
	}

	public void cacheInvalidateLanguage( Language lang, String key ) throws DotCacheException {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		cache.remove( generateLanguageKey( lang, key ), LANGUAGE_GROUP );
	}

	private String internalGetStringKey( Language lang, String key ) {
		try {
			String value = null;
			if ( lang != null && UtilMethods.isSet( key ) ) {
				List<LanguageKey> keys = langAPI.getLanguageKeys( lang );
				LanguageKey searchKey = new LanguageKey( lang.getLanguageCode(), lang.getCountryCode(), key, null );
				int index = -1;
				if ( ( index = Collections.binarySearch( keys, searchKey, new LanguageKeyComparator() ) ) >= 0 ) {
					value = keys.get( index ).getValue();
				}
			}
			return ( UtilMethods.isSet( value ) ? value : "" );
		} catch ( Exception e ) {
			return null;
		}
	}

	private String generateSubFoldersKey( Folder folder ) {
		return SUB_FOLDERS_GROUP + "_" + folder.getInode();
	}

	private List<Folder> cacheGetSubFolders( Folder folder ) throws DotCacheException {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		@SuppressWarnings( "unchecked" )
		CacheResult<List<Folder>> result = (CacheResult<List<Folder>>) cache.get( generateSubFoldersKey( folder ), SUB_FOLDERS_GROUP );
		return ( result != null && result.isValidTimestampResult() ? result.getObject() : null );
		/*
		 * AOP logic invalidator return
		 * (checkFolderValidity(result)?result.getObject():null);
		 */
	}

	private void cachePutSubFolders( Folder folder, List<Folder> subfolders ) {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		CacheResult<List<Folder>> result = new CacheResult<List<Folder>>();
		result.setValidityCheck( String.valueOf( System.currentTimeMillis() ) );
		/*
		 * AOP logic invalidator StringBuilder sb=new StringBuilder();
		 * for(Folder curFolder:subfolders){ if(sb.length()>1){ sb.append( ','
		 * ); } sb.append( curFolder.getInode() ); }
		 * result.setValidityCheck(sb.toString());
		 */
		result.setObject( subfolders );
		cache.put( generateSubFoldersKey( folder ), result, LANGUAGE_GROUP );
	}

	private boolean checkFolderValidity( CacheResult<List<Folder>> result ) {
		if ( result != null ) {
			String inodesCheck = result.getValidityCheck();
			for ( String curInode : invalidInodes ) {
				if ( inodesCheck.contains( curInode ) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public void cacheInvalidateFolder( Folder folder ) {
		if ( !invalidInodes.contains( folder.getInode() ) ) {
			invalidInodes.add( folder.getInode() );
		}
	}

	private List<Folder> internalGetSubFolders( Folder folder ) {
		List<Folder> subFolders = new ArrayList<Folder>();
		try {
			subFolders.addAll( folderAPI.findSubFolders( folder, APILocator.getUserAPI().getSystemUser(), false ) );
			// TODO sort
		} catch ( Exception e ) {
			Logger.warn( BankitCache.class, "Errore nel recupero subfolders", e );
		}
		return subFolders;
	}

	@Override
	public String getPrimaryGroup() {
		return PRIMARY_GROUP;
	}

	@Override
	public String[] getGroups() {
		return groups;
	}

	@Override
	public void clearCache() {// clear the cache
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		for ( String group : groups ) {
			cache.flushGroup( group );
		}
		invalidInodes.clear();
	}

	private class CacheResult<T> {
		private String validityCheck;
		private T object;

		public String getValidityCheck() {
			return validityCheck;
		}

		public void setValidityCheck( String validityCheck ) {
			this.validityCheck = validityCheck;
		}

		public T getObject() {
			return object;
		}

		public void setObject( T object ) {
			this.object = object;
		}

		public boolean isValidTimestampResult() {
			if ( cacheTtl == -1 ) {
				return true;
			} else {
				long check = Long.parseLong( validityCheck );
				long now = System.currentTimeMillis();
				return ( now - check ) < cacheTtl;
			}
		}

	}

}
