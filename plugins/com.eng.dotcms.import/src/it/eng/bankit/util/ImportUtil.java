package it.eng.bankit.util;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.LanguageCallback;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.web.util.UriUtils;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ImportUtil {


	private static User user = null;
	private static Host host ;
	private static List<Language> languages = null;
	private static String[] keyAlias = null;
	private static Map<String,String> aliasMap= null;
	private static String[] keyFileAlias = null;
	private static Map<String,String> fileAliasMap= null;
	private static Map<String, Language> languagesMap = null;
	private static   Language defaultLanguage = null;

	private static  Map<String, List<Relationship>> rels = new HashMap<String, List<Relationship>>(); 

	public static User 	 getUser(){
		if( user == null ){
			try {
				user = APILocator.getUserAPI().loadByUserByEmail( ImportConfig.getProperty("USER_MAIL"), 
						  APILocator.getUserAPI().getSystemUser(), true);
			}  catch ( Exception e1) {
				e1.printStackTrace();
			}
		}
		return user;
	}

	public static Host getHost(){
		if( host == null ){		 
			try {
				host = APILocator.getHostAPI().findByName( ImportConfig.getProperty("HOST_NAME"), user, true );
			}  catch ( Exception e1) {
				e1.printStackTrace();
			}
		}
		return host;
	}

	public static String getAlias( String folderName ){

		Map<String, String> als = getMapAlias();
		if( als.containsKey(folderName)){
			return als.get(folderName);
		}
		return folderName;
	}


	public static String getFileAlias( String fileName ){

		Map<String, String> mfals = getMapFileAlias();
		if( mfals.containsKey(fileName)){
			return mfals.get(fileName);
		}
		return fileName;
	}
	
	private static String[] getAliases(){
		if( keyAlias == null  ){
			keyAlias = ImportConfig.getProperties("FOLDER_ALIAS");

		}
		return keyAlias;
	}

	private static String[] getFilesAliases(){
		if( keyFileAlias == null  ){
			keyFileAlias = ImportConfig.getProperties("FILE_ALIAS");

		}
		return keyFileAlias;
	}
	public static Map<String, String> getMapAlias() {
		return initMapAlias( );
	}
	
	public static Map<String, String> getMapFileAlias() {
		return initMapFileAlias( );
	}


	private static Map<String, String> initMapAlias() {
		if( aliasMap == null ){
			aliasMap = new HashMap<String, String>();
		}
		String[] keyAlias2 = getAliases();
		for( String key : keyAlias2 ){
			String[] keyAl = key.split("=");
			aliasMap.put(keyAl[0], keyAl[1]);
		}	
		return aliasMap ;
	}

	private static Map<String, String> initMapFileAlias() {
		if( fileAliasMap == null ){
			fileAliasMap = new HashMap<String, String>();
		}
		String[] keyAlias2 = getFilesAliases();
		for( String key : keyAlias2 ){
			String[] keyAl = key.split("=");
			fileAliasMap.put(keyAl[0], keyAl[1]);
		}	
		return fileAliasMap ;
	}
	public static List<Language> getDotLanguages(){
		if( languages == null ){
			languages = APILocator.getLanguageAPI().getLanguages();			
		}
		return languages;
	}

	public static Language getLanguage(String code) {
		if (languagesMap == null) {
			List<Language> langs = getDotLanguages();
			languagesMap = new HashMap<String, Language>( langs.size());
			for (Language lang : langs) {
				languagesMap.put(lang.getLanguageCode(), lang);
			}
		}
		return languagesMap.get(code);
	}

	public static boolean isDefaultLang( Language lang ){
		if(  lang != null ){
			Language lDef = getDefaultLanguage();
			return lDef.getId() == lang.getId() ;
		}
		return false;
	}

	public static Language getLanguage(long languageID ) {
		List<Language> langs = getDotLanguages();
		for (Language lang : langs) {
			if( lang.getId() == languageID ){
				return lang;
			}
		}
		return null;
	}

	public static Language getDefaultLanguage(){
		if( defaultLanguage == null ){
			defaultLanguage =  APILocator.getLanguageAPI().getDefaultLanguage();
		}
		return defaultLanguage;
	}
	public static Relationship getRelationship( Structure st, String name ) {
		List<Relationship> listaRels;
		if( !rels.containsKey(st.getInode() ) ){
			 listaRels = RelationshipFactory.getAllRelationshipsByStructure( st );
			 rels.put(st.getInode(), listaRels );
		}else {
			listaRels =  rels.get( st.getInode() );
		}
		Relationship relatAllegato = null;
		for ( Relationship relat : listaRels ) {
			if ( relat.getRelationTypeValue().equalsIgnoreCase( name ) ) {
				relatAllegato = relat;
			}
		}
		return relatAllegato;
	}

	public static Category getCategoryByKey (String key ){
		Category cat = null;	
		if( UtilMethods.isSet(key)){
			try {
				cat = APILocator.getCategoryAPI().findByKey(key, ImportUtil.getUser(), true);			 
			} catch ( Exception e) {
				e.printStackTrace();
			}
		}
		return cat;
	}


	public static  String getRealtPath( String path ) {
		if( UtilMethods.isSet(path)){
			if(  path.lastIndexOf(".") != -1 ){
				String ext = path.substring( path.lastIndexOf(".") +1 );
				if( ext.endsWith(System.getProperty("file.separator") ) ){
					ext = ext.substring(0 , ext.lastIndexOf(System.getProperty("file.separator")  ));
					ext = ext.toLowerCase();
				}
				String[] allExts = ImportConfig.getProperties("ALL_EXTENSIONS");
				if(  ArrayUtils.contains(allExts, ext) ){
					if( path.endsWith(System.getProperty("file.separator")  )){
						path = path.substring( 0, path.lastIndexOf(System.getProperty("file.separator") )) ;
					}
					path = path.substring( 0, path.lastIndexOf(System.getProperty("file.separator") )) ;
				}
			}
		}
		return path;
	}
	
	public static String encodePathToURL(String path) throws UnsupportedEncodingException{
		return UriUtils.encodePath( path, "UTF-8" );
	}
	
	public static String sanitizeLuceneParameter( String query ) {
		if ( ( query == null ) || query.trim().equals( "" ) || query.trim().equals( "null" ) ) {
			return "";
		} else {
			query = query.replace( "\\", "\\\\" );
			query = query.replace( "^", "\\^" );
			query = query.replace( "?", "\\?" );
			query = query.replace( "-", "\\-" );
			query = query.replace( "!", "\\!" );
			query = query.replace( "(", "\\(" );
			query = query.replace( ")", "\\)" );
			query = query.replace( "[", "\\[" );
			query = query.replace( "]", "\\]" );
			query = query.replace( "{", "\\{" );
			query = query.replace( "}", "\\}" );
		}
		return query.trim();
	}
}
