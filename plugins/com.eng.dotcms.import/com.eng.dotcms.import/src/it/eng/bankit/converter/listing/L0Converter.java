package it.eng.bankit.converter.listing;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public class L0Converter extends GenericDetailConverter {

	public L0Converter(HmiStructure struttura) {		
		super(struttura);
	}


	private List<HmiStructure> getListChildrenText(HmiStructure structure , long languageId) {
		List<HmiStructure> children = structure.getSubFile();
		Language langContentlet =   ImportUtil.getLanguage( languageId  );
		List<HmiStructure> listaFileCorpo = new ArrayList<HmiStructure>();
		for( HmiStructure child : children ){
			File fileCorrente = child.getFile();
			//String langFile = child.getPropertyHmi(  HyperwaveKey.HW_Language );
			String formato = child.getPropertyHmi(  HyperwaveKey.Formato );
//			Language fileLang =   ImportUtil.getLanguage( langFile );
			if( FileUtil.isTextFile(fileCorrente)  || 
					FileUtil.isAttachFile( fileCorrente)  ||   
					FileUtil.isImageFile(fileCorrente) ) {
				
				if( formato != null && ( formato.equalsIgnoreCase("descr") || formato.equalsIgnoreCase("testo") )  ) {
					String title = child.getPropertyHmi(  HyperwaveKey.Title +":"+ langContentlet.getLanguageCode());
					if( UtilMethods.isSet(title ) ){						 
						listaFileCorpo.add(child);						 
					}
				}
			} 
		}
		return listaFileCorpo ;
	}
	public ContentletContainer parseContent() throws  Exception{


		Map<String, String> props = getHmiStructure().getPropertiesHmi();
 		String folderName =  getHmiStructure().getFilePath();
		getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );

		ContentletContainer container = new ContentletContainer(); 

		List<Contentlet> appoLista =  createDefaultListContentlet() ;		
	

		for( Contentlet con  : appoLista){ 
			
			ContentletWrapper cWrapper = new ContentletWrapper();	
			List<HmiStructure> listaText = getListChildrenText(getHmiStructure(), con.getLanguageId() );
			addCorpo( con, listaText );
			cWrapper.setLinksRelationShipName( getLinkRelationShipName() );
			cWrapper.setQuery( getLuceneQuery(con));
			cWrapper.setContentlet(con);
			container.add(cWrapper);
		}
		addLinksToContenlet( container , false );
		return container;

	}


	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}


}
