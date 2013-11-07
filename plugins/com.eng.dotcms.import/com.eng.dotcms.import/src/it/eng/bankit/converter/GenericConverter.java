package it.eng.bankit.converter;

import it.eng.bankit.bean.FolderWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

public abstract class GenericConverter implements FolderConverter {
	private static Logger logger = Logger.getLogger( GenericConverter.class );
	protected static final LanguageAPI langAPI = APILocator.getLanguageAPI();
	protected static final SimpleDateFormat dateTymeFormat = new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss" );
	protected static final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat( "yyyy/MM/dd" );
	protected static final String DateTimeRegExp="^(19|20)\\d\\d([- /.])(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])\\s([0-2][0-9]):([0-5][0-9]):([0-5][0-9])$";
	protected static final String DateOnlyRegExp="^(19|20)\\d\\d([- /.])(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])$";
	protected String AUTORE;
	protected String GOID;
	protected String DATA_IMPORTAZIONE;
	protected String DATA_CREAZIONE;
	protected String DATA_MODIFICA;
	protected String DATA_PUBBLICAZIONE;
	protected String ALERT;
	protected String SEQUENCE;
	protected String SEARCHABLE;

	protected HmiStructure hmiStructure;
	protected String structureName = null;
	protected Structure structure;
	protected Structure linkStructure;
	protected Map<String, String> properties;
	protected List<String> propertiesToProcess = new LinkedList<String>();

	protected String goId;
	protected String author;
	protected Date timeCreated;
	protected Date timeModified;
	protected Boolean alert;
	protected Integer sequence;
	protected String orderStyle;
	protected Boolean archived;
	protected Boolean searchable;

	protected void init() throws Exception {
		Assert.notNull( hmiStructure, "Nessuna struttura HMI associata" );
		readStructureProperties();
		readProperties();
		initContentlet();
	}

	protected void readStructureProperties() throws Exception {
		AUTORE = "autoreHw";
		GOID = "hwgoid";
		DATA_IMPORTAZIONE = "importDate";
		DATA_CREAZIONE = "timeCreated";
		DATA_MODIFICA = "timeModified";
		DATA_PUBBLICAZIONE = "dataPubblicazione";
		ALERT = "alert";
		SEQUENCE = "sequenza";
		SEARCHABLE = "ricercabile";
	}

	protected abstract FolderWrapper readFolderProperties() throws Exception;

	protected void readProperties() throws Exception {
		String linkStructureName = ImportConfig.getProperty( "STRUCTURE_LINK" );
		linkStructure = StructureCache.getStructureByVelocityVarName( linkStructureName );
		Assert.notNull( structureName, "Struttura non definita" );
		structure = StructureCache.getStructureByVelocityVarName( structureName );
		Assert.notNull( structure, "Struttura:" + structureName + " non trovata" );
		properties = hmiStructure.getPropertiesHmi();
		propertiesToProcess.clear();
		propertiesToProcess.addAll( properties.keySet() );
		propertiesToProcess.removeAll( HyperwaveKey.systemProperties );
		goId = readProperty( HyperwaveKey.GOid );
		author = readProperty( HyperwaveKey.Author );
		timeCreated = readDateProperty( HyperwaveKey.TimeCreated );
		timeModified = readDateProperty( HyperwaveKey.TimeModified );
		alert = readBooleanProperty( HyperwaveKey.Alert );
		sequence = readIntegerProperty( HyperwaveKey.Sequence );
		String hideFromSearch=readProperty( HyperwaveKey.HW_HideFromSearch);
		searchable=(hideFromSearch!=null&&hideFromSearch.equalsIgnoreCase( "yes" )?false:true);
		String presentation = readProperty( HyperwaveKey.PresentationHints );
		if ( presentation != null && presentation.equalsIgnoreCase( "hidden" ) ) {
			archived = true;
		}
	}

	protected String readProperty( String key ) {
		String property = properties.get( key );
		propertiesToProcess.remove( key );
		return property;
	}

	protected Integer readIntegerProperty( String key ) {
		Integer property = null;
		String stringInteger = properties.get( key );
		if ( stringInteger != null && !stringInteger.isEmpty() ) {
			try {
				property = Integer.parseInt( stringInteger );
			} catch ( Exception e ) {
				property = null;
				logger.error( "Error parsing Integer:" + stringInteger, e );
			}
			propertiesToProcess.remove( key );
		}
		return property;
	}

