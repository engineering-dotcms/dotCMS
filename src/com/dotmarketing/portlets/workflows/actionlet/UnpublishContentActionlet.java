package com.dotmarketing.portlets.workflows.actionlet;

import java.util.List;
import java.util.Map;

import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;

public class UnpublishContentActionlet extends ContentActionlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ESContentletIndexAPI indexAPI = new ESContentletIndexAPI();
	
	public String getName() {
		return "Unpublish content";
	}

	public String getHowTo() {

		return "This actionlet will unpublish the content.";
	}

	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		try {
			super.executeAction(processor, params);
			
			for(Contentlet c : contentletsToProcess) {
				APILocator.getContentletAPI().unpublish(c, processor.getUser(), true);
				APILocator.getContentletAPI().unlock(c, processor.getUser(), true);
				indexAPI.removeContentFromIndex(c,false,true);
				CacheLocator.getIdentifierCache().removeFromCacheByVersionable(c);
			}

		} catch (Exception e) {
			Logger.error(this.getClass(),e.getMessage(),e);
			throw new  WorkflowActionFailureException(e.getMessage());
		
		}

	}

	public WorkflowStep getNextStep() {

		return null;
	}

	@Override
	public  List<WorkflowActionletParameter> getParameters() {

		return null;
	}
}
