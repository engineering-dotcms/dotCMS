package org.dotcms.forum.business;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.dotcms.forum.velocity.bean.Reply;
import org.dotcms.forum.velocity.bean.Topic;
import org.dotcms.forum.velocity.bean.Thread;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.ibm.icu.text.SimpleDateFormat;

public class SubmitForumAPIImpl implements SubmitForumAPI {
	
	private static String TOPIC_STRUCTURE = "Topic";
	private static String TOPIC_THREAD_REL = "Parent_Topic-Child_Thread";
	private static String THREAD_REPLY_REL = "Parent_Thread-Child_Reply";
	
	private ContentletAPI conAPI;
	private UserAPI userAPI;
	
	public SubmitForumAPIImpl(){
		conAPI = APILocator.getContentletAPI();
		userAPI = APILocator.getUserAPI();
	}
	
	@Override
	public List<Topic> getFullTopics(String languageId, int limit, int offset, boolean sortByDate) throws DotDataException, DotSecurityException {
		List<Topic> topics = new ArrayList<Topic>();
		List<Contentlet> _topics =  conAPI.findByStructure(StructureCache.getStructureByVelocityVarName(TOPIC_STRUCTURE), userAPI.getSystemUser(), true, limit, offset);
		for(Contentlet _singleTopic:_topics){
			if(_singleTopic.getLanguageId()==Long.parseLong(languageId)){
				Topic topic = new Topic();
				topic.setIdentifier(_singleTopic.getIdentifier());
				topic.setInode(_singleTopic.getInode());
				topic.setTitle(_singleTopic.getTitle());
				topic.setDescription((String)_singleTopic.get("description"));
				topic.setUrlTitle((String)_singleTopic.get("urlTitle"));
				topic.setOwner(_singleTopic.getOwner());
				topic.setLastModified((Date)_singleTopic.get("lastModified"));
				topic.setModDate(_singleTopic.getModDate());
				List<Contentlet> _threads = conAPI.getRelatedContent(_singleTopic, RelationshipFactory.getRelationshipByRelationTypeValue(TOPIC_THREAD_REL), userAPI.getSystemUser(), true);
				List<Thread> threads = new ArrayList<Thread>();
				for(Contentlet _singleThread:_threads){
					Thread thread = new Thread();
					thread.setIdentifier(_singleThread.getIdentifier());
					thread.setTitle(_singleThread.getTitle());
					thread.setDescription((String)_singleThread.get("description"));
					thread.setLastModified(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aa").format(_singleThread.getModDate()));
					thread.setUser(userAPI.loadUserById(_singleThread.getOwner()).getFullName());
					thread.setLive(_singleThread.isLive());
					String open = (String)_singleThread.getMap().get("threadStatus");
					thread.setOpen(open.equals("yes")?true:false);
					List<Contentlet> _replies = conAPI.getRelatedContent(_singleThread, RelationshipFactory.getRelationshipByRelationTypeValue(THREAD_REPLY_REL), userAPI.getSystemUser(), true);
					List<Reply> replies = new ArrayList<Reply>();
					for(Contentlet _singleReply:_replies){
						Reply reply = new Reply();
						reply.setIdentifier(_singleReply.getIdentifier());
						reply.setTitle(_singleReply.getTitle());
						reply.setDescription((String)_singleReply.get("description"));
						reply.setLastModified(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(_singleReply.getModDate()));
						reply.setUser(userAPI.loadUserById(_singleReply.getOwner()).getFullName());
						reply.setLive(_singleReply.isLive());
						replies.add(reply);
					}
					thread.setReplies(replies);
					threads.add(thread);
				}
				topic.setThreads(threads);
				topics.add(topic);
			}
		}
		if(sortByDate)
			Collections.sort(topics);
		return topics;
	}
	
	@Override
	public List<Topic> getFullTopics(String query, int limit, int offset, String sortBy) throws DotDataException, DotSecurityException {
		List<Topic> topics = new ArrayList<Topic>();
		List<Contentlet> _topics = conAPI.search(query, limit, offset, sortBy, userAPI.getSystemUser(), false);  
		for(Contentlet _singleTopic:_topics){
			Topic topic = new Topic();
			topic.setIdentifier(_singleTopic.getIdentifier());
			topic.setInode(_singleTopic.getInode());
			topic.setTitle(_singleTopic.getTitle());
			topic.setDescription((String)_singleTopic.get("description"));
			topic.setUrlTitle((String)_singleTopic.get("urlTitle"));
			topic.setOwner(_singleTopic.getOwner());
			topic.setLastModified((Date)_singleTopic.get("lastModified"));
			topic.setModDate(_singleTopic.getModDate());
			List<Contentlet> _threads = conAPI.getRelatedContent(_singleTopic, RelationshipFactory.getRelationshipByRelationTypeValue(TOPIC_THREAD_REL), userAPI.getSystemUser(), true);
			List<Thread> threads = new ArrayList<Thread>();
			for(Contentlet _singleThread:_threads){
				Thread thread = new Thread();
				thread.setIdentifier(_singleThread.getIdentifier());
				thread.setTitle(_singleThread.getTitle());
				thread.setDescription((String)_singleThread.get("description"));
				thread.setLastModified(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aa").format(_singleThread.getModDate()));
				thread.setUser(userAPI.loadUserById(_singleThread.getOwner()).getFullName());
				thread.setLive(_singleThread.isLive());
				String open = (String)_singleThread.getMap().get("threadStatus");
				thread.setOpen(open.equals("yes")?true:false);
				List<Contentlet> _replies = conAPI.getRelatedContent(_singleThread, RelationshipFactory.getRelationshipByRelationTypeValue(THREAD_REPLY_REL), userAPI.getSystemUser(), true);
				List<Reply> replies = new ArrayList<Reply>();
				for(Contentlet _singleReply:_replies){
					Reply reply = new Reply();
					reply.setIdentifier(_singleReply.getIdentifier());
					reply.setTitle(_singleReply.getTitle());
					reply.setDescription((String)_singleReply.get("description"));
					reply.setLastModified(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(_singleReply.getModDate()));
					reply.setUser(userAPI.loadUserById(_singleReply.getOwner()).getFullName());
					reply.setLive(_singleReply.isLive());
					replies.add(reply);
				}
				thread.setReplies(replies);
				threads.add(thread);
			}
			topic.setThreads(threads);
			topics.add(topic);
		}
		return topics;
	}

	@Override
	public int countFullTopics(String languageId) throws DotDataException,DotSecurityException {
		List<Contentlet> _topics =  conAPI.findByStructure(StructureCache.getStructureByVelocityVarName(TOPIC_STRUCTURE), userAPI.getSystemUser(), true, 0, 0);
		int count = 0;
		for(Contentlet _singleTopic : _topics)
			if(_singleTopic.getLanguageId()==Long.parseLong(languageId) && _singleTopic.isLive())
				count++;
			
		return count;
	}

	@Override
	public int countFullTopics(String query, boolean withQuery) throws DotDataException, DotSecurityException {
		return conAPI.search(query, 0, 0, null, userAPI.getSystemUser(), false).size();  
	}
}
