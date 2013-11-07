package it.eng.bankit.converter.detail;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.converter.fileasset.LinkConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public class BoxMenuConverter extends GenericDetailConverter {

	public BoxMenuConverter(HmiStructure struttura) {
		super(struttura);
	}

	@Override
	public ContentletContainer parseContent() throws Exception {

		Map<String, String> props = getHmiStructure().getPropertiesHmi();
		ContentletContainer container = new ContentletContainer();
		String formato = props.get( HyperwaveKey.Formato );
		if( formato!= null && formato.equalsIgnoreCase("Box") ) {
			String folderName =  getHmiStructure().getFilePath();
			getDotcmsSysDataWriter().createFolderOnHost( folderName , getHmiStructure() );
			List<Language> langs =  ImportUtil.getDotLanguages();
			for( Language lang  : langs){ 
				ContentletWrapper cWrapper =  parseContentWrapper( lang.getLanguageCode() );
				if( cWrapper != null ){
					container.add(cWrapper);
				}
			}
		}
		return container;
	}


	// ho come figli d1...
	private List<ContentletWrapper> addDettaglioChildren( Contentlet con ) throws Exception{
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories();
		List<ContentletWrapper> listaDettagli = new ArrayList<ContentletWrapper>();		
		for( HmiStructure strutturaChild : childrenDir ){
			String formatoFiglio = strutturaChild.getPropertyHmi( HyperwaveKey.Formato );			
			if( UtilMethods.isSet( formatoFiglio )){

				FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
				System.out.println( " Figlio di BOX  - formatoFiglio " + formatoFiglio );
				ContentletContainer cLet =  conv.parseContent();
				if( cLet != null && ! cLet.isEmpty()  ){
					System.out.println( " HO CREATO I FIGLI DI BOX # " + cLet.getAll().size()  );
					ContentletWrapper wrapper = cLet.get( con.getLanguageId() );
					if( wrapper!= null ){
						listaDettagli.add( wrapper  );
					}
				}					
			}else {
				System.out.println( " Figlio di BOX  - formatoFiglio NULLO prob è collection/cluster --> devo chiamare DX"  );
				FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( "DX" , strutturaChild );
				ContentletContainer cLet =  conv.parseContent();
				if( cLet != null && ! cLet.isEmpty()  ){
					System.out.println( " HO CREATO I FIGLI DI BOX " + cLet.getAll().size()  );
					ContentletWrapper wrapper = cLet.get( con.getLanguageId() );
					if( wrapper!= null ){
						listaDettagli.add( wrapper  );
					}
				}

			}
		}
		return listaDettagli;
	}

	// ho come figli collection o cluster 
	private List<ContentletWrapper> createListingLinks( Language language ) throws Exception {
		List<ContentletWrapper> detta = new ArrayList<ContentletWrapper>();
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories();

		for( HmiStructure strutturaChild : childrenDir ){
			String formato = strutturaChild.getPropertyHmi(HyperwaveKey.Formato );

			if(  formato == null && (  strutturaChild.isContainerCluster() || strutturaChild.isContainerCollection()  )){
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
			}
		}
		return detta;
	}

	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
			throws Exception {
		
		List<HmiStructure> children = getHmiStructure().getSubFile();
		ContentletWrapper cWrapper = null;
		Contentlet con = createDefaultContentlet( correctLang );
		if( con != null  ) {
	
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
			cWrapper.setListingLinks( dettagli );		
			Language l = APILocator.getLanguageAPI().getLanguage(con.getLanguageId());
			List<ContentletWrapper> listingLinks =   createListingLinks( l );
			if( listingLinks!= null && listingLinks.size() >0  ){
				for( ContentletWrapper cw : listingLinks  )
					cWrapper.addListingLink( cw );
			}
			cWrapper.setQuery(  getLuceneQuery( con ));
			cWrapper.setContentlet(con);
		}
		return cWrapper;
	} 

}
