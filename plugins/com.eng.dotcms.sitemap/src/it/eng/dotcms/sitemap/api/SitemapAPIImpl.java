package it.eng.dotcms.sitemap.api;

import it.eng.dotcms.sitemap.wrapper.HtmlLinkWrapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;


public class SitemapAPIImpl extends SitemapAPI {
	private IdentifierAPI idAPI = APILocator.getIdentifierAPI();
	private LanguageAPI lAPI = APILocator.getLanguageAPI();
	private HostAPI hAPI = APILocator.getHostAPI();
	private UserAPI uAPI = APILocator.getUserAPI();
	private FolderAPI fAPI = APILocator.getFolderAPI();
	private ContentletAPI cAPI = APILocator.getContentletAPI();
	private static Set<String> TEMPLATES_STARTS_WITH_EXCLUDED; 
	private static Set<String> TEMPLATES_CONTAINS_EXCLUDED; 
	private static String PATH_EXCLUDED = "/media/fotogallery/";
	
	
	public static final String SITEMAP_CONTENTLET_ID = "2b9325e0-1569-11e3-8ffd-0800200c9a66";
	
	private Host host;
	private User sysUser;
	
	private static SitemapAPIImpl instance = null;
    public static SitemapAPIImpl getInstance() {
		if(instance==null)
			instance = new SitemapAPIImpl();
		
		return instance;
	}
    
    public SitemapAPIImpl() {
    	try {
			this.sysUser = uAPI.getSystemUser();
			this.host = hAPI.findDefaultHost(sysUser, false);
			
			//init templates excluded
			TEMPLATES_STARTS_WITH_EXCLUDED = new HashSet<String>();
			TEMPLATES_STARTS_WITH_EXCLUDED.add("Listing");
			TEMPLATES_STARTS_WITH_EXCLUDED.add("DettaglioConFolder");
			
			TEMPLATES_CONTAINS_EXCLUDED = new HashSet<String>();
			TEMPLATES_CONTAINS_EXCLUDED.add("D5");
			
		} catch (DotDataException e) {
			e.printStackTrace();
		} catch (DotSecurityException e) {
			e.printStackTrace();
		} 
    }
    
    //Trova le sezioni da analizzare
    public Map<Integer, Folder> findSections() {
    	Map<Integer, Folder> folderToReturn = new TreeMap<Integer, Folder>();
    	FolderAPI fAPI = APILocator.getFolderAPI();
    	HostAPI hAPI = APILocator.getHostAPI();
    	UserAPI uAPI = APILocator.getUserAPI();
    	
    	try {
			List<Folder> folders = 
					fAPI.findFoldersByHost(hAPI.findDefaultHost(uAPI.getSystemUser(), false),uAPI.getSystemUser(),false);
			
			for (Folder folder : folders) {
				if(folder.isShowOnMenu() || StringUtils.startsWithIgnoreCase(folder.getPath(), "/media"))
					folderToReturn.put(folder.getSortOrder(), folder);
			}
		} catch (DotHibernateException e) {
			e.printStackTrace();
		} catch (DotSecurityException e) {
			e.printStackTrace();
		} catch (DotDataException e) {
			e.printStackTrace();
		}
    	
    	return folderToReturn;
    }
    
