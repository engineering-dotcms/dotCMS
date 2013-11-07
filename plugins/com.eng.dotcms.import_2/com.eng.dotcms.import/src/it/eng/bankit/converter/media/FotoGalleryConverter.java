package it.eng.bankit.converter.media;

import it.eng.bankit.bean.BodyContent;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.GenericContainerConverter;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

public class FotoGalleryConverter extends GenericContainerConverter {
	private static Logger logger = Logger.getLogger( FotoGalleryConverter.class );
	private String templateName = "Fotogallery";
	protected String FORMATO;
	protected String ABSTRACT;
	protected String DESCRIZIONE_ESTESA;
	protected String IMMAGINE;
	protected String THUMBNAIL;
	protected String RELAZIONE;

	protected String formato;
	protected Map<String, String> abstractMap = new HashMap<String, String>( 2 );
	protected Map<String, String> descrizioneExtMap = new HashMap<String, String>( 2 );
	protected File immagine;
	protected File thumbnail;
	protected Map<String, Contentlet> contentsRelated = new HashMap<String, Contentlet>( 2 );

	public FotoGalleryConverter( HmiStructure hmiStructure ) {
		this.hmiStructure = hmiStructure;

	}

	@Override
	public void readStructureProperties() throws Exception {
		super.readStructureProperties();
		structureName = "Fotogallery";
		FORMATO = "visualizzazione";
		ABSTRACT = "sommario";
		DESCRIZIONE_ESTESA = "descrizioneEstesa";
		IMMAGINE = "immagine";
		THUMBNAIL = "thumbnail";
		RELAZIONE = ImportConfig.getProperty("REL_NAME_FOTOGALLERY-LINK");
	}

	@Override
	protected void readProperties() throws Exception {
		super.readProperties();
		formato = readProperty( HyperwaveKey.Formato );

		abstractMap.putAll( readMultilanguageProperty( HyperwaveKey.Abstract ) );

		for ( HmiStructure children : hmiStructure.getChildren() ) {
			Map<String, String> childProperties = children.getPropertiesHmi();
			String type = childProperties.get( HyperwaveKey.DocumentType );
			if ( type.equals( "text" ) ) {// Descrizione Estesa
				BodyContent bodyContent = BodyContent.read( children );
				descrizioneExtMap.put( bodyContent.getLanguage(), bodyContent.getContent() );
			} else if ( type.equals( "Image" ) ) {
				String presentationHints = childProperties.get( HyperwaveKey.PresentationHints );
				if(presentationHints.equalsIgnoreCase( "FullCollectionHead" )){
					thumbnail = children.getFile();
				}else if(presentationHints.equalsIgnoreCase( "CollectionHead" )){
					immagine = children.getFile();
				}
			} else if ( type.equals( "collection" ) ) {// Link a collection
				String childFormat = childProperties.get( HyperwaveKey.Formato );
				String childPresentationHints = childProperties.get( HyperwaveKey.PresentationHints );
				String childType = childProperties.get( HyperwaveKey.DocumentType );
				if ( childFormat == null && childType != null && childType.equalsIgnoreCase( "collection" ) && childPresentationHints != null
						&& childPresentationHints.equals( "Hidden" ) ) {
					retrieveLink( children );
				}
			}else{
				logger.warn( "Trovato child di formato inaspettato:" + type );
			}
		}
	}

	private void retrieveLink( HmiStructure linkHmi ) {
		for ( HmiStructure children : linkHmi.getChildren() ) {
			Map<String, String> childProperties = children.getPropertiesHmi();
			String type = childProperties.get( HyperwaveKey.DocumentType );
			String format = childProperties.get( HyperwaveKey.Formato );
			if ( format != null && format.equalsIgnoreCase( "DF" ) && type != null && type.equals( "collection" ) ) {
				try {
					String linkPath = childProperties.get( HyperwaveKey.Name );
					String linkSequence = childProperties.get( HyperwaveKey.Sequence );
					Map<String, String> title = readMultilanguageProperty( HyperwaveKey.Title, childProperties );
					for ( String languageCode : title.keySet() ) {
						Contentlet link = createContentlet( linkStructure, languageCode );
						link.setProperty( "titolo", title.get( languageCode ) );
						link.setProperty( "linkType", "I" );
						link.setProperty( "visualizzaIn", "CN" );
						link.setProperty( "linkInterno", linkPath );
						String identificativo=FileUtil.getFileNoExtension(children.getHmiFile());
						link.setProperty( "identificativo", identificativo );
						if ( linkSequence != null ) {
							link.setProperty( "sortOrder1", Integer.parseInt( linkSequence ) );
						}
						link.setDateProperty( DATA_IMPORTAZIONE, new Date() );
						link.setProperty( AUTORE, childProperties.get( HyperwaveKey.Author ) );
						link.setProperty( GOID, childProperties.get( HyperwaveKey.GOid ) );
						contentsRelated.put( languageCode, link );
					}
				} catch ( DotStateException e ) {
					logger.warn( e.getMessage() );
				} catch ( Exception e ) {
					logger.error( e );
				}
			}
		}
	}

	@Override
	protected void internalSetValues( Contentlet contentlet ) throws Exception {
		super.internalSetValues( contentlet );
		contentlet.setProperty( FORMATO, formato );
		if ( thumbnail != null ) {
			addFile( contentlet, THUMBNAIL, thumbnail );
		}
		if ( immagine != null ) {
			addFile( contentlet, IMMAGINE, immagine );
		}

	}

	@Override
	protected void internalSetLanguageValues( Contentlet contentlet, String language ) throws Exception {
		super.internalSetLanguageValues( contentlet, language );
		// Abstract
		if ( abstractMap.containsKey( language ) ) {
			contentlet.setProperty( ABSTRACT, abstractMap.get( language ) );
		}

		// Descrizione Estesa
		if ( descrizioneExtMap.containsKey( language ) ) {
			contentlet.setProperty( DESCRIZIONE_ESTESA, descrizioneExtMap.get( language ) );
		}
	}

	@Override
	protected ContentletWrapper populateWrapper( String languageCode ) {
		ContentletWrapper wrapper = super.populateWrapper( languageCode );
		Contentlet link = contentsRelated.get( languageCode );
		if ( link != null ) {
			ContentletWrapper linkWrapper = new ContentletWrapper();
			linkWrapper.setContentlet( link );
			wrapper.setLinks( Collections.singletonList( linkWrapper ) );
			wrapper.setLinksRelationShipName( RELAZIONE );
		}
		return ( wrapper );
	}

	@Override
	public String getTemplateName() {
		return templateName;
	}

	@Override
	public boolean isShowOnMenu() {
		return true;
	}

	public static boolean accept( HmiStructure hmiStructure ) {
		String formato = hmiStructure.getPropertyHmi( HyperwaveKey.Formato );
		return ( formato != null && ( formato.equalsIgnoreCase( "IF" ) || formato.equalsIgnoreCase( "LF" ) || formato.equalsIgnoreCase( "LF2" ) ) );
	}

}
