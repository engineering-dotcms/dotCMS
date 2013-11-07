package it.eng.bankit.converter.news;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.converter.listing.L0Converter;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.util.List;

import com.dotmarketing.portlets.languagesmanager.model.Language;

public class PInternaConverter extends GenericDetailConverter{

	public PInternaConverter(HmiStructure struttura) {		
		super(struttura);
	}
	protected ContentletContainer createContainer() throws Exception  {

		Language it = ImportUtil.getLanguage( ImportConfigKey.ITALIAN_LANGUAGE  );
		Language en = ImportUtil.getLanguage( ImportConfigKey.ENGLISH_LANGUAGE );

		ContentletContainer container = new ContentletContainer(); 

		List<HmiStructure> children = getHmiStructure().getSubDirectories();
 
		for( HmiStructure child : children ){


			L0Converter lc = new L0Converter( child );

			ContentletContainer cLet =  lc.parseContent();
			if( cLet != null && ! cLet.isEmpty()  ){

				if( container.getAll() != null && container.getAll().size() > 0  ){

					if( container.get(  en.getId() )!= null  ){
						ContentletWrapper cWrapperOld = 	 container.get(   en.getId()   );
						cWrapperOld.addListingLink(cLet.get( en.getId() ));
					} 
					if( container.get(  it.getId() )!= null  ){
						ContentletWrapper cWrapperOld = 	 container.get(   it.getId()   );
						cWrapperOld.addListingLink(cLet.get( it.getId() ));
					} 
				}else {
					container.addAll(cLet.getAll() );
				}


			}
		}
		return container;
	}
	public ContentletContainer parseContent() throws  Exception{
		String folderName =  getHmiStructure().getFilePath();
		getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );

		return  createContainer();

	}
	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}


}
