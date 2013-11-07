package it.eng.bankit.converter.detail;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.List;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;

public class SimpleContentConverter extends GenericDetailConverter {


	private boolean createPage = false;

	public SimpleContentConverter(HmiStructure struttura) {
		super(struttura);
	}

	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
	throws Exception {
		ContentletWrapper cWrapper = null;
		Contentlet con = createDefaultContentlet( correctLang );
		if( con != null ) {
 			if( createPage ){
				StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );				
				String pName = getHmiStructure().getFilePath();
				File file= getHmiStructure().getFile();
				String fName = file.getName();
				String nM = fName.substring( 0, fName.lastIndexOf(".") );
				pName =	pName.substring(0, pName.lastIndexOf(fName));
				sb.append( pName );
				if( !pName.endsWith(System.getProperty("file.separator"))){
					sb.append(pName + System.getProperty("file.separator")  );	
				}
				Folder path = it.eng.bankit.util.FolderUtil.findFolder( sb.toString() );
				getDotcmsSysDataWriter().createPageOnFolder( path, getHmiStructure() , nM  );
				con.setProperty("pageName" ,nM  );	
				
				
			}
			String hidden = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );
			cWrapper = new ContentletWrapper();
			addCorpo( con, getHmiStructure() );			
			List<ContentletWrapper> links = addLinksToContenlet( con , false );				
			cWrapper.setLinks(links);
			cWrapper.setQuery( getLuceneQuery( con ));				
			cWrapper.setLinksRelationShipName( getLinkRelationShipName() );			
			cWrapper.setContentlet(con);
			if( hidden != null && hidden.equalsIgnoreCase("Hidden")){
				cWrapper.setArchived(true);
			}
		}
		return cWrapper;
	}
	
	@Override
	protected   void setSpecificProperties(Contentlet contentlet , String langCode )  throws Exception {
		if( hasProperty( "pageName" ) ){
			contentlet.setProperty("pageName" , ImportConfigKey.FALSE_N_VALUE );	
		}
		super.setSpecificProperties(contentlet, langCode);
		
	}
	


	@Override
	public ContentletContainer parseContent() throws Exception {
		ContentletContainer container = new ContentletContainer(); 
		List<Language> langs =  ImportUtil.getDotLanguages();
		for( Language lang  : langs){ 
			ContentletWrapper cWrapper =  parseContentWrapper( lang.getLanguageCode() );
			if( cWrapper != null ){
				container.add(cWrapper);
			}
		}
		return container;
	}

	public boolean isCreatePage() {
		return createPage;
	}

	public void setCreatePage(boolean createPage) {
		this.createPage = createPage;
	}
	@Override
	public  Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_SIMPLE_CONTENT") );
		return structure;
	}


}
