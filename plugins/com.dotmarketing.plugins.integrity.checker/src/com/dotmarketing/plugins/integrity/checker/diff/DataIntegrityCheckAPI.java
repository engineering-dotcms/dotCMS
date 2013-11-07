package com.dotmarketing.plugins.integrity.checker.diff;

import java.util.List;

import com.dotcms.publisher.business.DotPublisherException;


public abstract class DataIntegrityCheckAPI {
	
	private static DataIntegrityCheckAPI dicAPI = null;
	public static DataIntegrityCheckAPI getInstance(){
		if(dicAPI == null){
			dicAPI = DataIntegrityCheckAPIImpl.getInstance();
		}
		return dicAPI;	
	}
	
	/**
	 * Gets the contents list with hash Md5 different between local and remote env
	 * @return
	 */
	public abstract List<DataIntegrityResultWrapper> getDiffList() throws DotPublisherException;
	
	/**
	 * Gets the contents list which are in local env but not in the remote one
	 * @return
	 */
	public abstract List<DataIntegrityResultWrapper> getLocalOrphans() throws DotPublisherException;
	
	/**
	 * Gets the contents list which are in remote env but not in the local one
	 * In this case there is no way to access to content detail cause it's not 
	 * present in the local DB
	 * @return
	 */
	public abstract List<DataIntegrityResultWrapper> getRemoteOrphans() throws DotPublisherException;
}
