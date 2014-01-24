package it.bankit.website.viewtool.list;

import it.bankit.website.util.CollectionUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.content.ContentMap;

public class SortUtil {
	private static Logger LOG = Logger.getLogger( SortUtil.class );
	private static String HW_SORT_ORDER_EXPRESION = "^(#|A|C|D|E|O|S|t|T|P|-|\\.)*$";
	private static String DEFAULT_SORT_DIRECTION = "asc";
	private static String INVERSE_SORT_DIRECTION = "desc";
	private static String[] titleMapping = { "titolo", "title" };
	private static String[] sequenceMapping = { "sequenza", "sortOrder1" };
	private static String[] dataEmanazioneMapping = { "dataEmanazione" };
	private static String[] dataCreazioneMapping = { "timeCreated" };
	private static String[] dataPubblicazioneMapping = { "dataPubblicazione" };

	public Collection<ContentMap> sort( Collection<ContentMap> contentlets, String orderFieldsAndType ) {
		return sort(contentlets,orderFieldsAndType,false);
	}
	// Esempio orderFieldAndType = titolo:desc,sequence:asc
	public Collection<ContentMap> sort( Collection<ContentMap> contentlets, String orderFieldsAndType,boolean removeNull ) {
		if ( UtilMethods.isSet( orderFieldsAndType ) ) {
			try {
				return CollectionUtil.sort( contentlets, orderFieldsAndType, removeNull );
			} catch ( Exception e ) {
				LOG.error( "Error in sorting collection by " + orderFieldsAndType, e );
			}
		} else {
			LOG.error( "No Sort orderFieldsAndType found" );
		}
		return null;
	}

	public String generateSortOrder( String hwSortOrder, String... structureNames ) {
		return removeStructureName( generateInternalSortOrder( hwSortOrder, findStructures( structureNames ) ) );
	}

	public String generateLuceneSortOrder( String hwSortOrder, String... structureNames ) {
		return translateToLucene( generateInternalSortOrder( hwSortOrder, findStructures( structureNames ) ) );
	}

	private Collection<Structure> findStructures( String[] structureNames ) {
		Collection<Structure> structures = new LinkedList<Structure>();
		for ( String curStructureName : structureNames ) {
			Structure curStructure = StructureCache.getStructureByVelocityVarName( curStructureName );
			if ( curStructure != null && curStructure.getInode() != null ) {
				structures.add( curStructure );
			}
		}
		return structures;
	}

	private String removeStructureName( String internalSortOrder ) {
		int dotIndex = 0;
		int commaIndex = 0;
		String internalString = internalSortOrder;
		do {// Rimuovo tutte le strutture
			dotIndex = internalString.indexOf( '.', dotIndex );
			if ( dotIndex > 0 ) {
				commaIndex = (internalString.lastIndexOf( ',', dotIndex )>0?internalString.lastIndexOf( ',', dotIndex )+1:0);
				String curStructureName=internalString.substring( commaIndex,dotIndex+1 );
				internalString=internalString.replace( curStructureName, "" );
			}
		} while ( dotIndex > 0 );
		// controllo che non ci siano campi duplicati
		String[] split = internalString.split( "," );
		Map<String, String> mapField = new HashMap<String, String>( split.length );
		for ( String curField : split ) {
			String[] fieldSplit = curField.split( ":" );
			String fieldName = fieldSplit[0];
			String direction = ( fieldSplit.length == 2 ? fieldSplit[1] : null );
			if ( mapField.containsKey( fieldName ) ) {
				internalString=internalString.replace( curField, "" );
				 if(internalString.startsWith( "," )){
						internalString=curField+internalString;
					}else if(internalString.contains( ",," )){
					internalString=internalString.replaceFirst( ",,", ","+curField+"," );
					
				}else if(internalString.endsWith( "," )){
					internalString=internalString+curField;
				}
				
			} else {
				mapField.put( fieldName, direction );
			}
		}
		while(internalString.contains( ",," )){
			internalString=internalString.replace( ",,", ",");
		}
		if(internalString.endsWith( "," )){
			internalString=internalString.substring( 0,internalString.length()-1 );
		}
		return internalString;
	}

	private String translateToLucene( String internalSortOrder ) {
		return internalSortOrder.replace( ':', ' ' );
	}

	private String generateInternalSortOrder( String hwSortOrder, Collection<Structure> structures ) {
		StringBuilder sortOrder = new StringBuilder();
		String internalSortOrder = ( UtilMethods.isSet( hwSortOrder ) ? hwSortOrder : "-D#T" );
		if(!internalSortOrder.matches( HW_SORT_ORDER_EXPRESION )){
			LOG.warn(internalSortOrder+ " non corretto: setto al valore di default (-D#T)");
			internalSortOrder = "-D#T";
		}
		String direction = DEFAULT_SORT_DIRECTION;
		for ( char curChar : internalSortOrder.toCharArray() ) {
			if ( curChar == '-' ) {
				direction = INVERSE_SORT_DIRECTION;
				continue;
			}
			if ( sortOrder.length() > 0 ) {
				sortOrder.append( ',' );
			}
			String[] curMapping = null;
			switch ( curChar ) {
			case 'T':
				curMapping = titleMapping;
				break;
			case '#':
				curMapping = sequenceMapping;
				break;
			case 'D':
				curMapping = dataEmanazioneMapping;
				break;
			case 'C':
				curMapping = dataCreazioneMapping;
				break;
			case 'P':
				curMapping = dataPubblicazioneMapping;
				break;
			default:
				LOG.warn( "Unsupported Order Type Format:" + curChar );
			}
			sortOrder.append( internalMappingOrder( structures, curMapping, direction ) );
			direction = DEFAULT_SORT_DIRECTION;// return to default
		}
		return sortOrder.toString();
	}

	private String internalMappingOrder( Collection<Structure> structures, String[] mapping, String direction ) {
		StringBuilder order = new StringBuilder();
		for ( Structure curStructure : structures ) {
			if ( structures.size() > 1 && order.length() > 0 ) {
				order.append( ',' );
			}
			String fieldName = null;
			int i = 0;
			while ( fieldName == null && i < mapping.length ) {
				Field field = curStructure.getFieldVar( mapping[i] );
				if ( field != null ) {
					fieldName = mapping[i];
					order.append( curStructure.getVelocityVarName() );
					order.append( '.' );
					order.append( fieldName );
					order.append( ':' );
					order.append( direction );
				}
				i++;
			}
		}
		return order.toString();
	}
}
