package it.bankit.website.viewtool.list;

import it.bankit.website.cache.BankitCache;
import it.bankit.website.util.FolderAnnoComparator;
import it.bankit.website.util.FolderNameComparator;
import it.bankit.website.util.ViewToolUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public class PaginationUtil implements ViewTool {

	private HttpServletRequest req;
	private boolean ADMIN_MODE;
	private Context context;
	private Host currentHost;
	private static final Logger LOG = Logger.getLogger(PaginationUtil.class);
	private static LanguageAPI langAPI;
	private static FolderAPI folderAPI;

	private static final String[] MONTH_NAME = { "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno", "luglio", "agosto", "settembre", "ottobre",
		"novembre", "dicembre" };

	private static Map<String, List<Folder>> listaAnniCache = new HashMap<String, List<Folder>>();

	@Override
	public void init(Object initData) {
		this.context = ((ViewContext) initData).getVelocityContext();
		this.req = ((ViewContext) initData).getRequest();
		HttpSession session = req.getSession();
		langAPI = APILocator.getLanguageAPI();
		folderAPI = APILocator.getFolderAPI();
		ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
		try {
			this.currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		} catch (Exception e) {
			LOG.error("Error finding current host", e);
		}
	}

	// recupera la current page in base al parametro "Parameter" della request
	public Integer getCurrentPage() {
		Integer curPage = 1;
		String requestUri = "pagination.currentPage:" + req.getRequestURI();
		String action = req.getParameter("action");
		if (action != null && action.equalsIgnoreCase("navpage.action")) {
			String currentPageParameter = req.getParameter("Parameter");
			if (UtilMethods.isSet(currentPageParameter)) {
				curPage = Integer.parseInt(currentPageParameter);
				req.getSession().setAttribute(requestUri, curPage);
			} else {
				Integer sessionPageParameter = (Integer) req.getSession().getAttribute(requestUri);
				if (sessionPageParameter != null) {
					curPage = sessionPageParameter;
				}
			}
		}
		return curPage;
	}


	public Folder getListingParentFolder(Folder currentFolder) {
		String keyAnno1 = getFolderKeyLanguage(currentFolder);
		try {
			
			int anno = Integer.parseInt(keyAnno1.trim());
			if (anno > 1980) {
				Folder folder = folderAPI.findParentFolder(currentFolder, APILocator.getUserAPI().getSystemUser(), true);
				return folder;
			}
		} catch (Exception e) {	}
		try {
			boolean isParent = hasChildrenAnno(currentFolder);
			if (isParent) {
				return currentFolder;
			}
			else {
				boolean isChild = hasParentAnno(currentFolder);
				if (isChild) {
					Folder folder = folderAPI.findParentFolder(currentFolder, APILocator.getUserAPI().getSystemUser(), true);
					return folder;
				}
			}
		} catch (Exception e) {
			//LOG.fatal(e.getStackTrace());
		}
		return currentFolder;

	}


	public Folder[] getSubFolder(Folder currentFolder, HttpServletRequest request, boolean showOnMenu) {
		List<Folder> fList = new ArrayList<Folder>();
		try {
			fList = folderAPI.findSubFolders(currentFolder, APILocator.getUserAPI().getSystemUser(), false);
			return fList.toArray(new Folder[fList.size()]);
		} catch (Exception ex) {
			LOG.fatal(ex.getStackTrace());
			return null;
		}
	}

	public Folder getFirstFolder(Folder currentFolder ) {
		List<Folder> fList = new ArrayList<Folder>();
		try {
			fList = BankitCache.getInstance().findSubFolders(currentFolder); 
			if( fList!= null && fList.size() >0  ){
				Collections.sort( fList, new FolderNameComparator());
				return fList.get( 0 );
			}
		} catch (Exception ex) {
			LOG.fatal(ex.getStackTrace());
			return null;
		}
		return null;
	}

	// restituisce l'elenco degli anni!!
	public List<Folder> getListaAnni( Folder folderSelezionato ) {
		List<Folder> fList = new ArrayList<Folder>();
		List<Folder> fListNew = new ArrayList<Folder>();
		Folder parent = null;
		Language language = ViewToolUtil. getRequestLanguage( req );
		String langCode = language.getId() +"";
		try {
			String keyAnnoSel = getFolderKeyLanguage(folderSelezionato);
			try {
				if( UtilMethods.isSet( keyAnnoSel )){
					Integer.parseInt( keyAnnoSel.trim() );
				}
				parent = folderAPI.findParentFolder(folderSelezionato, APILocator.getUserAPI().getSystemUser(), true);
			} catch (Exception e) {
				parent = folderSelezionato;
			}
			String keyCacheFolder =langCode+ "_"+parent.getInode();

			/*	if( listaAnniCache.containsKey( keyCacheFolder ) ){
				//return listaAnniCache.get( keyCacheFolder );
			}else { */
			fList = BankitCache.getInstance().findSubFolders(parent);
			for (Folder subFolderAnno : fList) {
				String keyAnno1 = getFolderKeyLanguage( subFolderAnno );
				try {
					if( UtilMethods.isSet( keyAnno1 )){
						Integer.parseInt( keyAnno1.trim() );
					}
					fListNew.add( subFolderAnno );
				} catch (Exception e) {
				}
			}
			Collections.sort( fListNew, new FolderAnnoComparator());
			listaAnniCache.put(keyCacheFolder , fListNew );
			/*	}*/
	} catch (Exception ex) {
		LOG.fatal(ex.getStackTrace());
	}
	return fListNew;

	}


	private String getFolderKeyLanguage(Folder folder) {
		String keyAnno1 = "";
		try {
			Identifier id = APILocator.getIdentifierAPI().loadFromCache(folder.getIdentifier());
			String uri = id.getURI();
			String conString = ViewToolUtil.convertPath( uri );
			Language language = ViewToolUtil.getRequestLanguage( req );
			keyAnno1 = APILocator.getLanguageAPI().getStringKey( language, conString );
		} catch (Exception e) {
			//LOG.fatal(e.getStackTrace());
		}
		return keyAnno1;
	}

	private boolean hasChildrenAnno(Folder folderC) throws Exception {
		List<Folder> fList = BankitCache.getInstance().findSubFolders( folderC );
		if (fList != null && fList.size() > 0) {
			for( Folder folder : fList ){
				String keyAnno1 = getFolderKeyLanguage(folder);
				try {
					int anno = Integer.parseInt(keyAnno1);
					if (anno > 1980) {
						return true;
					}
				} catch (Exception e) {
				}
			}
		}
		return false;
	}

	public Folder getCurrentFolderAnno(Folder currentFolder) {
		String keyAnno1 = getFolderKeyLanguage(currentFolder);
		try {
			int anno = Integer.parseInt(keyAnno1);
			if (anno > 1980) {
				return currentFolder;
			}
		} catch (Exception e) {
		}
		try {
			boolean isParent = hasChildrenAnno(currentFolder);
			if (isParent) {
				return getFisrtFolderForListing(currentFolder);
			}

			else {
				boolean isChild = hasParentAnno(currentFolder);
				if (isChild) {
					Folder folder = folderAPI.findParentFolder(currentFolder, APILocator.getUserAPI().getSystemUser(), true);
					return folder;
				}
			}
		} catch (Exception e) {
			//LOG.fatal(e.getStackTrace());
		}
		return currentFolder;
	}

	private boolean hasParentAnno(Folder folderC) throws Exception {

		Folder folder = folderAPI.findParentFolder(folderC, APILocator.getUserAPI().getSystemUser(), true);
		String keyAnno1 = getFolderKeyLanguage(folder);
		try {
			int anno = Integer.parseInt(keyAnno1);
			if (anno > 1980) {
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

	public List<Folder> getListaMesiFolder(Folder currentFolder) {
		List<Folder> fList = new ArrayList<Folder>();
		try {
			fList = BankitCache.getInstance().findSubFolders( currentFolder );
			Collections.sort(fList, new FolderAnnoComparator());
		} catch (Exception ex) {
			//LOG.fatal(ex.getStackTrace());
		}
		return fList;
	}

	public Map<String, String> getListaMesi(Folder currentFolder) {
		String[] mesi = MONTH_NAME;
		SortedMap lista = new TreeMap();
		try {
			int anno = getAnnoFromFolder(currentFolder);
			Calendar calendar = Calendar.getInstance();
			int nowAnno = calendar.get(Calendar.YEAR);
			int nowMese = calendar.get(Calendar.MONTH);
			if (anno == nowAnno) {
				mesi = (String[]) ArrayUtils.subarray(mesi, 0, nowMese + 1);
			}
			int y = mesi.length - 1;
			for (int i = 0; i < mesi.length; i++) {
				if (y < 9) {
					lista.put("0" + (y + 1), mesi[y]);
				} else {
					lista.put((y + 1) + "", mesi[y]);
				}

				y--;
			}
		} catch (Exception ex) {
			LOG.fatal(ex.getStackTrace());
		}
		return lista;
	}

	public String createUrlCambio(String baseUrl, Folder currentFolder, String currentMese) {
		String url = baseUrl;
		if (baseUrl.indexOf("." + com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION")) != -1) {
			url = url.substring(0, url.lastIndexOf("/"));
		}
		url = url + "/" + currentFolder.getName() + "/" + getNumeroMese(currentMese) + "/cambi_rif_";

		return url;
	}

	public int getNumeroMese(String current) {
		for (int i = 0; i < MONTH_NAME.length; i++) {
			if (MONTH_NAME[i].equalsIgnoreCase(current)) {
				return i;
			}
		}
		return 0;
	}

	public String getCurrentMese(Folder currentFolder, String currentMese) {
		String[] mesi = MONTH_NAME;
		try {
			if (!UtilMethods.isSet(currentMese)) {
				String reqString = req.getRequestURI();
				String baseCambiUrl = "cambi/rif/";
				// elima pagina
				if (reqString.endsWith("." + com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION"))) {
					reqString = reqString.substring(0, reqString.lastIndexOf("/"));
				}
				if (reqString.indexOf("cambi/rif/") != -1) {
					reqString = reqString.substring(reqString.indexOf("cambi/rif/") + baseCambiUrl.length());

					if (reqString.indexOf("/") != -1) {
						// ho anno e mese
						reqString = reqString.substring(reqString.indexOf("/") + 1);
						int indexMese = 0;
						if (reqString.startsWith("0")) {
							indexMese = Integer.parseInt(reqString.replaceFirst("0", ""));
						} else {
							indexMese = Integer.parseInt(reqString);
						}
						currentMese = mesi[indexMese - 1];
						return currentMese;
					} else {
						int anno = getAnnoFromFolder(currentFolder);
						Calendar calendar = Calendar.getInstance();
						int nowAnno = calendar.get(Calendar.YEAR);
						int nowMese = calendar.get(Calendar.MONTH);
						if (anno == nowAnno) {
							currentMese = mesi[nowMese];
						} else if (anno != -1) {
							currentMese = mesi[0];
						}
					}
					// reqString
				} else {
					int anno = getAnnoFromFolder(currentFolder);
					Calendar calendar = Calendar.getInstance();
					int nowAnno = calendar.get(Calendar.YEAR);
					int nowMese = calendar.get(Calendar.MONTH);
					if (anno == nowAnno) {
						currentMese = mesi[nowMese];
					} else if (anno != -1) {
						currentMese = mesi[0];
					}
				}
			}
		} catch (Exception ex) {
			LOG.fatal(ex.getStackTrace());
		}
		return currentMese;

	}

	public Folder getFisrtFolderForListing(Folder currentFolder) {
		List<Folder> fList = new ArrayList<Folder>();
		try {
			fList = BankitCache.getInstance().findSubFolders(currentFolder);
			List<Folder> fListNew = new ArrayList<Folder>();
			for (Folder f : fList) {
				String keyAnno1 = getFolderKeyLanguage(f);
				try {
					Integer.parseInt(keyAnno1);
					fListNew.add(f);
				} catch (Exception e) {
				}
			}
			Collections.sort(fListNew, new FolderAnnoComparator());
			return fListNew.get(0);

		} catch (Exception ex) {
			LOG.fatal(ex.getStackTrace());
			return null;
		}

	}

	public int getAnnoFromFolder(Folder currentFolder) throws Exception {
		String reqString = req.getRequestURI();
		String baseCambiUrl = "cambi/rif/";
		// elima pagina
		if (reqString.endsWith("." + com.dotmarketing.util.Config.getStringProperty("VELOCITY_PAGE_EXTENSION"))) {
			reqString = reqString.substring(0, reqString.lastIndexOf("/"));
		}
		if (reqString.indexOf("cambi/rif/") != -1) {
			reqString = reqString.substring(reqString.indexOf("cambi/rif/") + baseCambiUrl.length());

			if (reqString.indexOf("/") != -1) {
				// ho anno e mese
				reqString = reqString.substring(0, reqString.indexOf("/"));
				Integer.parseInt(reqString);
			}
		} else {
			String anno1 = currentFolder.getName();
			String keyAnno1 = APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(), anno1);
			try {
				return Integer.parseInt(keyAnno1);
			} catch (Exception e) {
				LOG.fatal(e.getStackTrace());
			}
			Folder f = getFisrtFolderForListing(currentFolder);
			anno1 = f.getName();
			keyAnno1 = APILocator.getLanguageAPI().getStringKey(APILocator.getLanguageAPI().getDefaultLanguage(), anno1);
			int anno1Int = 0;
			if (UtilMethods.isSet(keyAnno1)) {
				if (keyAnno1.length() != 4) {
					throw new Exception();
				} else {
					anno1Int = Integer.parseInt(keyAnno1);
				}
			}
			return anno1Int;

		}
		return Integer.parseInt(reqString);
	}

	public int getPosizionCurrentAnno(Folder currentFolder) {

		List<Folder> lista = getListaAnni(currentFolder);
		Folder[] listaFolder = lista.toArray(new Folder[lista.size()]);
		int y = -1;
		if (listaFolder != null) {
			for (int i = 0; i < listaFolder.length; i++) {
				Folder f = listaFolder[i];
				if (f.getInode().equalsIgnoreCase(currentFolder.getInode())) {
					y = i;
					break;
				}
			}
		}
		return y;
	}
}
