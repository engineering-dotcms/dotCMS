package it.eng.bankit.bean;

import java.io.File;

public class FileWrapper {

	private File file;

	private String currentPath;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getCurrentPath() {
		return currentPath;
	}

	public void setCurrentPath(String currentPath) {
		this.currentPath = currentPath;
	}
	
	public boolean isDirectory(){
		return file.isDirectory();
	}
	
}
