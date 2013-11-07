package it.eng.bankit.converter.media;

 
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.GenericFileAssetConverter;
import it.eng.bankit.util.HyperwaveKey;

import java.io.File;
import java.util.Date;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

public class VideoConverter extends GenericFileAssetConverter {
	protected String DATA_EMANAZIONE;
	protected String AUTORI;
	protected Date dataEmanazione;
	protected String autori;
	
	public VideoConverter(HmiStructure hmiStructure) {
		this.hmiStructure = hmiStructure;
	}
	
	public void readStructureProperties() throws Exception {
		super.readStructureProperties();
		structureName = "Video";
		DATA_EMANAZIONE = "dataEmanazione";
		SEQUENCE = "sequenza";
		DESCRIZIONE_BREVE = "sommario";
		AUTORI = "autori";
	}
	
	@Override
	protected void readProperties() throws Exception {
		super.readProperties();
		dataEmanazione = readDateProperty(HyperwaveKey.Data_emanazione);
		descrizioneShortMap.putAll(readMultilanguageProperty(HyperwaveKey.Abstract));
		autori = readProperty(HyperwaveKey.Autore);
	}
	
	@Override
	protected void internalSetValues(Contentlet contentlet) throws Exception {
		super.internalSetValues(contentlet);
		contentlet.setDateProperty(DATA_EMANAZIONE, dataEmanazione);
		contentlet.setProperty( AUTORI, autori );
	}

	@Override
	protected File retrieveFile() {
		if (hmiStructure.getFile()!=null){
			return hmiStructure.getFile();
		}else{
			return null;
		}
	}

	@Override
	public String getTemplateName() {
		return "PlayVideo";
	}
	
	@Override
	public boolean isShowOnMenu() {
		return false;
	}
	
	public static boolean accept(HmiStructure hmiStructure) {
		String formato = hmiStructure.getPropertyHmi(HyperwaveKey.Formato);
		String docType = hmiStructure.getPropertyHmi(HyperwaveKey.DocumentType);
		String mimeType = hmiStructure.getPropertyHmi(HyperwaveKey.MimeType);
		return (formato != null && formato.equalsIgnoreCase("DV")
				&& docType != null && docType.equalsIgnoreCase("Movie")
				&&(mimeType.equalsIgnoreCase("video/x-ms-wmv") || mimeType.equalsIgnoreCase("video/mpeg")));
	}
	
}
