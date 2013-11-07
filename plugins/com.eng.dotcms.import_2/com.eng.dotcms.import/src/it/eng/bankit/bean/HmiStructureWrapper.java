package it.eng.bankit.bean;

import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfigKey;

import java.io.File;
import java.util.List;

import com.dotmarketing.util.UtilMethods;

public class HmiStructureWrapper  {

	private HmiStructure hmiStructure;
	 
	public HmiStructureWrapper (HmiStructure hmiStructure ){
		this.hmiStructure = hmiStructure ;
	}


 

	public HmiStructure getHmiStructure() {
		return hmiStructure;
	}

	public boolean isShowOnMenu( ){
		boolean show = checkShow( getHmiStructure() );
		if( !show ){
			if( getHmiStructure() .getParentStructure()  != null ){
				String formato = getHmiStructure() .getParentStructure().getPropertyHmi( HyperwaveKey.Formato );
				
				 if( formato!= null && 
					(formato.startsWith("L1")  || 
							formato.startsWith("L2")   || 
							formato.startsWith("L3")   || 
							formato.startsWith("L4")   || 
							formato.startsWith("L5")   ) )
					 return false;
			}else{
				show = checkShow( getHmiStructure() .getParentStructure()  );
			}
		}
		return show;

	}

	private boolean checkShow( HmiStructure structure  ){

		if( structure  != null ){
			String formato = structure.getPropertyHmi( HyperwaveKey.Formato );
			String nascondi = structure.getPropertyHmi(HyperwaveKey.Nascondi );
			File file  = structure.getFile();
			 if( file!= null )
			 {
				String fName =  file.getName();
				if( UtilMethods.isSet( fName ) && fName.endsWith("links") ){
					return false;
				}
			 }
			if( formato!= null && (formato.startsWith("I"))){
				if(nascondi!= null && nascondi.equalsIgnoreCase(HyperwaveKey.Yes) )	{
					return false;
				}
				return true;
			}else if( formato!= null && 
					(formato.startsWith("L1")  || 
							formato.startsWith("L2")   || 
							formato.startsWith("L3")   || 
							formato.startsWith("L4")   || 
							formato.startsWith("L5")   )){
				return true;
			}else {

				String fileP = structure.getFilePath();
				if( UtilMethods.isSet(fileP) && fileP.indexOf("footer/")!= -1 ){
					String pHints = structure.getPropertyHmi(HyperwaveKey.PresentationHints);
					if( !UtilMethods.isSet(pHints) || !pHints.equalsIgnoreCase("Hidden") )	
					{
						return true;
					}
				}
			}

		}
		return false;
	}



	public boolean hasDocumentiCorrelati(){
		List<HmiStructure> dirs =   getHmiStructure().getSubDirectories();
		if( dirs != null && dirs.size() > 0){
			for(HmiStructure hmiDir : dirs   ){
				String present = hmiDir.getPropertyHmi(HyperwaveKey.PresentationHints);
				String fileName  = hmiDir.getFile().getName();
				if(  fileName.endsWith("links") && ( present != null && present.equalsIgnoreCase("Hidden")) ){
					return true;
				}
			}
		}
		return false;
	}

	public HmiStructure getDocumentoCorrelato(){
		List<HmiStructure> dirs =  getHmiStructure().getSubDirectories();
		if( dirs != null && dirs.size() > 0){
			for(HmiStructure hmiDir : dirs   ){
				String present = hmiDir.getPropertyHmi(HyperwaveKey.PresentationHints);
				String fileName  = hmiDir.getFile().getName();
				if(  fileName.endsWith("links") && ( present != null && present.equalsIgnoreCase("Hidden")) ){
					return hmiDir;
				}
			}
		}
		return null;
	}

 

	public boolean isRemote(){
		return ( getHmiStructure().getFile() == null && getHmiStructure().getHmiFile()!=  null );
	}

