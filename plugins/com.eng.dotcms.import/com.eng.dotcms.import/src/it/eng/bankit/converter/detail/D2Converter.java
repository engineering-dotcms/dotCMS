package it.eng.bankit.converter.detail;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class D2Converter extends GenericDetailConverter {

	public D2Converter(HmiStructure struttura) {	
		super(struttura);
	}


	public ContentletContainer parseContent() throws  Exception{
		Map<String, String> props = getHmiStructure().getPropertiesHmi();
		String formato = props.get( HyperwaveKey.Formato );
		if(formato != null && ( formato.equalsIgnoreCase("D2") 
				|| formato.equalsIgnoreCase("D1") 
				|| formato.equalsIgnoreCase("D3") 
				|| formato.equalsIgnoreCase("D4") 
				|| formato.equalsIgnoreCase("D0") ) ){ 
			return creaContentletContainer();
		}
		return null; 
	}

	protected ContentletContainer creaContentletContainer() throws Exception {
		String folderName =  getHmiStructure().getFilePath();
		getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );
		ContentletContainer container = new ContentletContainer(); 
		List<Language> langs =  ImportUtil.getDotLanguages();
		for( Language lang  : langs){ 
			ContentletWrapper cWrapper =  parseContentWrapper( lang.getLanguageCode() );
			if( cWrapper != null ){
				container.add(cWrapper);
			}
		}
		return container;
	}


	public ContentletWrapper parseContentWrapper( String correctLang ) throws Exception {
		ContentletWrapper cWrapper = null;
		Contentlet con = createDefaultContentlet( correctLang );
		String folderName =  getHmiStructure().getFilePath();
		getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );

		if( con != null  ) {
			String hidden = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );

			List<HmiStructure> children = getHmiStructure().getSubFile();
			cWrapper = new ContentletWrapper();

			addCorpo( con, getHmiStructure() );
			addImmagine( con, children );
			List<ContentletContainer> allegati =  creaListaAllegati( con, children );
			cWrapper.setAllegati( allegati);

			List<ContentletWrapper> imgsToload = addImmaginiAllegatiCorpo( con, children );
			cWrapper.setImmaginiAllegati(  imgsToload );

			List<ContentletWrapper>  links = addLinksToContenlet( con , false );				
			cWrapper.setLinks(links);

			List<ContentletWrapper> linksCorrelati =  processLinksCorrelati( con );
			cWrapper.setLinkCorrelati( linksCorrelati);

			
			cWrapper.setQuery(  getLuceneQuery( con ));				
			cWrapper.setLinksRelationShipName( getLinkRelationShipName() );

			List<ContentletWrapper>  dettagliCluster =  addClusterCollection( cWrapper , con  );  
			if( !dettagliCluster.isEmpty() ){
				for( ContentletWrapper singleDett : dettagliCluster ){
					cWrapper.addListingLink( singleDett );
				}
			}
			List<ContentletWrapper> simpleListingContent = getSimpleContent( children, correctLang );
			if( simpleListingContent != null && simpleListingContent.size() > 0  ){
				for( ContentletWrapper singleContent : simpleListingContent ){
					cWrapper.addListingLink( singleContent );
				}
			}
			cWrapper.setContentlet(con);

			if( hidden != null && hidden.equalsIgnoreCase("Hidden")){
				cWrapper.setArchived(true);
			}
		}
		return cWrapper;
	}


	private List<ContentletWrapper> getSimpleContent(List<HmiStructure> children , String lang ) {
		List<ContentletWrapper> listaC = new ArrayList<ContentletWrapper>();
		for( HmiStructure child : children  ){
	 		File fileCorrente = child.getFile();
			if( FileUtil.isTextFile( fileCorrente) && 
					!child.isCollectionHead()   ) {
				SimpleContentConverter sc = new SimpleContentConverter( child );
				sc.setCreatePage(true);
				ContentletWrapper cWrapper;
				try {
					cWrapper = sc.parseContentWrapper( lang );
					if( cWrapper != null  ){
						listaC.add( cWrapper );
					}
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
		}
		return listaC;
	}


	private List<ContentletWrapper>  addClusterCollection( ContentletWrapper cWrapper ,  Contentlet contentlet  ) { 
		List<HmiStructure> dirs = getHmiStructure().getSubDirectories() ;
		List<ContentletWrapper> listaC = new ArrayList<ContentletWrapper>();
		Language language = ImportUtil.getLanguage( contentlet.getLanguageId()  );
		try{
			for(HmiStructure direct : dirs  )
			{
				String fileName  = direct.getFile().getName();
				if( fileName.indexOf("links") == -1 ) { // se non è un oggetto links - correlati
					GenericDetailConverter conv = null;
					
					String formato = direct.getPropertyHmi(HyperwaveKey.Formato );
					if( formato != null && formato.startsWith("L")){
						conv = (GenericDetailConverter)  
						ConverterFactory.getInstance().getFolderConverter( formato , direct );
						//ContentletWrapper wrapper =  conv.parseContentWrapper( language.getLanguageCode()  );
						ContentletContainer cContainer =  conv.parseContent();
						if( cContainer != null ){
							ContentletWrapper wrapper =  cContainer.get(contentlet.getLanguageId()  );
							if( wrapper!=null ){
								listaC.add( wrapper );
							}
						}
					}else {
							if(  direct.isContainerCluster() ){
							conv = (GenericDetailConverter)  
							ConverterFactory.getInstance().getFolderConverter(  "ClusterCollection" , direct );
							
						}else if( direct.isCollectionDocumentType() ) {
							conv = (GenericDetailConverter)  
							ConverterFactory.getInstance().getFolderConverter(  "DocumentCollection" , direct );
						}else{
							conv = (GenericDetailConverter)  
							ConverterFactory.getInstance().getFolderConverter( formato , direct );
						}
						ContentletWrapper wrapper =  conv.parseContentWrapper( language.getLanguageCode()  );
						if( wrapper!= null ){
							listaC.add( wrapper );
						}
					}
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return listaC ;
	}

}
