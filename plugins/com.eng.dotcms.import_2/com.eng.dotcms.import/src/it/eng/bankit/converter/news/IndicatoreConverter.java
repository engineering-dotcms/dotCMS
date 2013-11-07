package it.eng.bankit.converter.news;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.converter.util.WYSYWGHelper;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.List;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class IndicatoreConverter extends GenericDetailConverter {

	public IndicatoreConverter(HmiStructure struttura) {
		super(struttura);
	}

	public  ContentletContainer  parseContent() throws  Exception{
		String formato = getHmiStructure().getPropertyHmi( HyperwaveKey.Formato );
		ContentletContainer container = null ; 
		if(formato != null && formato.startsWith("Ind") ) {
			container = createContainer();		 
		}
		return container;
	}

	private   String getHtmlLinkInterno(HmiStructure corpo, Language language ) {
		String langFile = language.getLanguageCode();
		String path = corpo.getPropertyHmi(  HyperwaveKey.Path );
		String title = corpo.getPropertyHmi(  HyperwaveKey.Title +":"+langFile );
		StringBuilder sBuilder = new StringBuilder("");
		sBuilder.append(  "    <a class='altri'  href='/"+path+"'>"  + title+
		"</a>");
		return sBuilder.toString();
	}

	protected Contentlet addCorpo( Contentlet con, HmiStructure structure  ) throws Exception {
		String testHtml = "";
		File fileCorrente = structure.getFile();
		Language language = ImportUtil.getLanguage( con.getLanguageId() );			

		if( fileCorrente != null ){
			if( FileUtil.isImageFile( fileCorrente ) ){
				testHtml = testHtml + WYSYWGHelper.getHtmlImage(structure, fileCorrente) ;
				System.out.println(  "IMMAGINE NEL TESTO  "  );
			}
			else if( FileUtil.isAttachFile( fileCorrente)  ){						 
				testHtml = testHtml   + WYSYWGHelper.getHtmlFromAttach( structure, language );
				System.out.println(  "isAttachFile NEL TESTO    "    );
			}
			else {
				testHtml = testHtml + WYSYWGHelper.getHtmlBody(structure , fileCorrente );			
			}
		}else {

			if( structure.getFilePath().endsWith(".hmi")){						
				testHtml = testHtml +getHtmlLinkInterno(structure, language);
			}
		}
		String val = con.getStringProperty("corpoNotizia");
		if( UtilMethods.isSet( val ) ){
			testHtml = val + testHtml;
		}

		con.setProperty("corpoNotizia", testHtml );

		return con;
	}


	protected ContentletContainer createContainer() throws Exception  {

		ContentletContainer container = new ContentletContainer(); 

		List<HmiStructure> children = getHmiStructure().getSubFile();
		List<HmiStructure> childrenLink = getHmiStructure().getChildrenLinks();

		for( HmiStructure child : children ){

			String lang = child.getPropertyHmi(HyperwaveKey.HW_Language );

			Contentlet contIt =   createDefaultContentlet(  child , lang    )  ;

			String hidden = child.getPropertyHmi(  HyperwaveKey.PresentationHints );
			String filePath = child.getFilePath();
			ContentletWrapper cWrapper = new ContentletWrapper();
			addCorpo(contIt, child );

			cWrapper.setQuery(  getLuceneQuery( contIt ));				

			cWrapper.setContentlet(contIt ); 	

			String keyInfo = "";
			if( ( filePath.indexOf("cambi") != -1 ) ){
				keyInfo ="cambi";						 
			}
			else if( ( filePath.indexOf("crescita") != -1 )  )	{
				keyInfo = "pil";						 				
			}else if( ( filePath.indexOf("coin") != -1 )  )	{
				keyInfo = "ecoin";					 				
			}else if( ( filePath.indexOf("inflazione") != -1 )  )	{
				keyInfo = "inflazione";					 				
			}else if( ( filePath.indexOf("tassi") != -1 )  )	{
				keyInfo = "tassi";	 
			}
			cWrapper.addCategories(keyInfo);
			if( hidden != null && hidden.equalsIgnoreCase("Hidden")){
				cWrapper.setArchived(true);
			}
			contIt.setProperty("tipologia", keyInfo);

			if( container.get(contIt.getLanguageId() )!= null  ){
				ContentletWrapper cWrapperOld = 	 container.get(contIt.getLanguageId() );
				cWrapperOld.addListingLink(cWrapper);

			}else {
				container.add(cWrapper);
			}

		}




		for( HmiStructure child : childrenLink ){

			String lang = child.getPropertyHmi(HyperwaveKey.HW_Language );

			Contentlet contIt =   createDefaultContentlet(  child , lang    )  ;

			String hidden = child.getPropertyHmi(  HyperwaveKey.PresentationHints );
			ContentletWrapper cWrapper = new ContentletWrapper();
			addCorpo(contIt, child );

			cWrapper.setQuery(  getLuceneQuery( contIt ));				

			cWrapper.setContentlet(contIt ); 	

			String keyInfo = "stampa";
			cWrapper.addCategories(keyInfo);
			if( hidden != null && hidden.equalsIgnoreCase("Hidden")){
				cWrapper.setArchived(true);
			}
			contIt.setProperty("tipologia", keyInfo);

			//container.add(cWrapper);
			if( container.get(contIt.getLanguageId() )!= null  ){
				ContentletWrapper cWrapperOld = 	 container.get(contIt.getLanguageId() );
				cWrapperOld.addListingLink(cWrapper);

			}else {
				container.add(cWrapper);
			}

		}
		return container; 

	}


	@Override
	public  Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_INDICATORE") );
		return structure;
	}


	public String getLinkRelationShipName(){
		return "";
	}

	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