	public boolean isCollectionHead(){
		String presentation = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );
		return ( presentation != null &&  presentation.equalsIgnoreCase("CollectionHead") );
	}

	public boolean isContainerCollection(){
		String collection = getHmiStructure().getPropertyHmi( HyperwaveKey.CollectionType );
		String formato = getHmiStructure().getPropertyHmi( HyperwaveKey.Formato);		
		return (formato == null &&  collection != null &&  collection.equalsIgnoreCase("Collection") );
	}


	public boolean isCollectionType(){
		String collection = getHmiStructure().getPropertyHmi( HyperwaveKey.CollectionType );
		String docType = getHmiStructure().getPropertyHmi( HyperwaveKey.DocumentType );
		String formato =getHmiStructure().getPropertyHmi( HyperwaveKey.Formato);		
		return (formato == null &&  ( collection != null &&  collection.equalsIgnoreCase("Collection") 
				&&	docType != null &&  docType.equalsIgnoreCase("Collection")  )
		);
	}

	public boolean isCollectionDocumentType(){
		String docType = getHmiStructure(). getPropertyHmi( HyperwaveKey.DocumentType );
		String formato = getHmiStructure().getPropertyHmi( HyperwaveKey.Formato);		
		String collection = getHmiStructure().getPropertyHmi( HyperwaveKey.CollectionType );
		
		return ( formato == null &&  ( collection == null ||   collection.equalsIgnoreCase("Collection") )
				 && ( docType != null &&  docType.equalsIgnoreCase("Collection")) );
	}


	public boolean isContainerCluster(){
 
		String docType = getHmiStructure().getPropertyHmi( HyperwaveKey.DocumentType );
	
		String collection = getHmiStructure().getPropertyHmi( HyperwaveKey.CollectionType );
		String formato = getHmiStructure().getPropertyHmi( HyperwaveKey.Formato);		
		boolean returnValue = ( formato == null &&  
				( collection != null &&  collection.equalsIgnoreCase("Cluster")  && 
						docType != null &&  docType.equalsIgnoreCase("collection") 	));
		if( returnValue  ){
			return returnValue;
		}else{
			
			if(  collection != null &&  collection.equalsIgnoreCase("Cluster")  && 
					docType != null &&  docType.equalsIgnoreCase("collection") 	 ){
				return true;
			}
		}
		return false;
	}

	public boolean isFullCollectionHead(){
		String presentation = getHmiStructure().getPropertyHmi( HyperwaveKey.PresentationHints );
		if( presentation != null &&  presentation.equalsIgnoreCase("FullCollectionHead")  )
		{
			return true;
		}
		List<HmiStructure> listaFigli = getHmiStructure().getSubFile();
		for( HmiStructure struct : listaFigli ){
			if( FileUtil.isTextFile(struct.getFile())   ){
				presentation = struct.getPropertyHmi( HyperwaveKey.PresentationHints );
				if( presentation != null &&  presentation.equalsIgnoreCase("FullCollectionHead") ){
					return true;
				}
			}
		}
		return false;

	}

	private String getBoxMenu( HmiStructure  struct){
		if( struct != null ){
			String formato = struct.getPropertyHmi( HyperwaveKey.Formato );
			if( formato!= null && formato.equalsIgnoreCase("Box") ){
				return struct.getFile().getName();
			}else {
				return getBoxMenu( struct.getParentStructure() );
			}
		}else return null;
	}

	public String getBoxMenu(){
		if( isBoxMenu() ){
			String formato = getHmiStructure().getPropertyHmi( HyperwaveKey.Formato );
			if( formato!= null && formato.equalsIgnoreCase("Box") ){
				return getHmiStructure().getFile().getName();
			}else {
				return getBoxMenu( getHmiStructure().getParentStructure() );
			}
		}
		return null;
	}


	private boolean isFormatoBox( HmiStructure struct ){
		if( struct != null ){
			String formato = struct.getPropertyHmi( HyperwaveKey.Formato );
			if( formato!= null && formato.equalsIgnoreCase("Box") && getHmiStructure().getFilePath().indexOf("homepage") ==-1 ){
				return true;
			}
			else {
				if( struct.getParentStructure() != null ) {
					isFormatoBox(struct.getParentStructure() );
				}
			}
		}else {
			return false;
		}

		return false;
	}

	public boolean isBoxMenu(){
		String formato = getHmiStructure().getPropertyHmi( HyperwaveKey.Formato );
		if( formato!= null && formato.equalsIgnoreCase("Box") ){
			return true;
		}
		HmiStructure parent = getHmiStructure().getParentStructure();
		return isFormatoBox( parent );
	}

	public boolean hasBoxParent(HmiStructure hmi ){
		HmiStructure parentSt = null;
		if(hmi == null ){
			parentSt = getHmiStructure().getParentStructure();
		}else {
			parentSt = hmi.getParentStructure();			
		}
		if( parentSt != null ){
			String form =parentSt.getPropertyHmi(HyperwaveKey.Formato );
			if( form != null && form.equalsIgnoreCase("Box")){
				return true;
			}else {
				hasBoxParent( parentSt );
			}
		}return false;
	}
	public boolean hasListingParent(HmiStructure hmi , String listingType ){
		HmiStructure parentSt = null;
		if(hmi == null ){
			parentSt = getHmiStructure().getParentStructure();
		}else {
			parentSt = hmi.getParentStructure();			
		}
		if( parentSt != null ){
			String form = parentSt.getPropertyHmi(HyperwaveKey.Formato );
			if( form != null && form.equalsIgnoreCase(listingType )){
				return true;
			}else {
				hasListingParent( parentSt , listingType );
			}
		}return false;
	}
	
	public String getLanguageFile() {
		String lang =  getHmiStructure().getPropertyHmi(  HyperwaveKey.HW_Language );
		String correct = null;
		if( lang != null && lang.equalsIgnoreCase( ImportConfigKey.ENGLISH_LANGUAGE )){
			String title =  getHmiStructure().getPropertyHmi(  HyperwaveKey.Title+":"+ImportConfigKey.ENGLISH_LANGUAGE  );
			if( UtilMethods.isSet(title ) )	{
				correct =  ImportConfigKey.ENGLISH_LANGUAGE ;
			}else {
				title =  getHmiStructure().getPropertyHmi(  HyperwaveKey.Title+":"+ImportConfigKey.ITALIAN_LANGUAGE  );
				if( UtilMethods.isSet(title ) ) {
					correct =  ImportConfigKey.ITALIAN_LANGUAGE;
				}
			}			
		}else {
			String title =  getHmiStructure().getPropertyHmi(  HyperwaveKey.Title+":" +ImportConfigKey.ITALIAN_LANGUAGE  );
			if( UtilMethods.isSet(title ) ){
				correct =  ImportConfigKey.ITALIAN_LANGUAGE ;
			}else {
				title =  getHmiStructure().getPropertyHmi(  HyperwaveKey.Title+":"+ImportConfigKey.ENGLISH_LANGUAGE  );
				if( UtilMethods.isSet(title ) ) {
					correct = ImportConfigKey.ENGLISH_LANGUAGE;
				}
			}
		}
		return  correct;
	}
	 

}
