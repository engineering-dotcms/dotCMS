package it.bankit.website.util;

import java.util.Comparator;

import com.dotmarketing.portlets.folders.model.Folder;

public class FolderAnnoComparator extends AbstractFolderComparator implements Comparator<Folder> {

	@Override
	public int compare(Folder fold1, Folder fold2) {

		String anno1 = fold1.getName();
		String anno2 = fold2.getName();

		String keyAnno1 = getFolderKeyLanguage(fold1).trim();
		String keyAnno2 = getFolderKeyLanguage(fold2).trim();
		Integer anno1Int = null;
		Integer anno2Int = null;
		try {
			anno1Int = Integer.parseInt(keyAnno1);
		} catch (Exception e) {
			anno1Int = 0;
		}
		try {
			anno2Int = Integer.parseInt(keyAnno2);
		} catch (Exception e) {
			anno2Int = 0;
		}
		if (anno1Int < anno2Int)
			return 1;
		if (anno1Int < anno2Int)
			return -1;
		return 0;
	}

}
