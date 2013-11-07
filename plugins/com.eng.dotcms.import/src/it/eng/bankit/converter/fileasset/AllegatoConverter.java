package it.eng.bankit.converter.fileasset;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.AbstractConverterImpl;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class AllegatoConverter extends AbstractConverterImpl {

	private boolean showOnMenu = false;
	private String langCode = null;

	public AllegatoConverter( HmiStructure struttura ){
		super(struttura);
	}

	public AllegatoConverter( HmiStructure struttura , boolean show ){
		super(struttura);
		showOnMenu = show;
	}

	public AllegatoConverter( HmiStructure struttura , boolean show , String langCode  ){
		super(struttura);
		showOnMenu = show;
		this.langCode = langCode;
	}

	@Override
	public ContentletContainer parseContent() throws Exception {
		ContentletContainer c = new ContentletContainer();
		ContentletWrapper cWrapper = null;
		if( UtilMethods.isSet( langCode ) ){
			cWrapper =  parseContentWrapper( langCode );	
		}else {
			String correctLang = getHmiStructure().getLanguageFile(); 		
			cWrapper = parseContentWrapper(correctLang);	
		}
		if( cWrapper != null ){
			c.add( cWrapper ) ;
		}
		return c;
	}


	public ContentletWrapper parseContentWrapper( String correctLang ) throws Exception {
		Contentlet contentletAllegato = creaAllegato( correctLang );
		Map<String, String> props = getHmiStructure().getPropertiesHmi();

		String fileName = FilenameUtils.getName( getHmiStructure().getFile().getName() );
		String hidden = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );
		ContentletWrapper cWrapper =  null;
		if( contentletAllegato != null ){

			cWrapper = new ContentletWrapper();
			Language lang = ImportUtil.getLanguage( contentletAllegato.getLanguageId() )  ; 
			Contentlet link = creaLinkAllegato( getHmiStructure() , fileName  , lang );	
			String aut = (String) contentletAllegato.get("autoreallegato");
			if( UtilMethods.isSet(aut ) ){
				link.setProperty("autore",aut );
			}else{
				String autore = props.get( HyperwaveKey.Autore );
				link.setProperty("autore",autore );
			}

			String dataEmanazione = getHmiStructure().getPropertyHmi( HyperwaveKey.Data_emanazione );

			if( UtilMethods.isSet(dataEmanazione ) ){
				try{
					Date timeM  = hwDateFormat.parse( dataEmanazione );
					link.setDateProperty( "dataEmanazione", timeM   );
				}catch (Exception e) {
					// TODO: handle exception
					Date timeM  = simpleHwDateFormat.parse( dataEmanazione );
					link.setDateProperty( "dataEmanazione", timeM   );
				}
//				try{
//					Date timeM  = simpleHwDateFormat.parse( dataEmanazione );
//					link.setDateProperty( "dataEmanazione", timeM   );
//				}catch (Exception e) {
//					// TODO: handle exception
//				}
			}

			link.setFolder( contentletAllegato.getFolder() );
			link.setHost( contentletAllegato.getHost() );
			link.setProperty("path", contentletAllegato.getFolder() );


			String filePath = getHmiStructure().getFilePath();
			if( filePath.indexOf("media/comsta")!= -1 ){
				cWrapper.addCategories("comunicatoStampa");
			}

			cWrapper.setLinkAllegato(link);
			cWrapper.setQuery( getLuceneQuery( contentletAllegato ));
			cWrapper.setContentlet(contentletAllegato);
			if(  !ImportUtil.isDefaultLang(lang) ) {
				Language defLang = ImportUtil.getDefaultLanguage();
				String titleDef = getHmiStructure().getPropertyHmi( HyperwaveKey.Title + ":" + defLang.getLanguageCode() ); 
				if( UtilMethods.isSet(titleDef) ){
					cWrapper.setTranslated(true);
				}				
			}

			if( hidden != null && hidden.equalsIgnoreCase("Hidden")){
				cWrapper.setArchived(true);
			}

		}
		return cWrapper; 
	}

	private Contentlet createDefaultData( String langCode  ){
		Contentlet allegato = null;
		HmiStructure hmiStr = getHmiStructure();
		String title = hmiStr.getPropertyHmi( HyperwaveKey.Title+":"+langCode );
		String sommario = hmiStr.getPropertyHmi( HyperwaveKey.Abstract+":"+langCode );
		String evento = hmiStr.getPropertyHmi( HyperwaveKey.Evento+":"+langCode );
		String luogo = hmiStr.getPropertyHmi( HyperwaveKey.Luogo+":"+langCode );
		String goid =  hmiStr.getPropertyHmi( HyperwaveKey.GOid );
		String autoreHw = hmiStr.getPropertyHmi( HyperwaveKey.Author );
		String description = hmiStr.getPropertyHmi( HyperwaveKey.Description );
		String organizzazione = hmiStr.getPropertyHmi( HyperwaveKey.Organizzazione+":"+langCode );
		String ruoloAutore = hmiStr.getPropertyHmi( HyperwaveKey.Ruolo_Autore+":"+langCode );
		if( title != null ){
			allegato = new Contentlet();
			allegato.setStructureInode( getDotStructure().getInode());
			allegato.setHost( ImportUtil.getHost().getIdentifier() );
			if( description != null ){
				allegato.setProperty("description", description );
			}
			Charset utf8=Charset.forName( "UTF-8" );
			byte[] bytesTitle=title.getBytes( utf8 );
			if(bytesTitle.length>255){
				allegato.setProperty("titoloLungo", title );
				byte[] reducedTitleBytes=ArrayUtils.subarray( bytesTitle, 0, 254 );
				String reducedTitle=new String(reducedTitleBytes,utf8);

				allegato.setProperty("title", reducedTitle );
			}else {
				allegato.setProperty("title", title );
			}
			if( sommario!= null ){
				allegato.setProperty("sommario", sommario );
			}
			if( !UtilMethods.isSet( description ) ){
				allegato.setProperty("description", description );
			}

			if( evento!= null ){
				allegato.setProperty("evento", evento );
			}
			if( luogo!= null ){
				allegato.setProperty("luogo", luogo );
			}
			allegato.setProperty("organizzazione", organizzazione );
			allegato.setProperty("ruoloAutore", ruoloAutore );

			Language  language= ImportUtil.getLanguage( langCode );
			allegato.setProperty("hwgoid", goid );
			allegato.setProperty("autoreHw", autoreHw  );
			allegato.setReviewInterval(null);
			allegato.setLanguageId( language.getId() );
			String timeCreated = hmiStr.getPropertyHmi(  HyperwaveKey.TimeCreated );
			String timeModified = hmiStr.getPropertyHmi(  HyperwaveKey.TimeModified );
			try{
				if( UtilMethods.isSet(timeCreated ) ){
					Date timeC  = hwDateFormat.parse(timeCreated);
					allegato.setDateProperty( "timeCreated", timeC );
				}
				if( UtilMethods.isSet(timeModified ) ){
					Date timeM  = hwDateFormat.parse(timeModified);
					allegato.setDateProperty( "timeModified", timeM );
				}
				String hidden = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );
				if( hidden == null || !hidden.equalsIgnoreCase("Hidden")){
					Date timeM  = hwDateFormat.parse( timeModified );
					allegato.setDateProperty( "dataPubblicazione", timeM );
				}
			}catch (Exception e) {
				LOG.error("ERROR parsing date ");
			}
		}
		return allegato;
	}

	protected Contentlet creaAllegato(String correctLang ) throws Exception {
		Map<String, String> props = getHmiStructure().getPropertiesHmi();
		Contentlet contentletAllegato = null;
		contentletAllegato = createDefaultData( correctLang );
		if( contentletAllegato != null  ){
			String autore = props.get( HyperwaveKey.Autore );
			String dataEmanazione = props.get( HyperwaveKey.Data_emanazione );
			String fileName = FilenameUtils.getName(  getHmiStructure().getFile().getName() );
			if( UtilMethods.isSet(dataEmanazione ) ){
				
				try{
					Date timeM  = hwDateFormat.parse( dataEmanazione );
					contentletAllegato.setDateProperty( "dataEmanazione", timeM   );			
				}catch (Exception e) {
					// TODO: handle exception
					Date timeM  = simpleHwDateFormat.parse( dataEmanazione );
					contentletAllegato.setDateProperty( "dataEmanazione", timeM   );
				}
				
			}

			String seq = props.get( HyperwaveKey.Sequence );
			if( seq != null   ){
				try{
					long seqLog = Long.parseLong(seq);
					contentletAllegato.setProperty("sortOrder", seqLog );
				}catch (Exception e) {		}
			}

			String alert= props.get( HyperwaveKey.Alert ) ;
			if ( alert != null ) {
				contentletAllegato.setProperty("alert", "True");
			}
			contentletAllegato.setProperty("autoreallegato", autore );
			StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );		
			String pathTocreate = ImportUtil.getRealtPath( getHmiStructure().getFilePath() );
			sb.append(pathTocreate  + System.getProperty("file.separator")  );	
			Folder folder = getDotcmsSysDataWriter().createFolderOnHost(  sb.toString()  , getHmiStructure());
			contentletAllegato.setFolder( folder.getInode()  );
			contentletAllegato.setHost( folder.getHostId());
			//contentletAllegato.setBinary( "fileAsset", getHmiStructure().getFile() );
			addFile(contentletAllegato, "fileAsset", getHmiStructure().getFile() );
			contentletAllegato.setProperty("fileName", fileName  );		
			contentletAllegato.setProperty("hostFolder", folder.getInode()  );		
		}

		return contentletAllegato ;
	}






	private Contentlet creaLinkAllegato( HmiStructure hmiStructure , String fileName , Language language ) {
		Contentlet con = null;
		LinkConverter lc = new LinkConverter( hmiStructure , showOnMenu );
		try {
			//			String lang = hmiStructure.getPropertyHmi(  HyperwaveKey.HW_Language );
			con = lc.createDefaultContentlet(  language.getLanguageCode() );
			con.setProperty("linkType", "A");			
			String importante = hmiStructure.getPropertyHmi(  HyperwaveKey.Importante );
			if( UtilMethods.isSet( importante ) && importante.equalsIgnoreCase("yes")){
				con.setProperty("importante", "true");		
			}
			String sommario = hmiStructure.getPropertyHmi(  HyperwaveKey.Abstract+":"+language.getLanguageCode() );
			if( UtilMethods.isSet(sommario)){
				String absHide = hmiStructure.getPropertyHmi(  HyperwaveKey.AbsHide );
				if( UtilMethods.isSet( absHide )  ){
					con.setProperty("mostraSommario", ImportConfigKey.FALSE_N_VALUE );		
				}else{
					con.setProperty("mostraSommario", ImportConfigKey.TRUE_S_VALUE );
				}
			}
 			System.out.println(  "CREATO LINK PER COLLEGAMENTO ALLEGATO :  " + fileName   + " Lingua " + language.getLanguage() );
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error( e.getMessage() );
		}
		return con;
	}



	@Override
	public String getLuceneQuery(Contentlet contentlet) {
		String valore  = super.getLuceneQuery(contentlet);
		valore = valore + "  +"+getDotStructure().getVelocityVarName()+".fileName:\"" + contentlet.getStringProperty("fileName")+"\"";
		valore = valore + "  +"+getDotStructure().getVelocityVarName()+".hostFolder:"+contentlet.getStringProperty("hostFolder") ;
		return valore ;
	}

	@Override
	public  Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("ALLEGATO_STRUCTURE") );
		return structure;
	}


	@Override
	protected String getLinkRelationShipName() {
		return null;
	}


}
