package it.eng.bankit.bean;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Cartella {

	public Cartella(String nomeFolder )
	{
		this.parent = nomeFolder;
	}
	
	private String parent;
	private Map<String , Boolean > files = new HashMap<String, Boolean>();


	public void addFile(String path ){
		files.put(path, Boolean.FALSE );
	}
	
	public void updateFile(String path , boolean processed  ){
		files.put(path, Boolean.valueOf( processed ));
	}

	public String getParent() {
		return parent;
	}
	public void setParent(String parent) {
		this.parent = parent;
	}
	public Map<String, Boolean> getFiles() {
		return files;
	}
	public void setFiles(Map<String, Boolean> files) {
		this.files = files;
	}

	public String getFirstToProcess(){
		Iterator<String> iter = files.keySet().iterator();
		boolean processed = true;
		String pathToProcess = null ;
		while (iter.hasNext() && processed ) {
			String fileName = (String) iter.next();

			Boolean isProcessed =  files.get(fileName);
			if( !isProcessed ){
				processed = false;
				pathToProcess = fileName;
				files.put(fileName, true);
			}
		}
		return pathToProcess;
	}
	
	
	public boolean isProcessed(){
		Iterator<String> iter = files.keySet().iterator();
		boolean processed = true;
		while (iter.hasNext() && processed ) {
			String string = (String) iter.next();
			Boolean isProc =  files.get(string);
			if( !isProc ){
				processed = false;
 			}
		}
		return processed;
	}
	
	
	public boolean needProcess(){
		Iterator<String> iter = files.keySet().iterator();
		boolean processed = true;
		while (iter.hasNext() && processed ) {
			String string = (String) iter.next();
			Boolean isProc =  files.get(string);
			if( isProc ){
				processed = false;				
			}
		}
		return processed;
	}
	 
}
