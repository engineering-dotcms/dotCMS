package it.bankit.website.cache;

public class CacheBean {
private String inode=null;
private String uri=null;
private String path=null;
private long languageId=0;

public String getInode() {
	return inode;
}
public void setInode( String inode ) {
	this.inode = inode;
}

public String getURI() {
	return uri;
}
public void setURI( String uri ) {
	this.uri = uri;
}
public String getPath() {
	return path;
}
public void setPath( String path ) {
	this.path = path;
}
public long getLanguageId() {
	return languageId;
}
public void setLanguageId( long languageId ) {
	this.languageId = languageId;
}

@Override
public String toString(){
	if(uri!=null&&path!=null){
		return uri+"->"+path;
	}
	else{
		return super.toString();
	}
}
}
