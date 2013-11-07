package it.eng.bankit.filereader;

import it.eng.bankit.util.FileUtil;

import java.io.File;

public class FileReaderFactory {


	private static FileReaderFactory factory;

	public static FileReaderFactory getInstance(){
		if( factory == null ){
			factory = new FileReaderFactory();
		}
		return factory;
	}

	public AbstractHWFileReader getFileReader( File file ){

		if( file == null )
		{
			return null;
		}else {
			String exten = FileUtil.getFileExtension(file);

//			if( exten != null && exten.equalsIgnoreCase("txt")){
//				return new HWTextReader(  ) ;
//			}else if( exten != null && exten.equalsIgnoreCase("xml")){
//				return new HWXmlReader(file);
//			} 
		}
		return null;
	}
}
