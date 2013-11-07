package it.eng.bankit.converter.news;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.util.WYSYWGHelper;
import it.eng.bankit.filereader.HWHtmlReader;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class NotiziaSecConverter extends StrilloConverter {

	public NotiziaSecConverter(HmiStructure struttura) {
		super(struttura);
	}

	public  ContentletContainer  parseContent() throws  Exception{

		String formato = getHmiStructure().getPropertyHmi( HyperwaveKey.Formato );
		ContentletContainer container = new ContentletContainer(); 
		if(formato != null && 	formato.equalsIgnoreCase("NS") ) {
			container = createContainer();	
		}
		return container;
	}

	protected List<HmiStructure> getAllFullCollectionHead(HmiStructure structure , long languageId) {
		List<HmiStructure> children = structure.getSubFile();
		List<HmiStructure> listaFileCorpo = new ArrayList<HmiStructure>();
		for( HmiStructure child : children ){
			File fileCorrente = child.getFile();
			String langFile = child.getPropertyHmi(  HyperwaveKey.HW_Language );
			String formato = child.getPropertyHmi(  HyperwaveKey.Formato );
			Language fileLang =   ImportUtil.getLanguage( langFile );
			if( FileUtil.isTextFile(fileCorrente)  || 
					FileUtil.isAttachFile( fileCorrente)  ||   
					FileUtil.isImageFile(fileCorrente) ) {
				if( formato != null && formato.equalsIgnoreCase("descr")) {
					if( langFile!= null ){
						if( fileLang.getId() ==  languageId  ){
							listaFileCorpo.add(child);
						}
					}
				}
			} 
		}
		return listaFileCorpo ;
	}


	protected Contentlet addCorpo( Contentlet con, HmiStructure structure  ) throws Exception {
		List<HmiStructure> listaFileCorpo = getAllFullCollectionHead( structure , con.getLanguageId() );
		if( listaFileCorpo.size() > 0 ){
			Comparator<HmiStructure> st =  structure.getSortComparator( con );
			Collections.sort( listaFileCorpo, st );
			String testHtml  = "";
			for( HmiStructure corpo : listaFileCorpo ){
				Language language = ImportUtil.getLanguage( con.getLanguageId() );				
				File fileCorrente = corpo.getFile();
				if( fileCorrente != null ){
					if( FileUtil.isImageFile( fileCorrente ) ){
						testHtml = testHtml + WYSYWGHelper.getHtmlImage(corpo, fileCorrente) ;
					}
					else if( FileUtil.isAttachFile( fileCorrente)  ){						 
						testHtml = testHtml   + WYSYWGHelper.getHtmlFromAttach( corpo, language);
					}
					else {
						testHtml = testHtml + HWHtmlReader.getContent( fileCorrente );
					}
				}else {
					if( corpo.getFilePath().endsWith(".hmi")){						
						testHtml = testHtml + WYSYWGHelper.getHtmlFromHmiLink(corpo, language);
					}
				}
				String val = con.getStringProperty("corpoNotizia");
				if( UtilMethods.isSet( val ) ){
					testHtml = val + testHtml;
				}
			}
			con.setProperty("corpoNotizia", testHtml );
		}
		return con;
	}




	protected void addImmagine(  Contentlet con   , List<HmiStructure> children  ) throws Exception {
		//		List<ContentletWrapper> immagini = new ArrayList<ContentletWrapper>();
		boolean found = false;
		Iterator<HmiStructure> it =	children.iterator();
		while( it.hasNext() && !found ){
			HmiStructure child = it.next();
			File fileCorrente = child.getFile();
			String presentationHints = child.getPropertyHmi(  HyperwaveKey.PresentationHints );
			if( presentationHints == null || presentationHints!= null && !presentationHints.equalsIgnoreCase("Hidden") ) {
				if( FileUtil.isImageFile( fileCorrente) &&  !child.isCollectionHead()  ) {
					String langFile = child.getPropertyHmi(  HyperwaveKey.HW_Language );
					if( isToIgnore( child ) ){
						addFile(con, "immagine", fileCorrente );
						//						con.setBinary("immagine", fileCorrente);
						found = true;
					}else {
						if( langFile!= null ){
							Language language =   ImportUtil.getLanguage( langFile  );
							if(language.getId() ==  con.getLanguageId()  ){	
								addFile(con, "immagine", fileCorrente );
								//								
								found = true;
							}
						}
					}
				}
			}
		}

	}


	private boolean isToIgnore( HmiStructure child ){
		String titleIt = child.getPropertyHmi( HyperwaveKey.Title+":"+ ImportConfigKey.ENGLISH_LANGUAGE  );
		String titleEn = child.getPropertyHmi( HyperwaveKey.Title+":"+ImportConfigKey.ITALIAN_LANGUAGE  );
		if( titleIt != null   && titleEn != null)
		{
			return true;
		}return false;
	}

	public Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_NS") );
		return structure;
	}

	public String getLinkRelationShipName(){
		return ImportConfig.getProperty( "REL_NAME_NS-LINK");
	}

}
