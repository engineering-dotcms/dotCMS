package com.eng.dotcms.additions.workflow.actionlets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UtilMethods;

public class AddHTMLPagePathActionlet extends WorkFlowActionlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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
			WorkflowComment lastCommentWithPaths = new WorkflowComment();
			// retrieve the last comment on this task
			WorkflowComment lastComment = APILocator.getWorkflowAPI().findWorkFlowComments(processor.getTask()).get(0);
			List<String> paths = getHTMLPagePaths(processor.getContentlet());
			lastCommentWithPaths.setPostedBy(processor.getContentlet().getModUser());			
			StringBuilder commentText = new StringBuilder();
			if(null!=lastComment && UtilMethods.isSet(lastComment.getComment())){
				commentText.append(lastComment.getComment());
				commentText.append("<br /><br />");
			}
			commentText.append("Queste sono le pagine in cui il contenuto potrebbe essere visualizzato:");
			commentText.append("<br />");
			commentText.append("<ul>");
			for(String page:paths){
				commentText.append("<li><a href=");
				commentText.append(page);
				commentText.append(">");
				commentText.append(page);
				commentText.append("</a>");
				commentText.append("</li>");
			}
			commentText.append("</ul>");
			lastCommentWithPaths.setComment(commentText.toString());
			lastCommentWithPaths.setWorkflowtaskId(processor.getTask().getId());
			APILocator.getWorkflowAPI().saveComment(lastCommentWithPaths);
		}catch(DotDataException e){
			e.printStackTrace();
		}catch (DotSecurityException e) {
			e.printStackTrace();
		}
	}
	
	private List<String> getHTMLPagePaths(Contentlet contentlet) throws DotDataException, DotSecurityException {
		Host currentHost = APILocator.getHostAPI().find(contentlet.getHost(), APILocator.getUserAPI().getSystemUser(), true);
		List<String> paths = new ArrayList<String>();
		
		Identifier id = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
		List<Identifier> ids = APILocator.getIdentifierAPI().findByURIPattern("htmlpage", id.getParentPath(), 
				true, false, true, currentHost);
		for(Identifier identifier:ids){
			StringBuilder page = new StringBuilder();
			page.append("http://");
			page.append(currentHost.getHostname());
			page.append(identifier.getParentPath());
			page.append(identifier.getAssetName());
			paths.add(page.toString());
		}
		return paths;
	}
}
