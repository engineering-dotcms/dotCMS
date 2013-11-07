package it.eng.bankit.app.util;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class FileReaderUtil {

	private static String START_BODY  = "<body>";
	private static String END_BODY    = "</body>";
	private static String START_TITLE = "<title>";
	private static String END_TITLE   = "</title>";

	private static String DEFAULT_ENCODING = "Windows-1252";
	private static Logger LOGGER = Logger.getLogger( FileReaderUtil.class );
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	public static String getFileExtension( File file ) {
		String ext = null;
		if ( file.isFile() ) {
			String fileName = file.getName();
			int index = fileName.lastIndexOf( "." );
			if ( index != -1 ) {
				ext = fileName.substring( index + 1 );
			}
		}
		return ext;
	}



	public static String getHtmlTitle( File file  ) {
		String  contents = null;
		try {
			String testo = FileUtils.readFileToString( file  );
			contents = org.apache.commons.lang.StringUtils.substringBetween( testo, START_TITLE  , END_TITLE  );
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage() , e );
		}  
		return contents;
	}


	public static String getHtmlBody(  File fileCorrente  ){
		String html = getContent( fileCorrente );
		return html;
	}
	
	public static String readFileOwner(File file) { 
		if(file!=null&&file.exists()){
			String cmd = "stat -c %U "+file.getAbsolutePath(); 
			Runtime run = Runtime.getRuntime(); 
			Process pr = null; 
			try { pr = run.exec(cmd); } 
			catch (IOException e) { e.printStackTrace(); } 
			try { pr.waitFor(); } 
			catch (InterruptedException e) { e.printStackTrace(); } 
			BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream())); 
			String line = ""; 
			StringBuilder sb=new StringBuilder();
			try { 
				while ((line=buf.readLine())!=null) { 
					sb.append(line); 
				} 
			} catch (IOException e) { e.printStackTrace(); } 
			return sb.toString();
		}
		return "";
		}	

	private static String readFile( File file ) throws IOException {

		BufferedInputStream bufferedStream = new BufferedInputStream( new FileInputStream( file ), DEFAULT_BUFFER_SIZE );
		CharsetDetector detector = new CharsetDetector();
		detector.setText( bufferedStream );
		CharsetMatch match = detector.detect();
		if ( match != null ) {
			Reader encoderReader = match.getReader();
			CharBuffer charBuffer = CharBuffer.allocate( DEFAULT_BUFFER_SIZE );
			StringWriter stringWriter = new StringWriter();
			int read = 0;
			do {
				read = encoderReader.read( charBuffer );
				if ( read > 0 ) {
					charBuffer.flip();
					stringWriter.write( charBuffer.toString() );
					charBuffer.clear();
				}
			} while ( read > 0 );

			return stringWriter.toString();
		} else {
			throw new CharConversionException( "Unknow charset for file:" + file.getAbsolutePath() );
		}
	}

	private static String getContent(File file ) {
		CharsetDecoder isoDecoder2 = Charset.forName( DEFAULT_ENCODING ).newDecoder();
		String ext =  getFileExtension(file);
		String testHtml = "";
		try {
			if( ext!= null && ext.equalsIgnoreCase("txt") ){
				StringBuilder contents = new StringBuilder();
				Scanner scanner = new Scanner(file, DEFAULT_ENCODING );


				String line = null; //not declared within while loop
				try {
					while (scanner.hasNextLine()){
						line = scanner.nextLine();

						if( !line.trim().equalsIgnoreCase("") ){
							line =  "<p>"+line+"</p>";
						} 
						contents.append(line + System.getProperty("line.separator") );
					}
				}
				finally{
					scanner.close();
				}

				testHtml = contents.toString(); 
			} else if( ext!= null && ext.equalsIgnoreCase("html") 
					|| ext.equalsIgnoreCase("htm") ){
				
				testHtml = getContentAsString( file );
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage() , e );

		} 
		return testHtml;
	}


	private static String getContentAsString(File file ) {
		String  contents = null;

		try {
			String testo =  readFile( file   );
			if( testo.contains(START_BODY ) && testo.contains(END_BODY )){
				contents = org.apache.commons.lang.StringUtils.substringBetween(testo, START_BODY , END_BODY );
			}else {
				contents = testo;
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage() , e );

		}  
		return contents;
	}

	private static String getContentAsString(File file , String encoding ) {
		String  contents = null;

		try {
			String testo = FileUtils.readFileToString( file , encoding );
			contents = org.apache.commons.lang.StringUtils.substringBetween( testo, START_BODY , END_BODY  );

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage() , e );

		}  
		return contents;
	}
}
