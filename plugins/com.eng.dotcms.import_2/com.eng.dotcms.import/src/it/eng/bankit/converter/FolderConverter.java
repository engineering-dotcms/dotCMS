package it.eng.bankit.converter;

import it.eng.bankit.bean.ContentletContainer;

import com.dotmarketing.portlets.structure.model.Structure;

public interface FolderConverter {
	public ContentletContainer parseContent() throws Exception;
	public  Structure getDotStructure();
}
