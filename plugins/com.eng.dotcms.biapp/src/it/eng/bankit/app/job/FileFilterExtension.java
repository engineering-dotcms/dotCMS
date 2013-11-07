package it.eng.bankit.app.job;

import java.io.File;
import java.io.FilenameFilter;

public class FileFilterExtension implements FilenameFilter {

	private String ext;
	
	public FileFilterExtension(String ext) { 
		this.ext = "." + ext; 
	} 

	public boolean accept(File dir, String name) { 
		return name.endsWith(ext); 
	} 

}
