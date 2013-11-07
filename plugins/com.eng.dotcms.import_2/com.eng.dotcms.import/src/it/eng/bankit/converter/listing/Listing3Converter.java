package it.eng.bankit.converter.listing;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.converter.index.CollectionClusterConverter;
import it.eng.bankit.converter.index.GenericIndexConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public class Listing3Converter extends GenericListingConverter {

	public Listing3Converter(HmiStructure struttura) {
		super(struttura);
	}

	public  ContentletContainer  parseContent() throws  Exception{
		Map<String, String> props = getHmiStructure().getPropertiesHmi();

		String formato = props.get( HyperwaveKey.Formato ); 
		ContentletContainer cCont = null;
		if( formato != null && ( formato.startsWith("L3") 
				|| formato.startsWith("L4") 
				|| formato.startsWith("L8"))) { 
			cCont = createContainer();
		}
		return cCont;
	}




	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
	throws Exception {
//		Language language =   ImportUtil.getLanguage( correctLang );
//		Language l = ImportUtil.getLanguage( ImportConfigKey.ITALIAN_LANGUAGE  );
//		Language le = ImportUtil.getLanguage( ImportConfigKey.ENGLISH_LANGUAGE  );

//		List<HmiStructure>  fcIT = getAllFullCollectionHead(getHmiStructure(), l.getId() );
//		List<HmiStructure>  fcEN = getAllFullCollectionHead(getHmiStructure(), le.getId() );
//		if( fcIT == null ||  fcIT.size()== 0  ) {
//			if( fcEN == null ||  fcEN.size()== 0  ) {
//				createDetail =  false;
//			}
//		}

		List<HmiStructure> childrenTesto = getHmiStructure().getSubFile() ;
		ContentletWrapper cWrapper = null;
		List<ContentletWrapper> allCWrapper ;
		Contentlet con = createDefaultContentlet( correctLang );
		if( con  != null ){ 
			allCWrapper = new ArrayList<ContentletWrapper>();

			cWrapper = new ContentletWrapper();
			addCorpo(  con, getHmiStructure() );
			List<ContentletWrapper> listingLinks =   createListingLinks( con );
			cWrapper.setListingLinks( listingLinks );

			List<ContentletWrapper> dettaglioDirs =   addDettaglioChildren(   cWrapper , con  );
			if( dettaglioDirs != null ) {
				for(ContentletWrapper cw : dettaglioDirs ){
					cWrapper.addListingLink( cw );
					allCWrapper.add(cw );
				}
			}
			addImmagine(   con, childrenTesto );

			cWrapper.setQuery(  getLuceneQuery( con ));
			cWrapper.setContentlet(con);

		}
		return cWrapper;
	}

	protected ContentletContainer createContainer() throws Exception {

		ContentletContainer container = new ContentletContainer(); 
		List<Contentlet> appoLista = new ArrayList<Contentlet>();
		Contentlet contIt = createDefaultContentlet( ImportConfigKey.ITALIAN_LANGUAGE );
		if( contIt != null ){
			appoLista.add( contIt );
		}
		Contentlet contEn = createDefaultContentlet( ImportConfigKey.ENGLISH_LANGUAGE );
		if( contEn != null ){
			appoLista.add( contEn );
		}

		// caso 1 La cartelle contiene solo file 
		List<HmiStructure> childrenTesto = getHmiStructure().getSubFile() ;
		// caso 2 La cartelle contiene altre cartelle tipo D0---Dx
		//		List<HmiStructure> childrenDirs = getHmiStructure().getSubDirectories() ;
		Language l = ImportUtil.getLanguage( ImportConfigKey.ITALIAN_LANGUAGE  );
		Language le = ImportUtil.getLanguage( ImportConfigKey.ENGLISH_LANGUAGE  );

		List<HmiStructure> fcIT = getAllFullCollectionHead( getHmiStructure(), l.getId() );

		List<HmiStructure>  fcEN = getAllFullCollectionHead( getHmiStructure(), le.getId() );
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
			addCorpo(  con, getHmiStructure() );
			List<ContentletWrapper> listingLinks =   createListingLinks( con );
			cWrapper.setListingLinks( listingLinks );

			List<ContentletWrapper> dettaglioDirs =   addDettaglioChildren(   cWrapper , con  );
			if( dettaglioDirs != null ) {
				for(ContentletWrapper cw : dettaglioDirs ){
					cWrapper.addListingLink( cw );
					allCWrapper.add(cw );
				}
			}
			addImmagine(   con, childrenTesto );
			//			List<ContentletWrapper>  dettagli  = getListaDettagli( con  );
			//			for( ContentletWrapper cw : dettagli ){
			//				cWrapper.addListingLink(cw);
			//				allCWrapper.add(cw );
			//			}
			cWrapper.setQuery(  getLuceneQuery( con ));
			cWrapper.setContentlet(con);
			//	container.add(cWrapper);

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

	protected   List<ContentletWrapper> addDettaglioChildren( ContentletWrapper cWrapper ,  Contentlet contentlet   ) throws Exception{
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories();

		Language l = ImportUtil.getLanguage( contentlet.getLanguageId()  );
		List<ContentletWrapper> listaDettagli = new ArrayList<ContentletWrapper>();		
		for( HmiStructure strutturaChild : childrenDir ){
			String formatoFiglio = strutturaChild.getPropertyHmi( HyperwaveKey.Formato );
			String fileName  = strutturaChild.getFile().getName();
			if(  !fileName.endsWith("links") ) {
				if(  !UtilMethods.isSet( formatoFiglio )   && 
						(  strutturaChild.isContainerCluster()  || strutturaChild.isCollectionType())){

					GenericIndexConverter conv = (GenericIndexConverter) ConverterFactory.getInstance().getFolderConverter(formatoFiglio , strutturaChild );
					ContentletWrapper wrapper =  conv.parseContentWrapper( l.getLanguageCode()  );
					if( wrapper!= null ){
						List<ContentletWrapper> listaLinks = wrapper.getListingLinks();
						if(listaLinks   != null && listaLinks.size() > 0 ) {
							listaDettagli.addAll( listaLinks );
						}
						wrapper.setListingLinks( new ArrayList<ContentletWrapper>() );
						listaDettagli.add( wrapper );						
					}

				}
				else if( UtilMethods.isSet( formatoFiglio ) ){
					if( formatoFiglio.equalsIgnoreCase("D0")   ) {
						GenericIndexConverter conv = (GenericIndexConverter) ConverterFactory.getInstance().getFolderConverter( null , strutturaChild );
						ContentletWrapper wrapper =  conv.parseContentWrapper( l.getLanguageCode()  );
						if( wrapper!= null ){
							List<ContentletWrapper> listaLinks = wrapper.getListingLinks();
							if(listaLinks   != null && listaLinks.size() > 0 ) {
								listaDettagli.addAll( listaLinks );
							}
							wrapper.setListingLinks( new ArrayList<ContentletWrapper>() );
							listaDettagli.add( wrapper );						
							//							if(wrapper.getAllegati() != null && wrapper.getAllegati().size() > 0   )	{
							//								List<ContentletContainer> cConts = wrapper.getAllegati();
							//								for( ContentletContainer c : cConts){										
							//									ContentletWrapper allCluster =  c.get(contentlet.getLanguageId()) ;
							//									//allCluster.getContentlet().setProperty("titolo", wrapper.getContentlet().get("titolo"));
							//									if( allCluster != null  ) {
							//										listaDettagli.add( allCluster );
							//									}
							//								}
							//							}
						}
					}else {
						FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );

						ContentletContainer cLet =  conv.parseContent();
						if( cLet != null && ! cLet.isEmpty()  ){
							ContentletWrapper wrapper = cLet.get( contentlet.getLanguageId() );
							if( wrapper!= null ){
								if( wrapper.getListingLinks()  != null && wrapper.getListingLinks().size() > 0 ) {
									listaDettagli.addAll(  wrapper.getListingLinks() );
								}
								if(wrapper.getAllegati() != null && wrapper.getAllegati().size() > 0   )	{
									List<ContentletContainer> cConts = wrapper.getAllegati();
									for( ContentletContainer c : cConts){										
										ContentletWrapper allCluster =  c.get(contentlet.getLanguageId()) ;			
										if( allCluster != null  ) {
											listaDettagli.add( allCluster );
										}
									}
								}
							}
						}							 
					}
				} else {
					String docuType = strutturaChild.getPropertyHmi(HyperwaveKey.DocumentType );
					String collType = strutturaChild.getPropertyHmi(HyperwaveKey.CollectionType );

					if( strutturaChild.isContainerCluster() || strutturaChild.isContainerCollection() 
							|| ( docuType!= null && docuType.equalsIgnoreCase("collection") ) 
							|| ( collType!= null && collType.equalsIgnoreCase("Cluster") )   ) {
						// forse è anno 
						if( formatoFiglio != null && formatoFiglio.equalsIgnoreCase("D0") 
								&& (  strutturaChild.isContainerCluster()  ||  strutturaChild.isCollectionDocumentType()   ) ){
							// si tratta di una directory ma non è un cluster --> 
							CollectionClusterConverter conv = (CollectionClusterConverter) 
							ConverterFactory.getInstance().getFolderConverter( "ClusterCollection" , strutturaChild );
							ContentletWrapper wrapper =  conv.parseContentWrapper( l.getLanguageCode()  );
							if( wrapper!= null ){ 
								listaDettagli = aggiungiDatiDACluster( strutturaChild ,   listaDettagli , wrapper , contentlet );						
							}	
						}if( strutturaChild.isContainerCluster()  ||  strutturaChild.isCollectionDocumentType()  ){
							GenericDetailConverter conv = (GenericDetailConverter) 
							ConverterFactory.getInstance().getFolderConverter( null , strutturaChild );
							ContentletWrapper wrapper =  conv.parseContentWrapper( l.getLanguageCode()  );
							if( wrapper != null ){
								List<ContentletWrapper> listaLinks = wrapper.getListingLinks();

								if(listaLinks   != null && listaLinks.size() > 0 ) {
									listaDettagli.addAll( listaLinks );
								}
								wrapper.setListingLinks( new ArrayList<ContentletWrapper>() );
								listaDettagli.add( wrapper );	
							}
						}
						//						ContentletWrapper wrapper =  conv.parseContentWrapper( l.getLanguageCode()  );
						//						if( wrapper!= null ){
						//							listaDettagli.add( wrapper );
						//							if( wrapper.getListingLinks()  != null && wrapper.getListingLinks().size() > 0 ) {
						//								listaDettagli.addAll(  wrapper.getListingLinks() );
						//							}
						//							if(wrapper.getAllegati() != null && wrapper.getAllegati().size() > 0   )	{
						//								List<ContentletContainer> cConts = wrapper.getAllegati();
						//								for( ContentletContainer c : cConts){										
						//									ContentletWrapper allCluster =  c.get(contentlet.getLanguageId()) ;
						//									//allCluster.getContentlet().setProperty("titolo", wrapper.getContentlet().get("titolo"));
						//									if( allCluster != null  ) {
						//										listaDettagli.add( allCluster );
						//									}
						//								}
						//							}
						//						}
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
