package it.eng.bankit.converter;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.fileasset.AliasLinkConverter;
import it.eng.bankit.converter.fileasset.LinkConverter;
import it.eng.bankit.converter.listing.LinksCorrelatiConverter;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;
import it.eng.bankit.writer.DotcmsSysDataWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;

public abstract class AbstractConverterImpl implements FolderConverter {


	protected Logger LOG = Logger.getLogger( this.getClass().getName()  );

	protected HmiStructure hmiStructure;
	private DotcmsSysDataWriter dotcmsSysDataWriter;	
	private String formato = null;

	@Override
	public abstract ContentletContainer parseContent() throws Exception  ;

	public abstract  Structure getDotStructure() ;

	protected static final SimpleDateFormat hwDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	protected static final SimpleDateFormat simpleHwDateFormat = new SimpleDateFormat("yyyy/MM/dd");


	public AbstractConverterImpl(HmiStructure struttura) {		
		hmiStructure = struttura;
	}

	public HmiStructure getHmiStructure() {
		return hmiStructure;
	}

	public void setHmiStructure(HmiStructure hmiStructure) {
		this.hmiStructure = hmiStructure;
	}


	protected abstract  String getLinkRelationShipName();


	public String getLuceneQuery( Contentlet contentlet ){
		String stName = getDotStructure().getVelocityVarName() ;
		StringBuffer  sb = new StringBuffer("");	 	
		sb.append( "+structureName:"+  stName + 
				" +languageId:"+ contentlet.getLanguageId()+" +deleted:false "+
				" +"+stName+".hwgoid:\"" + getHmiStructure().getPropertyHmi(HyperwaveKey.GOid)+"\""+
				" +conhost:"+contentlet.getHost()+
				" " );
		if( hasProperty("path")){
			sb.append("	+confolder:"+contentlet.getFolder() );
		}
		return sb.toString();
	} 

	protected boolean hasProperty( String velocityVarName ){
		if( UtilMethods.isSet( velocityVarName ) ) {
			Structure str = getDotStructure() ;
			List<Field> fields = FieldsCache.getFieldsByStructureVariableName(str.getVelocityVarName() );
			for( Field field : fields  ){
				String fieldName = field.getVelocityVarName();
				if( velocityVarName.equalsIgnoreCase(fieldName )  ){
					return true;
				}
			}
		}
		return false;
	}

	protected void addFile( Contentlet contentlet, String propertyName, File file ) throws Exception {
		try {
			File tmpFile = it.eng.bankit.util.FileUtil. createTempFile(file);
			if( tmpFile != null ){
				contentlet.setBinary( propertyName, tmpFile );
			}
		} catch ( IOException e ) {
			LOG.error(   e.getMessage(), e );
		}

		//		File tempDir;
		//		try {
		//			//tempDir = new File(APILocator.getFileAPI().getRealAssetPath()+ File.separator + "tmp_"+ ImportUtil.getUser().getFirstName() );
		// 			tempDir = File.createTempFile( "dotcms", "dir" );
		//			boolean deleted = tempDir.delete();
		//			boolean created = tempDir.mkdir();
		//			if ( deleted && created ) {
		//				File tempFile = new File( tempDir.getPath(), ImportUtil.encodePathToURL( file.getName() ) );
		// 				FileUtil.copyFile( file, tempFile , false );
		// 				contentlet.setBinary( propertyName, tempFile );
		//			} else {
		//				LOG.error( "Error in delete:" + deleted + " create:" + created + " Temp folder" );
		//			}
		//
		//		} catch ( IOException e ) {
		//			LOG.error(   e.getMessage(), e );
		//		}
	}

	public List<Contentlet>  createDefaultListContentlet() throws Exception{
		List<Contentlet> appoLista = new ArrayList<Contentlet>();
		Contentlet contIt = createDefaultContentlet( ImportConfigKey.ITALIAN_LANGUAGE );
		if( contIt != null ){
			appoLista.add( contIt );
		}
		Contentlet contEn = createDefaultContentlet( ImportConfigKey.ENGLISH_LANGUAGE );
		if(contEn!= null){
			appoLista.add( contEn );			
		}		
		return appoLista;
	}


	public Contentlet createDefaultContentlet( String langCode  ) throws Exception{
		Contentlet contentlet = null;
		String title = getHmiStructure().getPropertyHmi( HyperwaveKey.Title+":"+langCode );
		if( UtilMethods.isSet(title)) {
			contentlet = new Contentlet();
			Date data = new Date () ;

			Charset utf8=Charset.forName( "UTF-8" );				
			byte[] bytesTitle=title.getBytes( utf8 );
			if(bytesTitle.length>255){
				byte[] reducedTitleBytes=ArrayUtils.subarray( bytesTitle, 0, 254 );
				title =new String(reducedTitleBytes,utf8);
			}

			contentlet.setProperty("titolo", title );
			contentlet.setStructureInode( getDotStructure().getInode());
			//contentlet.setHost( ImportUtil.getHost().getIdentifier() );
			Language language = ImportUtil.getLanguage(langCode );
			contentlet.setLanguageId( language.getId());
			contentlet.setHost( ImportUtil.getHost().getIdentifier()  );
			contentlet.setDateProperty( "importDate", data   );
			contentlet.setLastReview( data );
			setDefaultImportData(getHmiStructure(),  contentlet );
			setSpecificProperties(   contentlet  ,   langCode  );
			LOG.debug("Default properties (title : ' "+title+" ') setted for lang " + langCode );
		}
		return contentlet ;
	}


