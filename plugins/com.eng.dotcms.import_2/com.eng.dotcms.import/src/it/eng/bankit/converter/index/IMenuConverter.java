package it.eng.bankit.converter.index;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.ImportUtil;

import java.util.List;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class IMenuConverter extends GenericIndexConverter {

	public IMenuConverter(HmiStructure struttura) {
		super(struttura);
	}

	private boolean useAllegati = false ;


	public void setUseAllegati(boolean useAllegati) {
		this.useAllegati = useAllegati;
	}


	//se non ha figli il menu è vuoto e non inserisco il content (almeno credo)
	public  ContentletContainer parseContent() throws  Exception{
		ContentletContainer container =  null;
		getDotcmsSysDataWriter().createFolderOnHost(getHmiStructure().getFilePath()  , getHmiStructure() );

		if( hasFileToProcess () ) {
			container = new ContentletContainer(); 
			List<Language> langs =  ImportUtil.getDotLanguages();
			for( Language lang  : langs){ 
				ContentletWrapper cWrapper = parseContentWrapper( lang.getLanguageCode() );
				if( cWrapper != null ){
					container.add(cWrapper);
				}
			}
		}		 
		return container;
	}


	private boolean hasFileToProcess( ){
		List<HmiStructure> children = getHmiStructure().getSubFile();
		List<HmiStructure> linksHmi = getHmiStructure().getChildrenLinks();
		List<HmiStructure> aliasLinks = getHmiStructure().getAliasLinks();
		boolean docCorrelati = getHmiStructure().hasDocumentiCorrelati();
		return (  !children.isEmpty() || !linksHmi.isEmpty()  || docCorrelati  || !aliasLinks.isEmpty() );
	}



	@Override
	public ContentletWrapper parseContentWrapper( String correctLang )
	throws Exception {
		List<HmiStructure> children = getHmiStructure().getSubFile();

		Contentlet contentlet = createDefaultContentlet(correctLang )	;
		if( contentlet != null ) {
			ContentletWrapper cWrapper = new ContentletWrapper();
			List<HmiStructure> listaFileCorpo = getAllFullCollectionHead(  getHmiStructure() , contentlet.getLanguageId() );
			if( listaFileCorpo.size() > 0 ){
				addCorpo(  contentlet, listaFileCorpo  );
				List<ContentletWrapper>  links = addLinksToContenlet( contentlet , false   );
				cWrapper.setLinks(links);
				addImmagine(   contentlet, children );
				List<ContentletWrapper> imgsToload = addImmaginiAllegatiCorpo( contentlet, children );
				cWrapper.setImmaginiAllegati(  imgsToload );
				if( useAllegati ){
					List<ContentletContainer> allegati =  creaListaAllegati( contentlet, children );
					cWrapper.setAllegati( allegati);
				}

			}
			if( !useAllegati ){
				List<ContentletWrapper> allegati =  addAllegatoMenuLinks( contentlet, children );
				for( ContentletWrapper allegatoLink : allegati ){
					cWrapper.addListingLink( allegatoLink ); 					
				}
			}

			List<ContentletWrapper> linksCorrelati =  processLinksCorrelati( contentlet );
			cWrapper.setLinkCorrelati( linksCorrelati);

			List<ContentletWrapper> listingLinks =   createMenuLinks( contentlet );
			for( ContentletWrapper listingLink : listingLinks ){
				cWrapper.addListingLink( listingLink ); 					
			}
			cWrapper.setLinksRelationShipName( getLinkRelationShipName() );
			cWrapper.setQuery(  getLuceneQuery( contentlet ));
			cWrapper.setContentlet( contentlet );
			return cWrapper;
		}
		return null;
	}



}
