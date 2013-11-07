package it.eng.bankit.app.importing;

import it.eng.bankit.app.util.CambiUtil;
import it.eng.bankit.app.util.DotFolderUtil;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class CambiInsert extends AbstractImport {

	private String structureName = "Cambio";
	private String templateName = "Cambi";
	private Structure stCambio;

	public CambiInsert( User user, Host host ) {
		super( user, host );
	}

	@Override
	protected void init() throws Exception {
		stCambio = StructureCache.getStructureByVelocityVarName( structureName );
		super.init();
	}

	protected void saveContentletsFile( File file, Folder folder, Language language ) throws Exception {

		Contentlet contentlet = null;
		Date today = new Date();
		if ( updateMode ) {
			contentlet = checkoutCambi( today, language );
		}

		if ( contentlet == null ) {
			contentlet = createContentlet( stCambio, language );
		}

		setCommonFields( contentlet, file );

		//contentlet.setFolder( folder.getInode() );
		//contentlet.setProperty( FileAssetAPI.HOST_FOLDER_FIELD, folder );

		String giornomese = CambiUtil.generateCambiGiornoMese( today );
		contentlet.setProperty( "mesegiorno", giornomese );
		contentlet.setDateProperty( "dataCambio", today );
		contentlet.setDateProperty( "dataPubblicazione", today );
		contentlet.setProperty( "alert", "True" );

		contentlet = persistContentlet( contentlet, "cambi_rif_" + giornomese );
		createHtmlPages( folder, today, language );
	}

	private void createHtmlPages( Folder folder, Date date, Language language ) throws DotStateException, DotDataException, DotSecurityException {
		String curPath = ( identifierApi.findFromInode( folder.getInode() ) ).getPath();
		String title = DotFolderUtil.escapePath( curPath );
		HTMLPage indexPage = DotFolderUtil.createIndexOnFolder( title, templateName, folder );
		if ( indexPage != null ) {
			boolean pubblicated = DotFolderUtil.publishPage( indexPage );
			contentToPublish.add( indexPage.getIdentifier() );
			Logger.info( CambiInsert.class, "Pubblicata(" + pubblicated + ") pagina index in path:" + curPath );
		} else {
			Logger.info( CambiInsert.class, "Pagina index già prensente al path:" + curPath );
		}
		String cambiUrl = CambiUtil.generateCambiFilename( date, language.getLanguageCode() );
		HTMLPage cambiPage = DotFolderUtil.createPageOnFolder( cambiUrl, cambiUrl.replace( ".html", "" ), templateName, folder );
		if ( cambiPage != null ) {
			boolean pubblicated = DotFolderUtil.publishPage( cambiPage );
			contentToPublish.add( cambiPage.getIdentifier() );
			Logger.info( CambiInsert.class, "Pubblicata(" + pubblicated + ") pagina " + cambiUrl + " in path:" + curPath );
		} else {
			Logger.info( CambiInsert.class, "Pagina index " + cambiUrl + " già prensente al path:" + curPath );
		}
	}

	private Contentlet checkoutCambi( Date dataCambio, Language lang ) throws DotContentletStateException, DotDataException, DotSecurityException, ParseException {
		StringBuilder query = new StringBuilder();
		query.append( "+structureName:" );
		query.append( structureName );
		query.append( " +" );
		query.append( structureName );
		query.append( ".dataCambio:" );
		query.append( luceneDateFormat.format( dataCambio ) );
		query.append( " +languageId:" );
		query.append( lang.getId() );
		query.append( " +conHost:" );
		query.append( host.getIdentifier() );
		query.append( " +deleted:false +working:true +live:true" );
		List<Contentlet> contents = contentletApi.checkoutWithQuery( query.toString(), user, true );
		if ( !contents.isEmpty() ) {
			return contents.get( 0 );
		}
		return null;
	}

}
