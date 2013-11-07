package it.eng.bankit.converter.index;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.converter.detail.D2Converter;
import it.eng.bankit.converter.detail.D5Converter;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.converter.detail.SimpleContentConverter;
import it.eng.bankit.converter.fileasset.AliasLinkConverter;
import it.eng.bankit.converter.fileasset.LinkConverter;
import it.eng.bankit.converter.listing.GenericListingConverter;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public class DocumentCollectionConverter extends GenericIndexConverter {

	public DocumentCollectionConverter(HmiStructure struttura) {
		super(struttura);
	}

	private boolean createDettaglio = false;

	public  ContentletWrapper parseContentWrapper( String lang ) throws  Exception{
		Language language = ImportUtil.getLanguage( lang );
		getDotcmsSysDataWriter().createFolderOnHost( getHmiStructure().getFilePath()  , getHmiStructure() );		 

		boolean createCorpo = needCreateCorpo( language );
		ContentletWrapper toreturn = null;
		List<HmiStructure> children = getHmiStructure().getSubFile();
		if( createCorpo ) {
			String formatoFile = hmiStructure.getPropertyHmi(HyperwaveKey.Formato );
			if( formatoFile !=null && formatoFile.startsWith("D5")){
				GenericDetailConverter dettaglio = new D5Converter( hmiStructure );
				toreturn = dettaglio.parseContentWrapper(lang);
			}else{
			GenericDetailConverter dettaglio = new D2Converter( hmiStructure );
			toreturn = dettaglio.parseContentWrapper(lang);
			}
		}else {
			List<ContentletWrapper> listaLinks = addLinks( language );
			for( ContentletWrapper link : listaLinks ){
				if(  toreturn   != null  ){
					toreturn.addListingLink( link );
				}else {
					toreturn = link;
				}
			}
			List<ContentletWrapper> listaDe = addDettaglioChildren(  lang );
			if( listaDe != null && !listaDe.isEmpty() ){
				for( ContentletWrapper wr : listaDe ){
					if( wr != null  ){
						if(  toreturn   != null  ){
							Contentlet c = wr.getContentlet();	
							c = checkValueCluster( c, getHmiStructure() , language);
							toreturn.addListingLink(wr);
						}else {
							Contentlet c = wr.getContentlet();	
							c = checkValueCluster( c, getHmiStructure() , language);
							toreturn = wr;
						}
					}
				}
			}
			for( HmiStructure child : children ){
				ContentletWrapper cWrapper = getAllegato( child  , lang ) ;
				if( cWrapper != null  ){
					if(  toreturn   != null  ){
						toreturn.addListingLink(cWrapper);
					}else {
						toreturn = cWrapper;
					}
				}
			}
			for( HmiStructure child : children ){
				ContentletWrapper cWrapper = getHtmlPage( child  , lang ) ;
				if( cWrapper != null  ){
					LinkConverter lc = new LinkConverter( child );
					ContentletWrapper cWrapperLink =  lc.parseContentWrapper(lang);
					if( cWrapper != null  ){
						if(  toreturn   != null  ){
							toreturn.addListingLink(cWrapper);
						}else {
							toreturn = cWrapper;
						}
					}
					if( cWrapperLink != null  ){
						if(  toreturn   != null  ){
							toreturn.addListingLink(cWrapperLink);
						}else {
							toreturn = cWrapperLink;
						}
					}
				}
			}
		}
		return toreturn;
	}

	private boolean needCreateCorpo( Language langguage ) {
		List<HmiStructure> listaFileCorpo = getAllFullCollectionHead( getHmiStructure() , langguage.getId() );
		return ( listaFileCorpo != null && listaFileCorpo.size() > 0  ) || createDettaglio  ;
	}

	protected ContentletWrapper getHtmlPage( HmiStructure child  , String lang  ) throws Exception {
		File fileCorrente = child.getFile();
		if( FileUtil.isTextFile( fileCorrente) && 
				!child.isCollectionHead()   ) {
			SimpleContentConverter sc = new SimpleContentConverter( child );
			sc.setCreatePage(true);
			ContentletWrapper cWrapper = sc.parseContentWrapper( lang );
			return cWrapper;
		}
		return null;
	}

	

	private List<ContentletWrapper>  addLinks(   Language language ) throws Exception{
		List<HmiStructure> links = getHmiStructure().getChildrenLinks();
		List<ContentletWrapper> listaLinks = new ArrayList<ContentletWrapper>();
		for( HmiStructure hmi : links  ){
			LinkConverter lConv = new LinkConverter( hmi );
			ContentletWrapper wrapperLink = lConv.parseContentWrapper( language.getLanguageCode()  );
			if( wrapperLink != null ) {
				listaLinks.add( wrapperLink );
			}
		}
		List<HmiStructure> aliasLinks = getHmiStructure().getAliasLinks();	 
		for( HmiStructure hmi : aliasLinks  ){
			AliasLinkConverter lConv = new AliasLinkConverter( hmi );
			ContentletWrapper wrapperLink = lConv.parseContentWrapper( language.getLanguageCode()  );
			if( wrapperLink != null ) {
				listaLinks.add( wrapperLink );
			}
		}
		return listaLinks;

	}

	public  ContentletContainer parseContent() throws  Exception{
		ContentletContainer container =  new ContentletContainer();	 
		List<Language> langs =  ImportUtil.getDotLanguages();
		for( Language lang  : langs){ 
			ContentletWrapper cWrapper =  parseContentWrapper( lang.getLanguageCode() );
			if( cWrapper != null ){
				container.add(cWrapper);
			}
		}
		return container;	 
	}

	private List<ContentletWrapper> addDettaglioChildren(  String lang    ) throws Exception{
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories(); // recupero i figli  degli anni (eventuali cartelle )
		List<ContentletWrapper> listaDettagli  = null;
		Language language = ImportUtil.getLanguage( lang );
		listaDettagli = new ArrayList<ContentletWrapper>();	
		for( HmiStructure strutturaChild : childrenDir ){ 
			String formatoFiglio = strutturaChild.getPropertyHmi( HyperwaveKey.Formato );

			String fileName  = strutturaChild.getFile().getName();
			if( fileName.indexOf("links") == -1 ) { // se non è un oggetto links - correlati
				if( strutturaChild.getFile() != null && strutturaChild.getFile().isDirectory()  ){
					// si tratta di una directory CLUSTER / COLLECTION --> 
					if( ( formatoFiglio == null || !formatoFiglio.equalsIgnoreCase("D0")) && (  strutturaChild.isCollectionDocumentType() ||   strutturaChild.isContainerCluster()  )){
						GenericDetailConverter  conv = (GenericDetailConverter ) ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
						ContentletWrapper wrapper =  conv.parseContentWrapper( language.getLanguageCode()  );
						if( wrapper != null ){
							 wrapper.setLinksRelationShipName( null );
							if( wrapper!= null ){
								listaDettagli.add( wrapper );
							}
						}
					}	 
					else {
						if( formatoFiglio != null && formatoFiglio.equalsIgnoreCase("D0")   ) {
							GenericIndexConverter conv = (GenericIndexConverter) ConverterFactory.getInstance().getFolderConverter( "DocumentCollection" , strutturaChild );
							ContentletWrapper wrapper =  conv.parseContentWrapper( language.getLanguageCode()  );
							if( wrapper!= null ){
								listaDettagli.add( wrapper );
								if( wrapper.getListingLinks()  != null && wrapper.getListingLinks().size() > 0 ) {
									listaDettagli.addAll(  wrapper.getListingLinks() );
								}
								if(wrapper.getAllegati() != null && wrapper.getAllegati().size() > 0   )	{
									List<ContentletContainer> cConts = wrapper.getAllegati();
									for( ContentletContainer c : cConts){										
										ContentletWrapper allCluster =  c.get(language.getId() ) ;
										//allCluster.getContentlet().setProperty("titolo", wrapper.getContentlet().get("titolo"));
										if( allCluster != null  ) {
											listaDettagli.add( allCluster );
										}
									}
								}
							}
						}
						else {
						 
							if( formatoFiglio != null && ( formatoFiglio.equalsIgnoreCase("DV")   ||   formatoFiglio.equalsIgnoreCase("DV") ) ){
								FolderConverter fc =  ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
								ContentletContainer cContainer =  fc.parseContent();
								if( cContainer != null ){
									ContentletWrapper wrapper =  cContainer.get( language.getId() );
									if( wrapper!=null ){
										listaDettagli.add( wrapper );
									}
								}
								
							}
							else {
								GenericDetailConverter conv = getConverter( formatoFiglio , strutturaChild );
	
								if( conv instanceof  GenericDetailConverter ){
									if( conv instanceof GenericListingConverter ){
										conv = (GenericListingConverter)  
										ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
										//ContentletWrapper wrapper =  conv.parseContentWrapper( language.getLanguageCode()  );
										ContentletContainer cContainer =  conv.parseContent();
										if( cContainer != null ){
											ContentletWrapper wrapper =  cContainer.get( language.getId() );
											if( wrapper!=null ){
												listaDettagli.add( wrapper );
											}
										}
									}
									else {
										GenericDetailConverter dConverter = (GenericDetailConverter)conv;
	
										if( conv instanceof IMenuConverter ){
											((IMenuConverter)conv).setUseAllegati( true );
										}
										ContentletWrapper wrapper =  dConverter.parseContentWrapper(language.getLanguageCode() );
										if( wrapper!= null ){
											listaDettagli.add( wrapper );								
										}
									}
								}
							}

						}
					}
				}
				else {			 
					if( formatoFiglio  == null ){
						formatoFiglio = "DX";
					}					// si tratta di una directory ma non è un cluster --> 
					FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
					ContentletContainer cLet =  conv.parseContent();
					if( cLet != null && ! cLet.isEmpty()  ){
						ContentletWrapper wrapper = cLet.get( language.getId() );
						if( wrapper!= null ){
							listaDettagli.add( wrapper );
						}
					}					
				}
			} 
		}
		return listaDettagli;
	}


	private GenericDetailConverter getConverter( String formatoFiglio , HmiStructure strutturaChild  ){
		String cType = strutturaChild.getPropertyHmi(HyperwaveKey.CollectionType );
		String dType = strutturaChild.getPropertyHmi(HyperwaveKey.DocumentType );
		if( strutturaChild.hasListingParent(strutturaChild.getParentStructure() , "L5") &&  formatoFiglio!= null 
				&& formatoFiglio.startsWith("D") ){
			FolderConverter fc = (FolderConverter)  ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
 			return (( GenericDetailConverter )fc);
		}
		
		if( UtilMethods.isSet( cType ) && UtilMethods.isSet( dType ) && ( formatoFiglio!= null 
				&& formatoFiglio.startsWith("D") && ( strutturaChild.getSubDirectories( ) == null || strutturaChild.getSubDirectories( ).isEmpty()) ) ){
			String newFormato = "";
			if( cType.equalsIgnoreCase("Cluster") )
				newFormato = "ClusterCollection";
			else if(  cType.equalsIgnoreCase("collection") ){
				newFormato = "DocumentCollection";
			}
			return (GenericDetailConverter) ConverterFactory.getInstance().getFolderConverter( newFormato , strutturaChild );

		}else {
			if( strutturaChild.getFilePath().indexOf("serv_pubblico/cultura-finanziaria/conoscere") != -1){
				String newFormato =   "DocumentCollection";
			 	return (GenericDetailConverter) ConverterFactory.getInstance().getFolderConverter( newFormato , strutturaChild );

			}else {	
			FolderConverter fc = (FolderConverter)  ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
			
 			return (( GenericDetailConverter )fc);
			}
		}

	}

	private Contentlet checkValueCluster(Contentlet c, 	HmiStructure strutturaChild , Language l ) {

		if(c != null ){
			String evento = strutturaChild.getPropertyHmi( HyperwaveKey.Evento+":"+l.getLanguageCode() );
			String luogo = strutturaChild.getPropertyHmi( HyperwaveKey.Luogo+":"+l.getLanguageCode() );
			String organizzazione = strutturaChild.getPropertyHmi( HyperwaveKey.Organizzazione+":"+l.getLanguageCode()  );
			String ruoloAutore = strutturaChild.getPropertyHmi( HyperwaveKey.Ruolo_Autore+":"+l.getLanguageCode()  );

			if( UtilMethods.isSet(evento )){
				c.setStringProperty("evento", evento);
			}
			if( UtilMethods.isSet( luogo )){
				c.setStringProperty("luogo", luogo);
			}
			if( UtilMethods.isSet(organizzazione )){
				c.setStringProperty("organizzazione", evento);
			}
			if( UtilMethods.isSet( ruoloAutore )){
				c.setStringProperty("ruoloAutore", ruoloAutore);
			}

			String autore = strutturaChild.getPropertyHmi( HyperwaveKey.Autore );
			String dataEmanazione = strutturaChild.getPropertyHmi( HyperwaveKey.Data_emanazione );
			if( UtilMethods.isSet( autore )){
				c.setStringProperty("autoreallegato", autore );
			}
			try{
				if( UtilMethods.isSet(dataEmanazione ) ){
					Date timeM  = simpleHwDateFormat.parse( dataEmanazione );
					c.setDateProperty( "dataEmanazione", timeM   );
				}
			}catch (Exception e) {
			}
		}
		return c;
	}


	public void setCreateDettaglio(boolean createDettaglio) {
		this.createDettaglio = createDettaglio;
	}

}
