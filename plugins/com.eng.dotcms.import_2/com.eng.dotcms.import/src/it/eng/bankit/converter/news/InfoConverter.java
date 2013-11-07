package it.eng.bankit.converter.news;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;

import java.util.List;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class InfoConverter extends BoxSidebarConverter {
 
	public InfoConverter(HmiStructure struttura) {
		super(struttura);
 	}


	public  ContentletContainer parseContent() throws  Exception{
		ContentletContainer container = new ContentletContainer(); 
		List<Contentlet> appoLista =  createDefaultListContentlet();
		List<HmiStructure> children = getHmiStructure().getSubFile();
		String filePath =  getHmiStructure().getFilePath();		
 		for( Contentlet con  : appoLista){ 
			ContentletWrapper cWrapper = new ContentletWrapper();
			List<HmiStructure>  listaFile =  getAllFullCollectionHead( getHmiStructure() , con.getLanguageId() );
			addCorpo( con , listaFile );
			Language l = ImportUtil.getLanguage(con.getLanguageId());
			setCorpoProperties(con, l.getLanguageCode() );
			addImmagine(   con, children );
			String keyInfo = "";
			if( ( filePath.indexOf("calendario_prossime_pubblicazioni") != -1 ) ){
				 keyInfo ="pubblicazioni";
				 con.setProperty("tipoInfo", keyInfo);
				 cWrapper.addCategories(keyInfo);
			}
			else if( ( filePath.indexOf("prossimi_appuntamenti") != -1 )  )	{
 				 keyInfo = "appuntamenti";
				 con.setProperty("tipoInfo", keyInfo);
				 cWrapper.addCategories(keyInfo);					
			}	
			List<ContentletWrapper>  links = addLinksToContenlet( con , false   );				
			cWrapper.setLinks( links );				
			cWrapper.setLinksRelationShipName( getLinkRelationShipName() );
			cWrapper.setContentlet(con);
			container.add(cWrapper);
		}
		return container;

	}


	@Override
	public  Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_INFO_NEWS") );
		return structure;
	}

	public String getLinkRelationShipName(){
		return ImportConfig.getProperty( "REL_NAME_INFO_NEWS-LINK");

	}


 
	private void setCorpoProperties(Contentlet contentlet, String langCode)
			throws DotStateException, DotDataException, DotSecurityException {
		String titoloPercorso = getHmiStructure().getPropertyHmi(HyperwaveKey.TitoloPercorso+":"+langCode  );
		if( UtilMethods.isSet( titoloPercorso ) ){
			contentlet.setProperty(  "titoloPercorso", titoloPercorso);
		}else {
			titoloPercorso = getHmiStructure().getPropertyHmi(HyperwaveKey.Title+":"+langCode ); 
			contentlet.setProperty(  "titoloPercorso", titoloPercorso);			 
		}
		String percorso = getHmiStructure().getPropertyHmi(HyperwaveKey.Percorso+":"+langCode  );
		if( UtilMethods.isSet( percorso ) ){
			contentlet.setProperty( "href",  percorso );
		}
		else { 
			percorso = getHmiStructure().getPropertyHmi(HyperwaveKey.Percorso  );
			if( UtilMethods.isSet( percorso ) ){
				contentlet.setProperty( "href",  percorso );
			}
		}	
	 	String val = contentlet.getStringProperty("corpoNotizia");
		
	  
	    if( UtilMethods.isSet(titoloPercorso ) ){
	    	  String nuovoValore =  "<ul>" + val ;
	    	  nuovoValore = nuovoValore + " <li><a class='altri' href='"+percorso+  "'>"+ titoloPercorso +"</a></li> </ul>";
	    	  contentlet.setStringProperty("corpoNotizia" , nuovoValore);    		
	    }
	    
 	}
 
}
