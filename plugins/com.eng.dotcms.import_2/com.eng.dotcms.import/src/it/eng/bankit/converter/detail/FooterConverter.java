package it.eng.bankit.converter.detail;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.util.FolderUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.UtilMethods;

public class FooterConverter extends D2Converter {

	public FooterConverter(HmiStructure struttura) {		
		super(struttura);
	}


	public ContentletContainer parseContent() throws  Exception{
		String folderName =  getHmiStructure().getFilePath();
		Folder folderFooter = getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );
		ContentletContainer container = new ContentletContainer(); 
		List<Contentlet> appoLista =  createDefaultListContentlet() ;		 		

		for( Contentlet con  : appoLista){ 			
			List<ContentletWrapper>  dettagli  = addFoooterFolder( con  );
			for( ContentletWrapper cw : dettagli ){
				ContentletWrapper cWrapper  = container.get(con.getLanguageId() );		 

				if( cWrapper != null ){
					cWrapper.addListingLink( cw );
				}else{
					container.add( cw );
				}
			}
		}
		List<HmiStructure> childrenLinks = getHmiStructure().getChildrenLinks();
		addFooterMenuLink( childrenLinks , folderFooter );
		return container;
	}

	public ContentletWrapper parseContentWrapper( String correctLang ) throws Exception {
		return null;
	}


	private void addFooterMenuLink(List<HmiStructure> childrenLinks , Folder folderFooter ) {
		Integer sort = Integer.getInteger("0");
		try{
 			for( HmiStructure c : childrenLinks ){
				String nameFile = c.getFilePath();
				if( nameFile.endsWith(".hmi") ){
					nameFile = nameFile.substring(nameFile.lastIndexOf("/") + 1 , nameFile.lastIndexOf("."));					 
				}
				String seq = c.getPropertyHmi(HyperwaveKey.Sequence );
				if( UtilMethods.isSet(seq ) ){
					try{
						sort = Integer.parseInt( seq );
					}catch (Exception e) { }
				}
 				String host = c.getPropertyHmi(HyperwaveKey.Host );
				String path = c.getPropertyHmi(HyperwaveKey.Path );
				Link link = new Link();
				link.setFriendlyName( nameFile );
				link.setTitle( nameFile  );

				link.setModUser( ImportUtil.getUser().getUserId() );
				link.setOwner(ImportUtil.getUser().getUserId());
				link.setShowOnMenu( true );
				link.setTarget( "self" );

				link.setIDate( new Date() );
				link.setModDate( new Date() );
				link.setSortOrder( sort );
				if(UtilMethods.isSet( host )  ) {
					if( host.equalsIgnoreCase(ImportConfig.getProperty("DEFAULT_SITE_LINK"))){
						link.setLinkCode(path);
						link.setLinkType(Link.LinkType.CODE.toString());
						Folder f = 	FolderUtil.createFolder("footer/"+nameFile, sort , false);
					}else{
						String protocol = c.getPropertyHmi(HyperwaveKey.Protocol );
						link.setLinkType(Link.LinkType.EXTERNAL.toString());
						link.setProtocal( protocol ); 
						link.setUrl( host + "/" + path );
					}
				}
				getDotcmsSysDataWriter().createMenuLink( link, folderFooter , c  );
			}
		}catch (Exception e) {
 		} 


	}


	private List<ContentletWrapper> addFoooterFolder( Contentlet con ) throws Exception{
		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories();
		List<ContentletWrapper> listaDettagli = new ArrayList<ContentletWrapper>();		
		for( HmiStructure strutturaChild : childrenDir ){
			String formatoFiglio = strutturaChild.getPropertyHmi( HyperwaveKey.Formato );
			String fileName  = strutturaChild.getFile().getName();
			Language lng = ImportUtil.getLanguage(con.getLanguageId() );
			if(  !fileName.endsWith("links") ) {
				if( strutturaChild.getFile() != null && strutturaChild.getFile().isDirectory()  ){
//					// si tratta di una directory CLUSTER / COLLECTION --> 
//					if( formatoFiglio == null  && (  strutturaChild.isContainerCluster()  || strutturaChild.isContainerCollection() )){
//						formatoFiglio = "ClusterCollection"; 
//					}else if( formatoFiglio == null ){
//						formatoFiglio = "DX"; 
//					}
					FolderConverter conv = ConverterFactory.getInstance().getFolderConverter( formatoFiglio , strutturaChild );
					if( conv instanceof GenericDetailConverter ){
						ContentletWrapper wrapper = (( GenericDetailConverter )conv).parseContentWrapper(lng.getLanguageCode() );
						if( wrapper!= null ){
							listaDettagli.add( wrapper );
						}
					}else {
						ContentletContainer cContainer =  conv.parseContent();
						if( cContainer != null  ){
							ContentletWrapper wrapper =  cContainer.get(lng.getId() );
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
}
