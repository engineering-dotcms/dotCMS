package it.eng.bankit.comparator;

import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.util.HyperwaveKey;

import java.util.Comparator;

import org.apache.log4j.Logger;

import com.dotmarketing.util.UtilMethods;

public class StructureSortSequenceComparator implements Comparator<HmiStructure> {
	protected Logger LOG = Logger.getLogger( this.getClass().getName()  );
	private boolean descOrder = true;
	
	public StructureSortSequenceComparator(){
 	}
	
	public StructureSortSequenceComparator( boolean desc ){
		this.descOrder = desc;		
	}
	
	
	@Override
	public int compare(HmiStructure o1, HmiStructure o2) {
		String sequ1 = o1.getPropertyHmi(HyperwaveKey.Sequence );
		String sequ2 = o2.getPropertyHmi(HyperwaveKey.Sequence );
		if( !UtilMethods.isSet(sequ1 )  && !UtilMethods.isSet(sequ2 ) ){
			return 0 ;
		}
		if( !UtilMethods.isSet(sequ1 )  && UtilMethods.isSet(sequ2 ) ){
			return -1 ;
		}
		if( UtilMethods.isSet(sequ1 )  && !UtilMethods.isSet(sequ2 ) ){
			return 1 ;
		}
		
		Integer val1 = 0;
		Integer val2 = 0;
		try{
		  val1 = Integer.valueOf( sequ1);
		}catch (Exception e) {
			LOG.info("Il valore sequence " +sequ1 +" non è intero" );
		}
		try{
		  val2 = Integer.parseInt(sequ2);
		}catch (Exception e) {
			LOG.info("Il valore sequence " +sequ2 +" non è intero" );
		}
		
		
		
		if( descOrder  ){
			return val1.compareTo( val2 ) ;
		}else{
			int ret = val1.compareTo( val2 );
			return ( ret == 0 ? 0 : ( ret == 1 ? -1 :1 ) );
		}		
	}

}
