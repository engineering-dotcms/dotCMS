package it.eng.bankit.converter.news;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.detail.SimpleContentConverter;
import it.eng.bankit.converter.fileasset.LinkConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class SelettoreConverter extends StrilloConverter {

	public SelettoreConverter(HmiStructure struttura) {
		super(struttura);
 	}
// creare anche nenu link
	public ContentletContainer parseContent() throws  Exception{

		Map<String, String> props = getHmiStructure().getPropertiesHmi();

		String formato = props.get( HyperwaveKey.Formato );
	 
	 
		if(formato != null &&   formato.equalsIgnoreCase("News") ){

 			String folderName =  getHmiStructure().getFilePath();
			getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );
			ContentletContainer container = new ContentletContainer(); 

 			List<Contentlet> appoLista = new ArrayList<Contentlet>();
 
			List<HmiStructure> childrenDirs = getHmiStructure().getSubDirectories();

			for( HmiStructure singleDir : childrenDirs  ){
				String dirName =  singleDir.getFilePath();
				getDotcmsSysDataWriter().createFolderOnHost( dirName, getHmiStructure() );
 				appoLista =  createDefaultListContentlet() ;		
 				for( Contentlet con  : appoLista){ 
					ContentletWrapper cWrapper = new ContentletWrapper();
					addCorpo(  con, getHmiStructure() );
					List<ContentletWrapper> listingLinks =   createListingLinks( con  , singleDir );
					cWrapper.setListingLinks( listingLinks );
 					cWrapper.setContentlet(con);
				 	container.add(cWrapper);
				}
			} 
			List<HmiStructure> childrenText = getHmiStructure().getSubFile();
			ContentletContainer containerSimple = null; 
			for( HmiStructure singleFile : childrenText  ){
				String l = singleFile.getPropertyHmi(HyperwaveKey.HW_Language );
				
				Language lan = ImportUtil.getLanguage( l );
				
				SimpleContentConverter sc = new SimpleContentConverter( singleFile );
				ContentletWrapper cWrapper = sc.parseContentWrapper(l);
				ContentletWrapper orgWrapper = container.get(lan.getId() ); 
				if(orgWrapper != null ){
					orgWrapper.addListingLink( cWrapper );
				}else {
					container.add( cWrapper );
				}
				 
			}
				

			return container;
		}
		return null;
	} 
	
	
	
	protected List<ContentletWrapper> createListingLinks( Contentlet contentlet ,  HmiStructure singleDir ) throws Exception {
		List<ContentletWrapper> detta = new ArrayList<ContentletWrapper>();
		List<HmiStructure> links = singleDir.getChildrenLinks();
		for( HmiStructure hmi : links  ){
			Language langContentlet =  APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());
 			String title = hmi.getPropertyHmi(  HyperwaveKey.Title+":"+langContentlet.getLanguageCode() );
			if( UtilMethods.isSet(title )){
				LinkConverter lConv = new LinkConverter( hmi );
				ContentletContainer cLet = lConv.parseContent();
				if( cLet != null && ! cLet.isEmpty()  ){
					if( cLet.get( contentlet.getLanguageId() )!= null ) {
						detta.add( cLet.get( contentlet.getLanguageId() )  );
					}
				}
			}
		}
		return detta;
	} 
	
	
	public  String getLinkRelationShipName(){
		return ImportConfig.getProperty( "REL_NAME_SELETTORE-LINK");
	}
	public Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_NS") );
		return structure;
	}
	
}
