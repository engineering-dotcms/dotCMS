package it.bankit.website.util;

import java.util.Comparator;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;

public abstract class AbstractFolderComparator implements Comparator<Folder> {

	public String getFolderKeyLanguage( Folder folder ){
		String keyAnno1 = "";
		try{
			Identifier id = APILocator.getIdentifierAPI().loadFromCache(folder.getIdentifier() );
			String uri = id.getURI();
			String conString = convertPath( uri );
			 
			Language language = APILocator.getLanguageAPI().getDefaultLanguage();
			keyAnno1 = APILocator.getLanguageAPI().getStringKey( language, conString );
		}catch (Exception e) {
			 
		}
		return keyAnno1;
	}
	
	public String convertPath(String path) {
		try {
			path = path.replace(System.getProperty("file.separator") , ".").replace(" ", "-");
			if (path.startsWith(".")) {
				path = path.substring(1);
			}
			if (path.endsWith(".")) {
				path = path.substring(0, path.lastIndexOf("."));
			}
		} catch (Exception e) {
			 
		}
		return path;
	}
}
