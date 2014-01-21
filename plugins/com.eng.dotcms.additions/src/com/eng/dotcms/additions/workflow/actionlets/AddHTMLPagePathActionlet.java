package com.eng.dotcms.additions.workflow.actionlets;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class AddHTMLPagePathActionlet extends WorkFlowActionlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<String> homePageStructures; 
	private static String START_PATH_COMMENT = "Il contenuto è consultabile al/ai seguente/i percorso/i:";
	private static String PRE_SERVLET = "/servlets/workflowRedirect?p=";
		
	{
		homePageStructures = new ArrayList<String>();
		homePageStructures.add("BoxSidebar");
		homePageStructures.add("InfoNews");
		homePageStructures.add("Notiziasecondaria");
		homePageStructures.add("Strillonp");
	}
	@Override
	public List<WorkflowActionletParameter> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return "Add HTMLPage path to contents";
	}

	@Override
	public String getHowTo() {
		return "This actionlet returns into the comments the path in which the contents are used";
	}

	@Override
	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {		
		try{
			String SERVER_PORT = Config.CONTEXT.getAttribute("WEB_SERVER_HTTP_PORT").toString();			
			String SERVER_SCHEMA = Config.CONTEXT.getAttribute("WEB_SERVER_SCHEME").toString();			
			Host currentHost = APILocator.getHostAPI().find(processor.getContentlet().getHost(), APILocator.getUserAPI().getSystemUser(), true);
			Logger.info(getClass(), "Start add path to comments");
			WorkflowComment lastCommentWithPaths = new WorkflowComment();
			// retrieve the last comment on this task
			List<WorkflowComment> comments = APILocator.getWorkflowAPI().findWorkFlowComments(processor.getTask());
			WorkflowComment lastComment = null;
			if(comments.size()>0)
				lastComment = comments.get(0);
			String _lastCommentId = (lastComment!=null)?lastComment.getId():"NULL";
			Logger.debug(getClass(), "Last comment on this task: " + _lastCommentId);
			List<String> paths = new ArrayList<String>();
			if(processor.getContentlet().getStructure().getStructureType()!=Structure.STRUCTURE_TYPE_FILEASSET){
				Logger.info(getClass(), "Ecccomi!!!!");
				Logger.info(getClass(), "Structure Type: " + processor.getContentlet().getStructure().getStructureType());
				paths = getHTMLPagePaths(processor.getContentlet(), currentHost);
			}else {
				Logger.info(getClass(), "Call getFileAssetPath");
				paths = getFileAssetPath(processor.getContentlet(), currentHost);
				Logger.info(getClass(), "Path size: " + paths.size());
			}
			Logger.debug(getClass(), "Number of pages into the contentlet path: " + paths.size());
			if(paths.size()>0){
				lastCommentWithPaths.setPostedBy(processor.getContentlet().getModUser());			
				StringBuilder commentText = new StringBuilder();
				if(null!=lastComment && UtilMethods.isSet(lastComment.getComment())){
					Logger.info(getClass(), "There is previous comment...");
					String lastCommentString = lastComment.getComment();
					if(lastCommentString.indexOf("_path_exists_")>0)
						commentText.append(lastCommentString.substring(lastCommentString.indexOf("_path_exists_")));
					else
						commentText.append(lastCommentString);
					commentText.append("<br /><br />");
				}
				commentText.append(START_PATH_COMMENT);
				commentText.append("<br />");
				commentText.append("<ul>");
				for(String page:paths){
					Logger.info(getClass(), "PATHHHH: " + page);
					String[] splitted = page.split("[|]");
					commentText.append("<li><a id=\"_path_exists_\" href=\"");
					commentText.append(SERVER_SCHEMA);
					commentText.append("://");
					commentText.append(currentHost.getHostname());
					if(!SERVER_PORT.equals("80")){
						commentText.append(":");
						commentText.append(SERVER_PORT);
					}
					commentText.append(PRE_SERVLET);
					commentText.append(splitted[1]);
					commentText.append("\">");
					commentText.append(splitted[0]);
					commentText.append("</a>");
					commentText.append("</li>");
				}
				commentText.append("</ul>");
				lastCommentWithPaths.setComment(commentText.toString());
				lastCommentWithPaths.setWorkflowtaskId(processor.getTask().getId());
				Logger.info(getClass(), "Save the comment...");
				if(null!=lastComment)
					APILocator.getWorkflowAPI().deleteComment(lastComment);
				APILocator.getWorkflowAPI().saveComment(lastCommentWithPaths);
				processor.setWorkflowMessage(lastCommentWithPaths.getComment());
				processor.getContentlet().setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, lastCommentWithPaths.getComment());
					Logger.info(getClass(), "Comment saved.");
			}
		}catch(DotDataException e){
			e.printStackTrace();
		}catch (DotSecurityException e) {
			e.printStackTrace();
		}
	}
	
	private List<String> getHTMLPagePaths(Contentlet contentlet, Host currentHost) throws DotDataException, DotSecurityException {
		Logger.debug(getClass(), "Current host: " + currentHost.getHostname());
		List<String> paths = new ArrayList<String>();
		if(!homePageStructures.contains(contentlet.getStructure().getVelocityVarName())){	
			Identifier id = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
			List<Identifier> ids = findByParentPath(id.getParentPath(),currentHost.getIdentifier());
			Logger.info(getClass(), "Number of ids of htmlpage into the path: " + id.getParentPath() + " = " + ids.size());
			for(Identifier identifier:ids){
				StringBuilder page = new StringBuilder();
				page.append("https://");
				page.append(currentHost.getHostname());
				page.append(identifier.getParentPath());
				page.append(identifier.getAssetName());
				page.append("|");
				page.append(identifier.getParentPath());
				page.append(identifier.getAssetName());
				Logger.debug(getClass(), "URL to append: " + page.toString());
				
				paths.add(page.toString());
			}
		}else{
			StringBuilder page = new StringBuilder();
			page.append("https://");
			page.append(currentHost.getHostname());
			page.append("|");
			page.append("/");
			paths.add(page.toString());
		}
		return paths;
		
	}
	
	private List<String> getFileAssetPath(Contentlet fileAsset, Host currentHost) throws DotDataException {
		Logger.info(getClass(), "Current host: " + currentHost.getHostname());
		List<String> paths = new ArrayList<String>();
		Identifier identifier = APILocator.getIdentifierAPI().find(fileAsset.getIdentifier());
		String[] split_point = identifier.getAssetName().split("[.]");
		StringBuilder page = new StringBuilder();
		page.append("https://");
		page.append(currentHost.getHostname());
		page.append(identifier.getParentPath());
		page.append(identifier.getAssetName());
		page.append("|");
		page.append("/dotAsset/");
		page.append(identifier.getId());
		page.append(".");
		if(split_point.length>0)
			page.append(split_point[split_point.length-1]);
		Logger.info(getClass(), "PAGE: " + page.toString());
		page.append("_");
		Logger.info(getClass(), "INODE: " + fileAsset.getInode());
		page.append(fileAsset.getInode());		
		page.append("&amp;random=");
		page.append(new GregorianCalendar().getTimeInMillis());
		Logger.info(getClass(), "URL to append: " + page.toString());
		
		paths.add(page.toString());
		return paths;
	}
	
	@SuppressWarnings("unchecked")
	private List<Identifier> findByParentPath(String parentPath, String host) throws DotDataException {
		DotConnect dc = new DotConnect();
		StringBuilder bob = new StringBuilder("select distinct i.* from identifier i ");
		bob.append("where i.asset_type = ? ");
		bob.append("and i.parent_path = ? ");
		bob.append("and i.host_inode = ? ");

		dc.setSQL(bob.toString());
		dc.addParam("htmlpage");
		dc.addParam(parentPath);
		dc.addParam(host);
		return convertDotConnectMapToPOJO(dc.loadResults());
	}
	
	private List<Identifier> convertDotConnectMapToPOJO(List<Map<String,String>> results){
		List<Identifier> ret = new ArrayList<Identifier>();
		if(results == null || results.size()==0){
			return ret;
		}
		
		for (Map<String, String> map : results) {
			Identifier i = new Identifier();
			i.setAssetName(map.get("asset_name"));
			i.setAssetType(map.get("asset_type"));
			i.setHostId(map.get("host_inode"));
			i.setId(map.get("id"));
			i.setParentPath(map.get("parent_path"));
			ret.add(i);
		}
		return ret;
	}
}
