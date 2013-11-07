package com.dotmarketing.plugins.integrity.checker.loader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPIImpl;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;


public class CSVLoaderAPIImpl extends CSVLoaderAPI {
 
    private static final
        String SQL_INSERT = "INSERT INTO ${table}(${keys}) VALUES(${values})";
    private static final String TABLE_REGEX = "\\$\\{table\\}";
    private static final String KEYS_REGEX = "\\$\\{keys\\}";
    private static final String VALUES_REGEX = "\\$\\{values\\}";
 
    private char separator = ',';
    
    private static CSVLoaderAPIImpl instance = null;
    public static CSVLoaderAPIImpl getInstance() {
		if(instance==null)
			instance = new CSVLoaderAPIImpl();
		
		return instance;
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
    public void loadCSV(String csvFile, String tableName,
            boolean truncateBeforeLoad) throws Exception {
 
        CSVReader csvReader = null;
   
        try {
             
            csvReader = new CSVReader(new FileReader(csvFile), this.separator);
 
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error occured while executing file. "
                    + e.getMessage());
        }
 
        String[] headerRow = csvReader.readNext();
 
        if (null == headerRow) {
        	csvReader.close();
            throw new FileNotFoundException(
                    "No columns defined in given CSV file." +
                    "Please check the CSV file format.");
        }
 
        String questionmarks = StringUtils.repeat("?,", headerRow.length);
        questionmarks = (String) questionmarks.subSequence(0, questionmarks
                .length() - 1);
 
        String query = SQL_INSERT.replaceFirst(TABLE_REGEX, tableName);
        query = query
                .replaceFirst(KEYS_REGEX, StringUtils.join(headerRow, ","));
        query = query.replaceFirst(VALUES_REGEX, questionmarks);
 
        System.out.println("Query: " + query);
 
        String[] nextLine;

        try {
        	HibernateUtil.startTransaction();
            DotConnect dc = new DotConnect();
 
            if(truncateBeforeLoad) {
                //delete data from table before loading csv
               dc.setSQL("DELETE FROM "+tableName);
               dc.loadResult();
            }
            
            int count = 0;
			final int batchsize = 1000;
            Date date = null;
            while ((nextLine = csvReader.readNext()) != null) {
            	dc.setSQL(query);
                if (null != nextLine) {
                    for (String string : nextLine) {
                        date = DateUtil.convertToDate(string);

        				if (null != date) {
        					dc.addParam(new java.sql.Date(date.getTime()));
                        } else {
                        	dc.addParam(string);
                        }
                    }
                    
                    dc.loadResult();
                    
                    count++;
                } 
                
                if(batchsize % count == 0) {
                	count = 0;
                	HibernateUtil.commitTransaction();
                	HibernateUtil.startTransaction();
                }
            }
            
            
            HibernateUtil.commitTransaction();
        } catch (Exception e) {
        	try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e1);
			}			
			Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to add element to data integrity table:" + e.getMessage(), e);
        } finally {
            csvReader.close();
        }
    }
 
    public char getSeparator() {
        return separator;
    }
 
    public void setSeparator(char separator) {
        this.separator = separator;
    }
 
}