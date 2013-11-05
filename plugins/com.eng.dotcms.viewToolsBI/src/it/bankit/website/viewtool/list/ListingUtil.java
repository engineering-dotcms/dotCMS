package it.bankit.website.viewtool.list;
import it.bankit.website.cache.BankitCache;
import it.bankit.website.util.CollectionUtil;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.content.ContentMap;
import com.dotmarketing.viewtools.content.PaginatedContentList;
import com.liferay.portal.model.User;

public class ListingUtil implements ViewTool {

	private ContentletAPI conAPI;
	private HttpServletRequest req;
	private UserWebAPI userAPI;
	private User user = null;
	private int MAX_LIMIT = 100;
	private boolean ADMIN_MODE;
	private boolean PREVIEW_MODE;
	private boolean EDIT_MODE;
	private boolean EDIT_OR_PREVIEW_MODE;
	private Context context;
	private Host currentHost;
	private SortUtil sortUtil;


	private String innerQuery = "select contentlet.identifier from contentlet contentlet  , " +
	"   tree t , structure struct, contentlet_version_info cvi " +
	"   where  cvi.identifier = contentlet.identifier and   relation_type = 'Parent_Dettaglio-Child_Link' " +
	" 	and struct.velocity_var_name =  'Link'  and cvi.identifier = child  and struct.inode = contentlet.structure_inode "; 

	@Override
	public void init(Object initData) {
		conAPI = APILocator.getContentletAPI();
		userAPI = WebAPILocator.getUserWebAPI();
		this.context = ((ViewContext) initData).getVelocityContext();
		this.req = ((ViewContext) initData).getRequest();
		try {
			user = userAPI.getLoggedInFrontendUser(req);
			HttpSession session = req.getSession();
			ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
			PREVIEW_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
			EDIT_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
			if (EDIT_MODE || PREVIEW_MODE) {
				EDIT_OR_PREVIEW_MODE = true;
			}
			this.currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(req);
		} catch (Exception e) {
			Logger.error(ListingUtil.class,"Error finding current host", e);
		}
	}



	public List<ContentMap> findContentletByImportance(String path, Folder f, String orderBy) throws DotDataException, DotStateException, DotSecurityException {
		String stateQuery =  addSQLDefaultToQuery();		
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		String parentPath = APILocator.getIdentifierAPI().find(f.getIdentifier()).getURI() + "/";
		Structure struct = StructureCache.getStructureByVelocityVarName("Link");
		String fieldName = struct.getFieldVar("importante").getFieldContentlet();
		String fieldName1;

		if (orderBy!=null && !"".equals(orderBy)) {
			fieldName1 = struct.getFieldVar(orderBy).getFieldContentlet();
		} else {
			fieldName1 = struct.getFieldVar("dataEmanazione").getFieldContentlet();
		}



		String query = "select {contentlet.*} from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where  "
			+ "(contentlet.inode = contentlet_1_.inode) " + "and   cvi.identifier = identifier.id  " + "  and  cvi.identifier = contentlet.identifier "
			+ " and cvi.lang = contentlet.language_id" + " 	and st.inode = contentlet.structure_inode" + "  and asset_type = 'contentlet' "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 

			+ "  and st.velocity_var_name =  'Link' and " + " parent_path like '" + parentPath + "%' and contentlet." + fieldName

			+ " like 'True,' "+ stateQuery + " order by  " + fieldName1 + "  desc"  ;
		Logger.debug(ListingUtil.class,  "findContentletByImportance query :  " + query  );
		hu.setSQLQuery(query);
		List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
		List<ContentMap> contentlets =  convertContentsToContentMap( fatties );
		return contentlets;
	}

