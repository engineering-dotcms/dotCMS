package it.bankit.website.viewtool.navigation;

import com.dotmarketing.viewtools.navigation.NavToolCache;

public interface NavToolCacheBankIT extends NavToolCache {

	public final String GROUP = "navCacheBI";

	NavResultBankIT getNav(String hostid, String folderInode);
	void putNav(String hostid, String folderInode, NavResultBankIT result);
	void removeNav(String hostid, String folderInode);
	void removeNavByPath(String hostid, String path);
	void removeNav(String folderInode);

}
