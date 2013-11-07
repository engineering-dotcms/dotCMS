package it.eng.bankit.writer;

import it.eng.bankit.bean.ContentletContainer;


import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.FolderWrapper;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.FolderUtil;
import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;
import it.eng.bankit.util.StructureUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;

public class DotcmsContWriter implements ItemWriter<ContentletContainer>, InitializingBean {
	private static Logger LOG = Logger.getLogger( DotcmsContWriter.class );
	//private static List<Category> EMPTY_CATEGORIES = Collections.emptyList();
	private ContentletAPI contentletAPI = null;
	private IdentifierAPI identifierAPI = null;
	private DotcmsMultilanguageWriter multilanguageWriter;

	@Override
	public void write( List<? extends ContentletContainer> listaContlets ) {
		if ( contentletAPI == null ) {
			contentletAPI = APILocator.getContentletAPI();
		}
		if ( identifierAPI == null ) {
			identifierAPI = APILocator.getIdentifierAPI();
		}

		for ( ContentletContainer curContent : listaContlets ) {
			Folder curPath = null;
			try {
				if ( curContent.getFolder() != null ) {
					curPath = findOrCreateFolder( curContent.getFolder() );
					if(curContent.hasFiles()){
						writeFiles(curContent.getFiles(),curPath);
					}
				}
				multilanguageWriter.clear();
				ContentletWrapper defaultLanguageWrapper = curContent.getDefaultVersion();
				if ( defaultLanguageWrapper != null ) {// Insert first content in default language
					System.out.println( " -------  internalInsert ITALIANO ---------- INIT LANG:  " + defaultLanguageWrapper.getContentlet().getLanguageId()   )  ;
					internalInsert( defaultLanguageWrapper, curPath );
					System.out.println( " -------  internalInsert ITALIANO ---------- END ")  ;
					System.out.println( " -- -- --- --- --- --- --- ")  ;
				}

				multilanguageWriter.clearCounts();
				// Insert all the other versions(english)
				Collection<ContentletWrapper> listaWrapper = curContent.getOtherVersions();
				for ( ContentletWrapper contWrapper : listaWrapper ) {
					System.out.println( " -------  internalInsert INGLESE --------- INIT LANG:  " + contWrapper.getContentlet().getLanguageId()   )  ;

					internalInsert( contWrapper, curPath );
					System.out.println( " -------  internalInsert INGLESE ---------- END ")  ;
				} /*  */
			} catch ( Exception e ) {
				LOG.error( "ERRORE in write ", e );
			}
		}
	}

	private void internalInsert( ContentletWrapper contWrapper, Folder curPath ) {
		try {
			Contentlet contentLet = contWrapper.getContentlet();
			if ( contentLet != null ) {
				if ( curPath != null ) { // assegna il folder path alla contentlet
					setContentletPath( contWrapper, curPath );
				}
				Map<Relationship, List<Contentlet>> relationships = processAllRelations( contWrapper );
				Map<Relationship, List<Contentlet>> relationshipsDettaglio = processRelationDettagli( contWrapper );
				relationships.putAll( relationshipsDettaglio );
				inserisciAllegatiCorpo( contWrapper );
				processaAllegatoSingolo( contWrapper );
				if( contWrapper.getLinkAllegato()  != null ){				 
					inserisciAllegato( contWrapper );
				}else {
					contentLet = insertContentlet( contWrapper, relationships );
					if ( contWrapper.isArchived() ) {
						APILocator.getContentletAPI().archive( contentLet, ImportUtil.getUser(), true );
					}

				}
				contWrapper.setContentlet( contentLet );
				insertListingLink( contWrapper  );				

			} 
		} catch ( Exception e ) {
			LOG.error( "ERRORE in [internalInsert] DURANTE checkin contentlet:" + contWrapper, e );
		}
	}

	private void inserisciAllegatiCorpo( ContentletWrapper contentletWrapper ) {
		Language lang = ImportUtil.getLanguage(  contentletWrapper.getContentlet().getLanguageId() );
		Language l = ImportUtil.getDefaultLanguage();
		if( lang.getId() == l.getId() ){
			inserisciImmaginiCorpo( contentletWrapper );
		}
	}