    /**
     * Create the links list that belong a section
     * @param section
     * @param htmlMap
     * @throws DotDataException
     * @throws DotStateException  
     * @throws DotSecurityException
     */
    public void loadSubFoldersToShow(Folder section, Set<HtmlLinkWrapper> links, Language lang, Map<String, Integer> foldersSortedMap) throws DotDataException, DotStateException, DotSecurityException {
		if(links != null) {
			List<HtmlLinkWrapper> tempList = new ArrayList<HtmlLinkWrapper>();
			if(foldersSortedMap == null)
				foldersSortedMap= new HashMap<String, Integer>();
			
	    	List<Identifier> ids = 	
					idAPI.findByURIPattern(
							"folder",
							StringUtils.stripEnd(section.getPath(), "/")+"*",
							true, false, true,
							host);
			
			for (Identifier id : ids) {
				HtmlLinkWrapper linkWrapper = null;
				Folder folder = fAPI.findFolderByPath(id.getPath(),host, sysUser, false);
				int depth = StringUtils.countMatches(id.getPath(), "/")-2;
				
				if(depth < 5) {					
					if(folder != null && UtilMethods.isSet(folder.getInode()) 
							&& (folder.isShowOnMenu() || folder.getPath().equalsIgnoreCase("/media/"))) 
					{
						boolean addLink = true;
						
						HTMLPage page = APILocator.getHTMLPageAPI().loadPageByPath(id.getParentPath()+"index.html", host);
						
						if(page != null && UtilMethods.isSet(page.getInode())) {
							Template pageTemplate = APILocator.getTemplateAPI().findLiveTemplate(page.getTemplateId(), sysUser, false);
							addLink = checkIfTemplateStartsWith(pageTemplate);
							
							if(addLink) {
								addLink = checkIfTemplateContains(pageTemplate);
							}
							
							if(addLink && 
									StringUtils.containsIgnoreCase(pageTemplate.getTitle(),"FullScreen") && 
									(depth-1) >= 1) 
							{
								addLink = false;
							}
							
							//Controllo contenuti cartella last level visible
							if(addLink) {
								addLink = checkLastLevelVisible(id);
							}
							
							
							//Controllo contenuti cartella nascondi contenuti box
							if(addLink ) {
								Folder parentFolder = fAPI.findFolderByPath(id.getParentPath(),host, sysUser, false);
								if(parentFolder.getTitle().endsWith("_box"))
									addLink = checkNascondiContenutiBox(id);
							}
							
							//Controllo check non esploso
							if(addLink) {
								addLink = checkNonEsploso(id);
							}
						}
						
						if(addLink) {
							String labelKey = StringUtils.substring(id.getPath(), 1, id.getPath().length()-1).replace("/", ".");
							String label = lAPI.getStringKey(lang, labelKey);
							
							if(label != null && label.length() > 0 && !labelKey.equals(label)) {
								if(id.getPath().startsWith(PATH_EXCLUDED) && depth < 4) {
									linkWrapper = new HtmlLinkWrapper(
											id.getId(),
											depth,
											label,
											id.getPath(),
											folder.getSortOrder(),
											id.getPath());
									
									foldersSortedMap.put(id.getPath(), linkWrapper.getOrder());
									tempList.add(linkWrapper);
								} else if(!id.getPath().startsWith(PATH_EXCLUDED)) {
									linkWrapper = new HtmlLinkWrapper(
											id.getId(),
											depth,
											label,
											id.getPath(),
											folder.getSortOrder(),
											id.getPath());
									
									foldersSortedMap.put(id.getPath(), linkWrapper.getOrder());
									tempList.add(linkWrapper);
								}
							}
						}
					}
				}
			}
			
			//Imposto l'ordinamento assoluto come concatenazione delle cartelle che compongono il path
			SortLinkUtil.sortHtmlLinksList(tempList, links, foldersSortedMap);
		}
    }
    
    private boolean checkIfTemplateStartsWith(Template pageTemplate) {
    	for(String excludedTemplate: TEMPLATES_STARTS_WITH_EXCLUDED) {
			if(pageTemplate.getTitle().startsWith(excludedTemplate)) {
				return false;
			}
		}
    	
    	return true;
    }
    
    private boolean checkIfTemplateContains(Template pageTemplate) {
    	for(String excludedTemplate: TEMPLATES_CONTAINS_EXCLUDED) {
			if(StringUtils.containsIgnoreCase(pageTemplate.getTitle(),excludedTemplate)) {
				return false;
			}
		}
    	
    	return true;
    }
    
    private boolean checkNascondiContenutiBox(Identifier idChild) throws DotDataException, DotSecurityException {
    	String luceneQuery = "+structureName:Dettaglio +live:true +Dettaglio.nascondiContenutiBox:*True* +parentPath:"+idChild.getParentPath();
    	List<Contentlet> contentlets = cAPI.search(luceneQuery, 1, 1, null, sysUser, false);
    	
    	if(contentlets != null && contentlets.size() > 0)
    		return false;
    	else
    		return true;
    }
    
    private boolean checkLastLevelVisible(Identifier idChild) throws DotDataException, DotSecurityException {
    	String luceneQuery = "+structureName:Dettaglio +live:true +Dettaglio.lastLevelVisible:*True* +parentPath:"+idChild.getParentPath();
    	List<Contentlet> contentlets = cAPI.search(luceneQuery, 1, 1, null, sysUser, false);
    	
    	if(contentlets != null && contentlets.size() > 0)
    		return false;
    	else
    		return true;
    }
    
