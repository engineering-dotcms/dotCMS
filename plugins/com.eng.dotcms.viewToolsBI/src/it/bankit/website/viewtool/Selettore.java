package it.bankit.website.viewtool;

import it.bankit.website.cache.BankitCache;
import it.bankit.website.viewtool.list.SortUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import bsh.util.Util;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class Selettore {

	private static String FOLDER_SEPARATOR = ".";
	private static String FOLDER_BLANK_SEPARATOR = "-";

	private static final String PDFICON = "/static/images/template/ico_pdf.gif";
	private static final String ZIPICON = "/static/images/template/ico_zip.gif";
	private static final String XLSICON = "/static/images/template/ico_excel.gif";
	private static final String PPTICON = "/static/images/template/ico_powerpoint.gif";
	private static final String DOCICON = "/static/images/template/ico_word.gif";
	private static final String PAGEICON = "/static/images/template/ico_pagina.gif";
	private static final String FOLDERICON = "/static/images/template/ico_cartella.gif";
	private static final String EPUBICON = "/static/images/template/ico_epub.jpg";
	private static final String WMVICON = "/static/images/template/ico_video.gif";

	private static final String ESTERNO = "/static/images/template/ico_linkesterno.gif";
	// private static final String INTERNO =
	// "/static/images/template/ico_linkinterno.gif";

	private static final String DOC = "doc";
	private static final String PDF = "pdf";
	private static final String PPT = "pwp";
	private static final String ZIP = "zip";
	private static final String CERT = "x-x509-ca-cert";
	private static final String PKCS = "x-pkcs7-mime";
	private static final String XLS = "xls";
	private static final String EPUB = "ePub";
	private static final String WMV = "wmv";

	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	private HTMLPageAPI pageAPI = APILocator.getHTMLPageAPI();
	private TemplateAPI templateApi = APILocator.getTemplateAPI();
	private com.dotmarketing.portlets.folders.business.FolderAPI folderAPI = APILocator.getFolderAPI();
	private SortUtil su = new SortUtil();
	private static final Logger LOG = Logger.getLogger(LanguageUtil.class);

	public String getLinks(String path, String languageID, String hostId, String mode) throws Exception {

		StringBuffer stringbuf = new StringBuffer("");		
		try{
			User user = APILocator.getUserAPI().getSystemUser();
			Contentlet dettaglio;
			String sortOrder = "Link.dataEmanazione desc , Link.sortOrder1 asc";
			String folderTranslation = "";

			if (UtilMethods.isSet(path) && !path.equals("/")) {
				String cp = convertPath(path);
				folderTranslation = BankitCache.getInstance().getStringKey(langAPI.getLanguage(languageID), cp);
			}

			String dettaglioQuery = "";
			dettaglioQuery = "+StructureName:Dettaglio +languageId:" + languageID + " +parentPath:" + path + "/";
			List<Contentlet> dettagli = APILocator.getContentletAPI().search(dettaglioQuery, -1, 0, "Dettaglio.dataEmanazione desc", user, false);

			if (dettagli.size() > 0) {
				dettaglio = (Contentlet) dettagli.get(0);
				if (dettaglio.getStringProperty("orderType") != null && !"".equals(dettaglio.getStringProperty("orderType"))) {
					sortOrder = su.generateLuceneSortOrder(dettaglio.getStringProperty("orderType"), "Link");
				}
			}


			if (!folderTranslation.equals("")) {
				String q = "";
				q = "+StructureName:Link +languageId:" + languageID + " +" + mode + ":true +path:" + path + "/*";
				List<Contentlet> linksList = APILocator.getContentletAPI().search(q, -1, 0, sortOrder, APILocator.getUserAPI().getSystemUser(), false);

				String qLinkSemplice = "";
				qLinkSemplice = "+StructureName:Linksemplice +languageId:" + languageID + " +live:true +parentPath:" + path + "/";

				List<Contentlet> linksSempliceList = APILocator.getContentletAPI().search(qLinkSemplice, -1, 0, "modDate desc", user, false);

				stringbuf.append("<div id=\"dt_" + folderTranslation + "\">");
				stringbuf.append("<div class=\"titolo\"><h3>" + folderTranslation + "</h3></div>");
				stringbuf.append("<ul>");
				nullLast(linksList);

				for (Contentlet link : linksList) {

					String titolo;
					String sommario = "";
					String href = "";
					String type = "";
					String labelType = "";
					String size = "";
					String allegatoId = "";
					String src = null;
					String ms = "";

					if (UtilMethods.isSet(link.getStringProperty("titoloLungo"))) {
						titolo = link.getStringProperty("titoloLungo");
					} else {
						titolo = link.getStringProperty("titolo");
					}
					String mostraS =link.getStringProperty("mostraSommario");
					if (UtilMethods.isSet( mostraS )) {
						ms = mostraS;
						if (link.getStringProperty("sommario") != null) {
							sommario = link.getStringProperty("sommario");
						}	
					}

					String linkType = link.getStringProperty("linkType");

					if ("A".equals(linkType)) {
						Contentlet allegatoDettaglio;

						if (UtilMethods.isSet(link.getStringProperty("allegatoId"))) {
							allegatoId = link.getStringProperty("allegatoId");
						} else if (UtilMethods.isSet(link.getStringProperty("allegato"))) {
							allegatoId = link.getStringProperty("allegato");
						}

						String queryForAllegatoDettaglio = "";

						if (allegatoId != null && !"".equals(allegatoId)) {

							queryForAllegatoDettaglio = "+identifier:" + allegatoId + " +languageId:" + languageID;
							List<Contentlet> allegatoList = APILocator.getContentletAPI().search( queryForAllegatoDettaglio, -1, 0, null, user, false);

							if (allegatoList.size() > 0) {
								allegatoDettaglio = allegatoList.get(0);
								File file = allegatoDettaglio.getBinary("fileAsset");
								size = FileSizeUtil.getsize(file);
								type = file.getName().substring(file.getName().lastIndexOf("."));
								labelType = assignType(type);
								String id = allegatoDettaglio.getIdentifier();
								Identifier i = APILocator.getIdentifierAPI().loadFromCache(id);
								if (i == null) {
									i = APILocator.getIdentifierAPI().find(id);
								}

								src = assignIcon(type);
								if (src.equals(WMV)) {
									href = i.getPath() + "internal&action=video.action";
								} else {
									href = i.getPath();
								}

							}

						}

					} else if ("I".equals(linkType)) {
						String li = link.getStringProperty("linkInterno");

						if (li != null && !"".equals(li) && li.endsWith(".html")) {

							HTMLPage linkedPage = pageAPI.loadPageByPath(li, hostId);
							Template linkedPageTemplate = null;

							if(UtilMethods.isSet(linkedPage) && linkedPage.isLive()){
								linkedPageTemplate = templateApi.findLiveTemplate(linkedPage.getTemplateId(), user, true);

								Folder folder = folderAPI.findFolderByPath(li.substring(0, li.lastIndexOf("/")), hostId, user, true);
								href = li.substring(0, li.lastIndexOf("/") + 1);

								if(UtilMethods.isSet(linkedPageTemplate) && linkedPageTemplate.getTitle().toLowerCase().contains("video")){							
									src = WMVICON;
								} else if (folderAPI.findMenuItems(folder, user, true).size() > 0) {
									src = FOLDERICON;								
								} else {
									src = PAGEICON;
								}
							}
						}

					} else {

					//	if (link.getStringProperty("identificativo") != null && !"".equals(link.getStringProperty("identificativo"))) {
						//	href = path + "/" + link.getStringProperty("identificativo");
						   href =  link.getStringProperty("linkEsterno");
							src = ESTERNO;
					//	}
					}

					if (!"".equals(href)) {
						stringbuf.append("<li>");
						stringbuf.append("<a href=\"" + href + "\"><img src=\"" + src + "\" class=\"ico\" width=\"14\" height=\"14\">" + titolo + "</a> " + buildSpan(link, size, labelType));
						if (!"".equals(ms) && !"".equals(sommario)) {
							stringbuf.append("<br/>" + sommario);
						}
						stringbuf.append("</li>");
					}

				}

				Contentlet linkSemplice = null;
				String linkSempliceType = "";
				if (linksSempliceList.size() > 0){
					linkSemplice = linksSempliceList.get(0);
					linkSempliceType = linkSemplice.getStringProperty("linkType");
				}

				if (linkSemplice != null && (!"".equals(linkSempliceType) && "I".equals(linkSempliceType))) {

					String linkSempHref = "";
					if("I".equals(linkSempliceType)){
						linkSempHref = linkSemplice.getStringProperty("linkInterno");
					} else {
						linkSempHref = path;
					}

					String titolo = linkSemplice.getStringProperty("titolo");
					stringbuf.append("<li class=\"noborder\"><a href=\"" + linkSempHref + "\" class=\"altri\">" + titolo + "</a></li>");
				}

				stringbuf.append("</ul>");
				stringbuf.append("</div>");

			}
		}catch (Exception e) {
			e.printStackTrace();
		}

		return stringbuf.toString();

	}
	public String convertPath(String path) {
		try {
			path = path.replace("/", FOLDER_SEPARATOR).replace(" ", FOLDER_BLANK_SEPARATOR);
			if (path.startsWith(FOLDER_SEPARATOR)) {
				path = path.substring(1);
			}
			if (path.endsWith(FOLDER_SEPARATOR)) {
				path = path.substring(0, path.lastIndexOf(FOLDER_SEPARATOR));
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		return path;
	}

	private String assignIcon(String type) {

		type = type.toLowerCase();
		if (type.contains(".pdf")) {
			return PDFICON;
		} else if (type.contains(".ppt")) {
			return PPTICON;
		} else if (type.contains(".zip")) {
			return ZIPICON;
		} else if (type.contains(".xls")) {
			return XLSICON;
		} else if (type.contains(".doc")) {
			return DOCICON;
		} else if (type.contains(".epub")) {
			return EPUBICON;
		} else if (type.contains(".wmv")) {
			return WMVICON;
		} else {
			return PAGEICON;
		}

	}

	private String assignType(String type) {
		type = type.toLowerCase();
		if (type.toLowerCase().contains(".pdf")) {
			return PDF;
		} else if (type.contains(".ppt")) {
			return PPT;
		} else if (type.contains(".zip")) {
			return ZIP;
		} else if (type.contains(".xls")) {
			return XLS;
		} else if (type.contains(".doc")) {
			return DOC;
		} else if (type.contains(".epub")) {
			return EPUB;
		} else if (type.contains(".cert")) {
			return CERT;
		} else if (type.contains(".zip.p7m")) {
			return PKCS;
		} else {
			return null;
		}

	}

	private String buildSpan(Contentlet link, String size, String type) {

		StringBuffer stringbuf = new StringBuffer();
		stringbuf.append("<span class=\"data\">");

		if (link.getDateProperty("dataEmanazione") != null) {
			stringbuf.append(DateUtil.formatDate(link.getDateProperty("dataEmanazione"), "dd-MM-yyyy") + "&nbsp;-&nbsp;");
		}

		if ("A".equals(link.getStringProperty("linkType"))) {
			if (!"".equals(type)) {
				stringbuf.append(type);
			}
			if (!"".equals(size)) {
				stringbuf.append(" " + size.replaceAll("KB", "kB") + " - ");
			}
		}

		if (link.getStringProperty("ruoloAutore") != null) {
			stringbuf.append(link.getStringProperty("ruoloAutore") + " - ");
		}
		if (link.getStringProperty("autore") != null) {
			stringbuf.append(link.getStringProperty("autore") + " - ");
		}
		if (link.getStringProperty("ruoloAllegato") != null) {
			stringbuf.append(link.getStringProperty("ruoloAllegato") + " - ");
		}
		if (link.getStringProperty("luogo") != null) {
			stringbuf.append(link.getStringProperty("luogo") + " - ");
		}
		if (link.getStringProperty("organizzazione") != null) {
			stringbuf.append(link.getStringProperty("organizzazione") + " - ");
		}
		if (link.getStringProperty("evento") != null) {
			stringbuf.append(link.getStringProperty("evento") + " - ");
		}
		String s = stringbuf.toString().trim();
		if (s.length() > 0) {
			if (s.endsWith(" -")) {
				s = s.substring(0, s.lastIndexOf(" -"));
			} else if (s.endsWith("&nbsp;-&nbsp;")) {
				s = s.substring(0, s.lastIndexOf("&nbsp;-&nbsp;"));
			}

		}
		s += "</span>";

		return s;

	}

	private String sizeRenderer(int size) {

		int divider = 0;
		String unit = "";

		if (size < 1000) {
			divider = 1;
			unit = "B";
		} else if (size < 1048576) {
			divider = 1024;
			unit = "kB";
		} else if (size < 1073741824) {
			divider = 1048576;
			unit = "MB";
		} else {
			divider = 1073741824;
			unit = "GB";
		}

		return Double.toString(Math.round(size / divider)) + " " + unit;

	}

	public List<Contentlet> nullLast(List<Contentlet> listOfElements) {

		List<Contentlet> orderedList = new ArrayList<Contentlet>();
		List<Contentlet> nullList = new ArrayList<Contentlet>();

		for (Object object : listOfElements) {

			if (object instanceof Contentlet) {

				Date dataEmanazioneField = ((Contentlet) object).getDateProperty("dataEmanazione");

				if (dataEmanazioneField != null && UtilMethods.isSet(((Contentlet) object).getIdentifier())) {
					nullList.add((Contentlet) object);
				} else {
					orderedList.add((Contentlet) object);
				}

			}

		}

		boolean merge = nullList.addAll(orderedList);

		return nullList;

	}
	
	
	
	
	public String getLinksByCategory(String path,String category,  String languageID, String hostId, String mode) throws Exception {
       StringBuffer stringbuf = new StringBuffer("");		
		try{
			User user = APILocator.getUserAPI().getSystemUser();
			Contentlet dettaglio;
			String sortOrder = "Link.dataEmanazione desc , Link.sortOrder1 asc, Link.titolo";
			String folderTranslation = "";

			if (UtilMethods.isSet(path) && !path.equals("/")) {
				String cp = convertPath(path);
				folderTranslation = BankitCache.getInstance().getStringKey(langAPI.getLanguage(languageID), cp);
			}

			String dettaglioQuery = "";
			dettaglioQuery = "+StructureName:Dettaglio +languageId:" + languageID + " +parentPath:" + path + "/";
			List<Contentlet> dettagli = APILocator.getContentletAPI().search(dettaglioQuery, -1, 0, "Dettaglio.dataEmanazione desc ", user, false);

			if (dettagli.size() > 0) {
				dettaglio = (Contentlet) dettagli.get(0);
				if (dettaglio.getStringProperty("orderType") != null && !"".equals(dettaglio.getStringProperty("orderType"))) {
					sortOrder = su.generateLuceneSortOrder("-D#T", "Link");
				}
			}else {
			  sortOrder = su.generateLuceneSortOrder("-D#T", "Link");
			}
			
			if (!folderTranslation.equals("")) {
				List<Contentlet> linksList = null;
				Category cat = APILocator.getCategoryAPI().findByKey(category, APILocator.getUserAPI().getSystemUser(), true);
				System.out.println(  " Categoria trovata " + cat );
			 	
				if( cat!= null && UtilMethods.isSet(cat.getInode() ) ){
					List<Category> cats = new ArrayList<Category>();
					cats.add(cat);
					
					linksList = APILocator.getContentletAPI().find(cats, Long.parseLong(languageID), true,  sortOrder , user, true);
	 			}
//				String q = "";
//				q = "+StructureName:Link +languageId:" + languageID + " +" + mode + ":true +path:" + path + "/*";
				//List<Contentlet> linksList = APILocator.getContentletAPI().search(q, -1, 0, sortOrder, APILocator.getUserAPI().getSystemUser(), false);

				String qLinkSemplice = "";
				qLinkSemplice = "+StructureName:Linksemplice +languageId:" + languageID + " +live:true +parentPath:" + path + "/";

				List<Contentlet> linksSempliceList = APILocator.getContentletAPI().search(qLinkSemplice, -1, 0, "modDate desc", user, false);

				stringbuf.append("<div id=\"dt_" + folderTranslation + "\">");
				stringbuf.append("<div class=\"titolo\"><h3>" + folderTranslation + "</h3></div>");
				stringbuf.append("<ul>");
				nullLast(linksList);
				

				for (Contentlet link : linksList) {
					System.out.println(  " link  " + link );
					String titolo;
					String sommario = "";
					String href = "";
					String type = "";
					String labelType = "";
					String size = "";
					String allegatoId = "";
					String src = null;
					String ms = "";

					if (UtilMethods.isSet(link.getStringProperty("titoloLungo"))) {
						titolo = link.getStringProperty("titoloLungo");
					} else {
						titolo = link.getStringProperty("titolo");
					}
					String mostraS =link.getStringProperty("mostraSommario");
					if (UtilMethods.isSet( mostraS )) {
						ms = mostraS;
						if (link.getStringProperty("sommario") != null) {
							sommario = link.getStringProperty("sommario");
						}	
					}

					String linkType = link.getStringProperty("linkType");

					if ("A".equals(linkType)) {
						Contentlet allegatoDettaglio;

						if (UtilMethods.isSet(link.getStringProperty("allegatoId"))) {
							allegatoId = link.getStringProperty("allegatoId");
						} else if (UtilMethods.isSet(link.getStringProperty("allegato"))) {
							allegatoId = link.getStringProperty("allegato");
						}

						String queryForAllegatoDettaglio = "";

						if (allegatoId != null && !"".equals(allegatoId)) {

							queryForAllegatoDettaglio = "+identifier:" + allegatoId + " +languageId:" + languageID;
							List<Contentlet> allegatoList = APILocator.getContentletAPI().search( queryForAllegatoDettaglio, -1, 0, null, user, false);

							if (allegatoList.size() > 0) {
								allegatoDettaglio = allegatoList.get(0);
								File file = allegatoDettaglio.getBinary("fileAsset");
								size = FileSizeUtil.getsize(file);
								type = file.getName().substring(file.getName().lastIndexOf("."));
								labelType = assignType(type);
								String id = allegatoDettaglio.getIdentifier();
								Identifier i = APILocator.getIdentifierAPI().loadFromCache(id);
								if (i == null) {
									i = APILocator.getIdentifierAPI().find(id);
								}

								src = assignIcon(type);
								if (src.equals(WMV)) {
									href = i.getPath() + "internal&action=video.action";
								} else {
									href = i.getPath();
								}

							}

						}

					} else if ("I".equals(linkType)) {
						String li = link.getStringProperty("linkInterno");

						if (li != null && !"".equals(li) && li.endsWith(".html")) {

							HTMLPage linkedPage = pageAPI.loadPageByPath(li, hostId);
							Template linkedPageTemplate = null;

							if(UtilMethods.isSet(linkedPage) && linkedPage.isLive()){
								linkedPageTemplate = templateApi.findLiveTemplate(linkedPage.getTemplateId(), user, true);

								Folder folder = folderAPI.findFolderByPath(li.substring(0, li.lastIndexOf("/")), hostId, user, true);
								href = li.substring(0, li.lastIndexOf("/") + 1);

								if(UtilMethods.isSet(linkedPageTemplate) && linkedPageTemplate.getTitle().toLowerCase().contains("video")){							
									src = WMVICON;
								} else if (folderAPI.findMenuItems(folder, user, true).size() > 0) {
									src = FOLDERICON;								
								} else {
									src = PAGEICON;
								}
							}
						}

					} else {

					//	if (link.getStringProperty("identificativo") != null && !"".equals(link.getStringProperty("identificativo"))) {
						//	href = path + "/" + link.getStringProperty("identificativo");
						   href =  link.getStringProperty("linkEsterno");
							src = ESTERNO;
					//	}
					}

					if (!"".equals(href)) {
						stringbuf.append("<li>");
						stringbuf.append("<a href=\"" + href + "\"><img src=\"" + src + "\" class=\"ico\" width=\"14\" height=\"14\">" + titolo + "</a> " + buildSpan(link, size, labelType));
						if (!"".equals(ms) && !"".equals(sommario)) {
							stringbuf.append("<br/>" + sommario);
						}
						stringbuf.append("</li>");
					}

				}

				Contentlet linkSemplice = null;
				String linkSempliceType = "";
				if (linksSempliceList.size() > 0){
					linkSemplice = linksSempliceList.get(0);
					linkSempliceType = linkSemplice.getStringProperty("linkType");
				}

				if (linkSemplice != null && (!"".equals(linkSempliceType) && "I".equals(linkSempliceType))) {

					String linkSempHref = "";
					if("I".equals(linkSempliceType)){
						linkSempHref = linkSemplice.getStringProperty("linkInterno");
					} else {
						linkSempHref = path;
					}

					String titolo = linkSemplice.getStringProperty("titolo");
					stringbuf.append("<li class=\"noborder\"><a href=\"" + linkSempHref + "\" class=\"altri\">" + titolo + "</a></li>");
				}

				stringbuf.append("</ul>");
				stringbuf.append("</div>");

			}
		}catch (Exception e) {
			e.printStackTrace();
		}

		return stringbuf.toString();

	}

}
