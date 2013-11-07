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

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;

public class StrilloConverter extends GenericDetailConverter {

	public StrilloConverter(HmiStructure struttura) {
		super(struttura);
	}


	public  ContentletContainer  parseContent() throws  Exception{
		String formato = getHmiStructure().getPropertyHmi( HyperwaveKey.Formato );
		ContentletContainer container = null ; 
		if(formato != null && formato.startsWith("NP") ) {
			container = createContainer();		 
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
				if( formato != null && formato.equalsIgnoreCase("descr") || formato.equalsIgnoreCase("testo")  || 
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


	protected ContentletContainer createContainer() throws Exception  {

		ContentletContainer container = new ContentletContainer(); 
		List<Language> langs =  ImportUtil.getDotLanguages();
		for( Language lang  : langs){ 
			ContentletWrapper cWrapper =  parseContentWrapper( lang.getLanguageCode() );
			if( cWrapper != null ){
				container.add(cWrapper);
			}
		}
		return container; 

	}


	public Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_NP") );
		return structure;
	}

	public String getLinkRelationShipName(){
		return ImportConfig.getProperty( "REL_NAME_STRILLO-LINK");
	}


	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
	throws Exception {
		String hidden = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );

		ContentletWrapper cWrapper = null;
		Contentlet con = createDefaultContentlet( correctLang );
		List<HmiStructure> children = getHmiStructure().getSubFile();
		if( con != null  ) {
			cWrapper = new ContentletWrapper();
			List<HmiStructure>  listaFile =  getAllFullCollectionHead( getHmiStructure() , con.getLanguageId() );
			addCorpo( con , listaFile );
			addImmagine( con, children );
			List<ContentletWrapper>  links = addLinksToContenlet( con , false   );				
			cWrapper.setLinks( links );				
			cWrapper.setQuery(  getLuceneQuery( con ));				
			cWrapper.setLinksRelationShipName( getLinkRelationShipName() );
			cWrapper.setContentlet(con); 	
			if( hidden != null && hidden.equalsIgnoreCase("Hidden")){
				cWrapper.setArchived(true);
			}
		}
		return cWrapper;
	}


}
