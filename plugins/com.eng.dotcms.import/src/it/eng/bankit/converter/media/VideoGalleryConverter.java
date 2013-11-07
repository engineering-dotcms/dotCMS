package it.eng.bankit.converter.media;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

public class VideoGalleryConverter extends AbstractVideoConverter {

	protected String FORMATO;
	protected String formato;

	public VideoGalleryConverter( HmiStructure hmiStructure ) {
		this.hmiStructure = hmiStructure;
	}

	public void readStructureProperties() throws Exception {
		super.readStructureProperties();
		structureName = "Videogallery";
		FORMATO = "visualizzazione";
	}

	@Override
	protected void readProperties() throws Exception {
		super.readProperties();
		formato = properties.get( HyperwaveKey.Formato );
	}

	@Override
	protected void internalSetValues( Contentlet contentlet ) throws Exception {
		super.internalSetValues( contentlet );
		contentlet.setProperty( FORMATO, formato );
	}

	@Override
	protected void internalSetLanguageValues( Contentlet contentlet, String language ) throws Exception {
		super.internalSetLanguageValues( contentlet, language );
	}

	@Override
	public ContentletContainer parseContent() throws Exception {
		ContentletContainer container=super.parseContent();
		if(!files.isEmpty()){
				container.setFiles( files );
		}
		return container;
	}
	
	@Override
	public String getTemplateName() {
		return "Videogallery";
	}

	@Override
	public boolean isShowOnMenu() {
		return true;
	}

	public static boolean accept( HmiStructure hmiStructure ) {
		String formato = hmiStructure.getPropertyHmi( HyperwaveKey.Formato );
		String docType = hmiStructure.getPropertyHmi( HyperwaveKey.DocumentType );
		String metaType = hmiStructure.getPropertyHmi( "xmlMetadata-Type" );

		return ( formato != null && formato.equalsIgnoreCase( "LV" ) || ( formato != null && formato.equalsIgnoreCase( "DV" ) && metaType == null && ( docType == null || docType
				.equalsIgnoreCase( "collection" ) ) ) );
	}

}
