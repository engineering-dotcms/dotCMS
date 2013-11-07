package it.eng.bankit.converter;

import it.eng.bankit.bean.FolderWrapper;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;

public abstract class GenericFileAssetConverter extends GenericMultilanguageConverter {
	private static Logger logger = Logger.getLogger( GenericFileAssetConverter.class );
	protected String TITOLO_LUNGO;
	protected String DESCRIZIONE_BREVE;
	protected String CHECKSUM;

	protected Map<String, String> descrizioneShortMap = new HashMap<String, String>();
	protected File file;
	protected Map<String, String> titleLongMap = new HashMap<String, String>();
	protected String checksum;

	protected abstract File retrieveFile();

	@Override
	public void readStructureProperties() throws Exception {
		super.readStructureProperties();
		TITOLO = FileAssetAPI.TITLE_FIELD;
		TITOLO_LUNGO = "titoloLungo";
		NOTE = FileAssetAPI.DESCRIPTION;
		CHECKSUM = "checksum";
	}

	@Override
	protected void readProperties() throws Exception {
		super.readProperties();

		file = retrieveFile();
		checksum = readProperty( HyperwaveKey.HW_Checksum );
	}

	@Override
	protected void readLanguageAndTitle( Map<String, String> properties ) throws Exception {
		super.readLanguageAndTitle( properties );

		if ( !titleMap.isEmpty() ) {
			for ( String langCode : titleMap.keySet() ) {
				String curTitle = titleMap.get( langCode );
				if ( curTitle.length() > 200 ) {
					Charset utf8=Charset.forName( "UTF-8" );
					byte[] bytesTitle=curTitle.getBytes( utf8 );
					if(bytesTitle.length>255){
						logger.debug( "Content(" + goId + ") in" + getStruct().getFilePath() + " Truncating title:" + curTitle );
						titleLongMap.put( langCode, curTitle );
						byte[] reducedTitleBytes=ArrayUtils.subarray( bytesTitle, 0, 255 );
						String reducedTitle=new String(reducedTitleBytes,utf8);
						titleMap.put( langCode,reducedTitle );
					}else{
						titleMap.put( langCode,curTitle );
					}
				}
			}
		}
	}
	
	@Override
	protected FolderWrapper readFolderProperties() throws DotStateException, DotDataException, DotSecurityException {
		FolderWrapper overridFolderWrapper=super.readFolderProperties();
		if(!titleLongMap.isEmpty()){
			overridFolderWrapper.setTranslations( titleLongMap );
		}
		return overridFolderWrapper;
	}

	/* To extend */
	protected void internalSetValues( Contentlet contentlet ) throws Exception {
		super.internalSetValues( contentlet );
		if ( file != null ) {
			addFile( contentlet, FileAssetAPI.BINARY_FIELD, file );
			contentlet.setProperty( FileAssetAPI.FILE_NAME_FIELD, file.getName() );
		} else {
			logger.warn( "No file found" );
		}
		
		if(!SEQUENCE.equals( FileAssetAPI.SORT_ORDER )){
			contentlet.setProperty( FileAssetAPI.SORT_ORDER, new Long(10) );
		}
		contentlet.setProperty( CHECKSUM, checksum );
	}

	/* To extend */
	protected void internalSetLanguageValues( Contentlet contentlet, String language ) throws Exception {
		super.internalSetLanguageValues( contentlet, language );
		// Titolo lungo
		if ( titleLongMap.containsKey( language ) ) {
			contentlet.setProperty( TITOLO_LUNGO, titleLongMap.get( language ) );
		}
		// Descrizione Breve
		if ( descrizioneShortMap.containsKey( language ) ) {
			String descrizioneBreve = descrizioneShortMap.get( language );
			/*
			 * if(descrizioneBreve.length()>255){ logger.warn(
			 * "Content ("+goId+") in"
			 * +getStruct().getFilePath()+"\n campo Descrizione Breve di lunghezza "
			 * +descrizioneBreve.length() +" troncata a 256 caratteri");
			 * logger.warn( descrizioneBreve ); descrizioneBreve =
			 * descrizioneBreve.substring( 0, 255 ); }
			 */
			contentlet.setProperty( DESCRIZIONE_BREVE, descrizioneBreve );
		}

	}

	protected void addFile( File file ) throws Exception {
		for ( Contentlet contentlet : multilangContentlet.values() ) {
			addFile( contentlet, FileAssetAPI.BINARY_FIELD, file );
		}
	}

	protected void addFile( String language, File file ) throws Exception {
		addFile( multilangContentlet.get( language ), FileAssetAPI.BINARY_FIELD, file );
	}

	@Override
	public String getIdentityQuery( String languageCode ) {
		if ( structureName != null && folder != null && file != null && languageCode != null ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "+structureName:" );
			sb.append( structureName );
			sb.append(" +conhost:");
			sb.append( ImportUtil.getHost().getIdentifier());
			sb.append( " +path:*" );
			sb.append( ImportUtil.sanitizeLuceneParameter(folder.getPath()+file.getName()) );
			sb.append( "* +" + structureName + ".fileName:*" );
			sb.append( ImportUtil.sanitizeLuceneParameter(file.getName()) );
			sb.append( "* +languageId:" );
			sb.append( findLanguageId( languageCode ) );
			sb.append( " +deleted:false +working:true" );
			return sb.toString();
		} else
			return null;
	}

}
