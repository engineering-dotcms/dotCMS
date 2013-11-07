package it.eng.bankit.bean;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.dotmarketing.business.APILocator;

public class ContentletContainer {
	private Map<Long, ContentletWrapper> contentletVersions = new HashMap<Long, ContentletWrapper>();
	private FolderWrapper folder;
	private Map<String, Collection<File>> files;

	public boolean isEmpty() {
		return contentletVersions.isEmpty();
	}

	public ContentletWrapper add( ContentletWrapper contentlet ) {
		if ( contentlet != null && contentlet.getContentlet() != null ) {
			Long languageId = contentlet.getContentlet().getLanguageId();
			contentletVersions.put( languageId, contentlet );
			// System.out.println( "DOPO " + contentletVersions.size() );
			return contentlet;
		}
		return null;
	}

	public void addAll( Collection<ContentletWrapper> contentlets ) {
		for ( ContentletWrapper curContentlet : contentlets ) {
			add( curContentlet );
		}
	}

	public ContentletWrapper getDefaultVersion() {
		return contentletVersions.get( APILocator.getLanguageAPI().getDefaultLanguage().getId() );
	}

	public Collection<ContentletWrapper> getOtherVersions() {
		ContentletWrapper defaultVersion = getDefaultVersion();
		Collection<ContentletWrapper> versions = getAll();
		if ( defaultVersion != null && versions.contains( defaultVersion ) ) {
			versions.remove( defaultVersion );
		}
		return versions;
	}

	public ContentletWrapper get( Long languageId ) {
		return contentletVersions.get( languageId );
	}

	public Collection<ContentletWrapper> getAll() {
		return contentletVersions.values();
	}

	public FolderWrapper getFolder() {
		return folder;
	}

	public void setFolder( FolderWrapper folder ) {
		this.folder = folder;
	}

	public Map<String, Collection<File>> getFiles() {
		return files;
	}

	public boolean hasFiles() {
		return ( files == null ? false : !files.isEmpty() );
	}

	public void setFiles( Map<String, Collection<File>> files ) {
		this.files = files;
	}
}