    private boolean checkNonEsploso(Identifier idChild) throws DotDataException, DotSecurityException {
    	String luceneQuery = "+structureName:Dettaglio +live:true +Dettaglio.nonEsploso:*True* +parentPath:"+idChild.getParentPath();
    	List<Contentlet> contentlets = cAPI.search(luceneQuery, 1, 1, null, sysUser, false);
    	
    	if(contentlets != null && contentlets.size() > 0)
    		return false;
    	else
    		return true;
    }
    
    
    public void loadSubLinksToShow(Folder section, Set<HtmlLinkWrapper> links, Language lang, Map<String, Integer> foldersSortedMap) throws DotDataException, DotStateException, DotSecurityException {
    	if(links != null) {
    		List<HtmlLinkWrapper> tempList = new ArrayList<HtmlLinkWrapper>();
    		
    		if(foldersSortedMap == null)
    			foldersSortedMap = new HashMap<String, Integer>();
    		
			//Aggiungo anche i link con showmenu tramite query lucene
	    	StringBuilder sb = new StringBuilder();
	    	sb.append("+structureName:Link +live:true +Link.showOnMenu:true* ");
	    	sb.append("+parentPath:"+section.getPath()+"* +languageId:"+lang.getId());
	    	
	    	List<Contentlet> linksShowOnMenu = null;
	    	try {
	    		linksShowOnMenu = cAPI.search(sb.toString(), 0, -1, null, sysUser, false);
	    	} catch(Exception e) {}
	    	
	    	if(linksShowOnMenu != null) {
				for (Contentlet linkShowOnMenu : linksShowOnMenu) {
					HtmlLinkWrapper linkWrapper = null;
					
					Identifier idLink = idAPI.find(linkShowOnMenu.getIdentifier());
					int depth = StringUtils.countMatches(idLink.getPath(), "/")-1;
					
					if(depth < 5) {
						switch (((String)linkShowOnMenu.get("linkType")).toCharArray()[0]) {
							case 'A':
								String allegatoId = (String) linkShowOnMenu.get( "allegatoId" );
								Identifier allegatoIdentifier = null;
								try {
									allegatoIdentifier = idAPI.find(allegatoId);
								} catch (Exception e) {
									
								}
								
								if(allegatoIdentifier != null && UtilMethods.isSet(allegatoIdentifier.getId())) {
									String pathA = allegatoIdentifier.getPath();
									linkWrapper = new HtmlLinkWrapper(
											idLink.getId(),
											depth,
											linkShowOnMenu.getTitle(),
											pathA,
											((Long)linkShowOnMenu.get("sortOrder1")).intValue(),
											idLink.getPath());
									
									foldersSortedMap.put(idLink.getPath(), linkWrapper.getOrder());
									tempList.add(linkWrapper);
								}
								break;
							case 'E':
								String pathE = idLink.getParentPath()+(String) linkShowOnMenu.get("identificativo");
								linkWrapper = new HtmlLinkWrapper(
										idLink.getId(),
										depth,
										linkShowOnMenu.getTitle(),
										pathE,
										((Long)linkShowOnMenu.get("sortOrder1")).intValue(),
										idLink.getPath());
								
								foldersSortedMap.put(idLink.getPath(), linkWrapper.getOrder());
								tempList.add(linkWrapper);
								break;
							case 'I':
								String pathI = (String) linkShowOnMenu.get("linkInterno");
								linkWrapper = new HtmlLinkWrapper(
										idLink.getId(),
										depth,
										linkShowOnMenu.getTitle(),
										pathI,
										((Long)linkShowOnMenu.get("sortOrder1")).intValue(),
										idLink.getPath());
								
								foldersSortedMap.put(idLink.getPath(), linkWrapper.getOrder());
								tempList.add(linkWrapper);
								break;
							default:
								break;
						} 
					}
				}
	    	}
	    	
	    	SortLinkUtil.sortHtmlLinksList(tempList, links, foldersSortedMap);
    	}
    }
    
