package it.eng.bankit.converter.news;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class BoxSidebarConverter extends GenericDetailConverter {

 	
	public BoxSidebarConverter(HmiStructure struttura) {
		super(struttura);
 	}


	public  ContentletContainer parseContent() throws  Exception{
		String formato = getHmiStructure().getPropertyHmi( HyperwaveKey.Formato );
 		ContentletContainer container = new ContentletContainer(); 
		if(formato != null && ( formato.startsWith("Box")   )) {
 			List<Language> langs =  ImportUtil.getDotLanguages();
 			for( Language lang  : langs){ 
 				ContentletWrapper cWrapper =  parseContentWrapper( lang.getLanguageCode() );
 				if( cWrapper != null ){
 					container.add(cWrapper);
 				}
 			}	 
		}
		return container;
	}
 

	protected List<HmiStructure> getAllFullCollectionHead(HmiStructure structure , long languageId) {

		List<HmiStructure> children = structure.getSubFile();
		List<HmiStructure> listaFileCorpo = new ArrayList<HmiStructure>();
		for( HmiStructure child : children ){
			File fileCorrente = child.getFile();
			String langFile = child.getPropertyHmi(  HyperwaveKey.HW_Language );
			String formato = child.getPropertyHmi(  HyperwaveKey.Formato );
			Language fileLang =   ImportUtil.getLanguage( langFile );
			if( FileUtil.isTextFile(fileCorrente)  || 
					FileUtil.isAttachFile( fileCorrente)  ||   
					FileUtil.isImageFile(fileCorrente) ) {
				if( formato != null && formato.equalsIgnoreCase("descr") 
						|| formato.equalsIgnoreCase("testo")  || 
						formato.equalsIgnoreCase("testotit")  ) {
					if( langFile!= null ){
						if( fileLang.getId() ==  languageId  ){
							listaFileCorpo.add(child);
						}
					}
				}
			} 
		}
		return listaFileCorpo ;
	}



	protected   void setBoxProperties(Contentlet contentlet , String langCode  ) throws DotStateException, DotDataException, DotSecurityException {
		String titoloPercorso = getHmiStructure().getPropertyHmi(HyperwaveKey.TitoloPercorso+":"+langCode  );
		if( UtilMethods.isSet( titoloPercorso ) ){
			contentlet.setProperty(  "titoloPercorso", titoloPercorso);
		}else {
			titoloPercorso = getHmiStructure().getPropertyHmi(HyperwaveKey.Title+":"+langCode ); 
			contentlet.setProperty(  "titoloPercorso", titoloPercorso);			 
		}
		
		String nascondiTitolo = getHmiStructure().getPropertyHmi(HyperwaveKey.nascondiTitolo );
		if( UtilMethods.isSet( nascondiTitolo ) ){
			contentlet.setProperty(  "mostraTitoloPercorso", "false");
		}
	 
		String percorso = getHmiStructure().getPropertyHmi(HyperwaveKey.Percorso+":"+langCode  );
		if( UtilMethods.isSet( percorso ) ){
			contentlet.setProperty( "href",  percorso );
		}
		else { 
			percorso = getHmiStructure().getPropertyHmi(HyperwaveKey.Percorso  );
			if( UtilMethods.isSet( percorso ) ){
				contentlet.setProperty( "href",  percorso );
			}
		}
		
 	
	}
	
	protected void setSpecificProperties(Contentlet contentlet , String langCode  ) throws DotStateException, DotDataException, DotSecurityException {
		 
		setBoxProperties(contentlet, langCode);
		String sfondo = getHmiStructure().getPropertyHmi(HyperwaveKey.Sfondo );
		String valueSfondo =  "";
		if( UtilMethods.isSet(sfondo) ){
			valueSfondo = ImportConfig.getProperty("SFONDO_BOXSIDEBAR_"+sfondo.toUpperCase() );
			if(  UtilMethods.isSet( valueSfondo) ) {
				contentlet.setProperty(  "background",  valueSfondo  );
			}else {
				ImportConfig.getProperty("SFONDO_BOXSIDEBAR_default");
			}
		}else {
			valueSfondo = ImportConfig.getProperty("SFONDO_BOXSIDEBAR_DEF");
			contentlet.setProperty(  "background",  valueSfondo );
		}
		 
		String abstr = getHmiStructure().getPropertyHmi(HyperwaveKey.Description+":"+langCode );
		if( UtilMethods.isSet(abstr ) ){
			contentlet.setProperty( "sommario",  abstr );
		}
	}


	public String getLinkRelationShipName(){
		return ImportConfig.getProperty( "REL_NAME_BOXSIDEBAR-LINK");
	}

	@Override
	public  Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_BOXSIDEBAR") );
		return structure;
	}


	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
			throws Exception {
		String hidden = getHmiStructure().getPropertyHmi(  HyperwaveKey.PresentationHints );
		ContentletWrapper cWrapper = null;
		List<HmiStructure> children = getHmiStructure().getSubFile();

		Contentlet contentlet = createDefaultContentlet(correctLang ); 
		if( contentlet != null  ){
			 cWrapper = new ContentletWrapper();
			List<HmiStructure>  listaFile =  getAllFullCollectionHead( getHmiStructure() , contentlet.getLanguageId() );
			addCorpo( contentlet , listaFile );			
			addImmagine( contentlet, children );
			List<ContentletWrapper>  links = addLinksToContenlet( contentlet , false   );				
			cWrapper.setLinks( links );				
			cWrapper.setLinksRelationShipName( getLinkRelationShipName() );
			cWrapper.setContentlet( contentlet );
			cWrapper.setQuery(  getLuceneQuery( contentlet ));				
			if( hidden != null && hidden.equalsIgnoreCase("Hidden")){
				cWrapper.setArchived(true);
			}
		}
		return cWrapper;
	}
  
}
