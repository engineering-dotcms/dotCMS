package com.dotmarketing.portlets.workflows.actionlet;

import java.util.List;
import java.util.Map;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class PublishContentActionlet extends ContentActionlet {

	private ESContentletIndexAPI indexAPI = new ESContentletIndexAPI();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "Publish content";
	}

	public String getHowTo() {

		return "This actionlet will publish the content.";
	}

	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		try {			
			super.executeAction(processor, params);
			
			if(processor.getContentlet().isArchived()){
				for(Contentlet c : contentletsToProcess)
					APILocator.getContentletAPI().unarchive(c, processor.getUser(), false);
			}
			
			for(Contentlet c : contentletsToProcess){
				if(!UtilMethods.isSet(APILocator.getIdentifierAPI().find(c.getIdentifier()).getSysPublishDate())){
					Logger.debug(getClass(), "STO PUBBLICANDO IL CONTENUTO: " + c.getTitle());
					APILocator.getContentletAPI().publish(c, processor.getUser(), true);
					Logger.debug(getClass(), "PUBBLICATO IL CONTENUTO: " + c.getTitle());
					APILocator.getContentletAPI().unlock(c, processor.getUser(), true);
					indexAPI.addContentToIndex(c,false,true,false,null);
					Logger.debug(getClass(), "AGGIUNTO IL CONTENUTO: " + c.getTitle());
				}
			}

		} catch (Exception e) {
			Logger.error(PublishContentActionlet.class,e.getMessage(),e);
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
