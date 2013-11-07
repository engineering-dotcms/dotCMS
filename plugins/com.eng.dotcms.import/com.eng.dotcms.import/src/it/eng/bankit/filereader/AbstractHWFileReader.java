package it.eng.bankit.filereader;

import java.io.File;

public abstract class AbstractHWFileReader  {
	
	private File file ;
	private String encoding ;
	
	public AbstractHWFileReader(File file) {
		super();
		this.file = file;
	}

	public AbstractHWFileReader(File file , String encoding ) {
		super();
		this.file = file;
		this.encoding = encoding ;
	}
 
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public abstract String getContentAsString() ;

	
}
