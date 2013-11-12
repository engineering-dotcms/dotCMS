//package it.eng.dotcms.sitemap;
//
//import it.eng.dotcms.sitemap.api.SitemapAPI;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.quartz.JobExecutionContext;
//import org.quartz.JobExecutionException;
//import org.quartz.StatefulJob;
//
//import com.dotmarketing.beans.Host;
//import com.dotmarketing.beans.Identifier;
//import com.dotmarketing.business.APILocator;
//import com.dotmarketing.business.IdentifierAPI;
//import com.dotmarketing.business.UserAPI;
//import com.dotmarketing.exception.DotDataException;
//import com.dotmarketing.exception.DotSecurityException;
//import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
//import com.dotmarketing.portlets.contentlet.business.HostAPI;
//import com.dotmarketing.portlets.contentlet.model.Contentlet;
//import com.dotmarketing.portlets.languagesmanager.model.Language;
//import com.dotmarketing.portlets.structure.factories.StructureFactory;
//import com.dotmarketing.util.Logger;
//import com.dotmarketing.util.UtilMethods;
//import com.liferay.portal.model.User;
//
///**
// * Cron job for the sitemap building.
// * 
// * @author Alberto Montichiara - Engineering Ingegneria Informatica S.p.a
// */
//public class SitemapBuilderJob implements StatefulJob {
//	private SitemapAPI sitemapAPI = SitemapAPI.getInstance();
//	private HostAPI hAPI = APILocator.getHostAPI();
//	private UserAPI uAPI = APILocator.getUserAPI();
//	private ContentletAPI cAPI = APILocator.getContentletAPI();
//	private IdentifierAPI iAPI = APILocator.getIdentifierAPI();
//	
//	public static final String SITEMAP_CONTENTLET_ID = "2b9325e0-1569-11e3-8ffd-0800200c9a66";
//	
//	@Override
//	public void execute(JobExecutionContext ctx) throws JobExecutionException {
//		Logger.info(this, "Starting sitemap job builder");
//		
//		try {
//			sitemapAPI.createTree();
//		} catch (DotDataException e) {
//			throw new JobExecutionException(e.getCause());
//		} catch (DotSecurityException e) {
//			throw new JobExecutionException(e.getCause());
//		}
//		
//		Logger.info(this, "Finished sitemap job builder");
//	}
//	
//	private Contentlet createDefaultMapContentlet(Language lang) throws DotDataException, DotSecurityException {
//		User sysUser = uAPI.getSystemUser();
//		Host h = hAPI.findDefaultHost(sysUser, false);
//		Map<String, Object> map = new HashMap<String, Object>();
//		
//		map.put("stInode", StructureFactory.getDefaultStructure().getInode());
//		map.put("owner", sysUser.getUserId());
//		map.put("lastReview", new Date());
//		map.put("modUser", sysUser.getUserId());
//		map.put("identifier", SITEMAP_CONTENTLET_ID);
//		map.put("sortOrder", new Long(0));
//		map.put("body", "auto-generated contentlet");
//		map.put("__DOTNAME__", "Sitemap");
//		map.put("host", h.getIdentifier());
//		map.put("modDate", new Date());
//		map.put("title", "Sitemap");
//		map.put("languageId", lang.getId());
//		map.put("disabledWYSIWYG", new ArrayList<Object>());
//		map.put("folder", h.getInode());
//		map.put("_dont_validate_me", true);
//		
//		Contentlet c = new Contentlet(map);
//		Identifier ident = APILocator.getIdentifierAPI().find(SITEMAP_CONTENTLET_ID);
//		if(!UtilMethods.isSet(ident) || !UtilMethods.isSet(ident.getInode()))
//			APILocator.getIdentifierAPI().createNew(c, h, SITEMAP_CONTENTLET_ID);
//		
//		APILocator.getContentletAPI().checkin(c, sysUser, false);
//		
//		Contentlet siteMapContentlet = cAPI.findContentletByIdentifier(
//				SITEMAP_CONTENTLET_ID, 
//				false, lang.getId(), sysUser, false);
//		APILocator.getContentletAPI().publish(siteMapContentlet, sysUser, false);
//		
//		return siteMapContentlet;
//	}
//	
//	
//	
////	User sysUser = uAPI.getSystemUser();
////	
////	for(Language lang : APILocator.getLanguageAPI().getLanguages()) {
////		//Logic
////		StringBuilder htmlMap = new StringBuilder("<ul>");
////		
////		//Prendo le sezioni con showOnMenu true
////		Map<Integer, Folder> sections = sitemapAPI.findSections();
////		
////		//Per ognuna carico i link da mostrare nel menu'
////		for (Integer sectionKey : sections.keySet()) {
////			Set<HtmlLinkWrapper> links = new TreeSet<HtmlLinkWrapper>();
////			Folder section = sections.get(sectionKey);
////			
////			sitemapAPI.loadSubFoldersToShow(section, htmlMap, links, lang);
////			sitemapAPI.loadSubLinksToShow(section, htmlMap, links, lang);
////			
////			for(HtmlLinkWrapper linkWrapper: links) {
////				//Inserire la possibilita di avere una traduzione del nome del folder
////				generateLinkHtml(htmlMap, linkWrapper);
//////				
//////				System.out.println(StringUtils.repeat("-", linkWrapper.getDepth())+" "
//////						+linkWrapper.getOrder()
//////						+" - "
//////						+linkWrapper.getLabel()
//////						+ " - "+linkWrapper.getHref());
////			}
////			
////			links = null;
////		}
////		
////		htmlMap.append("</ul>");
////	
////		Contentlet siteMapContentlet = null;
////		
////		//Creazione contentlet Sitemap di default
////		try {
////			siteMapContentlet = cAPI.findContentletByIdentifier(
////				SITEMAP_CONTENTLET_ID, 
////				true, lang.getId(), sysUser, false);
////		} catch (Exception e) {
////			siteMapContentlet = createDefaultMapContentlet(lang);
////		}
////		
////		if(siteMapContentlet != null) {
////			siteMapContentlet.setProperty("body", htmlMap.toString());
////			APILocator.getContentletAPI().publish(siteMapContentlet, sysUser, false);
////		}
////	}
//	
//	
//
//}
