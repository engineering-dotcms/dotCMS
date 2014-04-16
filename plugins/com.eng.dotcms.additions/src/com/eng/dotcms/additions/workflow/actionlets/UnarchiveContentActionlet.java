package com.eng.dotcms.additions.workflow.actionlets;

import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;

public class UnarchiveContentActionlet extends ContentActionlet {



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "BankIT Unarchive content";
	}

	public String getHowTo() {

		return "This actionlet will un archive the content and return it to a working state";
	}

	public void executeAction(WorkflowProcessor processor,Map<String,WorkflowActionClassParameter>  params) throws WorkflowActionFailureException {
		try {
			super.executeAction(processor, params);
			for(Contentlet c : contentletsToProcess)
				APILocator.getContentletAPI().unarchive(c, processor.getUser(), false);

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
