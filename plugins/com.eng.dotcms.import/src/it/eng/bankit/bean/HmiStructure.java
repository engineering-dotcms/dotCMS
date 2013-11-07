package it.eng.bankit.bean;

import it.eng.bankit.comparator.ComparatorFactory;
import it.eng.bankit.comparator.StructureSortNameComparator;
import it.eng.bankit.comparator.StructureSortSequenceComparator;
import it.eng.bankit.filereader.HmiFileReader;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.util.ImportConfigKey;
import it.eng.bankit.util.ImportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;

public class HmiStructure  {

	private File parent;
	private File file;
	private File hmiFile;
	private File hmiParent;	
	private String filePath ;
	private Map<String, String> propertiesHmi;
	private List<HmiStructure> children = new ArrayList<HmiStructure>();
	private List<HmiStructure> childrenLinks = new ArrayList<HmiStructure>();
	private HmiStructure parentStructure = null;
	private List<HmiStructure> aliasLinks = new ArrayList<HmiStructure>();
	private HmiStructureWrapper hmiStructureWrapper;
	private boolean alias = false;
	private String importDir;

	public HmiStructure(){
		super();
		hmiStructureWrapper = new HmiStructureWrapper(this);
	}
  
	public String getLanguageFile() {
		return hmiStructureWrapper.getLanguageFile();
	}


	public boolean isShowOnMenu( ){
		return hmiStructureWrapper.isShowOnMenu();
	}


	public List<HmiStructure> getSubDirectories(){
		List<HmiStructure> dirs = null;
		if( children != null ){
 			Comparator<HmiStructure> sortc =   new StructureSortSequenceComparator();
			dirs = new ArrayList<HmiStructure>();
			for( HmiStructure child : children ){
				if( child.getParentStructure() == null ){
					child.setParentStructure(this);
				}
				File f =  child.getFile();
				if( f.isDirectory() ){
					dirs.add( child );
				}
			}
			Collections.sort(dirs, sortc );		
		}
		return dirs;
	}

	public boolean hasDocumentiCorrelati(){
		return hmiStructureWrapper.hasDocumentiCorrelati();
	}

