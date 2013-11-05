package com.eng.dotcms.additions.publisher;

import java.util.List;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;

public class PublisherHostUtil {
	
	public static Host getBundleHost ( List<PublishQueueElement> bundle ) {
        if ( bundle.size() == 1 && bundle.get( 0 ).getType().equals( "contentlet" ) ) {
            try {
            	Identifier id = APILocator.getIdentifierAPI().find(bundle.get( 0 ).getAsset());
            	if(id != null && UtilMethods.isSet(id.getInode())) {
            		return APILocator.getHostAPI().find(
            				id.getHostId(), 
            				APILocator.getUserAPI().getSystemUser(), 
            				false);
            	}
            } catch (Exception e) {
            	return null;
            }

        } else {
            PublishQueueElement c;
            for ( int ii = 0; ii < bundle.size(); ii++ ) {
                c = bundle.get( ii );
                
                try {
                	Identifier id = APILocator.getIdentifierAPI().find(c.getAsset());
                	if(id != null && UtilMethods.isSet(id.getInode())) {
                		try {
	                		Host h = APILocator.getHostAPI().find(
	                				id.getHostId(), 
	                				APILocator.getUserAPI().getSystemUser(), 
	                				false);
	                		if(h != null && UtilMethods.isSet(h.getInode()))
	                			return h;
                		} catch (Exception e) {
							continue;
						}
                	}
                } catch (Exception e) {
                	continue;
                }
            }
        }
        return null;
    }
}
