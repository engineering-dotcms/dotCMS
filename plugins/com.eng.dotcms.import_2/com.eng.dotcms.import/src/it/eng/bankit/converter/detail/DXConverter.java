package it.eng.bankit.converter.detail;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.util.HyperwaveKey;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

public class DXConverter extends D2Converter {

	public DXConverter(HmiStructure struttura) {		
		super(struttura);
	}


	public ContentletContainer parseContent() throws  Exception{
		String folderName =  getHmiStructure().getFilePath();
		LOG.info(" DXConverter -->  FORMATO NULL " +  folderName  );
		getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );
		ContentletContainer container = new ContentletContainer(); 
		List<Contentlet> appoLista =  createDefaultListContentlet() ;		
		String hidden = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );
 
		List<HmiStructure> children = getHmiStructure().getSubFile();

		for( Contentlet con  : appoLista){ 
			ContentletWrapper cWrapper = new ContentletWrapper();
			addCorpo(  con, getHmiStructure() );
			addImmagine( con, children );
			List<ContentletContainer> allegati =  creaListaAllegati( con, children );
			cWrapper.setAllegati( allegati);

			List<ContentletWrapper> imgsToload = addImmaginiAllegatiCorpo( con, children );
			cWrapper.setImmaginiAllegati(  imgsToload );

			List<ContentletWrapper>  links = addLinksToContenlet( con , false   );				
			cWrapper.setLinks(links);

			List<ContentletWrapper> linksCorrelati =  processLinksCorrelati( con );
			cWrapper.setLinkCorrelati( linksCorrelati);		

			List<ContentletWrapper>  dettagli  = addDettaglioChildren( con  );
			for( ContentletWrapper cw : dettagli ){
				cWrapper.addListingLink(cw);
			}
			
			if( hidden != null && hidden.equalsIgnoreCase("Hidden")){
				cWrapper.setArchived(true);
			}
			cWrapper.setQuery(  getLuceneQuery( con ));				
			cWrapper.setLinksRelationShipName( getLinkRelationShipName() );

			cWrapper.setContentlet(con);
			container.add(cWrapper);
		}
		return container;
	}

	private List<ContentletWrapper> addDettaglioChildren( Contentlet con ) throws Exception{
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories();
		List<ContentletWrapper> listaDettagli = new ArrayList<ContentletWrapper>();		
		for( HmiStructure strutturaChild : childrenDir ){
			String formatoFiglio = strutturaChild.getPropertyHmi( HyperwaveKey.Formato );
			String fileName  = strutturaChild.getFile().getName();
			if(  !fileName.endsWith("links") ) {
				if( strutturaChild.getFile() != null && strutturaChild.getFile().isDirectory()  ){
					// si tratta di una directory CLUSTER / COLLECTION --> 
					if( formatoFiglio == null  && (  strutturaChild.isContainerCluster()  || strutturaChild.isContainerCollection() )){
						formatoFiglio = "ClusterCollection"; 
					}else if( formatoFiglio == null ){
						formatoFiglio = "DX"; 
					}
					FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
					ContentletContainer cLet =  conv.parseContent();
					if( cLet != null && ! cLet.isEmpty()  ){
						ContentletWrapper wrapper = cLet.get( con.getLanguageId() );
						if( wrapper!= null ){
							listaDettagli.add( wrapper );
						}
					}
				}
			}
		}
		return listaDettagli;
	}
}
