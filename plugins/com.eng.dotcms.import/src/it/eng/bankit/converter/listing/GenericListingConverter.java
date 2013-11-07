package it.eng.bankit.converter.listing;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.converter.fileasset.LinkConverter;
import it.eng.bankit.util.HyperwaveKey;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public abstract class GenericListingConverter extends GenericDetailConverter {

	public GenericListingConverter(HmiStructure struttura) {
		super(struttura);
 	}

	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	//Caso dettagli come folder
	protected List<ContentletWrapper> getListaDettagli(  Contentlet con  ) throws Exception{
		List<ContentletWrapper> listaDettagli = new ArrayList<ContentletWrapper>();				
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories();
		
		for( HmiStructure hmi : childrenDir ){
			String formatoFiglio = hmi.getPropertyHmi(  HyperwaveKey.Formato );
			 if( formatoFiglio != null && formatoFiglio.startsWith("D")){
				FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , hmi );
				ContentletContainer cLet =  conv.parseContent();
				if( cLet != null && ! cLet.isEmpty()  ){
					System.out.println( " Devo creare i dettagli   " + cLet.getAll().size()  );
					ContentletWrapper wrapper = cLet.get( con.getLanguageId() );
					if( wrapper!= null ){
						listaDettagli.add( wrapper  );
					}
				}	
			}			
		}
		return listaDettagli;
	}
 

	
	//Caso dettagli come folder
	protected List<ContentletWrapper> getListaDettagli(  Contentlet con  , HmiStructure structure ) throws Exception{
		
		List<ContentletWrapper> listaDettagli = new ArrayList<ContentletWrapper>();		
		List<HmiStructure> childrenDir = structure.getSubDirectories();
		
		for( HmiStructure hmi : childrenDir ){
			String formatoFiglio = hmi.getPropertyHmi(  HyperwaveKey.Formato );
			 if( formatoFiglio != null && formatoFiglio.startsWith("D")){
				
				FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , hmi );
				ContentletContainer cLet =  conv.parseContent();
				if( cLet != null && ! cLet.isEmpty()  ){
					System.out.println( " HO CREATO I FIGLI DI D5 " + cLet.getAll().size()  );
					ContentletWrapper wrapper = cLet.get( con.getLanguageId() );
					if( wrapper!= null ){
						listaDettagli.add( wrapper  );
					}
				}	
			}
			
		}
		return listaDettagli;
	}
 
	
	
	protected List<ContentletWrapper> createListingLinks( Contentlet contentlet ) throws Exception {
		List<ContentletWrapper> detta = new ArrayList<ContentletWrapper>();
		List<HmiStructure> links = getHmiStructure().getChildrenLinks();
		for( HmiStructure hmi : links  ){
			Language langContentlet =  APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

			String title = hmi.getPropertyHmi(  HyperwaveKey.Title+":"+langContentlet.getLanguageCode() );
			if( UtilMethods.isSet(title )){
				LinkConverter lConv = new LinkConverter( hmi );
				ContentletWrapper cLet = lConv.parseContentWrapper( langContentlet.getLanguageCode()  );
				if( cLet != null    ){
 						detta.add(cLet );
 				}
			}

		}
		return detta;
	} 

	
	protected List<ContentletWrapper> createListingLinks( Language language ) throws Exception {
		List<ContentletWrapper> detta = new ArrayList<ContentletWrapper>();
		List<HmiStructure> links = getHmiStructure().getChildrenLinks();
		for( HmiStructure hmi : links  ){
		 
			String title = hmi.getPropertyHmi(  HyperwaveKey.Title+":"+language.getLanguageCode().toLowerCase() );
			if( UtilMethods.isSet(title )){
				LinkConverter lConv = new LinkConverter( hmi );
				ContentletContainer cLet = lConv.parseContent();
				if( cLet != null && ! cLet.isEmpty()  ){
					if( cLet.get( language.getId()  )!= null ) {
						detta.add( cLet.get( language.getId()  )  );
					}
				}
			}

		}
		return detta;
	} 
	
	
	public    String getLinkRelationShipName(){
		return null;
	}

}
