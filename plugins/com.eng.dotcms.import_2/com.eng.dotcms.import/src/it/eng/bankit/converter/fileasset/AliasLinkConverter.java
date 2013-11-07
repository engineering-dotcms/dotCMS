package it.eng.bankit.converter.fileasset;

import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.ImportUtil;

import com.dotmarketing.util.UtilMethods;

public class AliasLinkConverter extends LinkConverter {

	
 	public AliasLinkConverter(HmiStructure struttura) {		
		super(struttura);
		isAlias = true;
	}

	protected String convertInternalPath ( String path ){
		LOG.debug("convertInternalPath " + path );
		String fileName = getHmiStructure().getFilePath();
		if( UtilMethods.isSet( fileName ) ){
			if( fileName.endsWith(".hmi") ){
				fileName = fileName.substring(fileName.lastIndexOf("/") +1 , fileName.lastIndexOf(".hmi"))  ;
			}
		}
		LOG.debug("NOME FILE DA CERCARE  " + fileName  );
		String newFilePath = ImportUtil.getFileAlias(fileName);	
		LOG.debug("PATH ALIAS  " + newFilePath );
		newFilePath = newFilePath + fileName;
		return newFilePath; 
	}


}