	public List<Contentlet>  createDefaultListContentlet( HmiStructure struct ) throws Exception{
		List<Contentlet> appoLista = new ArrayList<Contentlet>();
		Contentlet contIt = createDefaultContentlet(  struct , ImportConfigKey.ITALIAN_LANGUAGE );
		if( contIt != null ){
			appoLista.add( contIt );
		}
		Contentlet contEn = createDefaultContentlet(struct ,  ImportConfigKey.ENGLISH_LANGUAGE );
		if(contEn!= null){
			appoLista.add( contEn );			
		}		
		return appoLista;
	}

	protected boolean addValueCorpo( List<HmiStructure> listaFileCorpo){
		boolean found = false;
		boolean addHmi = false;
		boolean existText = false;
		if( listaFileCorpo == null || listaFileCorpo.isEmpty()   ){
			return addHmi;
		}
		for( HmiStructure corpo : listaFileCorpo ){
			File fileCorrente = corpo.getFile();			
			if( fileCorrente != null  && it.eng.bankit.util.FileUtil.isTextFile(fileCorrente) ){
				existText = true;
				if ( found )	{
					addHmi = true;
				}
			}else{
				if( corpo.getFilePath().endsWith(".hmi")){		
					found = true;
				}
			}
		}	
		if( !existText ){
			addHmi = true ;
		}
		return addHmi;
	}

	public Contentlet createDefaultContentlet( HmiStructure struct  ,  String langCode  ) throws Exception{
		Contentlet contentlet = null;
		String title = struct.getPropertyHmi( HyperwaveKey.Title+":"+langCode );
		if( UtilMethods.isSet(title)) {
			contentlet = new Contentlet();
			Date data = new Date () ;
			contentlet.setProperty("titolo", title );
			contentlet.setStructureInode( getDotStructure().getInode());
			Language language = ImportUtil.getLanguage(langCode );
			contentlet.setLanguageId( language.getId());
			contentlet.setHost( ImportUtil.getHost().getIdentifier()  );
			contentlet.setDateProperty( "importDate", data   );
			contentlet.setLastReview( data );
			setDefaultImportData( struct , contentlet );
			setSpecificProperties(   contentlet  ,   langCode  );
			LOG.debug("Default properties (title : ' "+title+" ') setted for lang " + langCode );
		}
		return contentlet ;
	}

	protected   void setSpecificProperties(Contentlet contentlet , String langCode  )  throws Exception {
	}

