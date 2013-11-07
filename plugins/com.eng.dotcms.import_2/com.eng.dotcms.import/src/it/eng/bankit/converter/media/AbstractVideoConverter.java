package it.eng.bankit.converter.media;

import it.eng.bankit.bean.BodyContent;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.GenericContainerConverter;
import it.eng.bankit.util.HyperwaveKey;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

public abstract class AbstractVideoConverter extends GenericContainerConverter {
	protected String DATA_EMANAZIONE;
	protected String ABSTRACT;
	protected String DESCRIZIONE_ESTESA;
	protected String TIME_OPEN;
	protected String LUOGO;

	protected Date dataEmanazione;
	protected Integer sequence;
	protected Map<String, String> abstractMap = new HashMap<String, String>( 2 );
	protected Map<String, String> descrizioneEstesaMap = new HashMap<String, String>( 2 );
	protected Date timeOpen;
	protected Map<String, String> luoghi;
	protected Map<String, Collection<File>> files = new HashMap<String, Collection<File>>( 0 );

	public void readStructureProperties() throws Exception {
		super.readStructureProperties();
		DATA_EMANAZIONE = "dataEmanazione";
		ABSTRACT = "sommario";
		DESCRIZIONE_ESTESA = "descrizioneEstesa";
		TIME_OPEN = "timeOpen";
		LUOGO = "luogo";
	}

	@Override
	protected void readProperties() throws Exception {
		super.readProperties();
		if ( properties.containsKey( HyperwaveKey.Data_emanazione ) ) {
			dataEmanazione = readDateProperty( HyperwaveKey.Data_emanazione );
		}

		abstractMap.putAll( readMultilanguageProperty( HyperwaveKey.Abstract ) );
		timeOpen = readDateProperty( HyperwaveKey.TimeOpen );
		luoghi = readMultilanguageProperty( HyperwaveKey.Luogo );

		for ( HmiStructure children : hmiStructure.getChildren() ) {
			Map<String, String> childProperties = children.getPropertiesHmi();
			String type = childProperties.get( HyperwaveKey.DocumentType );
			String formato = childProperties.get( HyperwaveKey.Formato );
			String mimeType = childProperties.get( HyperwaveKey.MimeType );
			if ( type != null && type.equals( "text" ) && mimeType != null && mimeType.startsWith( "text/" ) ) {// Descrizione
																												// Estesa
				BodyContent bodyContent = BodyContent.read( children );
				descrizioneEstesaMap.put( bodyContent.getLanguage(), bodyContent.getContent() );
			} else if ( formato == null ) {// simple files
				File file = children.getFile();
				if ( file.isDirectory() ) {
					String dirName = file.getName();
					List<File> curFiles = new ArrayList<File>();
					for ( File curFile : file.listFiles() ) {
						if ( !curFile.isDirectory() && !curFile.getName().endsWith( ".hmi" ) ) {//TODO filter only 4 extension asx,js,swf etc
							curFiles.add( curFile );
						}
					}
					if(!curFiles.isEmpty()){
						files.put( "/" + dirName + "/", curFiles );
					}
				} else {
					Collection<File> filesInRoot = files.get( "/" );
					if ( filesInRoot == null ) {
						filesInRoot = new ArrayList<File>();
						files.put( "/", filesInRoot );
					}
					filesInRoot.add( file );
				}

			}
		}
	}

	@Override
	protected void internalSetValues( Contentlet contentlet ) throws Exception {
		super.internalSetValues( contentlet );
		contentlet.setDateProperty( DATA_EMANAZIONE, dataEmanazione );
		contentlet.setProperty( TIME_OPEN, timeOpen );
	}

	@Override
	protected void internalSetLanguageValues( Contentlet contentlet, String language ) throws Exception {
		super.internalSetLanguageValues( contentlet, language );
		if ( abstractMap.containsKey( language ) ) {
			contentlet.setProperty( ABSTRACT, abstractMap.get( language ) );
		}
		if ( descrizioneEstesaMap.containsKey( language ) ) {
			contentlet.setProperty( DESCRIZIONE_ESTESA, descrizioneEstesaMap.get( language ) );
		}
		if ( luoghi.containsKey( language ) ) {
			contentlet.setProperty( LUOGO, luoghi.get( language ) );
		}
	}

}
