package it.eng.bankit.converter.index;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.converter.fileasset.AllegatoConverter;
import it.eng.bankit.converter.fileasset.LinkConverter;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public abstract class GenericIndexConverter extends GenericDetailConverter implements FolderConverter  {

	private Structure structure ;

	public GenericIndexConverter( HmiStructure struttura ){
		super(struttura);
	}



	@Override
	public Structure getDotStructure() {
		if( structure == null ){
			structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_INDEX") );
		}
		return structure;
	} 	

	protected List<ContentletWrapper> createMenuLinks( Contentlet contentlet ) throws Exception {
		List<ContentletWrapper> detta = new ArrayList<ContentletWrapper>();
		List<HmiStructure> links = getHmiStructure().getChildrenLinks();

		Comparator<HmiStructure> st =getHmiStructure().getLinksComparator(getHmiStructure(), contentlet);
		Collections.sort( links, st );

		for( HmiStructure hmi : links  ){
			Language langContentlet =  APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());
			String title = hmi.getPropertyHmi(  HyperwaveKey.Title+":"+langContentlet.getLanguageCode() );
			if( UtilMethods.isSet(title ) && ( !hmi.isCollectionHead() ) ){
				LinkConverter lConv = new LinkConverter( hmi , true );
				ContentletWrapper wrapp =  lConv.parseContentWrapper( langContentlet.getLanguageCode()  );
				if(wrapp != null){
					detta.add(wrapp);
				}
			}

		}
		return detta;
	} 

	protected List<ContentletWrapper> addAllegatoMenuLinks( Contentlet con  , List<HmiStructure> children ) throws Exception {
		List<ContentletWrapper> listaAll = new ArrayList<ContentletWrapper>();
		for( HmiStructure child : children ){
			File fileCorrente = child.getFile();
			String langFile = child.getPropertyHmi( HyperwaveKey.HW_Language );
			if( langFile!= null ){
				Language language =   ImportUtil.getLanguage( con.getLanguageId()  );
				String title = child.getPropertyHmi( HyperwaveKey.Title + ":" + language.getLanguageCode() );
				if( UtilMethods.isSet( title )){
					if( FileUtil.isAttachFile(fileCorrente) && !child.isCollectionHead()   ) {
						LOG.info("LINGUA (" + language.getLanguage() +") aggiungo allegato alla contentlet  " + con.getStringProperty("titolo")  );

						FolderConverter ac = getAllegatoConverter(child , true , language.getLanguageCode()  );	
						ContentletWrapper wrapp = null;
						if( ac instanceof AllegatoConverter ) {
							wrapp = ((AllegatoConverter) ac).parseContentWrapper( language.getLanguageCode() );
						}else{
							ContentletContainer cc = ac.parseContent();
							if( cc != null ){
								wrapp =  cc.get(language.getId() );
							}
						}
						if( wrapp != null ){				
							listaAll.add( wrapp );
						}
					} 
				}	 
			}
		}
		return listaAll;
	}
	
	
	protected ContentletWrapper getAllegato(HmiStructure child  , String lang  ) throws Exception {
		File fileCorrente = child.getFile();
		String langFile = child.getPropertyHmi(  HyperwaveKey.HW_Language );
		ContentletWrapper alleg  = null;
		if( langFile!= null ){
			Language language =   ImportUtil.getLanguage( langFile );
			if( FileUtil.isAttachFile(fileCorrente) && !child.isCollectionHead() ) {
				if( UtilMethods.isSet( lang )  ) {					
					FolderConverter ac = getAllegatoConverter(child , false , language.getLanguageCode()  );	
					if( ac instanceof AllegatoConverter ){						  						 
						ContentletWrapper  allegatoWrapper = ((AllegatoConverter)ac).parseContentWrapper(lang );
						return allegatoWrapper; 
					}else {
						ContentletContainer  allegatoContainer = ac.parseContent();
						if( allegatoContainer != null ){
							alleg =  allegatoContainer.get( language.getId() );				 
						}
					}
				}else{
					FolderConverter ac = getAllegatoConverter(child , false , language.getLanguageCode()  );												 
					ContentletContainer  allegatoContainer =  ac.parseContent();
					if( allegatoContainer != null ){
						alleg =  allegatoContainer.get( language.getId() );						 
					}
				}
			}
		}
		return alleg;
	}
	
	
}
