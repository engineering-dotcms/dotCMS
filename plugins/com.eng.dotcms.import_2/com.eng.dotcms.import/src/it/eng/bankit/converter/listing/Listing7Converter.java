package it.eng.bankit.converter.listing;

import it.eng.bankit.bean.ContentletContainer;

import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.detail.CambiConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public class Listing7Converter extends GenericListingConverter {

	//CAMBI 
	public Listing7Converter(HmiStructure struttura) {
		super(struttura);
	}

	public  ContentletContainer  parseContent() throws  Exception{
		Map<String, String> props = getHmiStructure().getPropertiesHmi();

		String formato = props.get( HyperwaveKey.Formato ); 
		ContentletContainer container = new ContentletContainer(); 
		String folderName =  getHmiStructure().getFilePath();
		getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() ); // creo il folder anno

		if(formato != null && ( formato.startsWith("L7")   )) {
			List<Contentlet> appoLista =  createDefaultListContentlet() ;		

			//		List<HmiStructure> childrenTesto = getHmiStructure().getSubFile() ;
			for( Contentlet con  : appoLista){ 
				ContentletWrapper cWrapper = new ContentletWrapper();
				Language l = ImportUtil.getLanguage(con.getLanguageId()  ); 
				List<HmiStructure> childrenTesto  = getHmiStructure().getSubFile( l.getLanguageCode() ) ;
				addCorpo( con, childrenTesto );		
				List<ContentletWrapper> dettaglioDirs =   addDettaglioChildren(   cWrapper , con  );
				if( dettaglioDirs != null ) {
					for(ContentletWrapper cw : dettaglioDirs )
						cWrapper.addListingLink( cw );
				}
				cWrapper.setQuery(  getLuceneQuery( con ));			 
				cWrapper.setContentlet(con);
				container.add(cWrapper);
			}
			return container;
		}


		return null;
	}


	private List<ContentletWrapper> addDettaglioChildren( ContentletWrapper cWrapper ,  Contentlet contentlet   ) throws Exception{

		List<HmiStructure> childrenDir = getHmiStructure().getSubDirectories( );
		List<ContentletWrapper> listaDettagli = new ArrayList<ContentletWrapper>();		
		for( HmiStructure strutturaChild : childrenDir ){
			Language l = ImportUtil.getLanguage(contentlet.getLanguageId()  ); 

			String titleAnno = strutturaChild.getPropertyHmi( HyperwaveKey.Title+":"+l.getLanguageCode() );

			List<HmiStructure> directory = strutturaChild.getSubDirectories();
			for( HmiStructure childMese : directory ){
				String titleMese = childMese.getPropertyHmi( HyperwaveKey.Title+":"+l.getLanguageCode() );
				
				CambiConverter cConverter = new CambiConverter( childMese );
				cConverter.setAnno(titleAnno);
				cConverter.setMese(titleMese);
				ContentletContainer cLet =  cConverter.parseContent();
				if( cLet != null && ! cLet.isEmpty()  ){
					ContentletWrapper wrapper = cLet.get( contentlet.getLanguageId() );
					if( wrapper!= null ){
						listaDettagli.add(  wrapper );
					}
				}
			}
		}
		return listaDettagli;
	}

}
