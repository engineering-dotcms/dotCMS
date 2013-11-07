package it.eng.bankit.converter.listing;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;

import java.util.Map;

public class Listing5Converter extends Listing3Converter {

	public Listing5Converter(HmiStructure struttura) {
		super(struttura);
	}

	public  ContentletContainer  parseContent() throws  Exception{
		Map<String, String> props = getHmiStructure().getPropertiesHmi();

		String formato = props.get( HyperwaveKey.Formato ); 
		ContentletContainer cCont = null;
		if( formato != null && ( formato.startsWith("L5")  )) { 
 			cCont = createContainer();
		}
		return cCont;			 
	}
	
//	
//	protected ContentletContainer createContainer() throws Exception {
//		ContentletContainer container = new ContentletContainer(); 
//		List<Contentlet> appoLista =  createDefaultListContentlet() ;		
//
//		// caso 1 La cartelle contiene solo file 
//		List<HmiStructure> childrenTesto = getHmiStructure().getSubFile() ;
//		// caso 2 La cartelle contiene altre cartelle tipo D0---Dx
//		//		List<HmiStructure> childrenDirs = getHmiStructure().getSubDirectories() ;
//
//		for( Contentlet con  : appoLista){ 
//			ContentletWrapper cWrapper = new ContentletWrapper();
//			addCorpo(  con, getHmiStructure() );
//			List<ContentletWrapper> listingLinks =   createListingLinks( con );
//			cWrapper.setListingLinks( listingLinks );
//
//			List<ContentletWrapper> dettaglioDirs =   addDettaglioChildren( cWrapper , con  );
//			if( dettaglioDirs != null ) {
//				for(ContentletWrapper cw : dettaglioDirs )
//					cWrapper.addListingLink( cw );
//			}
//			List<ContentletContainer> allegati =   creaListaAllegati( con, childrenTesto );
//			for(ContentletContainer contAllegato : allegati ){
//				ContentletWrapper wrapperAll =  contAllegato.get(con.getLanguageId() );
//				cWrapper.addListingLink( wrapperAll );
//			}
//			addImmagine(   con, childrenTesto );
//			cWrapper.setContentlet(con);
//			cWrapper.setQuery(  getLuceneQuery( con ));
//			container.add(cWrapper);
//		}
//		return container;
//	}
		
 
}
