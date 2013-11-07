package it.eng.bankit.util;

import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.structure.model.Field;

public class StructureUtil {

	public static Field findHostOrFolderField( String structureInode  ) {
		Field hostOrFolderField=null;
		for (Field curField : FieldsCache.getFieldsByStructureInode(structureInode)) {
			if (curField.getFieldType().equals(
					Field.FieldType.HOST_OR_FOLDER.toString())) {
				hostOrFolderField = curField;
				break;
			}
		}
		return hostOrFolderField;
	}
}
