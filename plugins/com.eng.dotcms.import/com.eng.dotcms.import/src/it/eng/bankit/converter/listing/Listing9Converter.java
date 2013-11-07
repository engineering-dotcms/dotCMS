package it.eng.bankit.converter.listing;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportUtil;

import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class Listing9Converter extends GenericListingConverter {

	public Listing9Converter(HmiStructure struttura) {
		super(struttura);
	}

	public  ContentletContainer  parseContent() throws  Exception{

		Map<String, String> props = getHmiStructure().getPropertiesHmi();

		String formato = props.get( HyperwaveKey.Formato );

		if(formato != null &&   formato.startsWith("L9")  ) {
			ContentletContainer container = new ContentletContainer(); 
			List<Contentlet> appoLista =  createDefaultListContentlet() ;		
		
			// caso 1 La cartelle contiene solo file 
			List<HmiStructure> childrenTesto = getHmiStructure().getSubFile() ;
			if( appoLista.size() > 0 ){
				for( Contentlet con  : appoLista){ 
					ContentletWrapper cWrapper = new ContentletWrapper();
					addCorpo(  con, getHmiStructure() );				
				//	List<Contentlet> links =   addLinksToContenlet(  con, false );
				//	cWrapper.setLinks( links );
					List<ContentletWrapper> listingLinks =   createListingLinks( con );
					cWrapper.setListingLinks( listingLinks );
					cWrapper.setQuery(  getLuceneQuery( con ) );
					addImmagine(   con, childrenTesto );
					List<ContentletWrapper>  dettagli  = getListaDettagli( con );			 
					cWrapper.setContentlet(con);
					container.add(cWrapper);
					container.addAll(dettagli);
				}
			}else {
				ContentletWrapper cWrapper = new ContentletWrapper();			
				Language langEn = ImportUtil.getLanguage("en");
				Language langIt = ImportUtil.getLanguage("it");
				List<ContentletWrapper> listingLinks =   createListingLinks( langEn );
				cWrapper.setListingLinks( listingLinks );
				ContentletWrapper cWrapperIt = new ContentletWrapper();
 				List<ContentletWrapper> listingLinksIt =   createListingLinks( langIt );
				cWrapperIt.setListingLinks( listingLinksIt );
				container.add(cWrapper);
				container.add( cWrapperIt );
			}

			return container;
		}
		return null;
	}

}
