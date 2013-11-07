package it.eng.bankit.writer;

import it.eng.bankit.util.ImportConfig;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;

public class DotcmsMultilanguageWriter implements InitializingBean {
	public enum Type {
		Contentlet, Link, AllegatoSingolo, Allegato, Cambi
	}

	private boolean translateMode = false;
	private Map<String, String> identifierBag = new HashMap<String, String>();
	private Map<String, String> keyBag = new HashMap<String, String>();
	private Map<String, Integer> externalLinkCount = new HashMap<String, Integer>();
	private Map<String, Integer> allegatoCount = new HashMap<String, Integer>();
	private String linkStructureName;
	private String cambiStructureName;

	public void clear() {
		identifierBag.clear();
		keyBag.clear();
		externalLinkCount.clear();
		allegatoCount.clear();
	}

	public void clearCounts() {
		externalLinkCount.clear();
		allegatoCount.clear();

	}

	public Contentlet preProcess( Contentlet contentlet) {
		if ( translateMode || contentlet.getStructure().isFileAsset() ) {
			setIdentifier( contentlet);
		}
		return contentlet;
	}

	public void postProcess( Contentlet contentlet ) {
		if ( translateMode || contentlet.getStructure().isFileAsset() ) {
			insertIdentifier( contentlet );
		}
	}

	private void insertIdentifier( Contentlet contentlet ) {
		String key = getKey( contentlet );
		if ( !identifierBag.containsKey( key ) ) {
			identifierBag.put( key, contentlet.getIdentifier() );
		}
	}

	private void setIdentifier( Contentlet contentlet ) {
 		String key = getKey( contentlet );
		if ( identifierBag.containsKey( key ) ) {
			contentlet.setIdentifier( identifierBag.get( key ) );
		}
	}

	private String getKey( Contentlet contentlet ) {
		String hash = String.valueOf( HashCodeBuilder.reflectionHashCode( contentlet ) );
		String key = keyBag.get( hash );
		if ( key == null ) {
			key = generateKey( contentlet );
			keyBag.put( hash, key );
		}
		return key;
	}

	private String generateKey( Contentlet contentlet ) {
		String key = null;
		if ( contentlet.getStructure().isFileAsset() ) {// Unique File Asset
			// Name
			key = "FileAsset_" + contentlet.getFolder() + "_" + contentlet.get( FileAssetAPI.FILE_NAME_FIELD );
		} else if ( contentlet.getStructure().getName().equalsIgnoreCase( linkStructureName ) ) {// Links
			// Multiple values
			StringBuilder sbKey = new StringBuilder();
			sbKey.append( Type.Link.name() );
			sbKey.append( '_' );
			sbKey.append( contentlet.getStringProperty( "linkType" ) );
			sbKey.append( '_' );
			if ( contentlet.getStringProperty( "linkType" ).equalsIgnoreCase( "I" ) ) {
				sbKey.append( contentlet.getStringProperty( "linkInterno" ) );
			} else if ( contentlet.getStringProperty( "linkType" ).equalsIgnoreCase( "E" ) ) {
				sbKey.append( getExternalLinkIndex( contentlet.getFolder() ) );
			} else {
				sbKey.append( getAllegatoIndex( contentlet.getFolder() ) );
			}
			key = sbKey.toString();
		} else if ( contentlet.getStructure().getName().equalsIgnoreCase( cambiStructureName ) ) {
			StringBuilder sbKey = new StringBuilder();
			sbKey.append( Type.Cambi.name() );
			sbKey.append( '_' );
			String mesegiorno = contentlet.getStringProperty( "mesegiorno" );
			sbKey.append( mesegiorno );
			key = sbKey.toString();
		} else if ( contentlet.getFolder() != null ) {// Caso contentlet legata
			// univocamente a folder
			key = contentlet.getFolder();
		} else {// Default case una sola contentlet per struttura
			key = contentlet.getStructure().getName();
		}
		return key;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		linkStructureName = ImportConfig.getProperty( "STRUCTURE_LINK" );
		cambiStructureName = ImportConfig.getProperty( "STRUCTURE_CAMBI" );
		Assert.notNull( linkStructureName, "Missing Link Structure Name" );
	}

	public boolean isTranslateMode() {
		return translateMode;
	}

	public void setTranslateMode( boolean translateMode ) {
		this.translateMode = translateMode;
	}

	private String getExternalLinkIndex( String path ) {
		Integer curCount = externalLinkCount.get( path );
		if ( curCount == null ) {
			curCount = new Integer( 0 );
		}
		externalLinkCount.put( path, curCount + 1 );
		return "_" + path + '_' + curCount;
	}

	private String getAllegatoIndex( String path ) {
		Integer curCount = allegatoCount.get( path );
		if ( curCount == null ) {
			curCount = new Integer( 0 );
		}
		allegatoCount.put( path, curCount + 1 );
		return "_" + path + '_' + curCount;
	}

}
