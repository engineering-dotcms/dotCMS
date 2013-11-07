package it.eng.bankit.converter.detail;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.AbstractConverterImpl;
import it.eng.bankit.converter.util.WYSYWGHelper;
import it.eng.bankit.util.FolderUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;

public class CambiConverter extends AbstractConverterImpl {

	private String anno ;
	private String mese ;

	public CambiConverter(HmiStructure struttura) {		
		super(struttura);
	}


	public ContentletContainer parseContent() throws  Exception{
		ContentletContainer container = new ContentletContainer(); 

		List<HmiStructure> filesCambi = getHmiStructure().getSubFile(); // file dei cambi
		// per ognuno di questi devo creare la pagina corrispondente e creare la contentlet cambio
		for( HmiStructure strutturaChild : filesCambi ){

			String lang = strutturaChild.getPropertyHmi( HyperwaveKey.HW_Language );

			Contentlet contIt = createDefaultContentlet(strutturaChild, lang );
			String timeCreated = strutturaChild.getPropertyHmi(HyperwaveKey.TimeCreated );
			
			String timeModified = strutturaChild.getPropertyHmi(  HyperwaveKey.TimeModified );
			Date timeM  = hwDateFormat.parse( timeModified );		 
			contIt.setDateProperty( "dataPubblicazione", timeM );


			StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );				
			sb.append( getHmiStructure().getFilePath() + System.getProperty("file.separator")  );				 
			Folder path = FolderUtil.findFolder( sb.toString() );
			File pageName = strutturaChild.getFile();
			String pName = pageName.getName().substring(0 , pageName.getName().lastIndexOf("."));

			getDotcmsSysDataWriter().createPageOnFolder( path, getHmiStructure() , pName  );
			String mesegiorno = pName.replaceAll("Cambi_rif_", "").replaceAll("cambi_rif_", "").replaceAll("_en", "");

			Date timeC  = getDateCambio( mesegiorno );
				//simpleHwDateFormat.parse( timeCreated );		
			contIt.setDateProperty( "dataCambio", timeC );
			contIt.setProperty("mesegiorno", mesegiorno );

			String alert= strutturaChild.getPropertyHmi( HyperwaveKey.Alert ) ;
			if ( alert != null ) {
				contIt.setProperty("alert", "True");
			}
			ContentletWrapper cWrapper = new ContentletWrapper();
			addCorpo( contIt, strutturaChild );

			cWrapper.setQuery( getLuceneQuery( contIt ));				
			cWrapper.setContentlet(contIt );
			ContentletWrapper cWrapperOld = container.get(contIt.getLanguageId() );
			if( cWrapperOld != null  ){
				cWrapperOld.addListingLink(cWrapper);
			}else {
				container.add(cWrapper);
			}

		}
		return container;
	}


	private Date getDateCambio(String mesegiorno ) {
		Calendar cal = Calendar.getInstance(); 
		int year = Integer.parseInt(getAnno());
		int month = Integer.parseInt(getMese() ) -1 ;
		int date = Integer.parseInt( mesegiorno.substring(0 , 2)  );		
		cal.set(year, month, date);		 
		return cal.getTime();
	}


	protected Contentlet addCorpo( Contentlet con, HmiStructure structure  ) throws Exception {
		String testHtml = "";
		File fileCorrente = structure.getFile();
		if( fileCorrente != null ){
			testHtml = testHtml + WYSYWGHelper.getHtmlBody(structure , fileCorrente );			
		} 
		con.setProperty("corpoNotizia", testHtml );

		return con;
	}

	@Override
	public Structure getDotStructure() {
		Structure structure = StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty("STRUCTURE_CAMBI") );
		return structure;
	}


	@Override
	protected String getLinkRelationShipName() {
		return "";
	}


	@Override
	protected void setSpecificProperties(Contentlet contentlet, String langCode)
	throws Exception {
		StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );				
		sb.append( getHmiStructure().getFilePath() + System.getProperty("file.separator")  );				 
		Folder path = FolderUtil.findFolder( sb.toString() );
		if( path  == null ){
			try{
				path = getDotcmsSysDataWriter().createFolderOnHost(sb.toString() , getHmiStructure() );
				contentlet.setProperty("path", path.getInode() );
				contentlet.setFolder( path.getInode()  );				 
			}catch (Exception e) {
				LOG.error("Errore " + e.getMessage() );
			}
		}else {
			contentlet.setProperty("path", path.getInode() );
			contentlet.setFolder( path.getInode()  );		
		}
	}


	public String getAnno() {
		return anno;
	}
	public void setAnno(String anno) {
		this.anno = anno;
	}
	public String getMese() {
		return mese;
	}

	public void setMese(String mese) {
		this.mese = mese;
	}
	
	

}
