package it.eng.bankit.converter.fileasset;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.AbstractConverterImpl;
import it.eng.bankit.util.FileUtil;

import java.util.List;

import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;

public class RePEcConverter  extends AbstractConverterImpl  {

	public RePEcConverter(HmiStructure struttura) {
		super(struttura);
	}

	@Override
	public Structure getDotStructure() {
		return null;
	}

	@Override
	public ContentletContainer parseContent() throws Exception {
		String folderName =  getHmiStructure().getFilePath();
		getDotcmsSysDataWriter().createFolderOnHost( folderName, getHmiStructure() );

		List<HmiStructure> listaSottoDirs = getHmiStructure().getSubDirectories();

		for( HmiStructure hmis : listaSottoDirs ){
			String folderNameHmis = hmis.getFilePath();
			Folder  fHmis = getDotcmsSysDataWriter().createFolderOnHost( folderNameHmis, getHmiStructure() );
		 	List<HmiStructure> filesRDF =  hmis.getSubFile();
			if( filesRDF != null && filesRDF.size() > 0 ){
				for( HmiStructure fileRdf : filesRDF  ){
					FileUtil.convertAndSaveFile(fileRdf.getFile(), fHmis );
				}
			}

			List<HmiStructure> sottoDirs = hmis.getSubDirectories();
			for( HmiStructure single : sottoDirs ){
				String folderNameSingle = single.getFilePath();
				Folder  fSingle = getDotcmsSysDataWriter().createFolderOnHost( folderNameSingle, single);
			 	List<HmiStructure> rdfs =  single.getSubFile();				
			 	if( rdfs != null && rdfs.size() > 0 ){
					for( HmiStructure fileRdf : rdfs  ){
						FileUtil.convertAndSaveFile(fileRdf.getFile(), fSingle);
					}
				}
			}	
		
		}
		return null;
	}


	@Override
	protected String getLinkRelationShipName() {
		return null;
	}

}
