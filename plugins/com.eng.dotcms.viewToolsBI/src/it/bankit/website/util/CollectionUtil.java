package it.bankit.website.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.dotmarketing.viewtools.content.ContentMap;

public class CollectionUtil {
	public static List<ContentMap> sort( Collection<ContentMap> contentlets, String orderFieldsAndType) {
		return sort(contentlets,orderFieldsAndType,false);
	}
	
	public static List<ContentMap> sort( Collection<ContentMap> contentlets, String orderFieldsAndType, boolean removeNull ) {
		List<ContentMap> result = new LinkedList<ContentMap>( contentlets );
		String[] filedsSplit = orderFieldsAndType.split( "," );
		String[] processedOrder = new String[filedsSplit.length];
		int i = 0;
		for ( String curFieldAndType : filedsSplit ) {
			processedOrder[i++] = curFieldAndType.trim();
		}
		if ( removeNull ) {
			for ( ContentMap curContent : contentlets ) {
				for(String curFiled:processedOrder){
					curFiled=curFiled.substring( 0,curFiled.indexOf( ':' ) );
					if ( curContent.get( curFiled ) == null ) {
						result.remove( curContent );
						break;
					}
				}
			}
		}
		Collections.sort( result, new ContentMapComparator( processedOrder ) );
		return result;
	}

}
