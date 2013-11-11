package it.eng.bankit.app.importing;

import it.eng.bankit.app.util.FileReaderUtil;
import it.eng.bankit.deploy.IDeployConst;

import java.io.File;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class IndicatoriInsert extends AbstractImport {

	protected  String structureName = "Indicatori";
	private String categoryKey;
	private Category tipologia;
	private Structure stIndicatori;

	public IndicatoriInsert( User user, Host host ) {
		super( user, host );
	}

	@Override
	protected void init() throws Exception {
		stIndicatori = StructureCache.getStructureByVelocityVarName( structureName );
		categoryKey = pluginAPI.loadProperty( IDeployConst.PLUGIN_ID, "cambi.indicatori.categoria" );
		tipologia = APILocator.getCategoryAPI().findByKey( categoryKey, user, true );
		super.init();
	}

	protected void saveContentletsFile( File file, Folder folder, Language language ) throws Exception {
		Contentlet contentlet = null;
		if ( updateMode ) {
			String titolo = FileReaderUtil.getHtmlTitle( file );
			contentlet = checkoutIndicatore( titolo, language );
		}
		if ( contentlet == null ) {
			Logger.info( IndicatoriInsert.class, "Indicatore Non presente in dotcms. Deve essere creata la contentlet  ");
			contentlet = createContentlet( stIndicatori, language );
		}
		setCommonFields( contentlet, file );
		contentlet.setProperty( FileAssetAPI.HOST_FOLDER_FIELD, host.getInode() );
		Logger.info( IndicatoriInsert.class,  "folderINDICATORI   " + folder );
		contentlet.setFolder( folder.getInode() );	
		contentlet.setProperty("path", folder.getInode() );
		contentlet = persistContentlet( contentlet, "cambi_Indicatori", tipologia );
	}

	private Contentlet checkoutIndicatore( String titolo, Language lang ) throws DotContentletStateException, DotDataException, DotSecurityException, ParseException {
		Contentlet contentlet = null;
		try{

			StringBuilder query = new StringBuilder();
			query.append( "+structureName:" );
			query.append( structureName );
			query.append( " +" );
			query.append( structureName );
			query.append( ".titolo:" );
			query.append( titolo );
			query.append( " +languageId:" );
			query.append( lang.getId() );
			query.append( " +conHost:" );
			query.append( host.getIdentifier() );
			query.append( " +deleted:false +working:true +live:true" );
			Logger.info( IndicatoriInsert.class, "  checkoutIndicatore query : " + query.toString()  );
			List<Contentlet> contents = contentletApi.checkoutWithQuery( query.toString(), user, true );
			if ( !contents.isEmpty() ) {
				Logger.info( IndicatoriInsert.class, "Indicatore trovato. Deve essere aggiornato  ");
				contentlet = contents.get( 0 );
			}
		}catch (Exception e) {
			Logger.warn( this.getClass(), "checkoutIndicatore -> Error quering content", e );
		}
		return contentlet;
	}

}
