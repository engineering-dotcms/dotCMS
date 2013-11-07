package it.eng.bankit.parser;

import it.eng.bankit.bean.Cartella;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.filereader.HmiFileReader;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.InitializingBean;

import com.dotmarketing.util.UtilMethods;

public class FolderHyperParser implements ItemReader<HmiStructure>, InitializingBean {

	private org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger( this.getClass().getName() );

	private List<String> pathProcessed = new LinkedList<String>();
	private String startDir = "";
	private List<Cartella> cartelleLivello = new LinkedList<Cartella>();
	private Cartella cartella = null;
	private String currentDir = "";

	private void init() {
		if (getCurrentDir() != null) {
			Cartella c = initCartella(getCurrentDir());
			cartella = c;
			cartelleLivello.add(c);
			startDir = getCurrentDir();
		}
	}

	private Cartella initCartella(String path) {
		Cartella c = new Cartella(path);
		File[] listaFile = new File(path).listFiles();
		if (listaFile != null) {
			for (File f : listaFile) {
				if (f.isDirectory()) {
					c.addFile(f.getPath());
				}
			}
		}
		// cartella = c;
		return c;
	}

	@Override
	public HmiStructure read() throws Exception  {
		HmiStructure ret = null;
		if (getCurrentDir() == null) {
			System.out.println(" ----------------------------  " );
			System.out.println(" IMPORT TERMINATO " );
			System.out.println(" ----------------------------  " );
			return null;
		}
		LOG.info(" ---START READING ---  " + getCurrentDir()  );

		File  dir = new File(getCurrentDir()) ;
		File[] listaFile = dir.listFiles();		

		if (listaFile != null && listaFile.length > 0  ) {
			LOG.info(" ---file presenti nella directory  ---  " +listaFile.length   );
			 
			boolean toProcees = false;
			int i = 0;
			int sizeDir = listaFile.length;
			while (!toProcees && i < sizeDir) {
				File file = listaFile[i];
				String exten = FilenameUtils.getExtension(file.getName());
				if (exten != null && exten.equalsIgnoreCase("hmi")) {
					String documentType = HmiFileReader.processFile(file).get( 	HyperwaveKey.DocumentType);
					if (documentType != null && documentType.equalsIgnoreCase("Remote")) {
						// if( !pathProcessed.contains(f.getPath() ) ) {
						// ret = processaHmiRemoto( f );
						// pathProcessed.add( f.getPath() );
						// toProcees = true;
						// }
					} else {
						File fileDir = getFileToProcess(listaFile, file);
						if (fileDir != null) {
							String pathFile = fileDir.getPath();
							if (fileDir.isDirectory()) {
								if (fileDir != null && !pathProcessed.contains(pathFile)) {
									ret = processaDirectory(fileDir, file);
									pathProcessed.add(fileDir.getPath());

									// cartella.updateFile(fileDir.getPath(),
									// Boolean.TRUE);
									LOG.info(  "PROCESSATO FILE  " + pathFile );
									toProcees = true;
								}
							} else if (!pathProcessed.contains(pathFile)
									&& documentType != null
									&& documentType.equalsIgnoreCase("Movie")) {
								ret = processaHmiFile(fileDir, file);
								pathProcessed.add(fileDir.getPath());
								LOG.info("PROCESSATO FILE  " + fileDir.getPath() );
								toProcees = true;
							}
						}
					}
				}
				i++;
			}
			if (!toProcees) {
				//				pathProcessed.add( getCurrentDir() );
				spostaInPathSuccessivo();
				return read();
			}
		}else {
			LOG.info(" ---NESSUN FILE presente nella directory  ---  "    );
			LOG.info(" ");
			
		}
		LOG.info(" ---FOLDER DA PROCESSARE ---  " + ret );

		return ret;

	}

