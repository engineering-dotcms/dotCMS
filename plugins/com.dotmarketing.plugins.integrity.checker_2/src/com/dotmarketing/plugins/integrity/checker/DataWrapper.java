package com.dotmarketing.plugins.integrity.checker;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;

@SuppressWarnings("serial")
public class DataWrapper implements  Serializable {
	private String pluginId = "com.dotmarketing.plugins.integrity.checker";
	
	private Map<String, Object> orderedMap; 
	private List<byte[]> assets;
	private Set<String> excludedProperties;
	private String excludedString = null;
			
	
	public DataWrapper(Contentlet content) {
		excludedProperties = new HashSet<String>();
		try {
			excludedString = APILocator.getPluginAPI().loadProperty(pluginId, "EXCLUDED_CONTENT_PROPERTIES");
		} catch (DotDataException e) {
			e.printStackTrace();
		}
		if(excludedString != null && !excludedString.trim().equals("")) {
			String properties [] = excludedString.split(",") ;
			for (String property : properties) {
				excludedProperties.add(property);
			}
		}
		initOrderedMap(content);
	}
	
	private void initOrderedMap(Contentlet content) {
		Map<String, Object> tempMaps = content.getMap();
		orderedMap = new TreeMap<String, Object>();
		assets = new ArrayList<byte[]>();
		
		for(String chiave:tempMaps.keySet()) {
			Object value = tempMaps.get(chiave);
			if(!(value instanceof Date) && !excludedProperties.contains(chiave) )
				orderedMap.put(chiave, value);
		}
		
		List<Field> fields=FieldsCache.getFieldsByStructureInode(content.getStructureInode());
		for(Field ff : fields) {
			if(ff.getFieldType().toString().equals(Field.FieldType.BINARY.toString())) {
				File sourceFile;
				try {
					sourceFile = content.getBinary( ff.getVelocityVarName());
				
					if(sourceFile != null && sourceFile.exists()) {
						 byte[] bytes=FileUtils.readFileToByteArray(sourceFile);
						 assets.add(bytes);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

		    }
		}
	}

	public Map<String, Object> getOrderedMap() {
		return orderedMap;
	}
}
