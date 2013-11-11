package it.eng.bankit.app.importing;

import it.eng.bankit.app.util.FileReaderUtil;

import java.io.File;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class SimpleContentInsert extends AbstractImport {
	protected String structureName = "SimpleContent";
	private Structure structure;

	public SimpleContentInsert( User user, Host host ) {
		super( user, host );
	}

	@Override
	protected void init() throws Exception {
		structure = StructureCache.getStructureByVelocityVarName( structureName );
		super.init();
	}

	protected void saveContentletsFile( File file, Folder folder, Language language ) throws Exception {
		Contentlet contentlet = null;
		if ( updateMode ) {
			String titolo = FileReaderUtil.getHtmlTitle( file );
			contentlet = checkoutSelettore( titolo, language, folder );
		} 
		
		if ( contentlet == null ) {
			contentlet = createContentlet( structure, language );
		}

		setCommonFields( contentlet, file );
		contentlet.setFolder( folder.getInode() );
		contentlet.setProperty( FileAssetAPI.HOST_FOLDER_FIELD, folder.getInode() );
		// String titolo=(String)contentlet.get("titolo");
		// removeOldContentlet(titolo,folder,language);
		contentlet = persistContentlet( contentlet, "cambi_Selettore" );
	}

	/*
	 * private void removeOldContentlet( String title, Folder folder, Language
	 * language ) throws Exception { Contentlet oldContentlet =
	 * checkoutSelettore( title, language, folder ); if ( oldContentlet != null
	 * ) {// rimuovi removeContentlet( oldContentlet ); Logger.info(
	 * SelettoreCambiInsert.class, "Eliminata vecchia versione Sellettore " +
	 * title ); } }
	 */

	protected Contentlet checkoutSelettore( String title, Language lang, Folder folder ) {
		Contentlet contentlet = null;
		try {
			StringBuilder query = new StringBuilder();
			if ( structure != null ) {
				query.append( "+structureName:" );
				query.append( structure.getVelocityVarName() );
			}
			if ( UtilMethods.isSet( title ) ) {
				query.append( " +" + structure.getVelocityVarName() + ".titolo:\"" );
				query.append( title );
				query.append( '\"' );
			}
			if ( folder != null ) {
				query.append( " +conFolder:" );
				query.append( folder.getInode() );
			}
			query.append( " +conHost:" );
			query.append( host.getIdentifier() );
			if ( lang != null ) {
				query.append( " +languageId:" );
				query.append( lang.getId() );
			}
			query.append( " +deleted:false +working:true +live:true" );
			 
				List<Contentlet> results = contentletApi.checkoutWithQuery( query.toString(), user, true );
				if ( results != null && results.size() > 0 ) {
					contentlet = results.get( 0 );
				}
			 
		} catch ( Exception e ) {
			Logger.warn( this.getClass() , "checkoutSelettore -> Error quering content", e );
		}
		return contentlet;
	}

}