	private HmiStructure processaHmiFile(File f, File hmiFile) {
		HmiStructure struttura = new HmiStructure();
		try {
			struttura.setFile(f);
			struttura.setHmiFile(hmiFile);
			String filePath = f.getParent().replaceFirst(startDir, "");
			struttura.setFilePath(filePath);
			List<HmiStructure> children = Collections.emptyList();
			List<HmiStructure> childrenLinks = Collections.emptyList();
			struttura.setChildren(children);
			struttura.setChildrenLinks(childrenLinks);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return struttura;
	}

	private File getFileToProcess(File[] listaFile, File fileHmi) {
		if (fileHmi != null) {
			String name = FileUtil.getFileNoExtension(fileHmi);
			for (File file : listaFile) {
				String fileName = null;
				if (file.isDirectory()) {
					fileName = file.getName();
				} else {
					String currExt = FileUtil.getFileExtension(file);
					if (currExt != null && !currExt.equalsIgnoreCase("hmi")) {
						fileName = file.getName();
					}
				}
				if (fileName != null && fileName.equalsIgnoreCase(name)) {
					return file;
				}
			}
		}
		return null;
	}

	private void spostaInPathSuccessivo() {
		Iterator<Cartella> iteratorCartelle = cartelleLivello.iterator();
		String pathToProcess = null;
		Cartella curCartella = null;
		while (pathToProcess == null && iteratorCartelle.hasNext()) {
			curCartella = iteratorCartelle.next();

			boolean need = isProcessCartella( curCartella );			
			if (need ) {
				pathToProcess = curCartella.getParent();
				// cartella = curCartella;
			}
		}
		if (pathToProcess != null) {
			setCurrentDir(pathToProcess);
		} else {
			cartella = null;
			// Crea Nuovo Livello
			List<Cartella> nuovoLivello = getCartella( cartelleLivello );
			cartelleLivello = nuovoLivello;
			boolean found = false;

			while( !found && !cartelleLivello.isEmpty() ){
				for (Cartella curChildren : cartelleLivello) {
					boolean need = isProcessCartella( curChildren );
					if( need  ){
						pathToProcess = curChildren.getParent();
						found = true;
						break;
					}
					if (curChildren.getFiles() != null 	&& !curChildren.getFiles().isEmpty()) {
						for (String children : curChildren.getFiles().keySet()) {
							if( !pathProcessed.contains(children)) {
								pathToProcess = children;
								found = true;
								break;
							} 
						}
						if( found ){
							break;
						}
					} 
				}
				if( !found ) {
					nuovoLivello = getCartella( cartelleLivello ); 
					cartelleLivello = nuovoLivello;
				}
			}
			setCurrentDir(pathToProcess);
		}
	}

	private List<Cartella>   getCartella( List<Cartella> cartelleLivello){
		List<Cartella> nuovoLivello = new LinkedList<Cartella>();
		for (Cartella cCartella : cartelleLivello) {
			for (String curChildren : cCartella.getFiles().keySet()) {
				Cartella nuovaCartella = initCartella( curChildren );
				nuovoLivello.add(nuovaCartella);
			}
		}
		return nuovoLivello;
	}


	private boolean   isProcessCartella( Cartella cartella ){
		boolean needProcess = false;
		boolean hmiPresent = false;
		File [] listaF = new File(cartella.getParent()  ).listFiles();

		for (File curChildren : listaF ) {
			if( curChildren.getName().endsWith(".hmi") ) {
				hmiPresent = true;				 
				break;
			}
		}
		if( hmiPresent ) {
			if( cartella != null ){	
				for (String curChildren : cartella.getFiles().keySet()) {
					if( !pathProcessed.contains(curChildren)) {
						needProcess = true;
						break;
					}
				}	
			}
		}
		return needProcess;
	}


	private HmiStructure processaDirectory(File file, File hmiFile) {
		HmiStructure struttura = new HmiStructure();
		try {
			struttura.setFile(file);
			struttura.setHmiFile(hmiFile);
			String filePath = file.getPath().replaceFirst(startDir, "");
			struttura.setFilePath(filePath);
			List<HmiStructure> children = readChildren(struttura);
			List<HmiStructure> childrenLinks = readLinkChildren(struttura);
			struttura.setChildrenLinks(childrenLinks);
			
			List<HmiStructure> aliasLinks = readAliasLink(struttura);
			struttura.setAliasLinks(aliasLinks);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return struttura;
	}

	private HmiStructure processaHmiRemoto(File hmiFile, HmiStructure parent) {
		HmiStructure struttura = new HmiStructure();
		struttura.setHmiFile(hmiFile);
		String filePath = hmiFile.getPath().replaceFirst(startDir, "");
		pathProcessed.add( hmiFile.getPath()  );
		struttura.setFilePath(filePath);
		struttura.setParentStructure(parent);
		return struttura;

	}

	private List<HmiStructure> readLinkChildren(HmiStructure strutturaParent)
	throws Exception {
		File parent = strutturaParent.getFile();
		List<HmiStructure> children = new ArrayList<HmiStructure>();
		if (parent != null) {
			File[] listaFile = new File(parent.getPath()).listFiles();
			if (listaFile != null) {
				for (File child : listaFile) {
					String exten = FilenameUtils.getExtension(child.getName());
					if (exten != null && exten.equalsIgnoreCase("hmi")) {
						String formato = HmiFileReader.processFile(child).get( HyperwaveKey.DocumentType );
						if (formato != null && formato.equalsIgnoreCase("Remote")) {
							children.add(processaHmiRemoto(child, 	strutturaParent));
						} 
					}
				}
			}
		}
		return children;

	}
	
	private List<HmiStructure> readAliasLink(HmiStructure strutturaParent)
	throws Exception {
 		File parent = strutturaParent.getFile();
		List<HmiStructure> children = new ArrayList<HmiStructure>();
		if (parent != null) {
			File[] listaFile = new File(parent.getPath()).listFiles();
			if (listaFile != null) {
				for (File child : listaFile) {
					String exten = FilenameUtils.getExtension(child.getName());
					if (exten != null && exten.equalsIgnoreCase("hmi")) {
						String formato = HmiFileReader.processFile(child).get( HyperwaveKey.DocumentType );
						if (formato == null || (  formato != null && !formato.equalsIgnoreCase("Remote") )) {						 
							if( child.isFile()  ){
								File fileDir = getFileToProcess(listaFile, child);
								if ( fileDir == null  ) {								 
									System.out.println( "AliasLink " + child.getPath()  );
									HmiStructure aliasHmi =  processaHmiRemoto(child, 	strutturaParent);
									aliasHmi.setImportDir( startDir );
									aliasHmi.setAlias(true);
									children.add( aliasHmi );
								}
							}
						}
					}
				}
			}
		}
		return children;

	}


	private boolean isIndex(HmiStructure strutturaParent )
	{		
		String formato = strutturaParent.getPropertyHmi(HyperwaveKey.Formato );
		String collection = strutturaParent.getPropertyHmi(HyperwaveKey.DocumentType );

		if( UtilMethods.isSet(formato )  && 
				(  formato.equalsIgnoreCase("HP") ||  formato.equalsIgnoreCase("I1") || formato.equalsIgnoreCase("I2") 
						|| formato.equalsIgnoreCase("I3")	|| formato.equalsIgnoreCase("I4")	 || formato.equalsIgnoreCase("I5")	
				) ){
			return true;
		}
		if( !UtilMethods.isSet(formato ) && !strutturaParent.isContainerCluster() 
				&& !strutturaParent.isContainerCollection() &&  !(collection!= null && collection.equalsIgnoreCase("collection"))) {
	 		return true;
		}
		return false;
	}
	private List<HmiStructure> readChildren(HmiStructure strutturaParent)
	throws Exception {

		List<HmiStructure> children = new ArrayList<HmiStructure>();
		File parent = strutturaParent.getFile();
		if (parent != null) {
			File[] listaFile = new File( parent.getPath()).listFiles();
			if (listaFile != null) {
				for (File child : listaFile) {
					File hmiFile = getFileHmi(listaFile, child);
					if (hmiFile != null &&  child.isFile() ) {
						HmiStructure struttura = creaHmiStructure( child , hmiFile , strutturaParent );						 
						strutturaParent.addChildStructure(struttura);
					} else if( hmiFile != null &&  child.isDirectory()  ) {
						if(    !isIndex( strutturaParent )  || ( isIndex(strutturaParent)) && child.getName().equalsIgnoreCase("links") ) {
							HmiStructure struttura = creaHmiStructure( child , hmiFile , strutturaParent );
							LOG.debug( "PROCESSATO FILE  " + child.getPath()  );
							if (isIterable(strutturaParent, struttura)) {
								pathProcessed.add( child.getPath() );								
								readChildren(struttura);
							}
							strutturaParent.addChildStructure(struttura);
						}
					}
				}
			}
		}
		return children;
	}


	private HmiStructure creaHmiStructure( File child , File hmiFile  , HmiStructure  strutturaParent) throws Exception {

		HmiStructure struttura = new HmiStructure();
		struttura.setFile(child);
		struttura.setHmiFile(hmiFile);
		String filePath = child.getPath().replaceFirst(	startDir, "");
		struttura.setFilePath(filePath);
		struttura.setParentStructure(strutturaParent);
		List<HmiStructure> childrenLinks = readLinkChildren(struttura);
		struttura.setChildrenLinks(childrenLinks);
 		
		List<HmiStructure> aliasLinks = readAliasLink(struttura);
		struttura.setAliasLinks(aliasLinks);
		return struttura ;
	}

	 

	private boolean isIterable(HmiStructure parent, HmiStructure childStr) {
	 	String formato = childStr.getPropertyHmi(HyperwaveKey.Formato);
		String formatoParent = parent.getPropertyHmi(HyperwaveKey.Formato);

		if( parent!= null &&  parent.isCollectionHead() ||  childStr.isCollectionHead() ){
			return true;
		}

		if( childStr.getFilePath().endsWith("links") ){  // caso dei link correlati
			return true;
		}

		if (formato != null && !formato.startsWith("LF") && !formato.equalsIgnoreCase("LV") && !formato.equalsIgnoreCase("IF")
				&& (formato.startsWith("L") || formato.startsWith("News")
						|| formato.startsWith("D5") || formato
						.equalsIgnoreCase("Box"))) {
			return true;
		} else if (formatoParent != null && !formatoParent.startsWith("LF") 
						&& !formatoParent.equalsIgnoreCase("LV") && !formatoParent.equalsIgnoreCase("IF")
						&& (formatoParent.startsWith("L")
								|| formatoParent.startsWith("News")
								|| formatoParent.startsWith("D5") || formatoParent
						.equalsIgnoreCase("Box"))) {
			return true;
		}
		String presentation = childStr.getPropertyHmi( HyperwaveKey.PresentationHints );
		if (formato == null && presentation != null
				&& presentation.equalsIgnoreCase("Hidden")
				&& ( formatoParent != null && formatoParent.equalsIgnoreCase("IF")) ) {
			return true;
		}	

		boolean hasDetailR =   hasListing( childStr );
		boolean hasDetailP =   hasListing( parent );
		if( hasDetailR || hasDetailP ){
			return true;
		}
	  
		if( parent!= null &&  parent.isContainerCollection() ||  childStr.isContainerCollection() ){
			boolean hasDetail = hasDetail( childStr  );
			return hasDetail;			
		}
	
		if( parent!= null &&  parent.isContainerCluster() ||  childStr.isContainerCluster() ){
			//return true;			
			boolean hasDetail = hasDetail( childStr  );
			return hasDetail;
		}	
		String collectionP = parent.getPropertyHmi( HyperwaveKey.CollectionType );
		 	
		if( collectionP != null && collectionP.equalsIgnoreCase("Cluster") ){
			//if( collectionC != null && collectionC.equalsIgnoreCase("Cluster") ){
				return true;
			//}
		}		 
		String filePath = childStr.getFilePath();
		if(filePath.indexOf("footer") != -1 ){
			return true;
		}
		if(filePath.indexOf("relann") != -1 ){
			return true;
		}
			
		if(filePath.indexOf("RePEc") != -1 ){
			return true;
		}
		return false;

	}
	
	private boolean hasListing( HmiStructure childStr ) {
		if( childStr.getParentStructure() == null   ){
			return false;
		}
		HmiStructure str = childStr.getParentStructure();
		String formato = str.getPropertyHmi(HyperwaveKey.Formato);

		if( UtilMethods.isSet(formato ) && ( formato.equalsIgnoreCase("L1") || 
				formato.equalsIgnoreCase("L2") || 
				formato.equalsIgnoreCase("L3") || formato.equalsIgnoreCase("L4") 
				|| formato.equalsIgnoreCase("L5") 
				|| formato.equalsIgnoreCase("L8") 
				|| formato.equalsIgnoreCase("L7")   )){
			return true;
		}else {
			hasListing( str );
		}
		 
		return false;
	}


	private boolean hasDetail( HmiStructure childStr ) {
		String formato = childStr.getPropertyHmi(HyperwaveKey.Formato);

		if( UtilMethods.isSet(formato ) && ( formato.equalsIgnoreCase("D1") || 
				formato.equalsIgnoreCase("D2") || 
				formato.equalsIgnoreCase("D3") || formato.equalsIgnoreCase("DF")  )){
			return true;
		}
		if( childStr.getFile() !=  null && childStr.getFile().isDirectory() )
		{
			File[] listaFile   = childStr.getFile().listFiles();
			for( File file : listaFile ){
				if( file.isFile() )
				{
					String ext = FileUtil.getFileExtension(file);
					if (ext != null && !ext.equalsIgnoreCase("hmi")) {
						return true;
					}
				}
			}

		}
		return false;
	}

	private File getFileHmi(File[] listaFile, File parent) {
		if (parent != null) {
 			String name = parent.getName();
			String parentExt = FileUtil.getFileExtension( parent );
			if( parentExt!= null && parentExt.equalsIgnoreCase("hmi") ){
				return null;
			}
 			for (File file : listaFile) {
				String fileExt = FileUtil.getFileExtension(file);

				String fileName = FileUtil.getFileNoExtension(file);

				if (fileName != null && fileName.equalsIgnoreCase(name)) {
					
					if( fileExt != null && fileExt.equalsIgnoreCase("hmi")){ 
						if( fileName.indexOf(".") > -1){
							if( name.indexOf(".")> -1  ){
								return file;
							} 
						}
						else 
							return file ;
					}

				}
			}
		}
		return null;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		init();
	}

	public List<String> getPathProcessed() {
		return pathProcessed;
	}

	public void setPathProcessed(List<String> pathProcessed) {
		this.pathProcessed = pathProcessed;
	}

	public String getCurrentDir() {
		return currentDir;
	}

	public void setCurrentDir(String currentDir) {
		this.currentDir = currentDir;
	}

	public String getStartDir() {
		return startDir;
	}

}