	private String addSubfoldersQuery( Folder folder , boolean useParentFolder , String structureVarName , boolean deepSubFolder ){
		String queryPath = " ";
		try{
			if( folder != null ){
				List<Folder> subs =  BankitCache.getInstance().findSubFolders(folder);
				if( subs != null && subs.size() >0 ){		
					String folderParentURI = APILocator.getIdentifierAPI().loadFromCache( folder.getIdentifier()).getURI() + "/";						
					for (Folder f : subs  ){			
						if( f != null  && UtilMethods.isSet(  f.getIdentifier() )){
							Identifier identif = APILocator.getIdentifierAPI().loadFromCache( f.getIdentifier()) ;
							if( identif != null ){
								String folderURI = APILocator.getIdentifierAPI().loadFromCache( f.getIdentifier()).getURI() + "/";					
								queryPath = queryPath  + " parent_path like '" + folderURI + "' or";	
							}
						}
					}
					if( queryPath.endsWith("or") ){
						queryPath = " and ( st.velocity_var_name like '"+structureVarName+"' and ( " + queryPath.substring( 0 , queryPath.lastIndexOf("or") ) + " ) )";
					}
					if( useParentFolder ){
						queryPath = queryPath.substring( 0 , queryPath.lastIndexOf(")") );
						if( !deepSubFolder ){
							queryPath = " " + queryPath + "  or ( st.velocity_var_name like  'Link'   and  parent_path like '"+folderParentURI+"')  )";
						}else {
							queryPath = " " + queryPath + "  or ( st.velocity_var_name like  'Link'   and  parent_path like '"+folderParentURI+"%')  )";
						}
					}
				}
			}
		}catch (Exception e) {
			e.printStackTrace();

		}
		return queryPath;
	}

	private String addPullSortOrder( String sort , Field defaultFieldOrder ){
		String defaultSort = " order by   " ;
		if( !UtilMethods.isSet(sort)  ){
			defaultSort = defaultSort  + defaultFieldOrder.getFieldContentlet()+ " desc nulls last" ;
			defaultSort = defaultSort + " , lower(title) asc " ;
		}
		if( UtilMethods.isSet(sort) && sort.contains("D") ){
			defaultSort = defaultSort +    defaultFieldOrder.getFieldContentlet()   ;
			if( sort.contains("-D")){
				defaultSort = defaultSort + " desc nulls last ";
			}else{
				defaultSort = defaultSort + " asc nulls last ";
			}
		}
		if( UtilMethods.isSet(sort) && sort.contains("#") ){
			defaultSort = defaultSort + getCommaValue(defaultSort) +   " integer1 " ;
			if( sort.contains("-#")){
				defaultSort = defaultSort + " desc ";
			}else {
				defaultSort = defaultSort + " asc ";
			}
		}
		else if( UtilMethods.isSet(sort) && ( sort.contains("t") || sort.contains("T") )  ){
			defaultSort = defaultSort  + getCommaValue(defaultSort) +   " lower(title) " ;
			if( sort.contains("-T")){
				defaultSort = defaultSort + " desc ";
			}else {
				defaultSort = defaultSort + " asc ";
			}
		}
		return defaultSort ;
	}


	private List<ContentMap> selectContentlets( String completeQuery , int currentPage, int contentsPerPage , String sort ){
		List<ContentMap> contentlets = null;
		try{
			HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
			int offset = contentsPerPage * (currentPage - 1);
			hu.setSQLQuery( completeQuery );
			hu.setFirstResult( offset );
			hu.setMaxResults( contentsPerPage );
			List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
			contentlets =  convertContentsToContentMap( fatties );
			if( UtilMethods.isSet(sort) && ( sort.indexOf("#") != -1  ||  sort.indexOf("t") != -1    ||  sort.indexOf("T") != -1   )){
				return  contentlets;
			}
			if( UtilMethods.isSet(sort) && sort.indexOf("D")  == -1 ){
				sort = sort+"-D" ;
			}
			else if( !UtilMethods.isSet(sort)){
				return  contentlets;
			}
			//sort deve essere fornito nel valore 
			String valore = getSortUtil().generateSortOrder(sort, "Dettaglio","Link" );
			contentlets = (List<ContentMap>) getSortUtil().sort(contentlets, valore );		
		}catch (Exception e) {
			e.printStackTrace();
			Logger.error(ListingUtil.class,"Error selectContentlets ", e);
		}
		return contentlets;
	}


