package it.eng.bankit.converter.media;

import it.eng.bankit.bean.BodyContent;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.GenericFileAssetConverter;
import it.eng.bankit.util.HyperwaveKey;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;

public class FotoConverter extends GenericFileAssetConverter {
	private String templateName;
	private String allegatoStructureName;
	protected String THUMBNAIL;
	protected String DIDASCALIA;
	protected String DESCRIZIONE_ESTESA;
	protected String COPYRIGHT;
	protected String ALLEGATO;
	protected String DIMENSIONE;
	protected Structure allegatoStructure;

	protected File thumbnail;
	protected Map<String, String> didascaliaMap = new HashMap<String, String>();
	protected Map<String, String> descrizioneExtMap = new HashMap<String, String>();
	protected Map<String, Contentlet> allegatoMap = new HashMap<String, Contentlet>();
	protected URL copyright;
	protected String dimensione;

	public FotoConverter( HmiStructure hmiStructure ) {
		this.hmiStructure = hmiStructure;
	}

	@Override
	public void readStructureProperties() throws Exception {
		super.readStructureProperties();
		allegatoStructureName = "AllegatoDettaglio";
		structureName = "Foto";
		templateName = "Dettaglio Foto";
		THUMBNAIL = "thumbnail";
		DESCRIZIONE_BREVE = "sommario";
		DIDASCALIA = "didascalia";
		DESCRIZIONE_ESTESA = "descrizioneEstesa";
		COPYRIGHT = "copyright";
		ALLEGATO = "allegatoHd";
		DIMENSIONE = "dimensione";
		allegatoStructure = StructureCache.getStructureByVelocityVarName( allegatoStructureName );
	}

	@Override
	protected File retrieveFile() {
		return file;
	}

	@Override
	protected void readProperties() throws Exception {
		super.readProperties();
		dimensione = readProperty( HyperwaveKey.Dimensione );
		descrizioneShortMap = readMultilanguageProperty( HyperwaveKey.Abstract );

		for ( HmiStructure children : hmiStructure.getChildren() ) {
			Map<String, String> childProperties = children.getPropertiesHmi();
			String type = childProperties.get( HyperwaveKey.DocumentType );
			if ( type.equals( "text" ) ) {// Descrizione Estesa
				readDescrizioneEstesa( children );
			} else if ( type.equalsIgnoreCase( "Generic" ) ) {// Allegato HD
				readAllegatoHD( children );
			} else if ( type.equalsIgnoreCase( "Image" ) ) {// Immagini
				readImage( children );
			} else if ( !type.equalsIgnoreCase( "collection" ) ) {
				Logger.warn( FotoConverter.class, "Trovato child di formato inaspettato:" + type );
			}
		}

		for ( HmiStructure childrenLink : hmiStructure.getChildrenLinks() ) {
			readCopyright( childrenLink );
		}

	}

	private void readDescrizioneEstesa( HmiStructure hmiStructure ) throws Exception {
		BodyContent bodyContent = BodyContent.read( hmiStructure );
		descrizioneExtMap.put( bodyContent.getLanguage(), bodyContent.getContent() );
	}

	private void readAllegatoHD( HmiStructure hmiStructure ) {
		File allegatoFile = hmiStructure.getFile();
		Map<String, String> allegatoProperties = hmiStructure.getPropertiesHmi();
		Map<String, String> allegatoTitle = readMultilanguageProperty( HyperwaveKey.Title, allegatoProperties );
		if ( !allegatoTitle.isEmpty() ) {
			for ( String languageCode : allegatoTitle.keySet() ) {
				try {
					Contentlet allegatoContentlet = createContentlet( allegatoStructure, languageCode );
					String titoloAllegato = allegatoTitle.get( languageCode );
					if ( titoloAllegato.length() < 255 ) {
						allegatoContentlet.setProperty( FileAssetAPI.TITLE_FIELD, titoloAllegato );
 
					} else {
 
						allegatoContentlet.setProperty( FileAssetAPI.TITLE_FIELD, titoloAllegato.substring( 0, 255 ) );
						allegatoContentlet.setProperty( TITOLO_LUNGO, titoloAllegato );
					}

					allegatoContentlet.setProperty( FileAssetAPI.FILE_NAME_FIELD, allegatoFile.getName() );
					addFile( allegatoContentlet, FileAssetAPI.BINARY_FIELD, allegatoFile );
					Boolean alert = Boolean.parseBoolean( allegatoProperties.get( HyperwaveKey.Alert ) );
					if ( alert != null ) {
						allegatoContentlet.setProperty( ALERT, alert.toString() );
					}
					allegatoContentlet.setProperty( AUTORE, allegatoProperties.get( HyperwaveKey.Author ) );
					allegatoContentlet.setProperty( GOID, allegatoProperties.get( HyperwaveKey.GOid ) );
					allegatoContentlet.setDateProperty( DATA_IMPORTAZIONE, new Date() );
					// allegatoContentlet.setDateProperty(DATA_CREAZIONE,new
					// Date());
					// allegatoContentlet.setDateProperty(DATA_MODIFICA,new
					// Date());
					allegatoMap.put( languageCode, allegatoContentlet );
				} catch ( Exception e ) {
					Logger.error( FotoConverter.class, "Errore allegato HD", e );
				}
			}
		}
	}

