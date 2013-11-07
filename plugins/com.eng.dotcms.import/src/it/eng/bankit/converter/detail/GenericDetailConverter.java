package it.eng.bankit.converter.detail;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.AbstractConverterImpl;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.converter.fileasset.AllegatoConverter;
import it.eng.bankit.converter.fileasset.ImageConverter;
import it.eng.bankit.converter.fileasset.VideoLinkConverter;
import it.eng.bankit.converter.util.WYSYWGHelper;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.FolderUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public abstract class GenericDetailConverter extends AbstractConverterImpl implements FolderConverter  {

	protected String sommario 		= "sommario";
	protected String path 			= "path";
	protected String alert 			= "alert";
	protected String sortOrder 		= "sortOrder1";
	protected String orderType 		= "orderType";
	protected String corpoNotizia 	= "corpoNotizia";
	protected String mostraSommario = "mostraSommario";

	public abstract ContentletContainer parseContent() throws Exception;

	public GenericDetailConverter( HmiStructure struttura ){
		super(struttura);		
	}

	@Override
	public  Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_DETTAGLIO") );
		return structure;
	}

	public abstract ContentletWrapper parseContentWrapper( String correctLang ) throws Exception  ;

	private void setContentletProperty(Contentlet contentlet , String propertyName , String hmiPropertyName , String propertyValue , String langCode  ) {
		if( hasProperty( propertyName ) ){
			String hmiValue = getHmiStructure().getPropertyHmi( hmiPropertyName );
			String valueTOset = hmiValue;
			if( UtilMethods.isSet( hmiValue )  ){
				if( UtilMethods.isSet( propertyValue )   )
					contentlet.setProperty(propertyName, propertyValue );	
				else {
					contentlet.setProperty(propertyName, valueTOset );	
				}
			} 
		}
	}

	protected void setSpecificProperties(Contentlet contentlet , String langCode  ) throws Exception{
		if( hasProperty( mostraSommario ) ){
			String absHide = getHmiStructure().getPropertyHmi( HyperwaveKey.AbsHide );
			if( UtilMethods.isSet( absHide )  ){
				contentlet.setProperty(mostraSommario, ImportConfigKey.FALSE_N_VALUE );	
			}else{
				contentlet.setProperty(mostraSommario, ImportConfigKey.TRUE_S_VALUE );
			}
		}
		setContentletProperty( contentlet, alert, HyperwaveKey.Alert,  "True", langCode );
		String sommario = getHmiStructure().getPropertyHmi( HyperwaveKey.Abstract+":"+langCode );
		
		if( hasProperty( "sommario" ) ){
			contentlet.setProperty("sommario", sommario );
		}

		if( hasProperty( path ) ){
			String valore = ImportUtil.getRealtPath(getHmiStructure().getFilePath() );
			StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );				

			sb.append( valore + System.getProperty("file.separator")  );				 
			Folder pathFolder = FolderUtil.findFolder( sb.toString() );
			if( pathFolder  == null ){
				try{
					pathFolder = getDotcmsSysDataWriter().createFolderOnHost(sb.toString() , getHmiStructure() );
					contentlet.setProperty(path , pathFolder.getInode() );
					contentlet.setFolder( pathFolder.getInode()  );				 
				}catch (Exception e) {
					LOG.error("Errore " + e.getMessage() );
				}
			}else {
				contentlet.setProperty(path , pathFolder.getInode() );
				contentlet.setFolder( pathFolder.getInode()  );		
			}
		}

		if( hasProperty( sortOrder ) ){
			String seq = getHmiStructure().getPropertyHmi( HyperwaveKey.Sequence );
			if( seq != null   ){
				try{
					long seqLog = Long.parseLong(seq);
					contentlet.setLongProperty(sortOrder , seqLog );
				}catch (Exception e) {}
			}
		}
		if( hasProperty( orderType  ) ){
			String sortOrder = getHmiStructure().getPropertyHmi( HyperwaveKey.SortOrder );
			if( UtilMethods.isSet( sortOrder )){
				contentlet.setProperty("orderType", sortOrder );
			}
		}
		if( hasProperty( "evento" ) ){
			String evento = getHmiStructure().getPropertyHmi( HyperwaveKey.Evento+":"+langCode );
			if( evento!= null ){
				contentlet.setProperty("evento", evento );
			}
		}
		if( hasProperty( "luogo" ) ){
			String luogo = getHmiStructure().getPropertyHmi( HyperwaveKey.Luogo+":"+langCode );
			if( luogo!= null ){
				contentlet.setProperty("luogo", luogo );
			}
		}
		if( hasProperty( "organizzazione" ) ){
			String organizzazione = getHmiStructure().getPropertyHmi( HyperwaveKey.Organizzazione+":"+langCode );
			contentlet.setProperty("organizzazione", organizzazione );
		}
		String ruoloAutore = getHmiStructure().getPropertyHmi( HyperwaveKey.Ruolo_Autore+":"+langCode );
		String autore = getHmiStructure().getPropertyHmi( HyperwaveKey.Autore  );


		if( hasProperty( "dataEmanazione" ) ){
			String dataEmanazione = getHmiStructure().getPropertyHmi( HyperwaveKey.Data_emanazione );
			if( UtilMethods.isSet( dataEmanazione ) ){
				try{
					Date timeM  = simpleHwDateFormat.parse( dataEmanazione );
					contentlet.setDateProperty( "dataEmanazione", timeM   );
				}catch (Exception e) {}
			}
		}
		if( hasProperty( "autoreallegato" ) ){
			contentlet.setProperty("autoreallegato", autore );
		}
		if( hasProperty( "ruoloAutore" ) ){
			contentlet.setProperty("ruoloAutore", ruoloAutore );
		}
	}

	protected Contentlet addCorpo( Contentlet con,  List<HmiStructure> listaFileCorpo   ) throws Exception {
		if( listaFileCorpo.size() > 0 ){
			return createContentletCorpo(con, listaFileCorpo, getHmiStructure() );
		}
		return con;
	}

	private Contentlet createContentletCorpo(Contentlet con,   List<HmiStructure> listaFileCorpo  , HmiStructure structure ){

		Comparator<HmiStructure> st =  structure.getSortComparator( con );
		Collections.sort( listaFileCorpo, st );
		StringBuffer testHtml  = new StringBuffer("");		 
		boolean addHmi = addValueCorpo( listaFileCorpo );
		for( HmiStructure corpo : listaFileCorpo ){
			Language language = ImportUtil.getLanguage( con.getLanguageId() );		
			String wysi =   WYSYWGHelper.getHTML(corpo, language, addHmi);
			testHtml.append( wysi )  ;
		}
		con.setProperty("corpoNotizia", testHtml.toString() );
		return con;
	}

	protected Contentlet addCorpo( Contentlet con ) throws Exception {
		return addCorpo( con , getHmiStructure() );
	}

	protected Contentlet addCorpo( Contentlet con, HmiStructure structure  ) throws Exception {
		List<HmiStructure> listaFileCorpo = getAllFullCollectionHead( structure , con.getLanguageId() );
		if( listaFileCorpo.size() > 0 ){
			Comparator<HmiStructure> st =  structure.getSortComparator( con );
			Collections.sort( listaFileCorpo, st );
			return createContentletCorpo(con, listaFileCorpo, structure);
		}
		return con;
	}


	//recupero solo gli HMI che hanno presentationHints=CollectioHead o FullCollectionhead (possono essere sia file che link)
	protected List<HmiStructure> getAllFullCollectionHead(HmiStructure structure , long languageId) {
		List<HmiStructure> children = structure.getSubFile();
		List<HmiStructure> links = structure.getChildrenLinks();
		List<HmiStructure> aliasLinks = structure.getAliasLinks();
		List<HmiStructure> listaFileCorpo = new ArrayList<HmiStructure>();
		Language lg = ImportUtil.getLanguage(languageId );
		for( HmiStructure child : children ){
			File fileCorrente = child.getFile();
			String title = child.getPropertyHmi( HyperwaveKey.Title + ":" + lg.getLanguageCode() );
			if( UtilMethods.isSet( title )){
				if( FileUtil.isTextFile(fileCorrente)  || 
						FileUtil.isAttachFile( fileCorrente)  ) {
					if( child.isCollectionHead() || child.isFullCollectionHead() ) {			 			 
						listaFileCorpo.add(child);
					}
				} else if (  
						FileUtil.isImageFile(fileCorrente) && ( child.isCollectionHead() || child.isFullCollectionHead() )){
					String titoloImg = child.getPropertyHmi( HyperwaveKey.Title +":"+ lg.getLanguageCode() );
					if( UtilMethods.isSet( titoloImg) ){
						listaFileCorpo.add(child);
					}
				}
			}
		}
		for( HmiStructure singleHmi : links ){
			String langFile = singleHmi.getPropertyHmi(  HyperwaveKey.HW_Language );			
			Language fileLang =   ImportUtil.getLanguage( langFile );
			if( singleHmi.isFullCollectionHead() || singleHmi.isCollectionHead()  ) {
				if( langFile!= null ){
					if( fileLang.getId() ==  languageId ){
						listaFileCorpo.add( singleHmi );
					}
				}
			}
		}
		for( HmiStructure singleHmi : aliasLinks ){
			String langFile = singleHmi.getPropertyHmi(  HyperwaveKey.HW_Language );			
			Language fileLang =   ImportUtil.getLanguage( langFile );
			if( singleHmi.isFullCollectionHead() || singleHmi.isCollectionHead()  ) {
				if( langFile!= null ){
					if( fileLang.getId() == languageId ){
						listaFileCorpo.add( singleHmi );
					}
				}
			}
		}
		return listaFileCorpo ;
	}


	protected FolderConverter getAllegatoConverter(HmiStructure child , boolean showOnMenu ,  String langCode ){
		String documentType = child.getPropertyHmi(  HyperwaveKey.DocumentType );
		if( UtilMethods.isSet( documentType ) && documentType.equalsIgnoreCase("Movie") ){
			return new VideoLinkConverter( child );
		}else{
			return new AllegatoConverter( child , false , langCode  );								 
		}
	}
	
	
	//recupero solo gli HMI che sono allegati (pdf. etc.. ) e che non hanno presentationHints=CollectioHead o FullCollectionhead 
	// i presentationHints=CollectioHead o FullCollectionhead  sono inseriti nel corpo 
	protected List<ContentletContainer> creaListaAllegati( Contentlet con, List<HmiStructure> children ) throws Exception {
		ContentletContainer allegato = null;
		List<ContentletContainer> listaAllegati = new ArrayList<ContentletContainer>();
		for( HmiStructure child : children ){
			File fileCorrente = child.getFile();
			String langFile = child.getPropertyHmi( HyperwaveKey.HW_Language );
 			if( langFile!= null ){
				Language language = ImportUtil.getLanguage( con.getLanguageId() );
				String title = child.getPropertyHmi( HyperwaveKey.Title + ":" + language.getLanguageCode() );
				if( UtilMethods.isSet( title )){
					if( FileUtil.isAttachFile(fileCorrente) && !child.isCollectionHead()   ) {
						LOG.info("LINGUA (" + language.getLanguage() +") creo allegato per la contentlet " + con.getStringProperty("titolo")  );
 						FolderConverter ac = getAllegatoConverter(child , false , language.getLanguageCode()  );								 
						allegato =  ac.parseContent();
						if( allegato != null ){
 							listaAllegati.add( allegato );
						}
					} 
				}	 
			}
		}
		return listaAllegati;
	}


	//server per aggiungere le immagini/allegati presenti nel corpo del testo 
	protected List<ContentletWrapper> addImmaginiAllegatiCorpo( Contentlet con, List<HmiStructure> children ) throws Exception {
		List<ContentletWrapper> listaAll = new ArrayList<ContentletWrapper>();
		for( HmiStructure child : children ){
			File fileCorrente = child.getFile();
			Language language =   ImportUtil.getLanguage( con.getLanguageId() );
			String titoloImg = child.getPropertyHmi(  HyperwaveKey.Title +":"+ language.getLanguageCode() );
			if( FileUtil.isImageFile( fileCorrente) && child.isCollectionHead() ){
				if( UtilMethods.isSet( titoloImg ) ) {
					ImageConverter imag = new ImageConverter(child);				 				
					ContentletWrapper wrapp =  imag.getContentlet( language.getLanguageCode() );
					if( wrapp != null ){
						listaAll.add( wrapp );
					}
				}
			} else if( FileUtil.isAttachFile(fileCorrente) && child.isCollectionHead() ) {
				if( UtilMethods.isSet( titoloImg ) ) {
					AllegatoConverter ac = new AllegatoConverter( child , false );	
					ContentletWrapper wrapp = ac.parseContentWrapper( language.getLanguageCode() );
					if( wrapp != null ){
						LOG.info("LINGUA (" + language.getLanguage() +") Aggiungo allegato che ha collectionHead  " + con.getStringProperty("titolo")  );
 						listaAll.add( wrapp );
 					}
				}
			} 
 		}
		return listaAll;
	}

	protected void addImmagine( Contentlet con, List<HmiStructure> children ) throws Exception {
		boolean found = false;
		Iterator<HmiStructure> it =	children.iterator();
		while( it.hasNext() && !found ){
			HmiStructure child = it.next();
			File fileCorrente = child.getFile();
			if( FileUtil.isImageFile( fileCorrente) &&  !child.isCollectionHead()  ) {
				String langFile = child.getPropertyHmi( HyperwaveKey.HW_Language );
				if( langFile!= null ){
					Language language = ImportUtil.getLanguage( langFile );
					if(language.getId() == con.getLanguageId()  ){	
						addFile(con, "immagine", fileCorrente);
						found = true;						 
					}
				}
			}
		}
	}
	
	

	public    String getLinkRelationShipName(){
		return ImportConfig.getProperty( "REL_NAME_DETTAGLIO-LINK");
	}


}