	public List<ContentMap> pullCLDirectContentlets(Folder folder, int currentPage, int contentsPerPage, String sort ) throws Exception {
		String stateQuery =  addSQLDefaultToQuery();
		Object obj = req.getSession().getAttribute("com.dotmarketing.htmlpage.language");
		if( UtilMethods.isSet(obj ) ){
			String language = ((Object) req.getSession().getAttribute("com.dotmarketing.htmlpage.language")).toString();
			long lDefault = APILocator.getLanguageAPI().getDefaultLanguage().getId();
			String lDefaultString = Long.toString(lDefault );
			if( !lDefaultString.equalsIgnoreCase(language) ){
				return pullContentlets( folder, currentPage, contentsPerPage, sort);

			}
		}
		Field f = StructureCache.getStructureByVelocityVarName("DettaglioCluster").getFieldVar("dataEmanazione");
		String defaultSort = addPullSortOrder( sort , f );
		String query = "select {contentlet.*} from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where   	  "
			+ "(contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "
			+ " and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 
			+ addSubfoldersQuery( folder , true , "DettaglioCluster", false ) 
			+ stateQuery 
			+ defaultSort  ;

		return  selectContentlets( query ,  currentPage,  contentsPerPage,  sort );


	}

	public List<ContentMap> pullDLDirectContentlets(Folder folder, int currentPage, int contentsPerPage, String sort ) throws Exception {
		String stateQuery =  addSQLDefaultToQuery();
		Field f = StructureCache.getStructureByVelocityVarName("Dettaglio").getFieldVar("dataEmanazione");	
		String defaultSort = addPullSortOrder( sort , f );

		String folderURI = APILocator.getIdentifierAPI().loadFromCache(  folder.getIdentifier()).getURI() + "/";

		//		String innerQuery = "select contentlet.identifier from contentlet contentlet  , " +
		//		"   tree t , structure struct, contentlet_version_info cvi " +
		//		"   where  cvi.identifier = contentlet.identifier and   relation_type = 'Parent_Dettaglio-Child_Link' " +
		//		" 	and struct.velocity_var_name =  'Link'  and cvi.identifier = child  and struct.inode = contentlet.structure_inode "; 

		String query = "select {contentlet.*} from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where   	  "
			+ "(contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "
			+ " and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 
			+ addSubfoldersQuery( folder , true , "Dettaglio", false ) 
			+ stateQuery 
			+ " and cvi.identifier  not in ( "+innerQuery +" )" 
			+ " and cvi.identifier  not in ( "+ addQuery(folderURI) +" )"
			+ defaultSort  ;		 
		return  selectContentlets( query ,  currentPage,  contentsPerPage,  sort );


	}


	public List<ContentMap> pullDirectContentlets(Folder folder, int currentPage, int contentsPerPage, String sort) throws Exception {
		String stateQuery =  addSQLDefaultToQuery();
		Field f = StructureCache.getStructureByVelocityVarName("Dettaglio").getFieldVar("dataEmanazione");	
		String defaultSort = addPullSortOrder( sort , f );
		//		String innerQuery = "select contentlet.identifier from contentlet contentlet  , " +
		//		"   tree t , structure struct, contentlet_version_info cvi " +
		//		"   where  cvi.identifier = contentlet.identifier and   relation_type = 'Parent_Dettaglio-Child_Link' " +
		//		" 	and struct.velocity_var_name =  'Link'  and cvi.identifier = child  and struct.inode = contentlet.structure_inode "; 

		String query = "select {contentlet.*} from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where   	  "
			+ "(contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "
			+ " and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 
			+ "  and ( st.velocity_var_name like  'Dettaglio'  ) "
			+ addSubfoldersQuery( folder, false ,  "Dettaglio", false ) 
			+ stateQuery 
			+ " and cvi.identifier  not in ( "+innerQuery +" )" 
			+ defaultSort  ;		 
		return  selectContentlets( query ,  currentPage,  contentsPerPage,  sort );


	}


