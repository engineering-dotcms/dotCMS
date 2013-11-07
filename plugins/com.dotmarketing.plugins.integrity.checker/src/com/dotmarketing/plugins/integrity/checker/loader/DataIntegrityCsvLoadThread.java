package com.dotmarketing.plugins.integrity.checker.loader;

import com.dotmarketing.util.Logger;


public class DataIntegrityCsvLoadThread implements Runnable {
	private CSVLoaderAPI csvAPI = CSVLoaderAPI.getInstance();
	
	private String localPath, remotePath;
	
	public DataIntegrityCsvLoadThread(String localPath, String remotePath)  {
		this.localPath = localPath;
		this.remotePath = remotePath;
	}
	
	public void run() {
		LoadStatus.isLoading = true;
		try {
			Logger.info(this, "*** Start integrity checker local loading ***");
			csvAPI.loadCSV(localPath, "DATA_INTEGRITY_CHECK_LOCAL", true);
			Logger.info(this, "*** End integrity checker local loading ***");
			
			Logger.info(this, "*** Start integrity checker remote loading ***");
			csvAPI.loadCSV(remotePath, "DATA_INTEGRITY_CHECK_REMOTE", true);
			Logger.info(this, "*** End integrity checker remote loading ***");
			
		} catch (Exception e) {
			Logger.error(DataIntegrityCsvLoadThread.class,e.getMessage(),e);
		} finally {
			LoadStatus.isLoading = false;
		}
	}
}
