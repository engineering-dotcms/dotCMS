package com.eng.dotcms.additions.util;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;

public class SortUtil {
	private static Logger LOG = Logger.getLogger(SortUtil.class);
	private static String HW_SORT_ORDER_EXPRESION = "^(#|A|C|D|E|O|S|t|T|P|-|\\.)*$";
	private static String DEFAULT_SORT_DIRECTION = "asc";
	private static String INVERSE_SORT_DIRECTION = "desc";
	private static String[] titleMapping = { "titolo", "title" };
	private static String[] sequenceMapping = { "sequenza", "sortOrder1" };
	private static String[] dataEmanazioneMapping = { "dataEmanazione" };
	private static String[] dataCreazioneMapping = { "timeCreated" };
	private static String[] dataPubblicazioneMapping = { "dataPubblicazione" };
	
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
	
	private String generateInternalSortOrder( String hwSortOrder, Collection<Structure> structures ) {
		StringBuilder sortOrder = new StringBuilder();
		String internalSortOrder = ( UtilMethods.isSet( hwSortOrder ) ? hwSortOrder : "-D#T" );
		if ( internalSortOrder.matches( HW_SORT_ORDER_EXPRESION ) ) {
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
		} else {
			LOG.error( "Invalid Hyperwave Sort order format" );
		}
		return sortOrder.toString();
	}
	
	private String translateToLucene( String internalSortOrder ) {
		return internalSortOrder.replace( ':', ' ' );
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