	//Recupera Dettaglio e Link a partire da un path ( scende in tutti i sotto path )
	public List<ContentMap> pullContentlets(Folder folder, int currentPage, int contentsPerPage, String sort) throws Exception {
		String stateQuery =  addSQLDefaultToQuery();	
		Field f = StructureCache.getStructureByVelocityVarName("Dettaglio").getFieldVar("dataEmanazione");	
		String defaultSort = addPullSortOrder( sort , f );
		String folderURI = APILocator.getIdentifierAPI().loadFromCache(  folder.getIdentifier()).getURI() + "/";

		//		String innerQuery = "select contentlet.identifier from contentlet contentlet  , " +
		//		"   tree t , structure struct, contentlet_version_info cvi " +
		//		"   where  cvi.identifier = contentlet.identifier and   relation_type = 'Parent_Dettaglio-Child_Link' " +
		//		" 	and struct.velocity_var_name =  'Link'  and cvi.identifier = child  and struct.inode = contentlet.structure_inode "; 

		String query = "select {contentlet.*} from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where  "
			+ "(contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "
			+ " and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 
			+ "	and st.velocity_var_name !=  'AllegatoDettaglio' and ( st.velocity_var_name like  'Dettaglio'  or st.velocity_var_name like  'Link' ) " 
			+ " and parent_path like '" + folderURI + "%' " +stateQuery 
			+ " and cvi.identifier  not in ( "+innerQuery +" )" 
			+ " and cvi.identifier  not in ( "+ addQuery(folderURI) +" )"
			+ defaultSort  ;

		return  selectContentlets( query ,  currentPage,  contentsPerPage,  sort );
	}


	//Recupera DettaglioCluster a partire da un path ( scende in tutti i sotto path )
	public List<ContentMap> pullClusterContentlets(Folder folder, int currentPage, int contentsPerPage, String sort) throws Exception {
		String stateQuery =  addSQLDefaultToQuery();		
		Field f = StructureCache.getStructureByVelocityVarName("DettaglioCluster").getFieldVar("dataEmanazione");	
		String defaultSort = " order by   " + f.getFieldContentlet()+ " desc nulls last" ;
		FieldsCache.getFieldsByStructureVariableName("DettaglioCluster");
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		String folderURI = APILocator.getIdentifierAPI().loadFromCache(  folder.getIdentifier()).getURI() + "/";
		int offset = contentsPerPage * (currentPage - 1);
		String query = "select {contentlet.*} from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where   	  "
			+ "(contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "			 
			+ " and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 
			+ "	and st.velocity_var_name like  'DettaglioCluster' "  + "	 and parent_path like '" + folderURI + "%' " +stateQuery 
			+ defaultSort  ;
		hu.setSQLQuery( query );
		hu.setFirstResult( offset );
		hu.setMaxResults( contentsPerPage );
		List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
		List<ContentMap> contentlets =  convertContentsToContentMap( fatties );
		if( UtilMethods.isSet(sort) && sort.indexOf("D")  == -1 ){
			sort = "-D"+sort ;
		}
		else if( !UtilMethods.isSet(sort)){
			sort = "-D#";
		}
		//sort deve essere fornito nel valore 
		String valore = getSortUtil().generateSortOrder(sort, "DettaglioCluster" );
		List contentletsToReturn  = (List<ContentMap>) getSortUtil().sort(contentlets, valore );		
		if( contentletsToReturn != null ){
			return contentletsToReturn;
		}
		return contentlets;
	}

	public PaginatedContentList<ContentMap> pullPerPage(String query, int currentPage, int contentsPerPage, String sort){
		PaginatedContentList<ContentMap> paginatedList = new PaginatedContentList<ContentMap>();
		try {
			List<Contentlet> contentlets=conAPI.search(addLuceneDefaultToQuery(query), MAX_LIMIT, -1, "modDate", user, true);
			List<ContentMap> contents=convertContentletToContentMap(contentlets);
			contents=CollectionUtil.sort( contents, sort );
			int startIndex=(currentPage-1)*contentsPerPage;
			int endIndex=Math.min((currentPage)*contentsPerPage,contents.size());
			int totalCount=contents.size();
			paginatedList.addAll(contents.subList( startIndex, endIndex ));
			paginatedList.setTotalResults(totalCount);
			paginatedList.setTotalPages((long)Math.ceil(((double)totalCount)/((double)contentsPerPage)));
			paginatedList.setNextPage(endIndex < totalCount);
			paginatedList.setPreviousPage(startIndex > 0);
		} catch ( Exception e ) {
			Logger.error( ListingUtil.class,"Error in pagination",e );
		} 
		return paginatedList;

	}


