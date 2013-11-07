package it.eng.bankit.util;


import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;

public class BankUtils {
	private static List<Category> EMPTY_CATEGORIES = Collections.emptyList();
	private static Map<Relationship, List<Contentlet>> EMPTY_RELATIONSHIP = Collections.emptyMap();
	private static BankUtils instance = null;
	private ContentletAPI contentletAPI;
	private LanguageAPI langAPI;
	// private RelationshipAPI relaAPI;
	private PermissionAPI permissionAPI;

	public static BankUtils getInstance() {
		if ( instance == null ) {
			instance = new BankUtils();
		}
		return instance;
	}

	private BankUtils() {
		contentletAPI = APILocator.getContentletAPI();
		langAPI = APILocator.getLanguageAPI();
		permissionAPI = APILocator.getPermissionAPI();
	}

	public void fixDefaultLanguage() throws Exception {
		Language defLanguage = langAPI.getDefaultLanguage();
		Language altLanguage = null;
		if ( defLanguage.getLanguageCode().equalsIgnoreCase( "it" ) ) {
			altLanguage = langAPI.getLanguage( "en", "US" );
		} else {
			altLanguage = langAPI.getLanguage( "it", "IT" );
		}

		int velocityCount = fixVelocityFileAssets( defLanguage, altLanguage );
		Logger.info( BankUtils.class, "Fixed " + velocityCount + " Velocity File Assets" );
		int cssCount = fixCssFileAssets( defLanguage, altLanguage );
		Logger.info( BankUtils.class, "Fixed " + cssCount + " CSS File Assets" );
		int imagesCount = fixImagesFileAssets( defLanguage, altLanguage );
		Logger.info( BankUtils.class, "Fixed " + imagesCount + " Images File Assets" );
		int javascriptCount = fixJavascriptFileAssets( defLanguage, altLanguage );
		Logger.info( BankUtils.class, "Fixed " + javascriptCount + " Javascript File Assets" );
		Logger.info( BankUtils.class, "The End" );

	}

	public void backupHost() throws Exception {
		//TODO
	}
	
	public void restoreHost(File backup)throws Exception{
		//TODO
	}
	private int fixVelocityFileAssets( Language defLanguage, Language altLanguage ) throws Exception {
		List<Contentlet> contentlets = contentletAPI.checkoutWithQuery( "+structureName:FileAsset +conhost:" + ImportUtil.getHost().getIdentifier()
				+ " +FileAsset.fileName:**.vtl* +languageId:" + altLanguage.getId() + " +live:true", ImportUtil.getUser(), false );
		return fixDefaultLanguage( contentlets, defLanguage, altLanguage );
	}

	private int fixCssFileAssets( Language defLanguage, Language altLanguage ) throws Exception {
		List<Contentlet> contentlets = contentletAPI.checkoutWithQuery( "+structureName:FileAsset +conFolder:9245daa3-fe66-4755-bd71-b2cfdb19bb2a +languageId:"
				+ altLanguage.getId() + " +live:true", ImportUtil.getUser(), false );
		return fixDefaultLanguage( contentlets, defLanguage, altLanguage );
	}

	private int fixImagesFileAssets( Language defLanguage, Language altLanguage ) throws Exception {
		List<Contentlet> contentlets = contentletAPI.checkoutWithQuery(
				"+structureName:FileAsset +(conFolder:bebe7d6d-1ca4-4b33-b9eb-400807f49110 conFolder:4c8bf8f1-67fa-4ea1-9b14-4c5a1fdf35cf) +languageId:"
						+ altLanguage.getId() + " +live:true", ImportUtil.getUser(), false );
		return fixDefaultLanguage( contentlets, defLanguage, altLanguage );
	}

	private int fixJavascriptFileAssets( Language defLanguage, Language altLanguage ) throws Exception {
		List<Contentlet> contentlets = contentletAPI.checkoutWithQuery( "+structureName:FileAsset +conFolder:8e61c3d3-831c-4452-b0be-825fff62db44 +languageId:"
				+ altLanguage.getId() + " +live:true", ImportUtil.getUser(), false );
		return fixDefaultLanguage( contentlets, defLanguage, altLanguage );
	}

	private int fixDefaultLanguage( List<Contentlet> contentlets, Language defLanguage, Language altLanguage ) {
		int count = 0;
		for ( Contentlet curAsset : contentlets ) {
			try {
				Contentlet working = contentletAPI.findContentletByIdentifier(curAsset.getIdentifier(),true,altLanguage.getId(),ImportUtil.getUser(), false);
				if ( working != null ) {
					List<Permission> permissions = permissionAPI.getPermissions( curAsset.getStructure() );
					working.setLanguageId( defLanguage.getId() );
					Contentlet newContentlet=contentletAPI.checkinWithoutVersioning( working, EMPTY_RELATIONSHIP, EMPTY_CATEGORIES, permissions, ImportUtil.getUser(), false );
					contentletAPI.publish( newContentlet, ImportUtil.getUser(), false );
					//contentletAPI.delete( working, ImportUtil.getUser(), false );
					count++;
				}
			} catch ( Exception e ) {
				Logger.error( BankUtils.class, e.getMessage(), e );
			}
		}
		return count;
	}
}
