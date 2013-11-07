package it.eng.bankit.converter.util;

import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.filereader.HWHtmlReader;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public class WYSYWGHelper {


	public static String getHTML(  HmiStructure corpo , Language language , boolean addHmi ){
		File fileCorrente = corpo.getFile();
		String hidden = corpo.getPropertyHmi( HyperwaveKey.PresentationHints );

		StringBuilder testoHTML  = new StringBuilder("");
 		if( fileCorrente != null ){
			if( FileUtil.isImageFile( fileCorrente ) ){
				if( hidden == null  || !hidden.equalsIgnoreCase("Hidden")){
					testoHTML.append(  WYSYWGHelper.getHtmlImage(corpo, fileCorrente)  );
					System.out.println(  "IMMAGINE NEL TESTO  "  );	
				}

			}
			else if( FileUtil.isAttachFile( fileCorrente)  ){	
				if( hidden == null   || !hidden.equalsIgnoreCase("Hidden")){
					testoHTML.append(  WYSYWGHelper.getHtmlFromAttach( corpo, language ) );
					System.out.println(  "isAttachFile NEL TESTO    "    );
				}
			}
			else {	
				if( hidden == null   || !hidden.equalsIgnoreCase("Hidden")){
					testoHTML.append(  WYSYWGHelper.getHtmlBody(corpo , fileCorrente ) );		
				}
			}
		}else {				
			if( corpo.getFilePath().endsWith(".hmi") && addHmi ){	
				if( hidden == null   || !hidden.equalsIgnoreCase("Hidden")){
					testoHTML.append(  WYSYWGHelper.getHtmlFromHmiLink(corpo, language) );
				}
			}
		}
		return testoHTML.toString();
	}

	public static String getHtmlFromAttach( HmiStructure corpo , Language language   ){
		String html = "";
		File fileCorrente = corpo.getFile();
		String langFile = language.getLanguageCode();
		String titolo = corpo.getPropertyHmi(HyperwaveKey.Title+":"+langFile  );
		String name = corpo.getPropertyHmi(HyperwaveKey.Name);
		if( !UtilMethods.isSet(name )){
			name = corpo.getFilePath();
		}
		//ImportUtil.getAlias(name.substring(0, name.indexOf("/")));

		String myme = corpo.getPropertyHmi(HyperwaveKey.MimeType );
		String src = null;
		if( myme!= null && myme.endsWith("pdf") ){
			src= "<ul> 	<li class='titolo'>  <a href='/"+name+"'>"+
			"<img class='ico' width='14' height='14' alt='' src='"+ImportConfig.getProperty("PDF_IMAGE")+"'>"+
			"  "+titolo+"</a> 	<span class='data'>pdf "+ getFileSizeKB(fileCorrente) + " kB</span>"+
			"</li> 	</ul>";
		}
		else if( myme!= null && myme.endsWith("xls") ){
			src= "<ul> 	<li class='titolo'>  <a href='/"+name+"'>"+
			"<img class='ico' width='14' height='14' alt='' src='"+ImportConfig.getProperty("XLS_IMAGE")+"'>"+
			"</a>   "+titolo+"	<span class='data'>vnd.ms-excel "+ getFileSizeKB(fileCorrente) + " kB</span>"+
			"</li> 	</ul>";
		}
		else if( myme!= null && myme.endsWith("ppt") ){
			src= "<ul> 	<li class='titolo'>  <a href='/"+name+"'>"+
			"<img class='ico' width='14' height='14' alt='' src='"+ImportConfig.getProperty("PPT_IMAGE")+"'>"+
			"</a>   "+titolo+"	<span class='data'>vnd.ms-powerpoint "+ getFileSizeKB(fileCorrente) + " kB</span>"+
			"</li> 	</ul>";
		}
		else if( myme!= null && myme.endsWith("zip") ){
			src= "<ul> 	<li class='titolo'>  <a href='/"+name+"'>"+
			"<img class='ico' width='14' height='14' alt='' src='"+ImportConfig.getProperty("ZIP_IMAGE")+"'>"+
			"</a>   "+titolo+"	<span class='data'>zip "+ getFileSizeKB(fileCorrente) + " kB</span>"+
			"</li> 	</ul>";
		}
		html = html + src ;
		return html;
	}
	public static String getHtmlBody( HmiStructure corpo , File fileCorrente , Language language  ){
		StringBuilder sb = new StringBuilder();;

		String formato = corpo.getPropertyHmi( HyperwaveKey.Formato );
		String titolo = corpo.getPropertyHmi( HyperwaveKey.Title + ":"+language.getLanguageCode() );
		sb.append("<ul>");
		sb.append("<li>");
		if(  UtilMethods.isSet(formato )  && formato.equalsIgnoreCase("testotit") ){
			sb.append("<strong>");
			sb.append(titolo);
			sb.append("</strong><br />");
		}

		sb.append( HWHtmlReader.getContent( fileCorrente ) ) ;
		sb.append("</li>");
		sb.append("</ul>");

		return sb.toString();
	}

	public static String getHtmlBody( HmiStructure corpo , File fileCorrente  ){
		String html =  HWHtmlReader.getContent( fileCorrente );
		return html;
	}


	public static String getHtmlBodyImage( HmiStructure corpo  ){
		String html = "";
		String value = corpo.getPropertyHmi( HyperwaveKey.Immagine );
		String dimensione = corpo.getPropertyHmi(HyperwaveKey.Dimensione );
		String classe = "foto";
		if( UtilMethods.isSet(value )){							
			if( UtilMethods.isSet(dimensione )){
				if( dimensione.equalsIgnoreCase("piccola")){
					classe = "fotosmall";
				}else if(  dimensione.equalsIgnoreCase("fissa") ){
					classe = "fotofissa";
				}
			}
			html =  "<img class='"+classe+"'' alt='' src='"+value+"'/>";
		}
		return html;
	}

	public static String getHtmlImage( HmiStructure corpo , File fileCorrente  ){
		String html = "";
		String src = corpo.getParentStructure().getFilePath();
		StringBuffer sb = new StringBuffer( System.getProperty("file.separator")  );
		sb.append( src );
		sb.append( System.getProperty("file.separator")  );
		sb.append(fileCorrente.getName());
		html = "<img class='foto' src='"+ sb.toString() +"' title = '"+fileCorrente.getName() +"' />";
		return html;
	}


	public static String getHtmlFromHmiLink( HmiStructure corpo , Language language ){
		String html = "";
		String docType = corpo.getPropertyHmi(  HyperwaveKey.DocumentType );
		if( docType!= null && docType.equalsIgnoreCase("Remote") ){
			String host =  corpo.getPropertyHmi( HyperwaveKey.Host  );
			if( UtilMethods.isSet( host) &&  
					!host.equalsIgnoreCase( ImportConfig.getProperty(ImportConfigKey.DEFAULT_SITE_LINK ))){
				getHtmlLinkEsterno( corpo ,language );
			}else 
				html = getHtmlLinkInterno( corpo , language );
		}
		else {
			html = getHtmlLinkInterno( corpo , language );
		}
		return html;
	}

	private static String getHtmlLinkEsterno(HmiStructure corpo, Language language ) {
		String langFile = language.getLanguageCode();

		String host =  corpo.getPropertyHmi(  HyperwaveKey.Host  );

		String path = corpo.getPropertyHmi(  HyperwaveKey.Path );
		String title = corpo.getPropertyHmi(  HyperwaveKey.Title +":"+langFile );
		String vhtml="";
		String port =  corpo.getPropertyHmi(  HyperwaveKey.Port  );
		String protocol =  corpo.getPropertyHmi(  HyperwaveKey.Protocol  );

		StringBuffer sbLink = new StringBuffer();
		sbLink.append(protocol);
		sbLink.append("://");
		sbLink.append( host );
		if(UtilMethods.isSet(port )   &&  !( port.equalsIgnoreCase("80"))){
			sbLink.append( ":"+port );						
		}
		if( UtilMethods.isSet(path )){
			sbLink.append(path );		
		}
		String linkExt = sbLink.toString();

		vhtml = " <ul>  <li class='titolo'> 	<a title='External link to "+title+"' href='/"+linkExt+"'>"+
		"<img class='ico'width='14' height='14' alt=''  src='"+ImportConfig.getProperty("LINK_EXT_IMAGE")+"' />"+ title+
		"</a>"+
		"<br>"+
		"<span class='pathlivello'>"+title+"</span>"+
		"</li>"+
		"</ul>";
		return vhtml;

	}
	/*
	 * 		
		}
	 */


	private static String getHtmlLinkInterno(HmiStructure corpo, Language language ) {
		String langFile = language.getLanguageCode();

		String path = corpo.getPropertyHmi(  HyperwaveKey.Path );
		if( UtilMethods.isSet(path) ){
			if( !path.endsWith("/")){
				path = path+"/";
			}
		}
		String pathTranslate = getPathLivello( path  , language );
		System.out.println( " pathTranslate " + pathTranslate ) ;
		String title = corpo.getPropertyHmi(  HyperwaveKey.Title +":"+langFile );

		String mimeT = corpo.getPropertyHmi(  HyperwaveKey.MimeType );

		StringBuilder sBuilder = new StringBuilder("");
		if( UtilMethods.isSet( mimeT ) ){
			if( mimeT.indexOf( "image") != -1) {
				if( corpo.isAlias() ){
					String fileName = corpo.getFilePath();
					if( UtilMethods.isSet( fileName ) ){
						if( fileName.endsWith(".hmi") ){
							fileName = fileName.substring(fileName.lastIndexOf("/") +1 , fileName.lastIndexOf(".hmi"))  ;
						}
					}		 
					String newFilePath = ImportUtil.getFileAlias(fileName);
					newFilePath = newFilePath + fileName;
					// /UIF/altre-funzioni/op-oro/mod-sw-oro/data-entry-oro/oro.jpg
					sBuilder.append(  " <img class='foto' alt='' src='/"+newFilePath+" ' />");

				}
			}else if( mimeT.indexOf( "text")!= -1  ){

				if( corpo.isAlias() ){
					String fileName = corpo.getFilePath();
					if( UtilMethods.isSet( fileName ) ){
						if( fileName.endsWith(".hmi") ){
							fileName = fileName.substring(fileName.lastIndexOf("/") +1 , fileName.lastIndexOf(".hmi"))  ;
						}
					}		 
					String newFilePath = ImportUtil.getFileAlias(fileName);
					newFilePath = newFilePath + fileName;

					File f = new File(corpo.getImportDir() + "/" + newFilePath );
					String html =  HWHtmlReader.getContent( f );
					sBuilder.append( html );
				}
			}
		}else {
			sBuilder.append(  " <ul>  <li class='titolo'> 	<a href='/"+path+"'>"+
					"<img class='ico'width='14' height='14' alt=''  src='"+ImportConfig.getProperty("LINK_INT_IMAGE")+"' />"+ title+
					"</a> <br /> <span class='pathlivello'>"+title+"</span> </li> </ul>");

		}
		return sBuilder.toString();
	}


	private static String getPathLivello(String path , Language lang ) {

		String keyReolaced = path.replaceAll(System.getProperty("file.separator") , ".").replaceAll(" " , "-");
		if( keyReolaced.startsWith(".")){
			keyReolaced = keyReolaced.substring(1);
		}
		if( keyReolaced.endsWith( ".")){
			keyReolaced = keyReolaced.substring(0 , keyReolaced.lastIndexOf("."));
		}

		String [] f = keyReolaced.split(".");

		if( f != null && f.length > 0  ){
			StringBuffer valore = new StringBuffer();
			for(  int i = 0 ; i < f.length ; i++){
				String pathT = APILocator.getLanguageAPI().getStringKey(lang, f[i]);
				if( UtilMethods.isSet( pathT )){
					valore.append( pathT );
				}
			}

		}

		return null;
	}
	private static long getFileSizeKB(File file ){
		if( file!= null ){
			return ( file.length() / 1000 );
		}
		return 0;
	}

}
