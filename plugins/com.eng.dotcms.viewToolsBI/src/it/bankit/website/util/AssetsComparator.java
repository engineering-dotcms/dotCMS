package it.bankit.website.util;

import java.util.Comparator;

import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.viewtools.content.ContentMap;

/**
 * @author will
 * 
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public class AssetsComparator implements Comparator {

	private int direction = 1;
	private String sort = StructureCache.getStructureByVelocityVarName("Link").getFieldVar("sortOrder1").getFieldContentlet();

	public AssetsComparator(int direction) {
		this.direction = direction;
	}

	public AssetsComparator() {
	}

	public int compare(Object o1, Object o2) {

		if ((o1 instanceof Contentlet) && (o2 instanceof Contentlet)) {

			if (o1 instanceof Contentlet && o2 instanceof Contentlet) {

				int sortValue1 = Integer.parseInt(((String) ((Contentlet) o1).getStringProperty("sortOrder1")));
				int sortValue2 = Integer.parseInt(((String) ((Contentlet) o2).getStringProperty("sortOrder1")));

				if ((int) sortValue1 < (int) sortValue2)
					return -1 * direction;
				if ((int) sortValue1 == (int) sortValue2)
					return 0;
				if ((int) sortValue1 > (int) sortValue2)
					return 1 * direction;

				return -1;

			} else {

				if (((WebAsset) o1).getSortOrder() < ((WebAsset) o2).getSortOrder())
					return -1 * direction;
				if (((WebAsset) o1).getSortOrder() == ((WebAsset) o2).getSortOrder())
					return 0;
				if (((WebAsset) o1).getSortOrder() > ((WebAsset) o2).getSortOrder())
					return 1 * direction;

				return -1;

			}
		} else if ((o1 instanceof Contentlet) && (o2 instanceof Folder)) {

			if (o1 instanceof Contentlet) {

				long sortValue = Integer.parseInt(((String)((Contentlet) o1).getStringProperty("sortOrder1")));

				if ((int) sortValue < ((Folder) o2).getSortOrder())
					return -1 * direction;
				if ((int) sortValue == ((Folder) o2).getSortOrder())
					return 0;
				if ((int) sortValue > ((Folder) o2).getSortOrder())
					return 1 * direction;

				return -1;

			} else {

				if (((WebAsset) o1).getSortOrder() < ((Folder) o2).getSortOrder())
					return -1 * direction;
				if (((WebAsset) o1).getSortOrder() == ((Folder) o2).getSortOrder())
					return 0;
				if (((WebAsset) o1).getSortOrder() > ((Folder) o2).getSortOrder())
					return 1 * direction;

				return -1;

			}

		} else if ((o1 instanceof Folder) && (o2 instanceof Contentlet)) {

			if (o2 instanceof Contentlet) {

				int sortValue = Integer.parseInt(((String)((Contentlet) o2).getStringProperty("sortOrder1")));

				if (((Folder) o1).getSortOrder() < (int) sortValue)
					return -1 * direction;
				if (((Folder) o1).getSortOrder() == (int) sortValue)
					return 0;
				if (((Folder) o1).getSortOrder() > (int) sortValue)
					return 1 * direction;

				return -1;

			} else {

				if (((Folder) o1).getSortOrder() < ((WebAsset) o2).getSortOrder())
					return -1 * direction;
				if (((Folder) o1).getSortOrder() == ((WebAsset) o2).getSortOrder())
					return 0;
				if (((Folder) o1).getSortOrder() > ((WebAsset) o2).getSortOrder())
					return 1 * direction;

				return -1;
			}

		} else if ((o1 instanceof Folder) && (o2 instanceof Folder)) {

			if (((Folder) o1).getSortOrder() < ((Folder) o2).getSortOrder())
				return -1 * direction;
			if (((Folder) o1).getSortOrder() == ((Folder) o2).getSortOrder())
				return 0;
			if (((Folder) o1).getSortOrder() > ((Folder) o2).getSortOrder())
				return 1 * direction;

			return -1;
		} else if ((o1 instanceof FileAsset) && (o2 instanceof FileAsset)) {

			if (((FileAsset) o1).getSortOrder() < ((FileAsset) o2).getSortOrder())
				return -1 * direction;
			if (((FileAsset) o1).getSortOrder() == ((FileAsset) o2).getSortOrder())
				return 0;
			if (((FileAsset) o1).getSortOrder() > ((FileAsset) o2).getSortOrder())
				return 1 * direction;

			return -1;
		} else if ((o1 instanceof FileAsset) && (o2 instanceof Folder)) {

			if (((FileAsset) o1).getSortOrder() < ((Folder) o2).getSortOrder())
				return -1 * direction;
			if (((FileAsset) o1).getSortOrder() == ((Folder) o2).getSortOrder())
				return 0;
			if (((FileAsset) o1).getSortOrder() > ((Folder) o2).getSortOrder())
				return 1 * direction;

			return -1;

		} else if ((o1 instanceof Folder) && (o2 instanceof FileAsset)) {

			if (((Folder) o1).getSortOrder() < ((FileAsset) o2).getSortOrder())
				return -1 * direction;
			if (((Folder) o1).getSortOrder() == ((FileAsset) o2).getSortOrder())
				return 0;
			if (((Folder) o1).getSortOrder() > ((FileAsset) o2).getSortOrder())
				return 1 * direction;

			return -1;
		} else if ((o1 instanceof FileAsset) && (o2 instanceof WebAsset)) {

			if (o2 instanceof Contentlet) {

				int sortValue = Integer.parseInt(((String)((Contentlet) o2).getStringProperty("sortOrder1")));

				if (((FileAsset) o1).getSortOrder() < (int) sortValue)
					return -1 * direction;
				if (((FileAsset) o1).getSortOrder() == (int) sortValue)
					return 0;
				if (((FileAsset) o1).getSortOrder() > (int) sortValue)
					return 1 * direction;

				return -1;
			}

			else {

				if (((FileAsset) o1).getSortOrder() < ((WebAsset) o2).getSortOrder())
					return -1 * direction;
				if (((FileAsset) o1).getSortOrder() == ((WebAsset) o2).getSortOrder())
					return 0;
				if (((FileAsset) o1).getSortOrder() > ((WebAsset) o2).getSortOrder())
					return 1 * direction;

				return -1;
			}

		} else if ((o1 instanceof WebAsset) && (o2 instanceof FileAsset)) {

			if (o1 instanceof Contentlet) {

				int sortValue = Integer.parseInt(((String) ((Contentlet) o1).getStringProperty("sort")));

				if ((int) sortValue < ((FileAsset) o2).getSortOrder())
					return -1 * direction;
				if ((int) sortValue == ((FileAsset) o2).getSortOrder())
					return 0;
				if ((int) sortValue > ((FileAsset) o2).getSortOrder())
					return 1 * direction;

				return -1;
			}

			else {

				if (((WebAsset) o1).getSortOrder() < ((FileAsset) o2).getSortOrder())
					return -1 * direction;
				if (((WebAsset) o1).getSortOrder() == ((FileAsset) o2).getSortOrder())
					return 0;
				if (((WebAsset) o1).getSortOrder() > ((FileAsset) o2).getSortOrder())
					return 1 * direction;

				return -1;
			}
		} else if ((o1 instanceof Folder) && (o2 instanceof ContentMap)) {
			int ContentSortOrder = Integer.parseInt((String) ((ContentMap) o2).get("sortOrder1"));

			if (((Folder) o1).getSortOrder() < ContentSortOrder)
				return -1 * direction;
			if (((Folder) o1).getSortOrder() == ContentSortOrder)
				return 0;
			if (((Folder) o1).getSortOrder() > ContentSortOrder)
				return 1 * direction;

			return -1;

		} else if ((o1 instanceof ContentMap) && (o2 instanceof Folder)) {

			long contentSortOrder = ((Long) ((ContentMap) o1).get("sortOrder1"));

			if (contentSortOrder < ((Folder) o2).getSortOrder())
				return -1 * direction;
			if (contentSortOrder == ((Folder) o2).getSortOrder())
				return 0;
			if (contentSortOrder > ((Folder) o2).getSortOrder())
				return 1 * direction;

			return -1;
		} else if ((o1 instanceof ContentMap) && (o2 instanceof ContentMap)) {
			Object s1 = ((ContentMap) o1).get("sortOrder1");
			Object s2 = ((ContentMap) o2).get("sortOrder1");
			int contentSortOrder = 0;
			int contentSortOrder2 = 0;
			try {
				contentSortOrder = Integer.parseInt(s1.toString());
				contentSortOrder2 = Integer.parseInt(s2.toString());
			} catch (Exception e) {
			}
			if (contentSortOrder < contentSortOrder2)
				return -1 * direction;
			if (contentSortOrder == contentSortOrder2)
				return 0;
			if (contentSortOrder > contentSortOrder2)
				return 1 * direction;

			return -1;
		} else {

			return -1;
		}

	}

}