	private Contentlet setDefaultImportData( HmiStructure struct,   Contentlet contentlet) throws Exception {
		Map<String, String> properties = struct.getPropertiesHmi();
		contentlet.setProperty("hwgoid", properties.get( HyperwaveKey.GOid ) );
		contentlet.setProperty("autoreHw", properties.get( HyperwaveKey.Author ) );
		String timeCreated = struct.getPropertyHmi(  HyperwaveKey.TimeCreated );
		String timeModified = struct.getPropertyHmi(  HyperwaveKey.TimeModified );
		if( UtilMethods.isSet(timeCreated ) ){
			Date timeC  = hwDateFormat.parse(timeCreated);
			contentlet.setDateProperty( "timeCreated", timeC );
		}
		if( UtilMethods.isSet(timeModified ) ){
			Date timeM  = hwDateFormat.parse(timeModified);
			contentlet.setDateProperty( "timeModified", timeM );
		}
		String hidden = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );
		if( hidden == null || !hidden.equalsIgnoreCase("Hidden")){
			Date timeM  = hwDateFormat.parse( timeModified );
			contentlet.setDateProperty( "dataPubblicazione", timeM );
		}
		return contentlet;	
	}


	//Crea i documenti correlati
	protected List<ContentletWrapper> processLinksCorrelati( Contentlet con ) throws Exception{
		List<ContentletWrapper> linksCorrelati = new ArrayList<ContentletWrapper>();
		HmiStructure hmiDir = getHmiStructure().getDocumentoCorrelato();

		if( hmiDir != null  ){
			Language langContentlet =  APILocator.getLanguageAPI().getLanguage(con.getLanguageId());
			LinksCorrelatiConverter lcc = new LinksCorrelatiConverter( hmiDir );
			ContentletWrapper cWrapperCorrelato =   lcc.parseContentWrapper( langContentlet.getLanguageCode()  );
			if(cWrapperCorrelato != null  ) {
				linksCorrelati.add(  cWrapperCorrelato );
			}
			List<HmiStructure>  dirs = hmiDir.getSubDirectories();
			if( dirs!= null  && dirs.size() >0  ){
				for(HmiStructure singleDir : dirs ){

					lcc = new LinksCorrelatiConverter( singleDir );
					cWrapperCorrelato =   lcc.parseContentWrapper( langContentlet.getLanguageCode()  );
					if(cWrapperCorrelato != null  ) {
						linksCorrelati.add(  cWrapperCorrelato );
					}
				}
			}

		}
		return  linksCorrelati;
	}

	protected List<ContentletWrapper> addLinksToContenlet( Contentlet contentlet , boolean showOnMenu  ) throws Exception {
		List<ContentletWrapper> listaLinks = new ArrayList<ContentletWrapper>();
		List<HmiStructure> links = getHmiStructure().getChildrenLinks();
		Comparator<HmiStructure> st = getHmiStructure().getLinksComparator( getHmiStructure() , contentlet ); 		
		Collections.sort( links, st );	
		for( HmiStructure hmi : links  ){
			Language langContentlet =  APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());
			String title = hmi.getPropertyHmi(  HyperwaveKey.Title+":"+langContentlet.getLanguageCode() ); // HO COMMENTATO && ( !hmi.isCollectionHead() ) 
			if( UtilMethods.isSet(title )  ){				
				LinkConverter lConv = new LinkConverter( hmi , showOnMenu  );
				ContentletWrapper linkWrapper = lConv.parseContentWrapper( langContentlet.getLanguageCode() );
				if( linkWrapper != null ){		
					listaLinks.add( linkWrapper );
				}
			}
		}
		List<ContentletWrapper>  alias = createAliasLinks4Contenlet( contentlet , showOnMenu );
		if( alias != null && alias.size() > 0 ){
			listaLinks.addAll( alias );
		}		
		return listaLinks;
	} 


	protected List<ContentletWrapper> createAliasLinks4Contenlet( Contentlet contentlet , boolean showOnMenu  ) throws Exception {
		List<ContentletWrapper> detta = new ArrayList<ContentletWrapper>();
		List<HmiStructure> links = getHmiStructure().getAliasLinks();
		Comparator<HmiStructure> st = getHmiStructure().getLinksComparator( getHmiStructure() , contentlet ); 		
		Collections.sort( links, st );	
		for( HmiStructure hmi : links  ){

			if( !hmi.isCollectionHead() ){
				Language langContentlet =  APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());
				String title = hmi.getPropertyHmi(  HyperwaveKey.Title+":"+langContentlet.getLanguageCode() ); // HO COMMENTATO && ( !hmi.isCollectionHead() ) 
				if( UtilMethods.isSet( title ) ){
					if( hmi.getParentStructure() == null ){
						hmi.setParentStructure( getHmiStructure() );
					}
					AliasLinkConverter lConv = new AliasLinkConverter( hmi  );
					ContentletWrapper linkWrapper = lConv.parseContentWrapper( langContentlet.getLanguageCode() );
					if( linkWrapper != null ){		
						detta.add( linkWrapper );
					}
				}
			}
		}
		return detta;
	} 

	protected void addLinksToContenlet(ContentletContainer container , boolean showOnMenu  ){
		try {
			List<ContentletWrapper> detta = new ArrayList<ContentletWrapper>();
			List<HmiStructure> links = getHmiStructure().getChildrenLinks();			
			for(ContentletWrapper cWrapper  : container.getAll() ){
				Comparator<HmiStructure> st = getHmiStructure().getLinksComparator( getHmiStructure() , cWrapper.getContentlet()  ); 		
				Collections.sort( links, st );	
				for( HmiStructure hmi : links  ){
					if( hmi.getParentStructure() == null ){
						hmi.setParentStructure(getHmiStructure());
					}
					LinkConverter lConv = new LinkConverter(hmi);
					ContentletContainer cLet = lConv.parseContent();
					if( cLet != null && !cLet.isEmpty()  ){
						ContentletWrapper linkWrapper = cLet.get( cWrapper.getContentlet().getLanguageId() ) ;
						if( linkWrapper != null ){						
							detta.add(linkWrapper);
						}
					}
				} 
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("ERROR in methd addLinksToContenlet " +  e.getMessage() );
		}
	}

	public String getFormato() {
		return formato;
	}

	public void setFormato(String formato) {
		this.formato = formato;
	}

	public DotcmsSysDataWriter getDotcmsSysDataWriter(){
		if( dotcmsSysDataWriter  == null   ) {
			dotcmsSysDataWriter = new DotcmsSysDataWriter();
		}
		return dotcmsSysDataWriter;
	}


}
