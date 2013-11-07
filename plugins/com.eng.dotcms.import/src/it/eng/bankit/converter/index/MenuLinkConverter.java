package it.eng.bankit.converter.index;

import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;
import it.eng.bankit.writer.DotcmsSysDataWriter;

import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.util.UtilMethods;

public class MenuLinkConverter   {

	boolean isMenuLink = false;
	private HmiStructure struttura = null;
	private DotcmsSysDataWriter dotcmsSysDataWriter;

	public MenuLinkConverter(HmiStructure struttura) {		
		this.struttura = struttura;
	}

	public Link createMenuLink( long languageId  ) throws  Exception{
		Language lang = APILocator.getLanguageAPI().getLanguage(languageId);
		try{
			String parentPath = struttura.getFilePath();
			Link  menuLink  = new Link();

			Map<String, String> mappa =  struttura.getPropertiesHmi();
			String formato =  struttura.getPropertyHmi(HyperwaveKey.DocumentType );
			if( formato != null && formato.equalsIgnoreCase("Remote")){

				String url = mappa.get(HyperwaveKey.Path );
				String titolo = mappa.get( HyperwaveKey.Title+":"+lang ); 
				menuLink.setShowOnMenu( true );
				if(titolo != null ){
					menuLink.setFriendlyName( titolo+"_"+lang.getLanguageCode().toUpperCase() );
					menuLink.setTitle(  titolo );
				} 
				if( parentPath.lastIndexOf(System.getProperty("file.separator") ) != -1 ){
					parentPath = parentPath.substring(0 , parentPath.lastIndexOf(System.getProperty("file.separator") )); 
				}
				StringBuffer sb = new StringBuffer(System.getProperty("file.separator")  );
				sb.append(parentPath + System.getProperty("file.separator")  );
				Folder fold = APILocator.getFolderAPI().findFolderByPath( sb.toString(), ImportUtil.getHost(), ImportUtil.getUser(), true);	
				if( fold == null || fold.getHostId().trim().equalsIgnoreCase("") ){
					fold = getDotcmsSysDataWriter(). createFolderOnHost(sb.toString(), struttura );
				} 			 	
				menuLink.setParent(fold.getInode());

				boolean isonhost =  isOnHost( mappa );
				if( isonhost ){
					menuLink.setLinkType (LinkType.INTERNAL.toString());
					getDotcmsSysDataWriter().createFolderOnHost(url, struttura );
					//	 fold =   APILocator.getFolderAPI().findFolderByPath( sb.toString(), ImportUtil.getHost(), ImportUtil.getUser(), true);					
					if( fold != null ){
						menuLink.setUrl( fold.getIdentifier() );
					} 
				}else {

					String host = struttura.getPropertyHmi( HyperwaveKey.Host );
					String path = struttura.getPropertyHmi( HyperwaveKey.Path );
					//			String port = struct.getPropertyHmi( HyperwaveKey.Port );
					String protocol = struttura.getPropertyHmi( HyperwaveKey.Protocol );
					//www
					StringBuffer sbLink = new StringBuffer();	
					if( !host.startsWith("www")){
						sbLink.append( "www." );
					}
					sbLink.append( host );
					if( UtilMethods.isSet( path )){
						sbLink.append(System.getProperty("file.separator")  +path );		
					}
					String linkExt = sbLink.toString();
					menuLink.setLinkType (LinkType.EXTERNAL.toString());
					menuLink.setProtocal(protocol);
					menuLink.setTarget("_blank");
					menuLink.setUrl(linkExt);					
				}
				return menuLink;
			}
		}catch (Exception e) {
			e.printStackTrace();
			throw new Exception();
		}
		return null;
	}

	private boolean isOnHost(Map<String, String> mappa) {
 		String protc = mappa.get(HyperwaveKey.Protocol );
		String host = mappa.get(HyperwaveKey.Host );
		if( protc == null || ( host != null &&  
				host.equalsIgnoreCase( ImportConfig.getProperty( ImportConfigKey.DEFAULT_SITE_LINK ))) ){
			return true;
		}
		return false;
	}
	public DotcmsSysDataWriter getDotcmsSysDataWriter(){
		if( dotcmsSysDataWriter  == null   )
		{
			dotcmsSysDataWriter = new DotcmsSysDataWriter();
		}
		return dotcmsSysDataWriter;
	}

}
