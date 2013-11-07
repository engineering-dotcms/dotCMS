package it.eng.bankit.converter.index;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.converter.fileasset.AliasLinkConverter;
import it.eng.bankit.converter.fileasset.LinkConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class CollectionClusterConverter extends GenericIndexConverter {

	public CollectionClusterConverter(HmiStructure struttura) {
		super(struttura);
	}	

	@Override
	public Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_DETTAGLIO_CLUSTER") );
		return structure;
	}

	@Override
	public String getLinkRelationShipName(){
		return null;
	}
 
	public  ContentletWrapper parseContentWrapper( String lang ) throws  Exception{

		String titoloClusterIT =  getHmiStructure().getPropertyHmi(HyperwaveKey.Title + ":" + lang );
		if( UtilMethods.isSet( titoloClusterIT )){
			
			if( titoloClusterIT.length() > 255  ){
				Charset utf8=Charset.forName( "UTF-8" );				
				byte[] bytesTitle=titoloClusterIT.getBytes( utf8 );
				if(bytesTitle.length>255){
					byte[] reducedTitleBytes=ArrayUtils.subarray( bytesTitle, 0, 254 );
					titoloClusterIT =new String(reducedTitleBytes,utf8);
				}
			 
				LOG.error("TITOLO ITA TROPPO LUNGO CONTROLLARE FILE " +  getHmiStructure().getFilePath()  );
			}
		}	 
		List<HmiStructure> children = getHmiStructure().getSubFile( lang );
		Language language = ImportUtil.getLanguage( lang );

		getDotcmsSysDataWriter().createFolderOnHost( getHmiStructure().getFilePath()  , getHmiStructure() );

		List<ContentletWrapper> listaDe = addDettaglioChildren( lang );
		ContentletWrapper toreturn = null;

		if( listaDe != null && !listaDe.isEmpty() ){			
			toreturn = new ContentletWrapper();
			Contentlet contentletCluster = createDefaultContentlet( lang );			
			toreturn.setContentlet(contentletCluster);
			toreturn.setQuery( getLuceneQuery(contentletCluster ));
			if( !listaDe.isEmpty() ){			
				toreturn.setDettagliCluster( listaDe );
			}
		}else {

			List<ContentletWrapper> links = addLinks( toreturn, language );
			if( !links.isEmpty() ){
				for(ContentletWrapper cWrapper  :links ){
					if(  toreturn   != null  ){
						Contentlet c = cWrapper.getContentlet();	
						c = checkValueCluster( c, getHmiStructure() , language);
						toreturn.addListingLink(cWrapper);
					}else {
						Contentlet c = cWrapper.getContentlet();	
						c = checkValueCluster( c, getHmiStructure() , language);
						toreturn = cWrapper;
					}
				}
			}


			for( HmiStructure child : children ){
				ContentletWrapper cWrapper = getAllegato( child  , lang ) ;
				if( cWrapper != null  ){
					if(  toreturn   != null  ){
						Contentlet c = cWrapper.getContentlet();	
						c = checkValueCluster( c, getHmiStructure() , language);
						toreturn.addListingLink(cWrapper);
					}else {
						Contentlet c = cWrapper.getContentlet();	
						c = checkValueCluster( c, getHmiStructure() , language);
						toreturn = cWrapper;
					}
				}
			}

			if( listaDe != null &&  !listaDe.isEmpty() ){
				for( ContentletWrapper wr : listaDe ){
					if( wr != null  ){
						if(  toreturn   != null  ){
							Contentlet c = wr.getContentlet();	
							c = checkValueCluster( c, getHmiStructure() , language);
							toreturn.addListingLink( wr);
						}else {
							toreturn = wr;
						}
					}
				}
			}
		}

		return toreturn;
	}


	private List<ContentletWrapper>  addLinks( ContentletWrapper toreturn  , Language language ) throws Exception{
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
		String titoloClusterIT =  getHmiStructure().getPropertyHmi(HyperwaveKey.Title + ":" + ImportConfigKey.ITALIAN_LANGUAGE );
		String titoloClusterEN =  getHmiStructure().getPropertyHmi(HyperwaveKey.Title + ":" + ImportConfigKey.ENGLISH_LANGUAGE );		
		if( UtilMethods.isSet( titoloClusterIT )){
			ContentletWrapper cWrapperIta = parseContentWrapper( ImportConfigKey.ITALIAN_LANGUAGE  );
			container.add( cWrapperIta );

		}
		if( UtilMethods.isSet( titoloClusterEN )){
			ContentletWrapper cWrapperEng = parseContentWrapper( ImportConfigKey.ENGLISH_LANGUAGE  );
			container.add( cWrapperEng );
		}
		return container;
	}


	private List<ContentletWrapper> addDettaglioChildren( String lang ) throws Exception{
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories(); // recupero i figli  degli anni (eventuali cartelle )

		List<ContentletWrapper> listaDettagli  = null;
		if( childrenDir != null && !childrenDir.isEmpty() ){
			 
			Language language = ImportUtil.getLanguage( lang );
			listaDettagli = new ArrayList<ContentletWrapper>();	
			for( HmiStructure strutturaChild : childrenDir ){
				String fileName  = strutturaChild.getFile().getName();
				String formatoFiglio = strutturaChild.getPropertyHmi( HyperwaveKey.Formato );
				FolderConverter conv =  null;
				if(  !fileName.endsWith("links") ) { 
					if( strutturaChild.getFile() != null && strutturaChild.getFile().isDirectory()  ){
						conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
						if( conv instanceof  DocumentCollectionConverter ){
							DocumentCollectionConverter collectionConverter =  ((DocumentCollectionConverter)conv);
							collectionConverter.setCreateDettaglio( true );
							ContentletWrapper wrapper =  ((DocumentCollectionConverter)conv).parseContentWrapper( lang );
							if( wrapper!= null ){ 
								listaDettagli.add( wrapper );
							}
						}
						else if( conv instanceof  GenericIndexConverter ){
							ContentletWrapper wrapper =  ((GenericIndexConverter)conv).parseContentWrapper( lang );
							if( wrapper!= null ){ 
								listaDettagli.add( wrapper );
							}
						}
						else {

							conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
							if( conv instanceof  GenericDetailConverter ){
								GenericDetailConverter dConverter = (GenericDetailConverter)conv;
								ContentletWrapper wrapper =  dConverter.parseContentWrapper(lang );
								if( wrapper!= null ){
									listaDettagli.add( wrapper );								 
								}
							}

						}
					}
					else {
						if( formatoFiglio  == null ){
							formatoFiglio = "DX";
						}
						// si tratta di una directory ma non è un cluster --> 
						conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
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
		}
		return listaDettagli;
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


}
