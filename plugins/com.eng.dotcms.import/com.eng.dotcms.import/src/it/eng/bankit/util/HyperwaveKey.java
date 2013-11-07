package it.eng.bankit.util;

import java.util.Arrays;
import java.util.Collection;

public interface HyperwaveKey {

	public static final String Author = "Author";
	public static final String Description= "Description";
	public static final String DocumentType = "DocumentType";
	public static final String CollectionType = "CollectionType";
	public static final String Formato = "Formato";
	// Id univoco in Hyperwave dell'oggetto
	public static final String GOid = "GOid";
	public static final String HW_ChildAccess = "HW_ChildAccess";
	public static final String HW_CompoundSearchable = "HW_CompoundSearchable";
	public static final String HW_ObjectName  = "HW_ObjectName";
	public static final String HW_OriginalAttributeEncoding = "HW_OriginalAttributeEncoding";
	public static final String HW_EffectiveAccess ="HW_EffectiveAccess";
	public static final String Name = "Name";
	public static final String Rights = "Rights";
	public static final String Sequence = "Sequence";
	public static final String SortOrder = "SortOrder";
	public static final String Subdocs = "Subdocs";
	public static final String TimeCreated = "TimeCreated";
	public static final String TimeModified = "TimeModified";
	public static final String TimeOpen = "TimeOpen";
	public static final String Title = "Title";
	public static final String Abstract = "Abstract";
	public static final String Nascondi = "Nascondi";

	public static final String Type = "Type";
	public static final String HW_Language = "HW_Language";
	public static final String HW_ContentEncoding = "HW_ContentEncoding";

	public static final String MimeType  ="MimeType";
	public static final String BodyType ="BodyType";
	public static final String Alert ="Alert";
	public static final String PresentationHints="PresentationHints";	
	//Attributo in visualizzazione di un allegato
	public static final String Data_emanazione ="Data_emanazione";
	//Attributo in visualizzazione di un allegato
	public static final String Autore ="Autore";
	//Attributo in visualizzazione di un allegato
	public static final String Luogo ="Luogo";

	public static final String Evento ="Evento";

	public static final String Organizzazione ="Organizzazione";

	public static final String AbsHide ="AbsHide";
	public static final String Immagine = "Immagine";

	public static final String Host= "Host";
	public static final String Path="Path";
	public static final String Port="Port";
	public static final String Protocol="Protocol";

	public static final String Yes = "yes";
	//presente sia in italiano che in inglese
	public static final String Ruolo_Autore = "Ruolo_autore";
	// ESEMPIO 	PLACE_INSERTUPLOADBASE=http://esi380.ac.bankit.it:8080
	public static final String PLACE_INSERTUPLOADBASE="PLACE_INSERTUPLOADBASE";

	public static final String  accessKey = "accessKey";
	public static final String  Percorso = "Percorso";
	public static final String  TitoloPercorso = "TitoloPercorso";

	public static final String Dimensione = "Dimensione";	
	public static final String nascondiTitolo = "nascondiTitolo";
	public static final String Posizione = "Posizione";	
	public static final String Sfondo = "Sfondo";	
	public static final String HW_HideFromSearch = "HW_HideFromSearch";
 
	public static final String vidfile = "vidfile";
	public static final String UrlHigh263 = "Url_high263";
	public static final String UrlHigh264 = "Url_high264";
	public static final String UrlLow263 = "Url_low263";
	public static final String UrlLow264 = "Url_low264";
	public static final String HW_Checksum = "HW_Checksum";
	public static final String xmlMetadata = "xmlMetadata-Type";
	
	public static final String Importante = "Importante";

	public static final Collection<String> systemProperties=Arrays.asList(new String[]{DocumentType,CollectionType,HW_ChildAccess,HW_CompoundSearchable,HW_ObjectName,HW_OriginalAttributeEncoding,Rights,HW_Language,Type,HW_ContentEncoding,MimeType,BodyType,HW_EffectiveAccess,Subdocs,Name,Formato,Path,xmlMetadata});

}
