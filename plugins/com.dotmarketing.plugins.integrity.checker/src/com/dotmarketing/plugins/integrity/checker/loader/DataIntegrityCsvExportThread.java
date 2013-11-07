package com.dotmarketing.plugins.integrity.checker.loader;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.plugins.integrity.checker.DataWrapper;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


public class DataIntegrityCsvExportThread implements Runnable {
	private String exportFilePath;
	private String luceneQuery;
	
	public DataIntegrityCsvExportThread(String exportFilePath, String luceneQuery)  {
		this.exportFilePath = exportFilePath;
		this.luceneQuery = luceneQuery;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void run() {
		
		try {
			ExportCsvStatus.isExporting = true;
			
			User systemUser = APILocator.getUserAPI().getSystemUser();
			
			File f = new File(exportFilePath);
			if(!f.exists())
				f.createNewFile();
			
			FileWriter writer = new FileWriter(f);
	
			Date now = new Date();
			String nowString = new SimpleDateFormat("dd/MM/yyyy").format(now);
	
			writer.append("id");
			writer.append(',');
			writer.append("inode");
			writer.append(',');
			/* writer.append("title");
			writer.append(','); */
			writer.append("md5");
			writer.append(',');
			writer.append("create_date");
			writer.append('\n');
			
			int limit = 50;
			int pages = 0;
			int counter = 0;
			PaginatedArrayList<ContentletSearch> results = 
					(PaginatedArrayList) APILocator.getContentletAPI().searchIndex(luceneQuery, 1, limit, null, systemUser, false);
			ExportCsvStatus.totalContent = results.getTotalResults();
			

			int offset = pages*limit;
			while(offset <= ExportCsvStatus.totalContent) {
				
				offset = pages*limit;
				if(offset == 0)
					offset = 1;
				
				pages++;
				
				List<Contentlet> contents = APILocator.getContentletAPI().search(luceneQuery, limit, offset, "modDate desc", systemUser, false);
				
				for(Contentlet con: contents) {
					ExportCsvStatus.currentCount++;
					
					XStream xstream = new XStream(new DomDriver());
		
					try {
						ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
						BufferedOutputStream output = new BufferedOutputStream(byteStream);
						
						DataWrapper data = new DataWrapper(con);
		
						xstream.toXML(data, output);		
						
						MessageDigest algorithm = MessageDigest.getInstance("MD5");
						algorithm.reset();
						algorithm.update(byteStream.toByteArray());
						byte messageDigest[] = algorithm.digest();
					            
						StringBuilder hexString = new StringBuilder();
						for (int i=0;i<messageDigest.length;i++) {
							hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
						}
						
						writer.append(UUIDGenerator.generateUuid());
					    writer.append(',');
					    writer.append(con.getInode());
					    writer.append(',');
					    /* writer.append(con.getTitle());
					    writer.append(','); */
					    writer.append(hexString.toString());
				        writer.append(',');
				        writer.append(nowString);
					    writer.append('\n');
		
		//				out.println(con.getInode() + " - "+ con.getTitle()+" ---- md5 version is "+hexString.toString()+"<br />");
		//				
		//				out.print("--------------------------------------------------------<br />");
		//				for(String chiave: data.getOrderedMap().keySet()) {
		//					out.print("Key: "+chiave+" Value: "+ data.getOrderedMap().get(chiave)+"<br />");
		//				}
		//				out.print("--------------------------------------------------------<br />");
		//				
		//				
		//				out.print("-------------------------XML-------------------------<br />");
		//				xstream.toXML(data, out);
		//				out.print("-------------------------END XML---------------------<br />");
						
						output.close();
						
						if(counter >= 500) {
					    	try {
								writer.flush();
								writer.close();
							} catch (IOException e) {}
					    	
					    	counter = 0;
					    	writer = new FileWriter(f, true);
						}
				    	
				    	counter++;
					} catch (Exception e) {
						e.printStackTrace();
					}		
				}
			}
			
			if(writer != null) {
				try {
					writer.flush();
					writer.close();
				} catch (IOException e) {}
			}
		} catch (Exception e) {
			Logger.error(DataIntegrityCsvExportThread.class,e.getMessage(),e);
		} finally {
			ExportCsvStatus.currentCount = 0;
			ExportCsvStatus.isExporting =false;
			ExportCsvStatus.totalContent = 0;
			
		}
	} 
}
