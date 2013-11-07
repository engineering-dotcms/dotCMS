package it.eng.bankit.converter.detail;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

public class DGConverter extends D2Converter {

	public DGConverter(HmiStructure struttura) {	
		super(struttura);
	}


	public ContentletContainer parseContent() throws  Exception{
		Map<String, String> props = getHmiStructure().getPropertiesHmi();
		String formato = props.get( HyperwaveKey.Formato );
		ContentletContainer container = null;
		if(formato != null &&  formato.equalsIgnoreCase("DG" ) ){ 
			container = creaContentletContainer();
		}
		return container; 
	} 	 

	protected Contentlet addCorpo( Contentlet con, HmiStructure structure  ) throws Exception {
		List<HmiStructure> lista = getAllFullCollectionHead(  structure ,   con.getLanguageId()) ;
		return addCorpo(con, lista );
	}


	protected List<HmiStructure> getAllFullCollectionHead(HmiStructure structure , long languageId) {
		List<HmiStructure> children = structure.getSubFile();
		List<HmiStructure> listaFileCorpo = new ArrayList<HmiStructure>();
		
		for( HmiStructure child : children ){
			File fileCorrente = child.getFile();

			if( FileUtil.isTextFile(fileCorrente)  ) {
				if( child.checkLanguages( languageId ) ) {
					String hidden = child.getPropertyHmi( HyperwaveKey.PresentationHints );
					if( hidden == null ){
						listaFileCorpo.add( child );	
					}
				}
			}
		}
		return listaFileCorpo ;
	}
}
