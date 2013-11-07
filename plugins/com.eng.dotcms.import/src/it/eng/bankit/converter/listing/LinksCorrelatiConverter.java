package it.eng.bankit.converter.listing;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.converter.fileasset.LinkConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class LinksCorrelatiConverter extends GenericDetailConverter {

	public LinksCorrelatiConverter(HmiStructure struttura) {
		super(struttura);
	}


	public  ContentletWrapper parseContentWrapper( String lang ) throws  Exception{
		Map<String, String> props = getHmiStructure().getPropertiesHmi();
		//		List<HmiStructure> children = getHmiStructure().getSubFile();
		ContentletWrapper cWrapper = null;
		String titolo = props.get(  HyperwaveKey.Title  + ":"+ lang );
		if( UtilMethods.isSet( titolo) ){
			List<HmiStructure> childrenTesto = getHmiStructure().getSubFile() ;
			Contentlet con = createDefaultContentlet(lang );
			if( con != null ){
				cWrapper = new ContentletWrapper();
				addCorpo( con, getHmiStructure()  );				
				addImmagine( con, childrenTesto );					
				List<ContentletWrapper>  links = addLinksToContenlet( con , false );
				cWrapper.setLinks(links);

				List<ContentletWrapper>  linksCh = parseFileTesto( childrenTesto , lang );
				if( linksCh!= null && linksCh.size() >0 ){
					for( ContentletWrapper c : linksCh ){
						cWrapper.addLink(c);
					}
				}				
				cWrapper.setLinksRelationShipName( getLinkRelationShipName() );
				cWrapper.setQuery(  getLuceneQuery( con ) );					 
				cWrapper.setContentlet(con);
				return cWrapper;
			}

		}
		return null;

	}
	private List<ContentletWrapper> parseFileTesto(List<HmiStructure> childrenTesto , String lang ) {
		List<ContentletWrapper> listaLinks = new ArrayList<ContentletWrapper>();
		if( childrenTesto!= null && childrenTesto.size() > 0 ){

			for( HmiStructure hmic : childrenTesto ){
				String bType = hmic.getPropertyHmi( HyperwaveKey.BodyType );
				if(UtilMethods.isSet( bType ) && bType.equalsIgnoreCase( "Body" ) ){

					StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );				
					String pName =hmic.getFilePath();
					File file= hmic.getFile();
					String fName = file.getName();
					String nM = fName.substring( 0 , fName.lastIndexOf(".") );
					pName =	pName.substring(0, pName.lastIndexOf(fName));
					sb.append( pName );
					if( !pName.endsWith(System.getProperty("file.separator"))){
						sb.append(pName + System.getProperty("file.separator")  );	
					}
					Folder path = it.eng.bankit.util.FolderUtil.findFolder( sb.toString() );
					getDotcmsSysDataWriter().createPageOnFolder(path, hmic  , nM  );

					LinkConverter lConv = new LinkConverter( hmic , false  );
					ContentletWrapper linkWrapper;
					try {
						linkWrapper = lConv.parseContentWrapper( lang );
						if( linkWrapper != null ){		
							listaLinks.add( linkWrapper );
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				}			
			}		
		}
		return listaLinks;
	}


	public  ContentletContainer  parseContent() throws  Exception{

		Map<String, String> props = getHmiStructure().getPropertiesHmi();
		String formato = props.get( HyperwaveKey.Formato );
		String pres = props.get( HyperwaveKey.PresentationHints );		
		if( formato == null && ( pres != null && pres.equalsIgnoreCase("Hidden")) ) {
			ContentletContainer container = new ContentletContainer(); 
			List<Contentlet> appoLista =  createDefaultListContentlet() ;		
			if( appoLista.size() > 0 ){
				for( Contentlet con  : appoLista){ 
					Language l = ImportUtil.getLanguage( con.getLanguageId() );
					ContentletWrapper cWrapper = parseContentWrapper(l.getLanguageCode() ); 
					container.add(cWrapper); 
				}
			} 
			return container;
		}
		return null;
	}




	protected void setSpecificProperties(Contentlet contentlet , String langCode  ) throws Exception  {
		super.setSpecificProperties(contentlet, langCode);
		String abstractIt = getHmiStructure().getPropertyHmi( HyperwaveKey.Abstract+":"+langCode  );
		contentlet.setProperty("corpoNotizia", abstractIt );	

		String nascondiTitolo = getHmiStructure().getPropertyHmi( HyperwaveKey.nascondiTitolo   );
		if( nascondiTitolo != null && nascondiTitolo.equalsIgnoreCase("yes") ){
			contentlet.setProperty( "mostraTitolo", ImportConfigKey.FALSE_N_VALUE );	
		}else  {
			contentlet.setProperty( "mostraTitolo", ImportConfigKey.TRUE_S_VALUE );
		}
	} 


	public String getLinkRelationShipName(){
		return ImportConfig.getProperty( "REL_NAME_DETTAGLIOCORRELATI-LINK");
	}

	@Override
	public Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_DETTAGLIOCORRELATI") );
		return structure;
	}
}
