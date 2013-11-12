package it.eng.dotcms.sitemap.api;

import it.eng.dotcms.sitemap.wrapper.HtmlLinkWrapper;

import java.util.Map;
import java.util.Set;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;


public abstract class SitemapAPI {
	private static SitemapAPI smAPI = null;
	public static SitemapAPI getInstance(){
		if(smAPI == null){
			smAPI = SitemapAPIImpl.getInstance();
		}
		return smAPI;	
	}
	
	public abstract Map<Integer, Folder> findSections();
	
	public abstract void loadSubFoldersToShow(Folder section, Set<HtmlLinkWrapper> links, Language lang, Map<String, Integer> foldersSortedMap) throws DotDataException, DotStateException, DotSecurityException;
	
	public abstract void loadSubLinksToShow(Folder section, Set<HtmlLinkWrapper> links, Language lang, Map<String, Integer> foldersSortedMap) throws DotDataException, DotStateException, DotSecurityException;
	
	public abstract void createPlainTree() throws DotStateException, DotDataException, DotSecurityException;
}
