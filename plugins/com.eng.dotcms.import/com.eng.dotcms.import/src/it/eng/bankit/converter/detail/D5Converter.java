package it.eng.bankit.converter.detail;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;

public class D5Converter extends GenericDetailConverter {

	public D5Converter(HmiStructure struttura) {		
		super(struttura);
		this.setFormato("D5");
	}

	public ContentletContainer  parseContent() throws  Exception{

 		Map<String, String> props = getHmiStructure().getPropertiesHmi();
		String formato = props.get( HyperwaveKey.Formato );

		if(formato != null && formato.equalsIgnoreCase( getFormato() )) {

			String folderName =  getHmiStructure().getFilePath();
			getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );

			ContentletContainer container = new ContentletContainer(); 
	//		List<Contentlet> appoLista =  createDefaultListContentlet() ;		

//			List<HmiStructure> children = getHmiStructure().getSubFile();
			
			List<Language> langs =  ImportUtil.getDotLanguages();
			for( Language lang  : langs){ 
				ContentletWrapper cWrapper =  parseContentWrapper( lang.getLanguageCode() );
				if( cWrapper != null ){
					container.add(cWrapper);
				}
			}
			
//			for( Contentlet con  : appoLista){ 
//				ContentletWrapper cWrapper = new ContentletWrapper();
//				addCorpo(  con, getHmiStructure() );
//				List<ContentletContainer> allegati =  creaListaAllegati( con, children );
//				cWrapper.setAllegati( allegati);
//				cWrapper.setLinksRelationShipName( getLinkRelationShipName() );
//				
//				List<ContentletWrapper> imgsToload = addImmaginiAllegatiCorpo( con, children );
//				cWrapper.setImmaginiAllegati(  imgsToload );
//				
//				List<ContentletWrapper>  links = addLinksToContenlet( con , false   );
//				cWrapper.setLinks(links);	
//				List<ContentletWrapper>  dettagli  = addDettaglioChildren( con  );
//				cWrapper.setDettagliD5( dettagli );
//				cWrapper.setQuery(  getLuceneQuery(  con  ));
//
//				cWrapper.setContentlet(con);				
//				container.add(cWrapper);
//			}
			return container;
		}
		return null;
	}


	private List<ContentletWrapper> addDettaglioChildren( Contentlet con ) throws Exception{
		Language lang = ImportUtil.getLanguage(con.getLanguageId()  );
		LOG.info(" D5Converter -->  DEVO CREARE I FIGLI per "  + getHmiStructure().getFilePath()  );
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories();
		List<ContentletWrapper> listaDettagli = new ArrayList<ContentletWrapper>();		
		for( HmiStructure strutturaChild : childrenDir ){
			String formatoFiglio = strutturaChild.getPropertyHmi( HyperwaveKey.Formato );
			LOG.info(" D5Converter -->  FORMATO FIGLIO "  + formatoFiglio  );
			FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
			if( conv  instanceof  GenericDetailConverter ){
				GenericDetailConverter gd = (GenericDetailConverter)conv;
				ContentletWrapper wrapper = gd.parseContentWrapper(  lang.getLanguageCode()  );
				if( wrapper!= null ){
					LOG.info(" D5Converter -->  FIGLIO CREATO e AGGIUNTO. LINGUA ( "  + con.getLanguageId() + " ) " );
					listaDettagli.add( wrapper  ); 
				}
			}else {
				ContentletContainer cLet =  conv.parseContent();
				if( cLet != null && ! cLet.isEmpty()  ){
					ContentletWrapper wrapper = cLet.get( con.getLanguageId() );
					if( wrapper!= null ){
						LOG.info(" D5Converter -->  FIGLIO CREATO e AGGIUNTO. LINGUA ( "  + con.getLanguageId() + " ) " );
						listaDettagli.add( wrapper  ); 
					}
				}					
			}
		}
		return listaDettagli;
	}

	public  String getLinkRelationShipName(){
		return ImportConfig.getProperty( "REL_NAME_DETTAGLIOCONTENITORE-LINK");
	}	
 
	@Override
	public  Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_D5") );
		return structure;
	}

	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
			throws Exception {
		ContentletWrapper cWrapper = null;
		Contentlet con = createDefaultContentlet( correctLang );
		String folderName =  getHmiStructure().getFilePath();
		getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );

		if( con != null  ) {
			List<HmiStructure> children = getHmiStructure().getSubFile();
			
			String hidden = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );
			 cWrapper = new ContentletWrapper();
			addCorpo(  con, getHmiStructure() );
			List<ContentletContainer> allegati =  creaListaAllegati( con, children );
			cWrapper.setAllegati( allegati);
			cWrapper.setLinksRelationShipName( getLinkRelationShipName() );
			
			List<ContentletWrapper> imgsToload = addImmaginiAllegatiCorpo( con, children );
			cWrapper.setImmaginiAllegati(  imgsToload );
			
			List<ContentletWrapper>  links = addLinksToContenlet( con , false   );
			cWrapper.setLinks(links);	
			List<ContentletWrapper>  dettagli  = addDettaglioChildren( con  );
			cWrapper.setDettagliD5( dettagli );
			cWrapper.setQuery(  getLuceneQuery(  con  ));
			if( hidden != null && hidden.equalsIgnoreCase("Hidden")){
				cWrapper.setArchived(true);
			}
			cWrapper.setContentlet(con);				
			 
		 
		}
		return cWrapper;
	}



}
