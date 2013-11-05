package it.bankit.website.hook;

import java.util.List;

import net.sf.hibernate.Session;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPostHookAbstractImp;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class ContentletPostHook extends ContentletAPIPostHookAbstractImp {

	private static final String GROUP = "navCache";
	private static final String structureName = "Link";

	private DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
	private HostAPI hostAPI = APILocator.getHostAPI();

	@Override
	public void publish(Contentlet contentlet, User user, boolean respectFrontendRoles) {

		String stName = contentlet.getStructure().getVelocityVarName();
		if (stName.equalsIgnoreCase(structureName)) {
			Logger.info(this.getClass(), "[INIT] publish contenuto pubblicato " + contentlet.getTitle()   + " - Folder " + contentlet.getFolder() );			
			String keyOnCache = contentlet.getHost() + ":" + contentlet.getFolder();
			Logger.info(this.getClass(), "[INIT] keyOnCache to remove  " + keyOnCache   );
			removeMenuCache(keyOnCache);
		}
		try {
			updateRelatedPage(contentlet, user, respectFrontendRoles);
		} catch (DotStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.publish(contentlet, user, respectFrontendRoles);
	}

	
	@Override
	public void publish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {

		for (Contentlet contentlet : contentlets) {
			String stName = contentlet.getStructure().getVelocityVarName();
			if (stName.equalsIgnoreCase(structureName)) {
				String keyOnCache = contentlet.getHost() + ":" + contentlet.getFolder();
				removeMenuCache(keyOnCache);
			}
		}

		super.publish(contentlets, user, respectFrontendRoles);
	}

	@Override
	public void unpublish(Contentlet contentlet, User user, boolean respectFrontendRoles) {

		String stName = contentlet.getStructure().getVelocityVarName();
		if (stName.equalsIgnoreCase(structureName)) {
			Logger.info(this.getClass(), "[INIT] unpublish contenuto de-pubblicato " + contentlet.getTitle()   + " - Folder " + contentlet.getFolder() );			
			String keyOnCache = contentlet.getHost() + ":" + contentlet.getFolder();
			removeMenuCache(keyOnCache);
		}
		super.unpublish(contentlet, user, respectFrontendRoles);
	}

	@Override
	public void unpublish(List<Contentlet> contentlets, User user, boolean respectFrontendRoles) {

		for (Contentlet contentlet : contentlets) {

			String stName = contentlet.getStructure().getVelocityVarName();
			if (stName.equalsIgnoreCase(structureName)) {
				String keyOnCache = contentlet.getHost() + ":" + contentlet.getFolder();
				removeMenuCache(keyOnCache);
			}
		}

		super.unpublish(contentlets, user, respectFrontendRoles);
	}

	@Override
	public void isInodeIndexed(String inode, boolean live, boolean returnValue) {

	}

	public void removeMenuCache(String key) {
		Logger.info(this.getClass(), "Rimuovo la cache del menu " + key  );
		cache.remove(key, GROUP);
		cache.flushGroup(GROUP);
	}
	
	private void updateRelatedPage(Contentlet contentlet, User user, boolean respectFrontendRoles) throws DotStateException, Exception {
		if(contentlet.getStructure().getStructureType()!=Structure.STRUCTURE_TYPE_FILEASSET){
			Identifier id = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
			Folder parentFolder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(), hostAPI.find(contentlet.getHost(), user, respectFrontendRoles), user, respectFrontendRoles);
			List<HTMLPage> pages = APILocator.getHTMLPageAPI().findWorkingHTMLPages(parentFolder);
			for(HTMLPage existingHTMLPage: pages){
				// Creation the version asset
				HTMLPage newHtmlPage = existingHTMLPage;
				Identifier identifier = APILocator.getIdentifierAPI().find(newHtmlPage);
				HTMLPage workingAsset = null;
				WebAssetFactory.createAsset(newHtmlPage, user.getUserId(), parentFolder, APILocator.getIdentifierAPI().find(newHtmlPage), false);
				HibernateUtil.flush();

				LiveCache.removeAssetFromCache(existingHTMLPage);
				workingAsset = (HTMLPage) WebAssetFactory.saveAsset(newHtmlPage, APILocator.getIdentifierAPI().find(newHtmlPage));

				// if we need to update the identifier
				if (InodeUtils.isSet(parentFolder.getInode()) && !workingAsset.getURI(parentFolder).equals(identifier.getURI())) {
					// assets cache
					LiveCache.removeAssetFromCache(newHtmlPage);
					LiveCache.removeAssetFromCache(existingHTMLPage);
					LiveCache.clearCache(hostAPI.find(contentlet.getHost(), user, respectFrontendRoles).getIdentifier());
					WorkingCache.removeAssetFromCache(newHtmlPage);
					CacheLocator.getIdentifierCache().removeFromCacheByVersionable(newHtmlPage);
		
					CacheLocator.getIdentifierCache().removeFromCacheByVersionable(existingHTMLPage);
					APILocator.getIdentifierAPI().updateIdentifierURI(workingAsset, parentFolder);
				}

				CacheLocator.getHTMLPageCache().remove(workingAsset);
				
				HibernateUtil.flush();
				HibernateUtil.getSession().refresh(workingAsset);

				if (RefreshMenus.shouldRefreshMenus(workingAsset)) {
					RefreshMenus.deleteMenu(workingAsset);
					if(identifier!=null)
					    CacheLocator.getNavToolCache().removeNavByPath(identifier.getHostId(), identifier.getParentPath());
				}
			}
		}
	}
}
