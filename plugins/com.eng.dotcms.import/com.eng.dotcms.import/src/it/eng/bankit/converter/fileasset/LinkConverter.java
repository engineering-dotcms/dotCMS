package it.eng.bankit.converter.fileasset;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.FilenameUtils;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class LinkConverter extends GenericDetailConverter {

	private boolean isMenuLink = false;
	protected boolean isAlias = false;

	public LinkConverter(HmiStructure struttura) {		
		super(struttura);
		isAlias = false; 
	}

	public LinkConverter(HmiStructure struttura , boolean showOnMenu ) {		
		super(struttura);
		isMenuLink = showOnMenu;
		isAlias = false;
	}

	public ContentletContainer parseContent() throws  Exception{
		String formato = getHmiStructure().getPropertyHmi(  HyperwaveKey.Formato );
		if( formato != null && formato.equalsIgnoreCase("img") ){
			return null;
		}
		ContentletContainer container = new ContentletContainer(); 		
		Contentlet cIta = createDefaultContentlet(ImportConfigKey.ITALIAN_LANGUAGE );
		Contentlet cEng = createDefaultContentlet(ImportConfigKey.ENGLISH_LANGUAGE );

		List<Contentlet> appoLista = new ArrayList<Contentlet>() ;
		if( cIta != null ){
			appoLista.add( cIta );
		}
		if( cEng != null ){
			appoLista.add( cEng );
		}
		for( Contentlet con  : appoLista){ 
			Language lang = ImportUtil.getLanguage(  con.getLanguageId() );
			ContentletWrapper cWrapper = parseContentWrapper( lang.getCountryCode() );
			container.add(cWrapper);

		}
		return container;

	}



	protected void setSpecificProperties(Contentlet contentlet , String langCode  )  throws Exception {
		super.setSpecificProperties(contentlet, langCode);
		contentlet.setProperty("showOnMenu", Boolean.toString( isMenuLink) ); 
		String presentation =  getHmiStructure().getPropertyHmi(  HyperwaveKey.PresentationHints ); 
		if( UtilMethods.isSet(presentation) && presentation.equalsIgnoreCase("CollectionHead")){
			contentlet.setProperty("visualizzaIn", "CN" );
		}
		else {
			contentlet.setProperty("visualizzaIn", "LA" );
		}
		String name = getHmiStructure().getFilePath();
		name = name.substring(name.lastIndexOf(System.getProperty("file.separator")  )   +1 );
		if( name.endsWith(".hmi") ){
			name = name.substring(0 , name.lastIndexOf(".hmi"));
		}
		String title = getHmiStructure().getPropertyHmi( HyperwaveKey.Title+":"+langCode );
		Charset utf8=Charset.forName( "UTF-8" );
		byte[] bytesTitle=title.getBytes( utf8 );
		if(bytesTitle.length>255){
			contentlet.setProperty("titoloLungo", title );
		}
		contentlet.setProperty("identificativo", name );	 
	}


	protected String convertInternalPath ( String path ){
		StringTokenizer st = new StringTokenizer(path,System.getProperty("file.separator") );
		StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );
		while (st.hasMoreTokens()) {
			String name = st.nextToken(); 	
			name = ImportUtil.getAlias( name );
			sb.append(name + System.getProperty("file.separator")  );			
		}
		return sb.toString(); 
	}

	@Override
	public  Structure getDotStructure(){
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_LINK") );
		return structure;
	}

	public boolean isMenuLink() {
		return isMenuLink;
	}

	public void setMenuLink(boolean isMenuLink) {
		this.isMenuLink = isMenuLink;
	}

	public boolean isAlias() {
		return isAlias;
	}

	@Override
	public ContentletWrapper parseContentWrapper(String correctLang)
	throws Exception {

		Contentlet contentlet = createDefaultContentlet( correctLang );
		if( contentlet != null  ) {
			Map<String, String> props = getHmiStructure().getPropertiesHmi();

			String host = props.get( HyperwaveKey.Host );
			String path = props.get( HyperwaveKey.Path );
			String port = props.get( HyperwaveKey.Port );
			String protocol = props.get( HyperwaveKey.Protocol );
			String docType = props.get( HyperwaveKey.DocumentType );

			ContentletWrapper cWrapper = new ContentletWrapper();
			if( host!= null ) {
				if( host.equalsIgnoreCase( ImportConfig.getProperty( ImportConfigKey.DEFAULT_SITE_LINK  ))){
					if( UtilMethods.isSet( path )  || isAlias() ) {

						if( UtilMethods.isSet( path ) && ( path.endsWith(".pdf")  ||  path.endsWith(".PDF") )){
							contentlet.setProperty("linkType", "A");
							if( UtilMethods.isSet(docType) && docType.equalsIgnoreCase("Remote")){
								contentlet.setProperty( "iconaInterno", "True" );										
							}
							String alName = FilenameUtils.getName( path );
							String pathFolder ="/"+ path.substring(0 , path.indexOf(alName) );
							String q = "+structureName:AllegatoDettaglio "+" +parentpath:"+pathFolder +" +AllegatoDettaglio.fileName:"+alName+"*";
							List<Contentlet> hits =    APILocator.getContentletAPI().search( q, 1, 0, "modDate desc", ImportUtil.getUser(), true );
							if( hits != null && hits.size()>0  ){
								try{
									Contentlet contentletAll =  	hits.get(0 );
									
									contentlet.setProperty( "allegato", contentletAll.getIdentifier() );
									contentlet.setProperty( "allegatoId", contentletAll.getIdentifier() );				
								}catch (Exception e) {
									System.out.print("ERRORE");
								}
							}
						}
						else {
							StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );
							String pathTocreate = ImportUtil.getRealtPath( path );
							sb.append( pathTocreate +System.getProperty("file.separator")  );	
							contentlet.setProperty("linkType", "I");	
							contentlet.setProperty("linkInterno", convertInternalPath( sb.toString() ) );
						}
					}else {
						contentlet.setProperty("linkType", "A");
						// capire come inseire il link allegato
					}
				}else {				 
					StringBuffer sbLink = new StringBuffer();
					sbLink.append(protocol);
					sbLink.append("://");
					sbLink.append( host );
					if(UtilMethods.isSet(port )   &&  !( port.equalsIgnoreCase("80"))){
						sbLink.append( ":"+port );						
					}
					if( UtilMethods.isSet(path )){
						if( !path.startsWith("/")){
							sbLink.append("/" );								
						} 
						sbLink.append(path );	
					}
					String linkExt = sbLink.toString();
					contentlet.setProperty("linkEsterno", linkExt );
					contentlet.setProperty("linkType", "E");	
				}
			}else {

				if( UtilMethods.isSet( docType ) && docType.equalsIgnoreCase("text") ){
					StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );
					String pathTocreate =  getHmiStructure().getFilePath()  ;
					if( !pathTocreate.startsWith(System.getProperty("file.separator")  )){
						pathTocreate = System.getProperty("file.separator")   + pathTocreate;
					}
					sb.append( pathTocreate +System.getProperty("file.separator")  );	
					contentlet.setProperty("linkType", "I");	
					contentlet.setProperty("linkInterno", convertInternalPath( pathTocreate ) );
				}else {
					if( UtilMethods.isSet( path )  || isAlias() ) {
						StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );
						String pathTocreate = ImportUtil.getRealtPath( path );
						sb.append( pathTocreate +System.getProperty("file.separator")  );	
						contentlet.setProperty("linkType", "I");	
						contentlet.setProperty("linkInterno", convertInternalPath( sb.toString() ) );
					}else {
						contentlet.setProperty("linkType", "A");
						// capire come inseire il link allegato
					}
				}
			}
			String presentationHidden = getHmiStructure().getPropertyHmi(  HyperwaveKey.PresentationHints  );
			if( presentationHidden != null && presentationHidden.equalsIgnoreCase("Hidden") ){
				cWrapper.setArchived(true);
			}
			cWrapper.setQuery( getLuceneQuery(contentlet));
			cWrapper.setContentlet(contentlet);
			return cWrapper ;
		}
		return null;
	}



}
