package it.eng.bankit.comparator;

import it.eng.bankit.bean.HmiStructure;

import java.util.Comparator;

import com.dotmarketing.util.UtilMethods;

public class ComparatorFactory {


	private static ComparatorFactory factory;

	public static ComparatorFactory getInstance(){
		if( factory == null ){
			factory = new ComparatorFactory();
		}
		return factory;
	}

	public Comparator<HmiStructure> getStructureComparator(String sort  , String langCode ){
		Comparator<HmiStructure>  compar = null;
		if( UtilMethods.isSet( sort ) ){
			if(sort != null && sort.equalsIgnoreCase("T") ){			
 				compar = new StructureSortNameComparator( langCode );
			}else  if( sort != null ){
				compar = new StructureSortSequenceComparator();
			}else if(sort != null && sort.equalsIgnoreCase("-T") ){
 				compar = new StructureSortNameComparator( langCode , false );
			}else if(sort != null && sort.equalsIgnoreCase("#") ){
				compar = new StructureSortSequenceComparator();
			} else if(sort != null && sort.equalsIgnoreCase("-#") ){
				compar = new StructureSortSequenceComparator( false ) ;
			} 
		}else {
			compar = new StructureSortSequenceComparator() ;
		}
		return compar;
	}
}