	public HmiStructure getDocumentoCorrelato(){
		List<HmiStructure> dirs =  getSubDirectories();
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

	public List<HmiStructure> getSubFile(){
		List<HmiStructure> dirs = null;
		if( children!= null ){
			dirs = new ArrayList<HmiStructure>();
			Comparator<HmiStructure> sortc =   new StructureSortSequenceComparator();
			for( HmiStructure child :children ){
				File f =  child.getFile();
				if( !f.isDirectory() ){
					if( child.getParentStructure() == null ){
						child.setParentStructure(this);
					}
					dirs.add( child );
				}
			}		
			Collections.sort(dirs, sortc );		
		}
		return dirs;
	}

	public List<HmiStructure> getSubFile( String language ){
		if( language  == null ){
			return getSubFile();
		}
		List<HmiStructure> listaFile = null;
		if( children!= null ){
			listaFile = new ArrayList<HmiStructure>();
			for( HmiStructure child :children ){
				File f =  child.getFile();
				if( !f.isDirectory() ) {
					String titolo = child.getPropertyHmi(  HyperwaveKey.Title +":"+language );
					if(UtilMethods.isSet(titolo) ){
						listaFile.add( child );
					}
				}
			}
		}
		return listaFile;
	}

	public List<HmiStructure> getSubDirectories(  String language  ){
		List<HmiStructure> dirs = null;
		if( children != null ){
			dirs = new ArrayList<HmiStructure>();
			for( HmiStructure child : children ){
				if( child.getParentStructure() == null ){
					child.setParentStructure(this);
				}
				File f =  child.getFile();
				if( f.isDirectory() ){
					String titolo = child.getPropertyHmi(  HyperwaveKey.Title +":"+language );
					if(UtilMethods.isSet(titolo) ){
						dirs.add( child );
					}
				}
			}
			//	Collections.sort(dirs, sortc );		
		}
		return dirs;
	}

	public List<HmiStructure> getChildren( String language ){
		if( language  == null || language.trim().equalsIgnoreCase("")  ){
			return children;
		}
		List<HmiStructure> lista = null;
		if( children!= null ){
			lista = new ArrayList<HmiStructure>();
			for( HmiStructure child :children ){
				String titolo = child.getPropertyHmi(  HyperwaveKey.Title +":"+language );
				if(UtilMethods.isSet(titolo) ){
					lista.add( child );
				}
			}			
		}
		return lista;
	}/**/

	public boolean hasChildren(){
		return 	( children!= null  && !children.isEmpty() );
	}


	public void addChildStructure(HmiStructure hmistruct ){
		children.add(hmistruct );
	}

	public boolean hasLinks(){
		return 	( childrenLinks != null  && !childrenLinks.isEmpty() );
	}	

	public void addLinkStructure(HmiStructure hmistruct ){
		childrenLinks.add(hmistruct );
	}


	public boolean isRemote(){
		return hmiStructureWrapper.isRemote();
	}

	public boolean isCollectionHead(){
		return hmiStructureWrapper.isCollectionHead();
	}

	public boolean isContainerCollection(){
		return hmiStructureWrapper.isContainerCollection();
	}


	public boolean isCollectionType(){
		return hmiStructureWrapper.isCollectionType();
	}

	public boolean isCollectionDocumentType(){
		return hmiStructureWrapper.isCollectionDocumentType();
	}

	public boolean isContainerCluster(){
		return hmiStructureWrapper.isContainerCluster();
	}

	public boolean isFullCollectionHead(){
		return hmiStructureWrapper.isFullCollectionHead();
	}

	 
	public String getBoxMenu(){
		return hmiStructureWrapper.getBoxMenu();
	}

 
	public boolean isBoxMenu(){
		return hmiStructureWrapper.isBoxMenu();
	}

	public boolean hasBoxParent(HmiStructure hmi ){
		return hmiStructureWrapper.hasBoxParent(hmi);
	}
	
	public boolean hasListingParent(HmiStructure hmi , String listingType ){
		return hmiStructureWrapper.hasListingParent(hmi , listingType );
	}

	public Map<String, String> getPropertiesHmi() {
		if( propertiesHmi == null ){
			try {
				propertiesHmi = HmiFileReader.processFile( hmiFile );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return propertiesHmi;
	}

	public Comparator<HmiStructure> getSortComparator(  Contentlet contentlet ){
		String sort = this.getPropertyHmi(HyperwaveKey.SortOrder);
		Language language =ImportUtil.getLanguage( contentlet.getLanguageId() );		
		Comparator<HmiStructure> compar =  ComparatorFactory.getInstance().getStructureComparator(sort,  language.getLanguageCode()  );
		return compar;
	}

	public Comparator<HmiStructure> getLinksComparator( HmiStructure structure , Contentlet contentlet ){

		Comparator<HmiStructure> compar =  getSortComparator( contentlet );

		if(compar == null ) {
			List<HmiStructure> links = this.getChildrenLinks();
			if( links != null && links.size() > 0 ){
				HmiStructure link =  links.get(0);
				String dataEmanazione =  link.getPropertyHmi(HyperwaveKey.Data_emanazione );
				String seq =  link.getPropertyHmi(HyperwaveKey.Sequence );
				String lang =  link.getPropertyHmi(HyperwaveKey.HW_Language  );
				if( UtilMethods.isSet( dataEmanazione )){
					compar = new StructureSortSequenceComparator();
				}else   if( UtilMethods.isSet( seq )){
					compar = new StructureSortSequenceComparator();
				} else {
					compar = new StructureSortNameComparator( lang );
				}
			}
		}
		return compar;
	}

	public String getPropertyHmi( String key ) {
		return  getPropertiesHmi().get( key );
	}

	public boolean isHmiDirectory(){
		return getFile().isDirectory();
	}

  



	public boolean checkLanguages( long languageId ){
		List<Language> langs = getLanguages();
		for( Language lang : langs ){
			if( lang.getId() == languageId ){
				return true;
			}
		}
		return false;  
	}


	public boolean existLanguageProperty( long languageId ){
		String langFile =  getPropertyHmi( HyperwaveKey.HW_Language );
		Language fileLang =   ImportUtil.getLanguage( langFile );
		if( langFile != null ){
			return  fileLang.getId() ==  languageId ;
		}
		return false;
	}

	public void addAliasLinkStructure(HmiStructure hmistruct ){
		aliasLinks.add( hmistruct );
	}

	public List<Language>  getLanguages(){
		List<Language> languages = new ArrayList<Language>();
		String titleIt = getPropertyHmi( HyperwaveKey.Title+":"+ImportConfigKey.ITALIAN_LANGUAGE  );
		if( UtilMethods.isSet( titleIt )){
			languages.add(  ImportUtil.getLanguage(ImportConfigKey.ITALIAN_LANGUAGE ) );
		}
		String titleEn = getPropertyHmi( HyperwaveKey.Title+":"+ImportConfigKey.ENGLISH_LANGUAGE );
		if( UtilMethods.isSet( titleEn )){
			languages.add(  ImportUtil.getLanguage( ImportConfigKey.ENGLISH_LANGUAGE ) );
		}
		return languages;
	}

	public File getParent() {
		return parent;
	}
	public void setParent(File parent) {
		this.parent = parent;
	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public File getHmiFile() {
		return hmiFile;
	}
	public void setHmiFile(File hmiFile) {
		this.hmiFile = hmiFile;
	}
	public List<HmiStructure> getChildren() {
		return children;
	}
	public void setChildren(List<HmiStructure> children) {
		this.children = children;
	} 

	public File getHmiParent() {
		return hmiParent;
	}
	public void setHmiParent(File hmiParent) {
		this.hmiParent = hmiParent;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public List<HmiStructure> getChildrenLinks() {
		return childrenLinks;
	}


	public void setChildrenLinks(List<HmiStructure> childrenLinks) {
		this.childrenLinks = childrenLinks;
	}


	public HmiStructure getParentStructure() {
		return parentStructure;
	}

	public void setParentStructure(HmiStructure parentStructure) {
		this.parentStructure = parentStructure;
	}

	public List<HmiStructure> getAliasLinks() {
		return aliasLinks;
	}

	public void setAliasLinks(List<HmiStructure> aliasLinks) {
		this.aliasLinks = aliasLinks;
	}

	public boolean isAlias() {
		return alias;
	}

	public void setAlias(boolean alias) {
		this.alias = alias;
	}

	public String getImportDir() {
		return importDir;
	}

	public void setImportDir(String importDir) {
		this.importDir = importDir;
	}


}
