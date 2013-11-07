package org.dotcms.forum.velocity;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.tools.view.tools.ViewTool;
import org.dotcms.forum.business.SubmitForumFactory;
import org.dotcms.forum.velocity.bean.Topic;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;

public class SubmitForumViewTool implements ViewTool {
	
	
	public List<Topic> getFullTopics(String languageId, boolean sortByDate){
		try{
			return SubmitForumFactory.getSubmitForumAPI().getFullTopics(languageId,0,0,sortByDate);
		}catch (Exception e) {
			return new ArrayList<Topic>();
		}
	}
	
	public List<Topic> getFullTopics(String languageId,int limit, int offset, boolean sortByDate){
		try{
			return SubmitForumFactory.getSubmitForumAPI().getFullTopics(languageId,limit,offset,sortByDate);
		}catch (Exception e) {
			return new ArrayList<Topic>();
		}
	}
	
	/**
	 * Con query lucene
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 24, 2013 - 12:16:49 PM
	 */
	public List<Topic> getFullTopics(String query, int limit, int offset, String sortBy){
		try{
			return SubmitForumFactory.getSubmitForumAPI().getFullTopics(query,limit,offset,sortBy);
		}catch (Exception e) {
			return new ArrayList<Topic>();
		}
	}
	
	public String getDetailPagePath(String structure){
		Structure aStructure = StructureCache.getStructureByVelocityVarName(structure);
		try {
			HTMLPage detailPage = APILocator.getHTMLPageAPI().loadLivePageById(aStructure.getDetailPage(), 
					APILocator.getUserAPI().getSystemUser(), false);			
			return APILocator.getHTMLPageAPI().getParentFolder(detailPage).getPath();
		} catch (DotSecurityException e) {
			e.printStackTrace();
			return "";
		} catch (DotDataException e) {			
			e.printStackTrace();
			return "";
		}
	}
	
	public int countTopics(String languageId){
		try {
			return SubmitForumFactory.getSubmitForumAPI().countFullTopics(languageId);
		} catch (Exception e) {
			return -1;
		} 
	}
	
	/**
	 * Con query lucene
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 24, 2013 - 12:16:49 PM
	 */
	public int countTopics(String query, boolean withQuery){
		try {
			return SubmitForumFactory.getSubmitForumAPI().countFullTopics(query,withQuery);
		} catch (Exception e) {
			return -1;
		} 
	}
	
	@Override
	public void init(Object initData) {
		// TODO Auto-generated method stub

	}

}
