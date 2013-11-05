package it.bankit.website.util;

import java.util.Comparator;
import java.util.Date;

import com.dotmarketing.util.Logger;
import com.dotmarketing.viewtools.content.ContentMap;

public class ContentMapComparator implements Comparator<ContentMap> {

	private String[] orderFields;

	public ContentMapComparator( String... orderFields ) {
		this.orderFields = orderFields;
	}

	@Override
	public int compare( ContentMap cm0, ContentMap cm1 ) {
		int ret = 0;
		if ( orderFields != null && orderFields.length > 0 ) {
			int i = 0;
			while ( ret == 0 && i < orderFields.length ) {
				String[] splitOrder = orderFields[i++].split( ":" );
				String curFieldName = splitOrder[0];
				String orderType = ( splitOrder.length > 1 ? splitOrder[1] : "asc" );
				try {
					Object value1 = cm0.get( curFieldName );
					Object value2 = cm1.get( curFieldName );
					ret = compareValues( value1, value2 );
					if ( orderType.equals( "desc" ) ) {
						ret *= -1;
					}
				} catch ( Exception e ) {
					Logger.error( ContentMapComparator.class, "Error comparing " + cm0 + " with " + cm1 );
				}
			}
		}
		if ( Logger.isDebugEnabled( ContentMapComparator.class ) ) {
			StringBuilder sb=new StringBuilder();
			sb.append( "Comparing " );
			sb.append(cm0.getContentletsTitle());
			sb.append( " with ");
			sb.append( cm1.getContentletsTitle());
			sb.append( " on ");
			sb.append( " with ");
			sb.append( " with ");
			for(String curField:orderFields){
				sb.append(curField);
				sb.append(',');
			}
			sb.append( " primo");
			if(ret==0){
				sb.append('=');
			}else if(ret==-1){
				sb.append('<');
			}else{
				sb.append('>');
			}
			sb.append( "secondo");
			Logger.debug( ContentMapComparator.class, sb.toString() );
		}
		return ret;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private int compareValues( Object value1, Object value2 ) {
		int ret = 0;
		if ( value1 == null || value2 == null ) {
			if ( value1 != null && value2 == null ) {
				ret = -1;
			} else if ( value1 == null && value2 != null ) {
				ret = +1;
			}
		} else if ( value1 instanceof Integer ) {
			ret = ( (Integer) value1 ).compareTo( (Integer) value2 );
		} else if ( value1 instanceof Long ) {
			ret = ( (Long) value1 ).compareTo( (Long) value2 );
		} else if ( value1 instanceof Date ) {
			ret = ( (Date) value1 ).compareTo( (Date) value2 );
		} else if ( value1 instanceof String ) {
			ret = ( (String) value1 ).compareTo( (String) value2 );
		} else if ( value1 instanceof Float ) {
			ret = ( (Float) value1 ).compareTo( (Float) value2 );
		} else if ( value1 instanceof Boolean ) {
			ret = ( (Boolean) value1 ).compareTo( (Boolean) value2 );
		} else if ( value1 instanceof Comparable ) {
			ret = ( (Comparable) value1 ).compareTo( (Comparable) value2 );
		}
		return ret;
	}
}