    public void createPlainTree() throws DotStateException, DotDataException, DotSecurityException {
    	Logger.info(this, "Start sitemap job builder");
    	for(Language lang : APILocator.getLanguageAPI().getLanguages()) {
			//Logic
			StringBuilder htmlMap = new StringBuilder("<ul>");
			
			//Prendo le sezioni con showOnMenu true
			Map<Integer, Folder> sections = findSections();
			
			//Per ognuna carico i link da mostrare nel menu'
			for (Integer sectionKey : sections.keySet()) {
				Set<HtmlLinkWrapper> links = new TreeSet<HtmlLinkWrapper>();
				Folder section = sections.get(sectionKey);
				Map<String, Integer> foldersSortedMap = new HashMap<String, Integer>();
				
				loadSubFoldersToShow(section, links, lang, foldersSortedMap);
				loadSubLinksToShow(section, links, lang, foldersSortedMap);
				
				for(HtmlLinkWrapper linkWrapper: links) {
					//Inserire la possibilita di avere una traduzione del nome del folder
					generateLinkHtml(htmlMap, linkWrapper);
				}
				
				foldersSortedMap = null;
				links = null;
			}
			
			htmlMap.append("</ul>");
		
			Contentlet siteMapContentlet = null;
			
			//Creazione contentlet Sitemap di default
			try {
				siteMapContentlet = cAPI.findContentletByIdentifier(
					SITEMAP_CONTENTLET_ID, 
					true, lang.getId(), sysUser, false);
			} catch (Exception e) {
				siteMapContentlet = createDefaultMapContentlet(lang);
			}
			
			if(siteMapContentlet != null) {
				siteMapContentlet.setProperty("body", htmlMap.toString());
				APILocator.getContentletAPI().publish(siteMapContentlet, sysUser, false);
			}
		}
    	Logger.info(this, "End sitemap building");
    }
    
    
    private void generateLinkHtml(StringBuilder htmlMap, HtmlLinkWrapper link) {
		switch(link.getDepth().intValue()) {
		case 0:
			htmlMap.append("<li class=\"testata_mappa\" id=\""+link.getAbsoluteOrder()+"\">");
			htmlMap.append("<a href=\"");
			htmlMap.append(link.getHref());
			htmlMap.append("\">");
			htmlMap.append(link.getLabel());
			htmlMap.append("</a>");
			break;
		case 1:
			htmlMap.append("<li id=\""+link.getAbsoluteOrder()+"\">");
			htmlMap.append("<a href=\"");
			htmlMap.append(link.getHref());
			htmlMap.append("\">");
			htmlMap.append(link.getLabel());
			htmlMap.append("</a>");
			break;
		case 2:
			htmlMap.append("<li class=\"rientro\" id=\""+link.getAbsoluteOrder()+"\">");
			htmlMap.append("<a href=\"");
			htmlMap.append(link.getHref());
			htmlMap.append("\">");
			htmlMap.append(link.getLabel());
			htmlMap.append("</a>");
			break;
		default:
			htmlMap.append("<li class=\"rientro"+(link.getDepth().intValue()-1)+"\" id=\""+link.getAbsoluteOrder()+"\">");
			htmlMap.append("<a href=\"");
			htmlMap.append(link.getHref());
			htmlMap.append("\" class=\"altri\">");
			htmlMap.append(link.getLabel());
			htmlMap.append("</a>");
			
			break;
		}
		
		
		htmlMap.append("</li>");
	}
    
    private Contentlet createDefaultMapContentlet(Language lang) throws DotDataException, DotSecurityException {
		User sysUser = uAPI.getSystemUser();
		Host h = hAPI.findDefaultHost(sysUser, false);
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("stInode", StructureFactory.getDefaultStructure().getInode());
		map.put("owner", sysUser.getUserId());
		map.put("lastReview", new Date());
		map.put("modUser", sysUser.getUserId());
		map.put("identifier", SITEMAP_CONTENTLET_ID);
		map.put("sortOrder", new Long(0));
		map.put("body", "auto-generated contentlet");
		map.put("__DOTNAME__", "Sitemap");
		map.put("host", h.getIdentifier());
		map.put("modDate", new Date());
		map.put("title", "Sitemap");
		map.put("languageId", lang.getId());
		map.put("disabledWYSIWYG", new ArrayList<Object>());
		map.put("_dont_validate_me", true);
		
		Contentlet c = new Contentlet(map);
		Identifier ident = APILocator.getIdentifierAPI().find(SITEMAP_CONTENTLET_ID);
		if(!UtilMethods.isSet(ident) || !UtilMethods.isSet(ident.getInode()))
			APILocator.getIdentifierAPI().createNew(c, h, SITEMAP_CONTENTLET_ID);
		
		APILocator.getContentletAPI().checkin(c, sysUser, false);
		
		Contentlet siteMapContentlet = cAPI.findContentletByIdentifier(
				SITEMAP_CONTENTLET_ID, 
				false, lang.getId(), sysUser, false);
		APILocator.getContentletAPI().publish(siteMapContentlet, sysUser, false);
		
		return siteMapContentlet;
	}
    
}
