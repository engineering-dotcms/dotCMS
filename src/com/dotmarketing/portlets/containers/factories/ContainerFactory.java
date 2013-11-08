package com.dotmarketing.portlets.containers.factories;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.services.ContainerServices;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
/**
 *
 * @author  will
 */
public class ContainerFactory {
    
      public static java.util.List getActiveContainers() {
        HibernateUtil dh = new HibernateUtil(Container.class);
        List<Container> activeContainers = null;
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.containers.model.Container where type='containers'");
			activeContainers = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		} 
        return activeContainers;
    }

     public static java.util.List getContainersByOrder(String orderby) {
        HibernateUtil dh = new HibernateUtil(Container.class);
        List<Container> containersByOrder = null ;
        try {
			dh.setQuery(
			    "from inode in class com.dotmarketing.portlets.containers.model.Container where type='containers' and working = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " or live = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + " order by " + orderby);
			containersByOrder = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		}

        return containersByOrder;
    }
 
    public static java.util.List getContainerByCondition(String condition) {
		HibernateUtil dh = new HibernateUtil(Container.class);
		List<Container> containers = null ;
		try {
			dh.setQuery("from inode in class  com.dotmarketing.portlets.containers.model.Container where type='containers' and " + condition + " order by title, sort_order");
			containers = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		}
		return containers;
	}

    public static boolean existsContainer(String friendlyName) {
        HibernateUtil dh = new HibernateUtil(Container.class);
        List<Container> list = null ;
        try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.containers.model.Container where type='containers' and friendly_name = ?");
			dh.setParam(friendlyName);
			list = (java.util.List) dh.list();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		}
        return list.size()>0;
    }
    
    public static Container getContainerByFriendlyName(String friendlyName) {
        HibernateUtil dh = new HibernateUtil(Container.class);
        Container container = null ;
        try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.containers.model.Container where type='containers' and friendly_name = ? and live=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue());
			dh.setParam(friendlyName);
			container = (Container) dh.load();
		} catch (DotHibernateException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
		}
        return container;
    }
    
    public static Container copyContainer (Container currentContainer) throws DotDataException, DotStateException, DotSecurityException {
    	
    	HostAPI hostAPI = APILocator.getHostAPI();
    	
		//gets the new information for the template from the request object
		Container newContainer = new Container();

		newContainer.copy(currentContainer);
       	newContainer.setFriendlyName(currentContainer.getFriendlyName()
				+ " (COPY) ");
       	newContainer.setTitle(currentContainer.getTitle() + " (COPY) ");
       	
        //Copy the structure
        Structure st = StructureCache.getStructureByInode(currentContainer.getStructureInode());
        newContainer.setStructureInode(st.getInode());

		//persists the webasset
		HibernateUtil.saveOrUpdate(newContainer);

		
		//Copy the host
		Host h;
		try {
			h = hostAPI.findParentHost(currentContainer, APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotSecurityException e) {
			Logger.error(ContainerFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} 
        //TreeFactory.saveTree(new Tree(h.getIdentifier(), newContainer.getInode()));

        //creates new identifier for this webasset and persists it
		Identifier newIdentifier = APILocator.getIdentifierAPI().createNew(newContainer, h);

		// save identifier id
		HibernateUtil.saveOrUpdate(newContainer);
		
		APILocator.getVersionableAPI().setWorking(newContainer);
		if(currentContainer.isLive())
		    APILocator.getVersionableAPI().setLive(newContainer);
		
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		//Copy permissions
		perAPI.copyPermissions(currentContainer, newContainer);
		
		//saves to working folder under velocity
		ContainerServices.invalidate(newContainer, newIdentifier,
				true);
    	
		return newContainer;
    }
    
    public static Structure getContainerStructure(Container container)
    {
    	Structure structure = StructureCache.getStructureByInode(container.getStructureInode());
    	return structure;
    }
    
    public static Set<Container> getContainerByContentlet(Contentlet contentlet) {
    	Set<Container> containers = new HashSet<Container>();
    	try{
			DotConnect dc = new DotConnect();
			StringBuffer buffy = new StringBuffer();
			buffy.append("select distinct cvi.identifier from multi_tree mt, container_version_info cvi ");
			buffy.append("where mt.parent2 = cvi.identifier ");
			buffy.append("and mt.child = ?");
			dc.setSQL(buffy.toString());
			dc.addParam(contentlet.getIdentifier());
			List<Map<String, Object>> res = dc.loadObjectResults();
			for(Map<String, Object> singleId : res) {
				String ident = (String)singleId.get("identifier");
				Container container = APILocator.getContainerAPI().getLiveContainerById(ident, APILocator.getUserAPI().getSystemUser(), false);
				if(null!=container && UtilMethods.isSet(container.getInode()))
					containers.add(container);
			}
			return containers;
    	}catch(DotDataException e){
    		Logger.error(HTMLPageFactory.class, "Error load Containers from contentlet: " + e.getMessage());
    		return null;
    	}catch(DotSecurityException e){
    		Logger.error(HTMLPageFactory.class, "Error load Containers from contentlet: " + e.getMessage());
    		return null;
    	}finally{
    		DbConnectionFactory.closeConnection();
    	}
    }
}
