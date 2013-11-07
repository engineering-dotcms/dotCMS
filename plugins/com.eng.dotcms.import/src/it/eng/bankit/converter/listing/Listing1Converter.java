package it.eng.bankit.converter.listing;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.converter.detail.D2Converter;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.converter.index.CollectionClusterConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.LanguageCallback;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class Listing1Converter extends GenericListingConverter {

	public Listing1Converter(  HmiStructure struttura) {
		super(struttura);
	}


	public  ContentletContainer  parseContent() throws  Exception{

		Map<String, String> props = getHmiStructure().getPropertiesHmi();

		String formato = props.get( HyperwaveKey.Formato );

		if(formato != null && ( formato.startsWith("L1") || formato.startsWith("L2") 
				|| formato.startsWith("L8")  ) ) {
			ContentletContainer container = new ContentletContainer(); 
			List<Contentlet> appoLista =  createDefaultListContentlet() ;		

			// caso 1 La cartelle contiene solo file 
			List<HmiStructure> childrenTesto = getHmiStructure().getSubFile() ;
			// caso 2 La cartelle contiene altre cartelle tipo D0---Dx
			//		List<HmiStructure> childrenDirs = getHmiStructure().getSubDirectories() ;
			Language l = ImportUtil.getLanguage( ImportConfigKey.ITALIAN_LANGUAGE  );
			Language le = ImportUtil.getLanguage( ImportConfigKey.ENGLISH_LANGUAGE  );

			List<HmiStructure>  fcIT = getAllFullCollectionHead(getHmiStructure(), l.getId() );
			List<HmiStructure>  fcEN = getAllFullCollectionHead(getHmiStructure(), le.getId() );
			boolean  createDetail = true;
			if( fcIT == null ||  fcIT.size()== 0  ) {
				if( fcEN == null ||  fcEN.size()== 0  ) {
					createDetail =  false;
				}
			}

			List<ContentletWrapper> allCWrapper ;
			for( Contentlet con  : appoLista){ 
				allCWrapper = new ArrayList<ContentletWrapper>();

				ContentletWrapper cWrapper = new ContentletWrapper();
				addCorpo( con );				
				addImmagine( con, childrenTesto );

				List<ContentletWrapper> listingLinks =   createListingLinks( con );
				cWrapper.setListingLinks( listingLinks );
				allCWrapper.addAll(listingLinks );

				List<ContentletWrapper> linksCorrelati =  processLinksCorrelati( con );
				cWrapper.setLinkCorrelati( linksCorrelati);

				List<ContentletWrapper> dettaglioDirs =   addDettaglioChildren(   cWrapper , con  );
				if( dettaglioDirs != null ) {
					for(ContentletWrapper cw : dettaglioDirs ) {
						cWrapper.addListingLink( cw );
						allCWrapper.add( cw );
					}
				}
				List<ContentletContainer> allegati =   creaListaAllegati( con, childrenTesto );
				if( allegati != null && allegati.size() > 0 ){
					//createDetail = true ; 
					for (ContentletContainer allegato : allegati ) {
						ContentletWrapper allegatoWrapper = allegato.get( con.getLanguageId()  );
						if(  allegatoWrapper != null ) {
							cWrapper.addListingLink( allegatoWrapper );
							allCWrapper.add( allegatoWrapper );
						}
					}
				}
				cWrapper.setQuery(  getLuceneQuery( con ));
				cWrapper.setContentlet( con );
				if( createDetail ){					
					container.add(cWrapper);
				}else {
					for( ContentletWrapper w : allCWrapper  ) {
						if( container.get( con.getLanguageId() ) != null  ){
							ContentletWrapper cWrapperOld = container.get(  con.getLanguageId() );
							cWrapperOld.addListingLink( w );
						}else {
							container.add( w );
						}
					}
				}
			}
			return container;
		}
		return null;
	}


	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
	throws Exception {
		Language language =   ImportUtil.getLanguage( correctLang );
		Language l = ImportUtil.getLanguage( ImportConfigKey.ITALIAN_LANGUAGE  );
		Language le = ImportUtil.getLanguage( ImportConfigKey.ENGLISH_LANGUAGE  );

		List<HmiStructure>  fcIT = getAllFullCollectionHead(getHmiStructure(), l.getId() );
		List<HmiStructure>  fcEN = getAllFullCollectionHead(getHmiStructure(), le.getId() );
		boolean  createDetail = true;
		if( fcIT == null ||  fcIT.size()== 0  ) {
			if( fcEN == null ||  fcEN.size()== 0  ) {
				createDetail =  false;
			}
		}

		List<HmiStructure> childrenTesto = getHmiStructure().getSubFile() ;
		ContentletWrapper cWrapper = null;
		List<ContentletWrapper> allCWrapper ;
		Contentlet con = createDefaultContentlet( correctLang );
		if( con  != null ){
	
			allCWrapper = new ArrayList<ContentletWrapper>();
	
			cWrapper = new ContentletWrapper();
			addCorpo( con );				
			addImmagine( con, childrenTesto );
	
			List<ContentletWrapper> listingLinks =   createListingLinks( con );
			cWrapper.setListingLinks( listingLinks );
			allCWrapper.addAll(listingLinks );
	
			List<ContentletWrapper> linksCorrelati =  processLinksCorrelati( con );
			cWrapper.setLinkCorrelati( linksCorrelati);
	
			List<ContentletWrapper> dettaglioDirs =   addDettaglioChildren(   cWrapper , con  );
			if( dettaglioDirs != null ) {
				for(ContentletWrapper cw : dettaglioDirs ) {
					cWrapper.addListingLink( cw );
					allCWrapper.add( cw );
				}
			}
			List<ContentletContainer> allegati =   creaListaAllegati( con, childrenTesto );
			if( allegati != null && allegati.size() > 0 ){
				//createDetail = true ; 
				for (ContentletContainer allegato : allegati ) {
					ContentletWrapper allegatoWrapper = allegato.get( con.getLanguageId()  );
					if(  allegatoWrapper != null ) {
						cWrapper.addListingLink( allegatoWrapper );
						allCWrapper.add( allegatoWrapper );
					}
				}
			}
			cWrapper.setQuery(  getLuceneQuery( con ));
			cWrapper.setContentlet( con );
			 
		}
		return cWrapper;

	}


	private List<ContentletWrapper> addDettaglioChildren( ContentletWrapper cWrapper ,  Contentlet contentlet   ) throws Exception{
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories();
		List<ContentletWrapper> listaDettagli = new ArrayList<ContentletWrapper>();		
		for( HmiStructure strutturaChild : childrenDir ){
			String formatoFiglio = strutturaChild.getPropertyHmi( HyperwaveKey.Formato );
			Language l = ImportUtil.getLanguage( contentlet.getLanguageId()  );
			String fileName  = strutturaChild.getFile().getName();
			if(  !fileName.endsWith("links") ) { 
				if( strutturaChild.getFile() != null && strutturaChild.getFile().isDirectory()  ){
					// si tratta di una directory CLUSTER / COLLECTION --> 
					if( strutturaChild.isContainerCluster()  ||  strutturaChild.isCollectionDocumentType()  ){
						GenericDetailConverter conv = (GenericDetailConverter) 
						ConverterFactory.getInstance().getFolderConverter( null , strutturaChild );
						ContentletWrapper wrapper =  conv.parseContentWrapper( l.getLanguageCode()  );
						if( wrapper!= null ){ 
							listaDettagli = aggiungiDatiDACluster( strutturaChild , listaDettagli , wrapper , contentlet );
						}
					}
					else if( formatoFiglio != null && formatoFiglio.equalsIgnoreCase("D0") 
							&& (  strutturaChild.isContainerCluster()  ||  strutturaChild.isCollectionDocumentType()   ) ){
						// si tratta di una directory ma non è un cluster --> 
						CollectionClusterConverter conv = (CollectionClusterConverter) 
						ConverterFactory.getInstance().getFolderConverter( "ClusterCollection" , strutturaChild );
						ContentletWrapper wrapper =  conv.parseContentWrapper( l.getLanguageCode()  );
						if( wrapper!= null ){ 
							listaDettagli = aggiungiDatiDACluster( strutturaChild ,   listaDettagli , wrapper , contentlet );						
						}	
					}
					else {
						// si tratta di una directory ma non è un cluster --> 
						FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
						GenericDetailConverter dConverter =  null;
						if( conv instanceof  D2Converter ){
							dConverter = (D2Converter)conv;

						} else 	if( conv instanceof  GenericDetailConverter ){  
							dConverter = (GenericDetailConverter)conv;

						}
						ContentletWrapper wrapper =  dConverter.parseContentWrapper(l.getLanguageCode() );
						if( wrapper!= null ){
							listaDettagli.add( wrapper );
						}
					}
				} 
			}
		}
		return listaDettagli;
	}

	private List<ContentletWrapper>  aggiungiDatiDACluster( HmiStructure strutturaChild , List<ContentletWrapper> listaDettagli ,  ContentletWrapper resultWrapper , Contentlet listingContentlet  ){

		listaDettagli.add( resultWrapper );

		if( strutturaChild.isCollectionDocumentType()  ){
			if( resultWrapper.getListingLinks()  != null && resultWrapper.getListingLinks().size() > 0 ) {							
				for( ContentletWrapper cw : resultWrapper.getListingLinks() ){
					listaDettagli.add( cw );
				}
			}
			if(resultWrapper.getAllegati() != null && resultWrapper.getAllegati().size() > 0   )	{
				List<ContentletContainer> cConts = resultWrapper.getAllegati();
				for( ContentletContainer c : cConts){
					ContentletWrapper allCluster =  c.get( listingContentlet.getLanguageId()) ;
					if( allCluster != null ){
						listaDettagli.add( allCluster );
					}
				}
			}
		}
		return listaDettagli;

	}

}
