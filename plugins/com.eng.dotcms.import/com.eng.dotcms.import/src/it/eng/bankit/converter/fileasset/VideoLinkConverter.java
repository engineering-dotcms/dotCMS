package it.eng.bankit.converter.fileasset;

import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.media.VideoConverter;
import it.eng.bankit.util.FolderUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;
import it.eng.bankit.writer.DotcmsSysDataWriter;

public class VideoLinkConverter extends VideoConverter {
	protected Logger LOG = Logger.getLogger( this.getClass().getName()  );
	private DotcmsSysDataWriter dotcmsSysDataWriter;

	public VideoLinkConverter(HmiStructure hmiStructure) {
		super(hmiStructure);
	}


	@Override
	public ContentletContainer parseContent() throws Exception {
		ContentletContainer cContainer = super.parseContent();
		if( cContainer != null ){			
			List<Language> languages = ImportUtil.getDotLanguages();
			for(Language language : languages ){
				ContentletWrapper wrapperVideo = cContainer.get(language.getId()  );
				if( wrapperVideo != null) {
					creaLinkWrapper( wrapperVideo, getStruct()  );
				}
			}			
		}
		return cContainer;		
	}


	private void creaLinkWrapper( ContentletWrapper wrapperVideo , HmiStructure struttura ) {
		Contentlet contentletVideoAsset = wrapperVideo.getContentlet();
		if( contentletVideoAsset != null ) {
			setPropertyFolderVideo( wrapperVideo , struttura );
 			String fileName = FilenameUtils.getName( struttura.getFile().getName() );	
			Language lang = APILocator.getLanguageAPI().getLanguage( contentletVideoAsset.getLanguageId() ); 
			Contentlet link = creaLinkAllegato( struttura , fileName , lang );			
			link.setFolder( contentletVideoAsset.getFolder() );
			link.setHost( contentletVideoAsset.getHost() );
			link.setProperty("path", contentletVideoAsset.getFolder() );
			wrapperVideo.setLinkAllegato( link );
			//cWrapper.setQuery( getLuceneQuery( "" ));
			wrapperVideo.setContentlet(contentletVideoAsset);
		}
	}

	private Contentlet creaLinkAllegato( HmiStructure hmiStructure , String fileName , Language language ) {
		Contentlet con = null;
		LinkConverter lc = new LinkConverter( hmiStructure , false  );
		try {
			con = lc.createDefaultContentlet(  language.getLanguageCode() );
			con.setProperty("linkType", "A");			
			String importante = hmiStructure.getPropertyHmi(  HyperwaveKey.Importante );
			if( UtilMethods.isSet( importante ) && importante.equalsIgnoreCase("yes")){
				con.setProperty("importante", "true");		
			}
			String sommario = hmiStructure.getPropertyHmi(  HyperwaveKey.Abstract+":"+language.getLanguageCode() );
			if( UtilMethods.isSet(sommario)){
				String absHide = hmiStructure.getPropertyHmi(  HyperwaveKey.AbsHide );
				if( UtilMethods.isSet( absHide )  ){
					con.setProperty("mostraSommario", ImportConfigKey.FALSE_N_VALUE );		
				}else{
					con.setProperty("mostraSommario", ImportConfigKey.TRUE_S_VALUE );
				}
			}
			System.out.println(  "CREATO LINK PER COLLEGAMENTO VIDEO :  " + fileName   + " Lingua " + language.getLanguage() );
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error( e.getMessage() );
		}
		return con;
	}

	protected void setPropertyFolderVideo ( ContentletWrapper alleg , HmiStructure struttura ) {
		try{
			StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );		
			String pathTocreate = ImportUtil.getRealtPath( getStruct().getFilePath() );
			sb.append(pathTocreate  + System.getProperty("file.separator")  );	
			Folder folder = FolderUtil.findOrCreateFolder( sb.toString() );
			if( folder != null && UtilMethods.isSet(folder.getInode()  ) ) {
				alleg.getContentlet().setFolder( folder.getInode()  );
				alleg.getContentlet().setHost( folder.getHostId());			
				getDotcmsSysDataWriter().createPageOnFolder(folder, struttura, "playVideo");
			}
		}catch (Exception e) {			
		}

	}

	public String getLuceneQuery(Contentlet contentlet) {
		//		String valore  = super.getLuceneQuery(contentlet);
		//		valore = valore + "  +"+getDotStructure().getVelocityVarName()+".fileName:\"" + contentlet.getStringProperty("fileName")+"\"";
		//		valore = valore + "  +"+getDotStructure().getVelocityVarName()+".hostFolder:"+contentlet.getStringProperty("hostFolder") ;
		return "" ;
	}


	public DotcmsSysDataWriter getDotcmsSysDataWriter(){
		if( dotcmsSysDataWriter  == null   ) {
			dotcmsSysDataWriter = new DotcmsSysDataWriter();
		}
		return dotcmsSysDataWriter;
	}
}
