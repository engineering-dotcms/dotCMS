/**
 * 
 */
package it.eng.bankit.converter;

import it.eng.bankit.util.HyperwaveKey;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * @author cesare Converter per tutte le tipologie di contenitori
 */
public abstract class GenericContainerConverter extends GenericMultilanguageConverter {
	private static Logger logger = Logger.getLogger( GenericContainerConverter.class );
	protected String ORDER_STYLE;

	@Override
	public void readStructureProperties() throws Exception {
		super.readStructureProperties();
		ORDER_STYLE = "orderType";
	}

	@Override
	protected void readProperties() throws Exception {
		super.readProperties();
		String sortOrderString = readProperty( HyperwaveKey.SortOrder );
		if ( sortOrderString != null && !sortOrderString.isEmpty() ) {
			if ( NumberUtils.isDigits( sortOrderString ) ) {
				sequence = Integer.parseInt( sortOrderString );
				propertiesToProcess.remove( HyperwaveKey.SortOrder );
			} else if ( sortOrderString.matches( "^(#|A|C|E|O|S|t|T|-|\\.)*$" ) ) {
				orderStyle = sortOrderString;
				propertiesToProcess.remove( HyperwaveKey.SortOrder );
			} else {
				logger.warn( "Unknow format for SortOrder:" + sortOrderString );
			}
		}
	}

	@Override
	protected void internalSetValues( Contentlet contentlet ) throws Exception {
		super.internalSetValues( contentlet );
		if ( orderStyle != null ) {
			contentlet.setProperty( ORDER_STYLE, orderStyle );
		}
	}

}
