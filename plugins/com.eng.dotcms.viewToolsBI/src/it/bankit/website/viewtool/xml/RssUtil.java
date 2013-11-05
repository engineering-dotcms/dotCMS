package it.bankit.website.viewtool.xml;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import org.springframework.web.util.UriUtils;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.content.ContentMap;
import com.dotmarketing.viewtools.content.FileAssetMap;
import com.sun.syndication.feed.rss.Category;

public class RssUtil implements ViewTool {
	protected static final SimpleDateFormat dateFormat = new SimpleDateFormat( "dd MMMMM yyyy hh.mm" );
	protected static final String languageKeyBankit = "lang";
	protected static final String languageKeyHiperWave = "internal&action=_setlanguage.action?LANGUAGE";
	private IdentifierAPI identifierAPI;
	private HttpServletRequest req;
	private String basePath;

	@Override
	public void init( Object initData ) {
		identifierAPI = APILocator.getIdentifierAPI();
		this.req = ( (ViewContext) initData ).getRequest();
		basePath = "http://" + req.getServerName();
		if ( req.getServerPort() != 80 ) {
			basePath += ":" + req.getServerPort();
		}
	}

	public List<Map<String, Object>> convert( @SuppressWarnings( "rawtypes" ) List contentlets ) {
		if ( contentlets != null ) {
			List<Map<String, Object>> conversion = new ArrayList<Map<String, Object>>( contentlets.size() );
			for ( Object curContent : contentlets ) {
				try {
					if ( curContent instanceof ContentMap ) {
						conversion.add( convertContentletMap( (ContentMap) curContent ) );
					} else {
						Logger.error( RssUtil.class, "Unsupported Conversion from:" + curContent.getClass().getName() );
					}
				} catch ( DotDataException e ) {
					Logger.error( RssUtil.class, "Error conversion", e );
				}
			}
			return conversion;
		} else {
			return null;
		}

	}

