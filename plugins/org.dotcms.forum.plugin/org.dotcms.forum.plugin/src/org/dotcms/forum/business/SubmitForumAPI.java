package org.dotcms.forum.business;

import java.util.List;

import org.dotcms.forum.velocity.bean.Topic;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public interface SubmitForumAPI {
	
	List<Topic> getFullTopics(String languageId, int limit, int offset, boolean sortByDate) throws DotDataException, DotSecurityException;
	
	List<Topic> getFullTopics(String query, int limit, int offset, String sortBy) throws DotDataException, DotSecurityException;
	
	int countFullTopics(String languageId) throws DotDataException, DotSecurityException;
	
	int countFullTopics(String query, boolean withQuery) throws DotDataException, DotSecurityException;
}