	// inserimento link
	private void insertListingLink( ContentletWrapper contentletWrapper ) {
		List<ContentletWrapper> listingLinks = contentletWrapper.getListingLinks();

		for ( ContentletWrapper listingLink : listingLinks ) {
			Map<Relationship, List<Contentlet>> relationships = processAllRelations( listingLink );
			Map<Relationship, List<Contentlet>> relationshipsDettaglio = processRelationDettagli( listingLink );
			relationships.putAll( relationshipsDettaglio );
			if ( listingLink.getLinkAllegato() != null ) {
				inserisciAllegato( listingLink );
			}else {
				insertContentlet( listingLink, relationships );
			}
			inserisciImmaginiCorpo( listingLink );
			insertListingLink( listingLink );
		}
	}

	private Contentlet insertContentlet( ContentletWrapper contentWrapper, Map<Relationship, List<Contentlet>> relationships ) {
		Contentlet contentLet = contentWrapper.getContentlet();
		try {
			LOG.info("---  insertContentlet  INIT --- "   );
			String exist = existContentlet( contentWrapper.getQuery() );
			if ( !UtilMethods.isSet( exist ) ) {
				Identifier id = APILocator.getIdentifierAPI().loadFromCache( contentLet.getFolder()  );
				if( id != null ){  			
					LOG.info("  CONTENTLET  TITOLO " + contentLet.get("title")  +" DA INSERIRE " +  "FOLDER " + id.getURI() + " STRUTTURA " + contentLet.getStructure().getVelocityVarName() );
				}
				List<Category> cats = processaCategorie( contentWrapper );
				if(contentWrapper.isTranslated()   ){
					relationships = new HashMap<Relationship, List<Contentlet>> ();
				}
				contentLet = saveContentlet( contentLet,relationships,cats );
				if( !contentWrapper.isArchived()  ){
					APILocator.getContentletAPI().publish(contentLet, ImportUtil.getUser() , true);
				}
				LOG.info("---  insertContentlet  END INSERT OK  --- "   );
			}else {
				System.out.println( "---     ---");
				System.out.println( "contentlet    " + contentLet.get("titolo")  + " ESISTEEEEE!!");
				Identifier id = APILocator.getIdentifierAPI().find( exist );
				contentLet = APILocator.getContentletAPI().findContentletForLanguage(contentLet.getLanguageId(), id); 
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			LOG.error( "ERRORE insertContentlet -->  " + contentLet.get("title" ));
		}
		return contentLet;
	}

	private List<Contentlet> inserisciAllegato( ContentletWrapper contentletWrapper ) {
		List<Contentlet> allegatiCorpo = new ArrayList<Contentlet>();
		try {
			LOG.info( "inserisciAllegato  [INIT] " );
			Contentlet contentlet = contentletWrapper.getContentlet();
			contentlet = insertContentlet( contentletWrapper, new HashMap<Relationship, List<Contentlet>>() );
			LOG.info( "INSERITO ALLEGATO --> TITOLO " + contentlet.get("title") + " FOLDER " + contentlet.getFolder() );
			//	Identifier identifier = APILocator.getIdentifierAPI().find( contentlet );
			
			Contentlet linkAllegato = contentletWrapper.getLinkAllegato();
			if( UtilMethods.isSet( contentlet.getIdentifier() ) ){
				List<Category> listCategories =  null;
				List<String> categorie = contentletWrapper.getCategories();
				if( categorie!= null && categorie.size() > 0 ) {
					listCategories = new ArrayList<Category>( categorie.size() );
				 	for ( String categ : categorie ) {
						Category category = ImportUtil.getCategoryByKey( categ );
						listCategories.add( category );
					}
				}
				linkAllegato.setProperty( "allegato", contentlet.getIdentifier() );
				linkAllegato.setProperty( "allegatoId", contentlet.getIdentifier() );				
				linkAllegato = saveContentlet( linkAllegato , null , listCategories );
				LOG.info( "INSERITO linkAllegato Associato all'allegato --> TITOLO " + linkAllegato.get("title")  );
				if( !contentletWrapper.isArchived()  ){
					APILocator.getContentletAPI().publish(linkAllegato, ImportUtil.getUser() , true);
					LOG.info( "LINk ALLEGATO --> TITOLO " + linkAllegato.getTitle() + " pubblicato " );					
				}
				else if ( contentletWrapper.isArchived() ) {
					APILocator.getContentletAPI().archive( linkAllegato, ImportUtil.getUser(), true );
				}
			}
			allegatiCorpo.add( contentlet );
			LOG.info( "inserisciAllegato  [FINE] " );

		} catch ( Exception e ) {
			LOG.error( "METHOD inserisciAllegato ERRORE    ", e );
			e.printStackTrace();
		}
		return allegatiCorpo;
	}

	private void inserisciImmaginiCorpo( ContentletWrapper contentletWrapper ) {
		List<ContentletWrapper> imgs = contentletWrapper.getImmaginiAllegati();
		try {			
			if( imgs != null && imgs.size() > 0  ){
				System.out.println( "METODO inserisciImmaginiCorpo [INIZIO]");
				for ( ContentletWrapper conWrapper : imgs ) {
					Contentlet contentlet = conWrapper.getContentlet();
					String ex = existContentlet( conWrapper.getQuery() );
					contentlet = insertContentlet( conWrapper, new HashMap<Relationship, List<Contentlet>>() );
					if( !conWrapper.isArchived() &&  UtilMethods.isSet( ex ) ) {
						APILocator.getContentletAPI().publish( contentlet, ImportUtil.getUser(), true );
					}
					LOG.info( "INSERITA IMMAGINE --> TITOLO " + contentlet.getTitle() + " FOLDER " + contentlet.getFolder() );
					LOG.info("METODO inserisciImmaginiCorpo [FINE]");	
				}
			}
		} catch ( Exception e ) {
			LOG.error( "METHOD inserisciImmaginiCorpo ERRORE  ", e );
		}
	}

	private Folder findOrCreateFolder( FolderWrapper folderWrapper ) throws Exception {
		Folder folder = FolderUtil.findFolder( folderWrapper.getPath() );
		if ( folder != null ) {
			return folder;
		} else {
			LOG.debug( "Folder:" + folderWrapper.getPath() + " on host:" + ImportUtil.getHost().getHostname() + "not found" );
			folder = FolderUtil.createFolder( folderWrapper.getPath(), folderWrapper.getSortOrder(), folderWrapper.isShowOnMenu(),
					folderWrapper.getTranslations(), folderWrapper.getAccessKeys() );
			FolderUtil.createIndexOnFolder( FolderUtil.escapePath( folderWrapper.getPath()), folderWrapper.getTemplateName(), folder );
			return folder;
		}
	}

	private void writeFiles(Map<String,Collection<File>> files,Folder folder) throws DotDataException{
		Folder curFolder=folder;
		String basePath=identifierAPI.loadFromCache(folder.getIdentifier()).getPath();
		basePath = basePath.substring( 0, basePath.length()-1);
		for(String path:files.keySet()){
			try {
				if(!path.equals( System.getProperty("file.separator") )){
					curFolder=FolderUtil.findOrCreateFolder( basePath+path );
				}
				for(File file:files.get( path )){
					try {
						FileUtil.convertAndSaveFile(file,curFolder);
					} catch ( Exception e ) {
						LOG.error( "Error converting file:"+file.getName(),e );
					}
				}
			}catch ( Exception e ) {
				LOG.error( "Error accessing folder:"+path,e );
			}
		}
	}

	private void setContentletPath( Contentlet contentlet, Folder path ) {
		String hostOrFolderProperty = StructureUtil.findHostOrFolderField( contentlet.getStructureInode() ).getVelocityVarName();
		contentlet.setFolder( path.getInode() );
		contentlet.setProperty( hostOrFolderProperty, path.getInode() );
	}

	private void setContentletPath( ContentletWrapper contentletWrapper, Folder path ) throws Exception {

		Contentlet contentlet = contentletWrapper.getContentlet();
		setContentletPath( contentlet, path );

		if ( contentletWrapper.getAllegato() != null ) {
			setContentletPath( contentletWrapper.getAllegato(), path );
		}
		if ( contentletWrapper.getLinks() != null && !contentletWrapper.getLinks().isEmpty() ) {
			for ( ContentletWrapper curLinkWrapper : contentletWrapper.getLinks() ) {
				setContentletPath( curLinkWrapper.getContentlet(), path );
			}
		}
	}

	public String existContentlet( String luceneQuery ) {
		if ( luceneQuery != null ) {
			LOG.info( "Q: " + luceneQuery );
			try {
				List<Contentlet> hits = new ArrayList<Contentlet>();
				hits = APILocator.getContentletAPI().search( luceneQuery, 1, 0, "modDate desc", ImportUtil.getUser(), true );
				LOG.info( "Q ESISTE --> " + ( hits != null && hits.size() > 0 ) );
				if(   hits != null && hits.size() > 0 ) {
					return hits.get(0).getIdentifier();
				}
			} catch ( Exception e ) {
				LOG.error( "ERRORE LUCENE QUERY existContentlet ", e );
			}
		}
		return null;
	}

	private Contentlet saveContentlet(Contentlet contentlet) throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException, DotDataException, DotSecurityException{
		return saveContentlet(contentlet,null, null);
	}

	private Contentlet saveContentlet(Contentlet contentlet,Map<Relationship, List<Contentlet>> relationships,List<Category> categorie) throws DotDataException, DotContentletValidationException, DotContentletStateException, IllegalArgumentException, DotSecurityException{
		try{	
			LOG.info("METODO saveContentlet [INIZIO]" );
			LOG.info("TITOLO " + contentlet.get("titolo") + " Struttura " + contentlet.getStructure().getVelocityVarName() );
			List<Permission> permissionList = APILocator.getPermissionAPI().getPermissions( contentlet.getStructure() );

			multilanguageWriter.preProcess( contentlet );
			if ( relationships == null && categorie == null ){
				contentlet = APILocator.getContentletAPI().checkin( contentlet, permissionList,  ImportUtil.getUser(), true );
			}else if(relationships == null && categorie != null ){
				contentlet = APILocator.getContentletAPI().checkin( contentlet, categorie, permissionList, ImportUtil.getUser(), true );
			}else{
				contentlet = APILocator.getContentletAPI().checkin( contentlet, relationships, categorie, permissionList, ImportUtil.getUser(), true );
			}
			multilanguageWriter.postProcess( contentlet );
			LOG.info("METODO saveContentlet [FINE]");
		}catch (Exception e) {
			System.out.println( " ERRORE -saveContentlet    contentlet " + contentlet.getStringProperty("titolo"));
			e.printStackTrace();
		}
		return contentlet;
	}

	private Map<Relationship, List<Contentlet>> processRelationDettagli( ContentletWrapper contWrapper ) {
		Map<Relationship, List<Contentlet>> relationships = new HashMap<Relationship, List<Contentlet>>();

		Map<Relationship, List<Contentlet>> relationshipsDettagli = processDettagli( contWrapper );

		Iterator<Relationship> it = relationshipsDettagli.keySet().iterator();
		while ( it.hasNext() ) {
			Relationship object = it.next();
			relationships.put( object, relationshipsDettagli.get( object ) );
		}

		Map<Relationship, List<Contentlet>> relationshipsDettagliCluster = processDettagliCluster( contWrapper );

		Iterator<Relationship> itCl = relationshipsDettagliCluster.keySet().iterator();
		while ( itCl.hasNext() ) {
			Relationship object = itCl.next();
			relationships.put( object, relationshipsDettagliCluster.get( object ) );
		}
		return relationships;
	}


	private Map<Relationship, List<Contentlet>> processAllRelations( ContentletWrapper contWrapper ) {
		Map<Relationship, List<Contentlet>> relationships = new HashMap<Relationship, List<Contentlet>>();
		Map<Relationship, List<Contentlet>> relationshipsAllegati = processAllegati( contWrapper );
		Map<Relationship, List<Contentlet>> relationshipsLinks = processLink( contWrapper );
		Map<Relationship, List<Contentlet>> relationshipsCorrelati = processCorrelati( contWrapper );
		List<Contentlet> cLsit = new ArrayList<Contentlet>();
		Iterator<Relationship> itAlleg = relationshipsAllegati.keySet().iterator();
		while ( itAlleg.hasNext() ) {
			Relationship object = itAlleg.next();
			cLsit.addAll( relationshipsAllegati.get( object ) );
		}

		Iterator<Relationship> itLinks = relationshipsLinks.keySet().iterator();
		while ( itLinks.hasNext() ) {
			Relationship object = itLinks.next();
			relationships.put( object, relationshipsLinks.get( object ) );
		}
		Relationship relatLinks = ImportUtil.getRelationship( contWrapper.getContentlet().getStructure(), contWrapper.getLinksRelationShipName() );
		List<Contentlet> listaLinks = relationships.get( relatLinks );
		if ( listaLinks != null  ) {
			if ( cLsit.size() > 0 ) {
				listaLinks.addAll( cLsit );
				relationships.remove( relatLinks );
				relationships.put( relatLinks, listaLinks );
			}
		} else {
			if ( cLsit.size() > 0 ) {
				relationships.put( relatLinks, cLsit );
			}
		}
		Iterator<Relationship> itCorrelati = relationshipsCorrelati.keySet().iterator();
		while ( itCorrelati.hasNext() ) {
			Relationship object = itCorrelati.next();
			relationships.put( object, relationshipsCorrelati.get( object ) );
		}
		return relationships;
	}

	private List<Category> processaCategorie( ContentletWrapper contWrapper ) {
		List<Category> categories = new ArrayList<Category>();
		List<String> cats = contWrapper.getCategories();
		for ( String categ : cats ) {
			Category category = ImportUtil.getCategoryByKey( categ );
			categories.add( category );
		}
		return categories;
	}

	private Map<Relationship, List<Contentlet>> processDettagliCluster( ContentletWrapper contWrapper ) {
		Contentlet parent = contWrapper.getContentlet();
		Map<Relationship, List<Contentlet>> relationships = new HashMap<Relationship, List<Contentlet>>();

		List<ContentletWrapper> listaDettagli = contWrapper.getDettagliCluster();

		if ( listaDettagli.size() > 0 ) {
			LOG.info( "Processo Dettaglio Cluster -Dettagli Associati :  " + listaDettagli );

			List<Contentlet> listC = new ArrayList<Contentlet>();
			Relationship relationDettaglio = ImportUtil.getRelationship( parent.getStructure(), 
					ImportConfig.getProperty( "REL_NAME_DETTAGLIOCLUSTER-DETTAGLIO" ) );
			for ( ContentletWrapper dettaglioWrapper : listaDettagli ) {
				try {
					Contentlet dettaglio = dettaglioWrapper.getContentlet();
					Map<Relationship, List<Contentlet>> relationshipsDettCluster = processAllRelations( dettaglioWrapper );
					inserisciAllegatiCorpo( dettaglioWrapper );
					dettaglio = insertContentlet( dettaglioWrapper, relationshipsDettCluster );
					LOG.info( "Inserito il dettaglio per associazione cluster " + dettaglio.get( "titolo" ) );
					insertListingLink( dettaglioWrapper );

					listC.add( dettaglio );
				} catch ( Exception e ) {
					LOG.error( "ERRORE INSERIMENTO DETTAGLIO cluster ", e );
					e.printStackTrace();
				}
			}
			relationships.put( relationDettaglio, listC );
		}
		return relationships;
	}
	private Map<Relationship, List<Contentlet>> processDettagli( ContentletWrapper contWrapper ) {
		Contentlet parent = contWrapper.getContentlet();
		Map<Relationship, List<Contentlet>> relationships = new HashMap<Relationship, List<Contentlet>>();

		List<ContentletWrapper> listaDettagli = contWrapper.getDettagliD5();

		if ( listaDettagli.size() > 0 ) {
			LOG.info( "Processo Dettaglio Contenitore-Dettagli Associati :  " + listaDettagli );

			List<Contentlet> listC = new ArrayList<Contentlet>();
			Relationship relationDettaglio = ImportUtil.getRelationship( parent.getStructure(),
					ImportConfig.getProperty( "REL_NAME_DETTAGLIOCONTENITORE-DETTAGLIO" ) );
			for ( ContentletWrapper dettaglioWrapper : listaDettagli ) {
				try {

					Contentlet dettaglio = dettaglioWrapper.getContentlet();
					Map<Relationship, List<Contentlet>> relationshipsFiglioD5 = processAllRelations( dettaglioWrapper );
					inserisciAllegatiCorpo( dettaglioWrapper );
					dettaglio = insertContentlet( dettaglioWrapper, relationshipsFiglioD5 );
					LOG.info( "Inserito il figlio per D5  " + dettaglio.get( "titolo" ) );
					listC.add( dettaglio );
				} catch ( Exception e ) {
					LOG.error( "ERRORE INSERIMENTO DETTAGLIO FIGLIO ", e );
					e.printStackTrace();
				}
			}
			relationships.put( relationDettaglio, listC );
		}
		return relationships;
	}

	private Map<Relationship, List<Contentlet>> processCorrelati( ContentletWrapper contWrapper ) {
		Contentlet parent = contWrapper.getContentlet();
		Map<Relationship, List<Contentlet>> relationships = new HashMap<Relationship, List<Contentlet>>();
		List<ContentletWrapper> listaCorrelati = contWrapper.getLinkCorrelati();

		if ( listaCorrelati.size() > 0 ) {
			LOG.info( "Processa i doc correlati :  " );
			List<Contentlet> listC = new ArrayList<Contentlet>();
			Relationship relationDettaglioCorrelato = ImportUtil.getRelationship( parent.getStructure(),
					ImportConfig.getProperty( "REL_NAME_DETTAGLIO-DETTAGLIOCORRELATI" ) );
			for ( ContentletWrapper correlatoWrapper : listaCorrelati ) {
				try {
					Contentlet correlato = correlatoWrapper.getContentlet();
					Map<Relationship, List<Contentlet>> relationshipsCorrelati = processAllRelations( correlatoWrapper );
					correlato = insertContentlet( correlatoWrapper, relationshipsCorrelati );
					listC.add( correlato );
				} catch ( Exception e ) {
					LOG.error( "ERRORE INSERIMENTO DOC CORRELATI  ", e );
					System.err.println( " ***ERRORE processCorrelati ***  " );
					e.printStackTrace();
				}
			}
			relationships.put( relationDettaglioCorrelato, listC );
		}
		return relationships;
	}

	private Map<Relationship, List<Contentlet>> processLink( ContentletWrapper contWrapper ) {
		Contentlet contentLet = contWrapper.getContentlet();
		Map<Relationship, List<Contentlet>> relationships = new HashMap<Relationship, List<Contentlet>>();

		List<ContentletWrapper> listaLink = contWrapper.getLinks();
		if ( listaLink.size() > 0 ) {
			List<Contentlet> listC = new ArrayList<Contentlet>();

			Relationship relatLinks = ImportUtil.getRelationship( contentLet.getStructure(), contWrapper.getLinksRelationShipName() );
			if ( relatLinks != null ) {
				for ( ContentletWrapper linkWrapper : listaLink ) {
					try {
						Contentlet link = linkWrapper.getContentlet();
						String ex = existContentlet( linkWrapper.getQuery() );
						if( !UtilMethods.isSet( ex ) ) {
							link = saveContentlet( link );
							if ( linkWrapper.isArchived() ) {
								LOG.info( "DEVO ARCHIVIARE   LINK   " );
								//							APILocator.getContentletAPI().archive( link, ImportUtil.getUser(), true );
							}else {
								if( !contWrapper.isArchived() ){
									APILocator.getContentletAPI().publish( link, ImportUtil.getUser(), true );
								}
							}
							LOG.info( "INSERITO OGGETTO LINK ( " + link.getLanguageId() + " )--> TITOLO " + link.get( "titolo" )  );
							listC.add( link );
						}else{
							LOG.info( "Il LINK ( " + link.getLanguageId() + " )--> ESISTE " + link.get( "titolo" )  );							
						}
					} catch ( Exception e ) {
						LOG.error( "ERRORE processLink INSERIMENTO LINK ", e );
					}
				}
				relationships.put( relatLinks, listC );
			}
		}
		return relationships;
	}

	private Map<Relationship, List<Contentlet>> processAllegati( ContentletWrapper contWrapper ) {
		Contentlet contentLet = contWrapper.getContentlet();
		Map<Relationship, List<Contentlet>> relationships = new HashMap<Relationship, List<Contentlet>>();
		List<ContentletContainer> listaAllegati = contWrapper.getAllegati();
		Relationship relatLinks = ImportUtil.getRelationship( contentLet.getStructure(), contWrapper.getLinksRelationShipName() );
		if( listaAllegati.size() > 0 && relatLinks == null   ){
			System.out.println(  " STAI PROCESSANDO GLI ALLEGATI MA HAI UNA RELAZIONE NULL " + contentLet.getStructure().getVelocityVarName() + " " +contWrapper.getLinksRelationShipName()  );
		}
		if ( listaAllegati.size() > 0 && relatLinks != null  ) {
			List<Contentlet> listC = new ArrayList<Contentlet>();
			for ( ContentletContainer allegato : listaAllegati ) {
				try {
 					ContentletWrapper attachWrapper = allegato.get( contentLet.getLanguageId() );					
					Contentlet cont = attachWrapper.getContentlet();
					String ex = existContentlet( attachWrapper.getQuery() );
					if( !UtilMethods.isSet( ex ) ) {

						cont = saveContentlet( cont );
						LOG.info( "INSERITO OGGETTO ALLEGATO ( " + cont.getLanguageId() + " )--> TITOLO " +cont.get( "titolo" ));
						if ( attachWrapper.isArchived() ) {
							//APILocator.getContentletAPI().archive( cont, ImportUtil.getUser(), true );
							LOG.info( "DEVO ARCHIVIARE   ALLEGATO archive " );
						} else {
							APILocator.getContentletAPI().publish( cont, ImportUtil.getUser(), true );
						}
					}
					Contentlet link = attachWrapper.getLinkAllegato();

					String identifier = cont.getIdentifier(); 
					if( UtilMethods.isSet( cont.getIdentifier() ) ){
						link.setProperty( "allegato", identifier);
						link.setProperty( "allegatoId", identifier );	
						link = saveContentlet( link );
						if ( attachWrapper.isArchived() ) {
							//APILocator.getContentletAPI().archive( cont, ImportUtil.getUser(), true );
							LOG.info( "DEVO ARCHIVIARE   ALLEGATO archive " );
						} else {
							APILocator.getContentletAPI().publish( link, ImportUtil.getUser(), true );
						}
					}
					LOG.info( "INSERITO OGGETTO LINK PER CORRELAZIONE ALLEGATO ( " + link.getLanguageId() + " )--> TITOLO " +  link.get( "titolo" ) );
					if(!attachWrapper.isTranslated()   ){
						listC.add( link );
					}
				} catch ( Exception e ) {
					LOG.error( "ERRORE INSERIMENTO ALLEGATO  ", e );
				}
			}

			relationships.put( relatLinks, listC );
		}
		return relationships;
	}

	private void processaAllegatoSingolo( ContentletWrapper contentletWrapper ) throws Exception {

		if ( contentletWrapper.getAllegato() != null ) {
			Contentlet allegato = contentletWrapper.getAllegato();
			Contentlet curContentlet = contentletWrapper.getContentlet();
			allegato = saveContentlet( allegato );
			if (contentletWrapper.isArchived()){
				APILocator.getContentletAPI().archive( allegato, ImportUtil.getUser(), true );
			}else{
				contentletAPI.publish( allegato, ImportUtil.getUser(), true );
			}
			curContentlet.setProperty( contentletWrapper.getLinksRelationShipName(), allegato.getIdentifier() );
			LOG.info( "INSERITO ALLEGATO SINGOLO ( " + allegato.getLanguageId() + " )--> TITOLO " + allegato.getTitle() );
		}
	}

	public DotcmsMultilanguageWriter getMultilanguageWriter() {
		return multilanguageWriter;
	}

	public void setMultilanguageWriter( DotcmsMultilanguageWriter multilanguageWriter ) {
		this.multilanguageWriter = multilanguageWriter;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull( multilanguageWriter, "Missing Multilanguage Writer" );
	}

}
