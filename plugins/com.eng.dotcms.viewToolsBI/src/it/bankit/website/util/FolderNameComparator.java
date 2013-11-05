package it.bankit.website.util;

import java.util.Comparator;

import com.dotmarketing.portlets.folders.model.Folder;

public class FolderNameComparator extends AbstractFolderComparator implements Comparator<Folder> {

	@Override
	public int compare(Folder fold1, Folder fold2) {
  	 	 		 	
		String keyAnno1 = getFolderKeyLanguage( fold1 );
		String keyAnno2 =  getFolderKeyLanguage( fold2 );		 
		return keyAnno1.compareTo(keyAnno2);
	}
 
}
