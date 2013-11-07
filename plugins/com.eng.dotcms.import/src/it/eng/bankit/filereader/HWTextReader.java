package it.eng.bankit.filereader;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class HWTextReader       {
	
	public static String getContentAsString(File file ) {
		String  contents = null;

		try {
			contents = FileUtils.readFileToString( file );
			
		} catch (IOException e) {
			e.printStackTrace();
		}  
		return contents;
	}

}