	private Map<String, Object> convertContentletMap( ContentMap content ) throws DotDataException {
		Map<String, Object> curMap = new HashMap<String, Object>( 5 );
		Structure structure = content.getStructure();
		Long languageId = (Long) content.get( "languageId" );
		Language language = APILocator.getLanguageAPI().getLanguage( languageId );
		curMap.put( "guid", content.get( "identifier" ) );
		String titoloLungo = (String) findValue( content, "titoloLungo" );
		if ( UtilMethods.isSet( titoloLungo ) ) {
			curMap.put( "title", titoloLungo );
		} else {
			curMap.put( "title", findValue( content, "titolo", "title" ) );
		}
		//if ( content.get( "mostraSommario" ) != null ) {
		String sommario = (String) findValue( content, "sommarioEmail", "sommario", "description", "descrizione", "descrizioneBreve" );
		
		if( UtilMethods.isSet(sommario )  && sommario.length() >= 4000 ){
			sommario = sommario.substring(0 , 3996) + "...";
		}
		curMap.put( "description", sommario  );
		//}
		curMap.put( "pubdate", findValue( content, "dataPubblicazione", "modDate" ) );
		
		
		curMap.put( "dataEmanazione", findValue( content, "dataEmanazione", "dataCambio" ) );
		
		
		String autore = (String) findValue( content, "autoreallegato", "autore", "autori" );
		if ( UtilMethods.isSet( autore ) ) {
			curMap.put( "author", autore );
		}

		String folderInode = (String) content.get( "folder" );
		Identifier id = identifierAPI.findFromInode( folderInode );
		String categoryName = "";
		String categoryDomain = "";
		String completePath = id.getPath();
		if ( completePath.length() > 0 ) {
			if ( structure.isFileAsset() ) {
				categoryDomain = completePath.substring( 0, completePath.length() - 1 );
			} else {
				categoryDomain = completePath.substring( 0, completePath.lastIndexOf( "/" ) );
			}
			categoryName = categoryDomain.substring( categoryDomain.lastIndexOf( "/" ) + 1 );
		}

		Category category = new Category();
		category.setValue( categoryName );
		category.setDomain( categoryDomain );
		curMap.put( "categories", Collections.singletonList( category ) );

		String link = null;
		try {
			if ( structure.isFileAsset() && !structure.getName().equalsIgnoreCase( "Foto" ) ) {
				FileAssetMap fileAssetMap = (FileAssetMap) content.get( "fileAsset" );
				link = basePath + fileAssetMap.getUri();
			}else if ( structure.getName().equalsIgnoreCase( "Link" ) ) {
				String lType = (String) content.get("linkType");
				if( UtilMethods.isSet(lType ) ){
					if( lType.equalsIgnoreCase("I")  )
					{
						link = basePath + (String) content.get("linkInterno");

					}else if(  lType.equalsIgnoreCase("A")   ){
						try{					
						
							System.out.println( "Verifico se è un allegato ");
							FileAssetMap fileAssetMap = (FileAssetMap) content.get( "allegato" );
								System.out.println( "fileAssetMap.getUri()  " + fileAssetMap.getUri() );
							link = basePath + fileAssetMap.getUri();
						}catch (Exception e) {
							e.printStackTrace();
							System.out.println( "RSSUTIL Errore FileAssetMap " + content.get( "allegato" )  );
						}	
					}else if ( lType.equalsIgnoreCase("E")  ){
						String lEsterno = (String) content.get("linkEsterno");
						if( lEsterno.startsWith("http://") ||  lEsterno.startsWith("https://") ) {
							link = lEsterno;
						}  else {
							link = "http://" + lEsterno;
						}
					}
				}

			}else if ( structure.getName().equalsIgnoreCase( "Cambio" ) ) {

				String rifCambio = (String) content.get( "mesegiorno" );

				Date dataCambio = (Date) content.get( "dataCambio" );
				Calendar calCambio = Calendar.getInstance();
				calCambio.setTime(dataCambio);
				
				int mese = calCambio.get( Calendar.MONTH ) + 1;

				String meseString = String.valueOf( mese);
				if ( mese < 10 ) {
					meseString = "0" + mese;
				}
				int giorno = calCambio.get( Calendar.DAY_OF_MONTH );
				String giornoString = String.valueOf(giorno);
				if ( giorno < 10 ) {
					giornoString = "0" + giorno;
				}

				int anno = calCambio.get( Calendar.YEAR );


				String pathCambi = "/banca_centrale/cambi/rif/"+anno+"/"+meseString+"/";
				if ( !language.getLanguageCode().equalsIgnoreCase( "it" ) ) {
					rifCambio += "_" + language.getLanguageCode();
				}
				/*
				 * Versione dotcms String query=UriUtils.encodeQuery(
				 * "?"+languageKeyBankit
				 * +"="+language.getLanguageCode(),"UTF-8");
				 */
				rifCambio = giornoString +meseString;
				System.out.println(  " rifCambio " + rifCambio );
				String query = UriUtils.encodeQuery( ";" + languageKeyHiperWave + "=" + language.getLanguageCode(), "UTF-8" );
				
				System.out.println(" PATH CAMBI  " + basePath + pathCambi + "cambi_rif_" + rifCambio + ".html" + query );
				
				link = basePath + pathCambi + "cambi_rif_" + rifCambio + ".html" + query;
			} else {
				/*
				 * Versione dotcms String query=UriUtils.encodeQuery(
				 * "?"+languageKeyBankit
				 * +"="+language.getLanguageCode(),"UTF-8"); String
				 * path=UriUtils.encodePath( basePath + completePath +
				 * "index.html", "UTF-8" );
				 */
				String query = UriUtils.encodeQuery( ";" + languageKeyHiperWave + "=" + language.getLanguageCode(), "UTF-8" );
				String path = UriUtils.encodePath( basePath + completePath, "UTF-8" );
				link = path + query;
			}
			curMap.put( "link", link );
		} catch ( UnsupportedEncodingException e ) {
			Logger.error( RssUtil.class, "Errore nell'encoding del path:" + completePath, e );
		}
		return curMap;
	}

	private Object findValue( ContentMap content, String... mapping ) {
		for ( String curMapping : mapping ) {
			Field curField = content.getStructure().getFieldVar( curMapping );
			if ( curField != null ) {
				Object curValue = content.get( curMapping );
				if ( curValue != null ) {
					if ( curValue instanceof String ) {
						if ( UtilMethods.isSet( (String) curValue ) ) {
							return curValue;
						}
					} else if ( curValue instanceof Date ) {
						return curValue;
					} else {
						return curValue.toString();
					}
				}
				return "";
			}
		}
		return "";
	}

}
