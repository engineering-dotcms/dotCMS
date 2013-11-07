package it.eng.bankit.filereader;

import it.eng.bankit.util.ImportConfigKey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.CharacterCodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HmiFileReader {

	private static Map<String, String> properties; 
 

	public static final Map<String, String> processFile(File fFile ) throws FileNotFoundException, UnsupportedEncodingException, CharacterCodingException {
		properties = new HashMap<String, String>();
 		Scanner scanner = new Scanner(fFile,"UTF-8");
 		try {
			while ( scanner.hasNextLine() ){
				processLine( scanner.nextLine() );
			}
			return properties;
		}
		finally {
			scanner.close();
		}
	}


	private static void processLine(String aLine){
		int indexUguale = aLine.indexOf("=");
		if( aLine.indexOf("=") != -1  ){
			
			String key = aLine.substring(0 , indexUguale );
			String value = aLine.substring(indexUguale + 1 );
 			if( value.startsWith( ImportConfigKey.ENGLISH_LANGUAGE+ ":")  ) {
				key= key + ":"+ImportConfigKey.ENGLISH_LANGUAGE;
				value = value.substring(3);
			}
			else if(  value.startsWith(ImportConfigKey.ITALIAN_LANGUAGE +":")  ) {
				key= key+":"+ImportConfigKey.ITALIAN_LANGUAGE;
				value = value.substring(3);
			}
			properties.put(key, value);
		}
	}

}
