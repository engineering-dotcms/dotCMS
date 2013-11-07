package it.eng.bankit.writer;

import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.FolderUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;
import it.eng.bankit.util.TemplateManager;

import java.util.Date;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;

public class DotcmsSysDataWriter {


	private Logger LOG = Logger.getLogger( this.getClass().getName() );

	public Folder createFolderOnHost( String folderName , HmiStructure struct , boolean createPage ) throws Exception{
		Folder 	fold  = null;
		try{
			String folString = ImportUtil.getRealtPath( folderName );
			fold = FolderUtil.findFolder( folString );
			if( fold != null ){
				return fold;
			}
			StringTokenizer st = new StringTokenizer(folderName,System.getProperty("file.separator") );
			StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );
			Folder parent = null;
			while (st.hasMoreTokens()) {
				String name = st.nextToken(); 	
				name = ImportUtil.getAlias( name );
				sb.append( name + System.getProperty("file.separator")  );
				Folder folder  =  APILocator.getFolderAPI().findFolderByPath( sb.toString(), ImportUtil.getHost(), ImportUtil.getUser(), true);					
				if (folder == null || !InodeUtils.isSet(folder.getInode())) {
					addI18NVariable( folString , struct );
					folder= createFolderObject( struct , name , parent );
					LOG.debug("Creo il folder "+ name + " (path di partenza :  " + folderName +" ) ");
					APILocator.getFolderAPI().save( folder,  ImportUtil.getUser() ,  true);
					fold = folder;
					if( createPage ) {
						createPageOnFolder( folder  , struct  );
					}
				}else {
					//	LOG.info("La cartella "+ name + " esiste sull'host  ");
					addI18NVariable( folderName , struct );	
					if( createPage ) {
						createPageOnFolder( folder  , struct   );
					}
				}
				parent = folder;
			}
		}catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage() , e );
			throw new  Exception();
		}
		return fold;
	}

	public Folder createFolderOnHost( String folderName , HmiStructure struct ) throws Exception{
		Folder 	fold  = null;
		try{
			String folString = ImportUtil.getRealtPath( folderName );
			fold = FolderUtil.findFolder( folString );
			if( fold != null ){
				return fold;
			}
			StringTokenizer st = new StringTokenizer(folderName,System.getProperty("file.separator") );
			StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );
			Folder parent = null;

			while (st.hasMoreTokens()) {
				String name = st.nextToken(); 	
				name = ImportUtil.getAlias( name );
				sb.append(name + System.getProperty("file.separator")  );
				Folder folder  =  APILocator.getFolderAPI().findFolderByPath( sb.toString(), ImportUtil.getHost(), ImportUtil.getUser(), true);					
				if (folder == null || !InodeUtils.isSet(folder.getInode())) {
					addI18NVariable( folderName , struct );	
					folder = createFolderObject( struct , name , parent );
					LOG.info("Creo il folder "+ name + " (path di partenza :  " + folderName +" ) ");
					APILocator.getFolderAPI().save( folder, ImportUtil.getUser(), true);
					fold = folder;
					createPageOnFolder( folder  , struct );
				}else {	
					addI18NVariable( folderName , struct );	

					createPageOnFolder( folder , struct  );
				}
				parent = folder;
			}
		}catch (Exception e) {
			e.printStackTrace();
			LOG.error(e.getMessage() , e );
			throw new  Exception();
		}
		return fold;	 
	}


	private Folder createFolderObject( HmiStructure struct , String name , Folder parent ) throws Exception {
		String title = name ;
		if( struct.isBoxMenu() ){
			String menu = struct.getBoxMenu();
			if( menu!= null )  {
				name = name+"_box";
			}					
		}
		return FolderUtil.createFolderObject( name, title, parent, null, getSortOrder(struct), isShowOnMenu(struct) ) ;
	}

	public void addI18NVariable(String key, HmiStructure struct ){

		String titleIt = struct.getPropertyHmi( HyperwaveKey.Title+":"+ImportConfigKey.ITALIAN_LANGUAGE );
		String titleEn = struct.getPropertyHmi( HyperwaveKey.Title +":"+ImportConfigKey.ENGLISH_LANGUAGE );

		Language langIT = ImportUtil.getLanguage( ImportConfigKey.ITALIAN_LANGUAGE );
		Language langEN = ImportUtil.getLanguage( ImportConfigKey.ENGLISH_LANGUAGE );

		String keyReolaced = key.replaceAll(System.getProperty("file.separator") , ".").replaceAll(" " , "-");
		if( keyReolaced.startsWith(".")){
			keyReolaced = keyReolaced.substring(1);
		}
		if( keyReolaced.endsWith( ".")){
			keyReolaced = keyReolaced.substring(0 , keyReolaced.lastIndexOf("."));
		}
		if( UtilMethods.isSet(titleIt) ){
			FolderUtil.addLanguageVariable(langIT , keyReolaced, titleIt );
		}
		if( UtilMethods.isSet(titleEn) ){
			FolderUtil.addLanguageVariable(langEN , keyReolaced, titleEn );		
		}
		String akIt = struct.getPropertyHmi( HyperwaveKey.accessKey+":"+ImportConfigKey.ITALIAN_LANGUAGE );
		String akEn = struct.getPropertyHmi( HyperwaveKey.accessKey+":"+ImportConfigKey.ENGLISH_LANGUAGE );
		if( UtilMethods.isSet( akIt )){
			FolderUtil.addLanguageVariable( langIT , keyReolaced +ImportConfigKey.ACCESS_KEY_PREFIX  ,  akIt  );
		}
		if( UtilMethods.isSet( akEn )){
			FolderUtil.addLanguageVariable(langEN ,keyReolaced + ImportConfigKey.ACCESS_KEY_PREFIX , akEn );			
		} 		
	}



	private boolean excludePageCreation( HmiStructure struct ){
		String formato = struct.getPropertyHmi(HyperwaveKey.Formato );
		String hidden = struct.getPropertyHmi(HyperwaveKey.PresentationHints );
		if( formato != null && formato.equalsIgnoreCase("PI") ){
			return true;
		}
		if( UtilMethods.isSet(hidden ) && hidden.equalsIgnoreCase("Hidden") ){
			return false;
		}
		return false ;
	}

	public HTMLPage createPageOnFolder( Folder  fold , HmiStructure struct ) {			
		HTMLPage htmlPage =  null;
		if( excludePageCreation( struct ) ) {
			return htmlPage;
		}
		try{
			Template templatePage = TemplateManager.getInstance().findTemplate( struct );
			if( templatePage != null ){
				htmlPage = createPage( fold, ImportConfig.getProperty( ImportConfigKey.DEFAULT_PAGE_NAME ), fold.getName() );
				htmlPage.setPageUrl( ImportConfig.getProperty( ImportConfigKey.DEFAULT_PAGE_NAME ) +  "."+com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION") );
				HTMLPage existingHTMLPage = APILocator.getHTMLPageAPI( ).getWorkingHTMLPageByPageURL(htmlPage.getPageUrl(), fold);
				boolean pageExists = (existingHTMLPage != null) && InodeUtils.isSet(existingHTMLPage.getInode());
				if( !pageExists ){
					APILocator.getHTMLPageAPI().
					saveHTMLPage( htmlPage , templatePage, fold, ImportUtil.getUser(), true);
				}
			}
		}catch (Exception e) {
			LOG.error(e.getMessage() , e );
			e.printStackTrace();
		}
		return  htmlPage;
	}

	public HTMLPage createPageOnFolder( Folder  fold , HmiStructure struct , String pageName) {			
		HTMLPage htmlPage =  null;
		if( excludePageCreation( struct ) ) {
			return htmlPage;
		}
		try{
			Template templatePage = TemplateManager.getInstance().findTemplate( struct );
			if( templatePage != null ){
				htmlPage = createPage(fold, pageName, fold.getName() );
				HTMLPage existingHTMLPage = APILocator.getHTMLPageAPI( ).getWorkingHTMLPageByPageURL(htmlPage.getPageUrl(), fold);
				boolean pageExists = (existingHTMLPage != null) && InodeUtils.isSet(existingHTMLPage.getInode());
				if( !pageExists ){
					htmlPage = APILocator.getHTMLPageAPI( ).
					saveHTMLPage( htmlPage , templatePage, fold, ImportUtil.getUser(), true);
				}
			}
		}catch (Exception e) {
			LOG.error(e.getMessage() , e );
			e.printStackTrace();
		}
		return  htmlPage;
	}

	private HTMLPage createPage( Folder fold , String pageName , String friendlyName ){
		HTMLPage	htmlPage = new HTMLPage();
		Date data = new Date();
		htmlPage.setParent( fold.getInode() );
		htmlPage.setFriendlyName( pageName  );
		htmlPage.setHttpsRequired( false );
		htmlPage.setIDate( data );
		htmlPage.setMetadata( "" );
		htmlPage.setModDate( data );
		htmlPage.setModUser( ImportUtil.getUser().getUserId() );
		htmlPage.setOwner( ImportUtil.getUser().getUserId() );
		htmlPage.setPageUrl( pageName +  "."+com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION") );
		htmlPage.setRedirect( "" );
		htmlPage.setShowOnMenu( false );
		htmlPage.setSortOrder( 1 );
		htmlPage.setStartDate( data );
		htmlPage.setTitle( ImportConfig.getProperty( ImportConfigKey.DEFAULT_PAGE_NAME )  );
		htmlPage.setType( "htmlpage" );
		return htmlPage ;
	}

	private int getSortOrder( HmiStructure structure ) {
		String seq = structure.getPropertyHmi( HyperwaveKey.Sequence );
		if( seq != null ){
			return Integer.parseInt( seq );
		}
		return 0;
	}

	private boolean isShowOnMenu( HmiStructure structure ){
		return structure.isShowOnMenu();

	}


	public Link createMenuLink( Link menuLink , Folder folderDest , HmiStructure structure ){

		try {
			Language l = ImportUtil.getLanguage(ImportConfigKey.ITALIAN_LANGUAGE );
			Language lEn = ImportUtil.getLanguage(ImportConfigKey.ENGLISH_LANGUAGE );
			String key = folderDest.getName();
			key = key + "."+ menuLink.getTitle();
			//Italiano
			String value = structure.getPropertyHmi( HyperwaveKey.Title +":" + ImportConfigKey.ITALIAN_LANGUAGE   );
			FolderUtil.addLanguageVariable( l , key ,  value );	
			//English 
			value = structure.getPropertyHmi( HyperwaveKey.Title +":" + ImportConfigKey.ENGLISH_LANGUAGE   );
			FolderUtil.addLanguageVariable( lEn , key ,  value );
			WebAssetFactory.createAsset(menuLink, ImportUtil.getUser().getUserId() , folderDest);

			// APILocator.getMenuLinkAPI().save(menuLink, folderDest, ImportUtil.getUser(), true);
		} catch (Exception e) {	e.printStackTrace() ;	}
		return menuLink;
	}
}