	protected Date readDateProperty( String key ) {
		Date property = null;
		String stringDate = properties.get( key );
		if ( stringDate != null ) {
			try {
				if(stringDate.matches( DateTimeRegExp )){
					property = dateTymeFormat.parse( stringDate );
				}else if(stringDate.matches( DateOnlyRegExp )){
					property = dateOnlyFormat.parse( stringDate );
				}else{
					logger.warn( "Unparsable Date property("+key+"):"+stringDate );
				}
			} catch ( Exception e ) {
				property = null;
				logger.error( "Error parsing Date property("+key+"):" + stringDate, e );
			}
			propertiesToProcess.remove( key );
		}
		return property;
	}

	protected Boolean readBooleanProperty( String key ) {
		Boolean property = null;
		String stringProperty = properties.get( key );
		if ( stringProperty != null ) {
			property=(stringProperty.equalsIgnoreCase( "true" ) || stringProperty.equalsIgnoreCase( "yes" ));
			propertiesToProcess.remove( key );
		}
		return property;
	}

	protected URL readExternalLink( HmiStructure linkStructure ) throws MalformedURLException {
		URL externalLink = null;
		Map<String, String> linkProperties = linkStructure.getPropertiesHmi();
		String protocol = linkProperties.get( HyperwaveKey.Protocol );
		String host = linkProperties.get( HyperwaveKey.Host );
		Integer port = null;
		if ( linkProperties.containsValue( HyperwaveKey.Port ) ) {
			port = Integer.parseInt( linkProperties.get( HyperwaveKey.Port ) );
		}
		String path = linkProperties.get( HyperwaveKey.Path );
		if ( !path.startsWith( System.getProperty( "file.separator" ) ) ) {
			path = System.getProperty( "file.separator" ) + path;
		}

		if ( port != null ) {
			externalLink = new URL( protocol, host, port, path );
		} else {
			externalLink = new URL( protocol, host, path );
		}
		return externalLink;

	}

	protected Contentlet createContentlet( Structure structure, String languageCode ) {
		Contentlet contentlet = new Contentlet();
		contentlet.setStructureInode( structure.getInode() );
		contentlet.setHost( ImportUtil.getHost().getIdentifier() );

		long langId=findLanguageId(languageCode);
		if ( langId != 0 ) {
			contentlet.setLanguageId( langId );
		} else {
			logger.warn( "Linguaggio:" + languageCode + " Sconosciuto" );
		}
		return contentlet;
	}

	/* To extend */
	protected void internalSetValues( Contentlet contentlet ) throws Exception {
		contentlet.setProperty( AUTORE, author );
		contentlet.setProperty( GOID, goId );
		contentlet.setDateProperty( DATA_IMPORTAZIONE, new Date() );
		contentlet.setDateProperty( DATA_CREAZIONE, timeCreated );
		contentlet.setDateProperty( DATA_MODIFICA, timeModified );
		contentlet.setDateProperty( DATA_PUBBLICAZIONE, timeModified );
		contentlet.setProperty( ALERT, ( alert != null && alert ? "True" : "false" ) );
		contentlet.setProperty( SEQUENCE, getSortOrder() );
	}

	protected abstract void initContentlet() throws Exception;

	public abstract String getTemplateName();
	
	public abstract String getIdentityQuery( String languageCode );

	public abstract boolean isShowOnMenu();

	protected long findLanguageId(String languageCode){
		Language language = null;
		if ( languageCode.equals( "en" ) ) {// Default language
			language = langAPI.getLanguage( "en", "US" );
		} else {
			language = langAPI.getLanguage( languageCode, languageCode.toUpperCase() );
		}
		if(language!=null){
			return language.getId();
		}else{
			return 0;
		}
		
	}
	
	public Integer getSortOrder() {
		return ( sequence == null ? 0 : sequence );
	}

	public User getUser() {
		return ImportUtil.getUser();
	}

	public Host getHost() {
		return ImportUtil.getHost();
	}

	public HmiStructure getStruct() {
		return hmiStructure;
	}

	public void setStruct( HmiStructure hmiStructure ) {
		this.hmiStructure = hmiStructure;
	}

	public Structure getDotStructure() {
		return structure;
	}

}
