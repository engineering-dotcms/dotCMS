package it.eng.bankit.filereader;

import it.eng.bankit.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class HWHtmlReader   {

	private static String START_BOBY = "<body>";
	private static String END_BOBY = "</body>";
	//	private static String DEFAULT_ENCODING = "UTF-8";
	private static String DEFAULT_ENCODING = "Windows-1252";
	private static Logger LOGGER = Logger.getLogger(HWHtmlReader.class );
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	public static String getContent(File file ) {
		CharsetDecoder isoDecoder2 = Charset.forName( DEFAULT_ENCODING ).newDecoder();
		String ext = FileUtil.getFileExtension(file);
		String testHtml = "";
		try {
			if( ext!= null && ext.equalsIgnoreCase("txt") ){
				//	testHtml = FileUtils.readFileToString(file);

				//...checks on aFile are elided
				StringBuilder contents = new StringBuilder();
				//				BufferedReader input =  null;
				//				ByteBuffer byteBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
				// 				input =  new BufferedReader(new FileReader(file));
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
 			} else if( ext!= null && ( ext.equalsIgnoreCase("html") 
					|| ext.equalsIgnoreCase("htm")) ){
				testHtml =   getContentAsString(file);
			}

		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error( e );
		} 
		return testHtml;
	}


	public static String getContentAsString(File file ) {
		String  contents = null;

		try {
			String testo = FileUtil.readFile( file   );
			if( testo.contains(START_BOBY ) && testo.contains(END_BOBY )){
			contents = org.apache.commons.lang.StringUtils.substringBetween(testo, START_BOBY , END_BOBY );
			}else {
				contents = testo;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}  
		return contents;
	}

	public static String getContentAsString(File file , String encoding ) {
		String  contents = null;

		try {
			String testo = FileUtils.readFileToString( file , encoding );
			contents = org.apache.commons.lang.StringUtils.substringBetween( testo, START_BOBY , END_BOBY  );

		} catch (IOException e) {
			e.printStackTrace();
		}  
		return contents;
	}


}