	private void readCopyright( HmiStructure copyrightStructure ) throws MalformedURLException {
		Map<String, String> copyrightProperties = copyrightStructure.getPropertiesHmi();

		String titleIT = copyrightProperties.get( HyperwaveKey.Title + ":it" );
		String titleEN = copyrightProperties.get( HyperwaveKey.Title + ":en" );
		if ( ( titleIT == null && titleEN == null ) || ( titleIT != null && titleEN != null && !titleIT.equals( titleEN ) )
				|| ( titleIT != null && !titleIT.equals( "Copyright" ) ) ) {
			Logger.warn( FotoConverter.class, "Non Standard Copyright found{" + titleIT + titleEN + "}" );
		}
		copyright = readExternalLink( copyrightStructure );
	}

	private void readImage( HmiStructure imageStructure ) {
		File image = imageStructure.getFile();
		Map<String, String> imageProperties = imageStructure.getPropertiesHmi();
		String typeImage = imageProperties.get( HyperwaveKey.PresentationHints );
		if ( typeImage.equals( "CollectionHead" ) ) {// Immagine principale
			file = image;
		} else if ( typeImage.equals( "FullCollectionHead" ) ) {// Thumbnail
			thumbnail = image;
			didascaliaMap = readMultilanguageProperty( HyperwaveKey.Abstract, imageProperties );
		} else {
			Logger.warn( FotoConverter.class, "Immagine (" + image.getName() + ") non riconosciuta con PresentationHints:" + typeImage );
		}
	}

	@Override
	protected void internalSetValues( Contentlet contentlet ) throws Exception {
		super.internalSetValues( contentlet );

		if ( thumbnail != null ) {
			addFile( contentlet, THUMBNAIL, thumbnail );
		}
		if ( copyright != null ) {
			contentlet.setProperty( COPYRIGHT, copyright.toExternalForm() );
		}
		if ( dimensione != null ) {
			contentlet.setProperty( DIMENSIONE, dimensione );
		}
	}

	@Override
	protected void internalSetLanguageValues( Contentlet contentlet, String language ) throws Exception {
		super.internalSetLanguageValues( contentlet, language );

		// Didascalia
		if ( didascaliaMap.containsKey( language ) ) {
			contentlet.setProperty( DIDASCALIA, didascaliaMap.get( language ) );
		}

		// Descrizione Estesa
		if ( descrizioneExtMap.containsKey( language ) ) {
			contentlet.setProperty( DESCRIZIONE_ESTESA, descrizioneExtMap.get( language ) );
		}

	}

	@Override
	protected ContentletWrapper populateWrapper( String languageCode ) {
		ContentletWrapper wrapper = super.populateWrapper( languageCode );
		Contentlet allegato = allegatoMap.get( languageCode );
		if ( allegato != null ) {
			wrapper.setAllegato( allegato );
			wrapper.setLinksRelationShipName( ALLEGATO );
		}
		return ( wrapper );
	}

	@Override
	public String getTemplateName() {
		return templateName;
	}

	@Override
	public boolean isShowOnMenu() {
		return false;
	}

	public static boolean accept( HmiStructure hmiStructure ) {
		String formato = hmiStructure.getPropertiesHmi().get( HyperwaveKey.Formato );
		return ( formato != null && formato.equalsIgnoreCase( "DF" ) );
	}

}
