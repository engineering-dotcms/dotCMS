package com.dotmarketing.plugins.integrity.checker.loader;


public abstract class CSVLoaderAPI {
 
    private static CSVLoaderAPI csvAPI = null;
	public static CSVLoaderAPI getInstance(){
		if(csvAPI == null){
			csvAPI = CSVLoaderAPIImpl.getInstance();
		}
		return csvAPI;	
	}
     
    /**
     * Parse CSV file using OpenCSV library and load in 
     * given database table. 
     * @param csvFile Input CSV file
     * @param tableName Database table name to import data
     * @param truncateBeforeLoad Truncate the table before inserting 
     *          new records.
     * @throws Exception
     */
    public abstract void loadCSV(String csvFile, String tableName,
            boolean truncateBeforeLoad) throws Exception;
 
    public abstract char getSeparator();
 
    public abstract void setSeparator(char separator);
 
}