	// Totale contenlete di tipo link o dettaglio
	public int getTotalContentlets( Folder f, String sort ) throws Exception {
		String stateQuery =  addSQLDefaultToQuery();
		Field field = StructureCache.getStructureByVelocityVarName("Dettaglio").getFieldVar("dataEmanazione");	
		String defaultSort = " order by   " + field.getFieldContentlet()+ " desc nulls last" ;
		if( UtilMethods.isSet(sort) && sort.indexOf("D")  == -1 ){
			sort = "-D"+sort ;
		}
		else if( !UtilMethods.isSet(sort)){
			sort = "-D";
		}
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		String folderURI = APILocator.getIdentifierAPI().loadFromCache( f.getIdentifier() ).getURI() + "/";
		String query = "select {contentlet.*}  from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where   	  "
			+ "(contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "
			+ " 			and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 
			+ "	and st.velocity_var_name !=  'AllegatoDettaglio' and ( st.velocity_var_name like  'Dettaglio'  or st.velocity_var_name like  'Link' )  " + "	 and parent_path like '" + folderURI + "%' " +stateQuery 
			+ " and cvi.identifier  not in ( select child from tree where relation_type = 'Parent_Dettaglio-Child_Link' )" 
			+ " and cvi.identifier  not in ( "+ addQuery(folderURI) +" )"
			+ defaultSort ;
		hu.setSQLQuery(query);
		List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
		return fatties.size();

	}

	// Totale contenlete di tipo dettaglioCluster
	public int getTotalClusterContentlets( Folder f, String sort ) throws Exception {
		String stateQuery =  addSQLDefaultToQuery();
		Field field = StructureCache.getStructureByVelocityVarName("DettaglioCluster").getFieldVar("dataEmanazione");	
		String defaultSort = " order by  " + field.getFieldContentlet()+ " desc nulls last" ;
		if( UtilMethods.isSet(sort) && sort.indexOf("D")  == -1 ){
			sort = "-D"+sort ;
		}
		else if( !UtilMethods.isSet(sort)){
			sort = "-D";
		}
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		String folderURI = APILocator.getIdentifierAPI().loadFromCache( f.getIdentifier() ).getURI() + "/";
		String query = "select {contentlet.*}  from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where   	  "
			+ "(contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "
			+ " 			and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 
			+ "	and st.velocity_var_name !=  'AllegatoDettaglio' and st.velocity_var_name like  'DettaglioCluster'  and parent_path like '" + folderURI + "%' " +stateQuery 
			+ " and cvi.identifier  not in ( select child from tree where relation_type = 'Parent_Dettaglio-Child_Link' )" 
			+ " and cvi.identifier  not in ( "+ addQuery(folderURI) +" )"
			+ defaultSort ;
		hu.setSQLQuery(query);
		List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
		return fatties.size();

	}


	public int getDirectTotalContentlets( Folder f, String sort ) throws Exception {
		String stateQuery =  addSQLDefaultToQuery();
		Field field = StructureCache.getStructureByVelocityVarName("Dettaglio").getFieldVar("dataEmanazione");	
		String defaultSort = " order by   " + field.getFieldContentlet()+ " desc nulls last" ;
		if( UtilMethods.isSet(sort) && sort.indexOf("D")  == -1 ){
			sort = "-D"+sort ;
		}
		else if( !UtilMethods.isSet(sort)){
			sort = "-D";
		}
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		String folderURI = APILocator.getIdentifierAPI().loadFromCache( f.getIdentifier() ).getURI() + "/";
		String query = "select {contentlet.*}  from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where   	  "
			+ "(contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "
			+ " 			and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 
			+ "	 and ( st.velocity_var_name like  'Dettaglio'  )  " 
			+ addSubfoldersQuery( f  , false , "Dettaglio", false ) 
			+ stateQuery 
			+ " and cvi.identifier  not in ( select child from tree where relation_type = 'Parent_Dettaglio-Child_Link' )" 
			+ " and cvi.identifier  not in ( "+ addQuery(folderURI) +" )"
			+ defaultSort ;
		hu.setSQLQuery(query);
		List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
		return fatties.size();

	}

