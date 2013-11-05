package com.dotcms.publishing;

import java.io.Serializable;
import java.util.List;


public interface IPublisher extends Serializable{


	public PublisherConfig process(PublishStatus status) throws DotPublishingException;
	
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException;
	
	public List<Class> getBundlers();
	
	
	
	
}
