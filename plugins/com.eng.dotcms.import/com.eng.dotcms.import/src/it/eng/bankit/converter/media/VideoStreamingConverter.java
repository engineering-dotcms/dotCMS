package it.eng.bankit.converter.media;

import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;

import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;

public class VideoStreamingConverter extends AbstractVideoConverter {
	private static Logger logger = Logger.getLogger( VideoStreamingConverter.class);
	protected String VID;
	protected String URL_HIGH263;
	protected String URL_HIGH264;
	protected String URL_LOW263;
	protected String URL_LOW264;
	protected String LINK;
	protected String AUTORI;
	protected String CHECKSUM;
	
	protected String vId;
	protected String urlHigh263;
	protected String urlHigh264;
	protected String urlLow263;
	protected String urlLow264;
	protected URL link;
	protected String autori;
	protected Set<String> streamingLanguages;
	protected String checksum;

	public VideoStreamingConverter(HmiStructure hmiStructure) {
		this.hmiStructure = hmiStructure;
	}

	public void readStructureProperties() throws Exception {
		super.readStructureProperties();
		structureName = "VideoContent";
		VID = "vid";
		URL_HIGH263 = "urlH263";
		URL_HIGH264 = "urlH264";
		URL_LOW263 = "urlL263";
		URL_LOW264 = "urlL264";
		LINK = "linkEsterno";
		AUTORI = "autori";
		CHECKSUM = "checksum";
	}

	@Override
	protected void readProperties() throws Exception {
		super.readProperties();
		vId = readProperty(HyperwaveKey.vidfile);
		urlHigh263 = readProperty(HyperwaveKey.UrlHigh263);
		urlHigh264 = readProperty(HyperwaveKey.UrlHigh264);
		urlLow263 = readProperty(HyperwaveKey.UrlLow263);
		urlLow264 = readProperty(HyperwaveKey.UrlLow264);
		autori = readProperty(HyperwaveKey.Autore);
		checksum = readProperty(HyperwaveKey.HW_Checksum);
		
		if (hmiStructure.getChildrenLinks()!=null&&!hmiStructure.getChildrenLinks().isEmpty()){
			link=readExternalLink(hmiStructure.getChildrenLinks().get(0));
		}
		
		for(HmiStructure childStruct:hmiStructure.getChildren()){
			boolean textType=childStruct.getPropertyHmi( HyperwaveKey.DocumentType ).equalsIgnoreCase( "text" );
			boolean mymeType=childStruct.getPropertyHmi( HyperwaveKey.MimeType ).equalsIgnoreCase( "text/plain" );
			boolean documentType=childStruct.getPropertyHmi( HyperwaveKey.Type ).equalsIgnoreCase( "Document" );
			boolean vidOK=childStruct.getPropertyHmi(HyperwaveKey.vidfile).equals( vId ); 
			if (textType&&mymeType&&documentType&&vidOK){//(check for language specification file
				streamingLanguages=readMultilanguageProperty(HyperwaveKey.Title,childStruct.getPropertiesHmi()).keySet();
			}else{
				logger.warn( "Child Struct strange type "+childStruct.getFilePath() );
			}
		}
	}

	@Override
	protected void internalSetValues(Contentlet contentlet) throws Exception {
		super.internalSetValues(contentlet);
		contentlet.setProperty( AUTORI, autori );
		contentlet.setProperty( CHECKSUM, checksum );
	}
	
	@Override
	protected void internalSetLanguageValues(Contentlet contentlet,
			String language) throws Exception {
		super.internalSetLanguageValues(contentlet, language);
		
		if(streamingLanguages.contains( language )){
			contentlet.setProperty(VID, vId);
			contentlet.setProperty(URL_HIGH263, urlHigh263);
			contentlet.setProperty(URL_HIGH264, urlHigh264);
			contentlet.setProperty(URL_LOW263, urlLow263);
			contentlet.setProperty(URL_LOW264, urlLow264);
		}
	}

	@Override
	public String getTemplateName() {
		return "Video";
	}

	@Override
	public boolean isShowOnMenu() {
		return false;
	}

	public static boolean accept(HmiStructure hmiStructure) {
		String formato = hmiStructure.getPropertyHmi(HyperwaveKey.Formato);
		String metaType = hmiStructure.getPropertyHmi(HyperwaveKey.xmlMetadata);
		boolean vidPresent=UtilMethods.isSet(hmiStructure.getPropertyHmi(HyperwaveKey.vidfile));
		return (formato != null && formato.equalsIgnoreCase("DV") && vidPresent
				&& metaType != null && metaType.equalsIgnoreCase("Video"));
	}
}