	public int getDLDirectTotalContentlets( Folder f, String sort ) throws Exception {
		String stateQuery =  addSQLDefaultToQuery();
		Field field = StructureCache.getStructureByVelocityVarName("Dettaglio").getFieldVar("dataEmanazione");	
		String defaultSort = addPullSortOrder( sort , field );
		HibernateUtil hu = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
		String folderURI = APILocator.getIdentifierAPI().loadFromCache( f.getIdentifier() ).getURI() + "/";
		String query = "select {contentlet.*}  from   contentlet contentlet , "
			+ " inode contentlet_1_  ,identifier identifier , contentlet_version_info cvi, structure st  " + " where   	  "
			+ "(contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "
			+ " 			and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  "
			+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 
			+ "	 and ( st.velocity_var_name like  'Dettaglio' or st.velocity_var_name like  'Link'  )  " 
			+ addSubfoldersQuery( f  , true , "Dettaglio", false ) 
			+ stateQuery 
			+ " and cvi.identifier  not in ( select child from tree where relation_type = 'Parent_Dettaglio-Child_Link' )" 
			+ " and cvi.identifier  not in ( "+ addQuery(folderURI) +" )"
			+ defaultSort ;
		hu.setSQLQuery(query);
		List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties = hu.list();
		return fatties.size();

	}


	private String addQuery( String folderURI ) {
		String stateQuery =  addSQLDefaultToQuery();
		String q = "select id from identifier ,  inode contentlet_1_  ,contentlet contentlet , contentlet_version_info cvi, structure st " +
		" where   parent_path like '" + folderURI + "'  and asset_type = 'contentlet'  and st.velocity_var_name ='Dettaglio'  "
		+ " and (contentlet.inode = contentlet_1_.inode)  	and   cvi.identifier = identifier.id   and   cvi.identifier = contentlet.identifier "
		+ " and identifier.host_inode = '" + currentHost.getIdentifier() +"'" 			 		
		+ " and cvi.lang = contentlet.language_id  and st.inode = contentlet.structure_inode  " + stateQuery ;

		return q;
	}


	private List<ContentMap> convertContentsToContentMap( List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatties ){
		List<ContentMap> contentlets = new ArrayList<ContentMap>();
		try{
			for (com.dotmarketing.portlets.contentlet.business.Contentlet fatCont : fatties) {
				ContentMap cMAp = new ContentMap(conAPI.convertFatContentletToContentlet(fatCont), user, EDIT_OR_PREVIEW_MODE, currentHost, context);
				contentlets.add(cMAp);
			}
		}catch (Exception e) {
			Logger.error(ListingUtil.class,"convertContentsToContentMap " + e );
		}
		return contentlets;
	}

	private String addLuceneDefaultToQuery(String query){
		String q = query;
		if(!query.contains("languageId")){
			if(UtilMethods.isSet(req.getSession().getAttribute("com.dotmarketing.htmlpage.language"))){
				q += " +languageId:" + req.getSession().getAttribute("com.dotmarketing.htmlpage.language");
			} 
		}
		if(!(query.contains("live:") || query.contains("working:") )){      
			if(EDIT_OR_PREVIEW_MODE){
				q +=" +working:true ";
			}else{
				q +=" +live:true ";
			}
		}
		if(!UtilMethods.contains(query,"deleted:")){      
			q+=" +deleted:false ";
		}
		return q;
	}
	private String addSQLDefaultToQuery( ) {
		String q = "";
		if (UtilMethods.isSet( req.getSession().getAttribute( com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE )) ) {
			q += " and cvi.lang  = " + req.getSession().getAttribute( com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE );
		}
		if (EDIT_OR_PREVIEW_MODE) {
			q += " and  ( contentlet.inode = cvi.working_inode or  contentlet.inode =  cvi.live_inode ) ";
		} else {
			q += " and  contentlet.inode = cvi.live_inode ";
		}
		q += " and cvi.deleted =  " + DbConnectionFactory.getDBFalse();
		return q;
	}

	private List<ContentMap> convertContentletToContentMap(List<Contentlet> contentlets){
		List<ContentMap> contents=new ArrayList<ContentMap>(contentlets.size());
		for(Contentlet contentlet:contentlets){
			contents.add( new ContentMap(contentlet,user,EDIT_OR_PREVIEW_MODE,currentHost,context) );
		}
		return contents;
	}


	private String getCommaValue(  String defaultSort  ){
		if(  UtilMethods.isSet(defaultSort) ){
			if( defaultSort.indexOf(" desc ")!= -1){
				return " , ";
			}
			if(  defaultSort.indexOf(" asc ") !=-1 )  {
				return " , ";
			}
		}
		return " ";
	}

	private SortUtil getSortUtil(){
		if( sortUtil == null ){
			sortUtil = new SortUtil();
		}
		return sortUtil;
	}

}
