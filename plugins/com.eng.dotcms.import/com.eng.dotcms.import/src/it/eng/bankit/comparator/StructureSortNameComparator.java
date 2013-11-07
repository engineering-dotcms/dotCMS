package it.eng.bankit.comparator;

import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.dotmarketing.util.UtilMethods;

public class StructureSortNameComparator implements Comparator<HmiStructure> {

	private String languageCode = null;
	
	private boolean descOrder = true;
	
	public StructureSortNameComparator( String langCode ){
		this.languageCode = langCode;
		descOrder = true;
	}
	
	public StructureSortNameComparator( String langCode , boolean desc ){
		this.languageCode = langCode;
		descOrder = desc;		
	}
	
	@Override
	public int compare(HmiStructure struct1, HmiStructure o2) {
		String sequ1 = struct1.getPropertyHmi(HyperwaveKey.Title+":"+languageCode  );
		String sequ2 = o2.getPropertyHmi(HyperwaveKey.Title+":"+languageCode  );
		
		if( !UtilMethods.isSet( sequ1 )  && !UtilMethods.isSet(sequ2 ) ){
			return 0 ;
		}
		
		List<String> array = new ArrayList<String>();
		
		array.add( sequ1 );
		array.add( sequ2 );

		if( !UtilMethods.isSet(sequ1 )  && UtilMethods.isSet(sequ2 ) ){
			return -1 ;
		}
		if( UtilMethods.isSet(sequ1 )  && !UtilMethods.isSet(sequ2 ) ){
			return 1 ;
		}
		
		if( descOrder  ){
			return sequ1.compareTo( sequ2 ) ;
		}else{
			int ret = sequ1.compareTo( sequ2 );
			return ( ret == 0 ? 0 : ( ret == 1 ? -1 :1 ) );
		}
		
 		//return sequ1.compareTo( sequ2 ) ;
	}

}
