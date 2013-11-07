package com.eng.dotcms.contentlet.whatschanged.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;

public class WhatsChangedUtil {
	
	public static List<Contentlet> getSelectedVersions(List<Contentlet> versions, String inodes) {
		List<Contentlet> result = new ArrayList<Contentlet>();
		for(String inode : splitInodes(inodes)){
			Contentlet tmp = getSelectedVersion(versions,inode);
			if(null!=tmp) result.add(tmp);
		}
		return result;
	}
	
	public static List<Field> getTextFields(List<Field> structureFields) {
		List<Field> textStructureFields = new ArrayList<Field>();
		for(Field f:structureFields){
			if(f.getMap().get("fieldFieldType").equals(FieldType.WYSIWYG.toString()) 
					|| f.getMap().get("fieldFieldType").equals(FieldType.TEXT_AREA.toString())
					|| f.getMap().get("fieldFieldType").equals(FieldType.TEXT.toString())){
				textStructureFields.add(f);
			}
		}
		return textStructureFields;
	}
	
	private static Contentlet getSelectedVersion(List<Contentlet> versions, String inode){		
		for(Contentlet c : versions){
			if(c.getInode().equals(inode))
				return c;
		}
		return null;
	}
	
	private static List<String> splitInodes(String inodes){
		return Arrays.asList(inodes.split("[|]"));
	}
}
