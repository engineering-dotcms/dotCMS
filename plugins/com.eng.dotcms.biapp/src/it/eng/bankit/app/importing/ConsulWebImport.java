package it.eng.bankit.app.importing;

import it.eng.bankit.app.job.ConsulWebJob;
import it.eng.bankit.app.util.DotFolderUtil;
import it.eng.bankit.deploy.IDeployConst;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;

import junit.framework.Assert;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.viewtools.content.util.ContentUtils;
import com.liferay.portal.model.User;

public class ConsulWebImport extends AbstractImport {

	private static String dettaglioStructureName = "Dettaglio";
	private static String allegatoStructureName = "AllegatoDettaglio";
	private static String linkStructureName = "Link";
	private static String backupBasePath = "/backup";
	private Folder consulwebFolder;
	private Folder backupFolder;
	private String relationName;
	private Structure stDettaglio;
	private Structure stAllegato;
	private Structure stLink;
	private boolean remotePublication = false;
	private boolean backup = false;
	private boolean published = false;
	private boolean fileAsserWorkArround = false;

	public ConsulWebImport( User user, Host host ) {
		super( user, host );
	}

	public void init( String path ) throws Exception {
		if ( !initialized ) {
			if ( UtilMethods.isSet( path ) ) {
				consulwebFolder = DotFolderUtil.findOrCreateFolder( path, false, host, user );
			}
			stDettaglio = StructureCache.getStructureByVelocityVarName( dettaglioStructureName );
			stAllegato = StructureCache.getStructureByVelocityVarName( allegatoStructureName );
			stLink = StructureCache.getStructureByVelocityVarName( linkStructureName );
			relationName = pluginAPI.loadProperty( IDeployConst.PLUGIN_ID, "consWeb.link.relazione" );
			fileAsserWorkArround = readBooleanProperty( "dotCms.fileAssetWorkArround" );
			if ( !updateMode ) {
				checkBackupDir();
			}
			super.init();
		}
	}

	public void importFiles( File fileIt, File fileEn ) throws Exception {
		Assert.assertTrue( "Not initialized", initialized );
		Logger.info( ConsulWebJob.class, "Importo il file: " + fileIt.getName() );
		saveContentletsFile( fileIt, consulwebFolder, langIt );
		Logger.info( ConsulWebJob.class, "Importo il file: " + fileEn.getName() );
		saveContentletsFile( fileEn, consulwebFolder, langEn );
		if ( remotePublication ) {
			remotePublish( true );
		}
		published = true;
	}

	protected void saveContentletsFile( File file, Folder folder, Language language ) throws Exception {

		Contentlet allegato = null;
		Contentlet link = null;
		String title = ( language.getLanguageCode().equalsIgnoreCase( "it" ) ? "Elenco" : "List" );

		if ( updateMode ) {
			allegato = checkoutAllegato( folder, language );
			if ( fileAsserWorkArround && allegato != null && !language.equals( languageApi.getDefaultLanguage() ) ) {
				/*
				 * Workarround per aggirare il problema di disponibilità
				 * dell'url finale di un fileasset multilanguage con file
				 * differenti
				 */
				Contentlet allegatoPezzotto = checkoutAllegatoPezzotto( folder );
				if ( allegatoPezzotto != null ) {
					updateFileAsset( allegatoPezzotto, title, file, folder );
					allegatoPezzotto = persistContentlet( allegatoPezzotto, "consulWeb_FileAsset_" + language.getId() );
				} else {
					Logger.warn( ConsulWebImport.class, "nessun allegato in default language trovato, update parziale" );
				}
			}
		}
		if ( allegato == null ) {
			allegato = createContentlet( stAllegato, language );
			if ( fileAsserWorkArround && !language.equals( languageApi.getDefaultLanguage() ) ) {
				/*
				 * Workarround per aggirare il problema di disponibilità
				 * dell'url finale di un fileasset multilanguage con file
				 * differenti
				 */
				Contentlet allegatoPezzotto = createContentlet( stAllegato, languageApi.getDefaultLanguage() );
				updateFileAsset( allegatoPezzotto, title, file, folder );
				allegatoPezzotto = persistContentlet( allegatoPezzotto, "consulWeb_FileAsset_" + language.getId() );
			}
		}

		updateFileAsset( allegato, title, file, folder );
		allegato = persistContentlet( allegato, "consulWeb_FileAsset_" + language.getId() );

		if ( updateMode ) {
			link = checkoutLink( folder, language );
		}
		if ( link == null ) {
			link = createContentlet( stLink, language );
		}

		updateLinkAllegato( link, allegato );
		link = persistContentlet( link, "consulWeb_Link" );
		Contentlet dettaglio = getContentlet( stDettaglio, language, folder );
		if ( dettaglio != null ) {
			boolean needNewRelation = true;

			if ( updateMode ) {
				List<Contentlet> checkLinks = ContentUtils.pullRelated( relationName, dettaglio.getIdentifier(), false, 1, "modDate desc", user, null );
				if ( !checkLinks.isEmpty() ) {
					if ( checkLinks.get( 0 ).getIdentifier().equals( link.getIdentifier() ) ) {
						needNewRelation = false;
					} else {
						removeRelatedLink( dettaglio, checkLinks.get( 0 ) );
					}
				}
			}

			if ( needNewRelation ) {
				APILocator.getRelationshipAPI().addRelationship( dettaglio.getIdentifier(), link.getIdentifier(), relationName );
			}

		} else {
			Logger.warn( ConsulWebImport.class, "Nessun dettaglio trovato a cui relazionare il link " + link.getTitle() );
		}
	}

	private void updateFileAsset( Contentlet contentletAllegato, String title, File file, Folder folder ) throws Exception {
		setFileAssetFields( contentletAllegato, title, file );
		contentletAllegato.setFolder( folder.getInode() );
		contentletAllegato.setProperty( FileAssetAPI.HOST_FOLDER_FIELD, folder.getInode() );
	}

	private Contentlet checkoutAllegato( Folder folder, Language lang ) throws DotContentletStateException, DotDataException, DotSecurityException, ParseException {
		StringBuilder query = new StringBuilder();
		query.append( "+structureName:" );
		query.append( allegatoStructureName );
		query.append( " +languageId:" );
		query.append( lang.getId() );
		if ( lang.getId() == languageApi.getDefaultLanguage().getId() ) {
			query.append( " -" + allegatoStructureName + ".fileName:*_en.pdf" );
		} else {
			query.append( " +" + allegatoStructureName + ".fileName:*_en.pdf" );
		}
		query.append( " +conFolder:" );
		query.append( folder.getInode() );
		query.append( " +conHost:" );
		query.append( host.getIdentifier() );
		query.append( " +deleted:false +working:true +live:true" );
		List<Contentlet> contents = contentletApi.checkoutWithQuery( query.toString(), user, true );
		if ( !contents.isEmpty() ) {
			return contents.get( 0 );
		}
		return null;
	}

	private Contentlet checkoutAllegatoPezzotto( Folder folder ) throws DotContentletStateException, DotDataException, DotSecurityException, ParseException {
		StringBuilder query = new StringBuilder();
		query.append( "+structureName:" );
		query.append( allegatoStructureName );
		query.append( " +languageId:" );
		query.append( languageApi.getDefaultLanguage().getId() );
		query.append( " +" + allegatoStructureName + ".fileName:*_en.pdf" );
		query.append( " +conFolder:" );
		query.append( folder.getInode() );
		query.append( " +conHost:" );
		query.append( host.getIdentifier() );
		query.append( " +deleted:false +working:true +live:true" );
		List<Contentlet> contents = contentletApi.checkoutWithQuery( query.toString(), user, true );
		if ( !contents.isEmpty() ) {
			return contents.get( 0 );
		}
		return null;
	}

	private void updateLinkAllegato( Contentlet link, Contentlet allegato ) {
		link.setProperty( "titolo", allegato.getStringProperty( "title" ) );
		link.setProperty( "linkType", "A" );
		link.setProperty( "visualizzaIn", "LA" );
		link.setProperty( "mostraTitolo", "S" );
		link.setProperty( "identificativo", allegato.get( FileAssetAPI.FILE_NAME_FIELD ) );
		link.setFolder( allegato.getFolder() );
		link.setProperty( FileAssetAPI.HOST_FOLDER_FIELD, allegato.getFolder() );
		link.setProperty( "allegato", allegato.getIdentifier() );
		link.setProperty( "allegatoId", allegato.getIdentifier() );
		link.setLongProperty( "sortOrder1", 10L );
	}

	private Contentlet checkoutLink( Folder folder, Language lang ) throws DotContentletStateException, DotDataException, DotSecurityException, ParseException {
		StringBuilder query = new StringBuilder();
		query.append( "+structureName:" );
		query.append( linkStructureName );
		query.append( " +" );
		query.append( linkStructureName );
		query.append( ".linkType:A" );
		query.append( " +conFolder:" );
		query.append( folder.getInode() );
		query.append( " +languageId:" );
		query.append( lang.getId() );
		query.append( " +conHost:" );
		query.append( host.getIdentifier() );
		query.append( " +deleted:false +working:true +live:true" );
		List<Contentlet> contents = contentletApi.checkoutWithQuery( query.toString(), user, true );
		if ( !contents.isEmpty() ) {
			return contents.get( 0 );
		}
		return null;
	}

	private void checkBackupDir() throws DotDataException, DotSecurityException {
		backupFolder = DotFolderUtil.findOrCreateFolder( backupBasePath + File.separator + this.getClass().getSimpleName(), false, host, user );
		Assert.assertTrue( "Impossibile create backup folder", backupFolder != null && UtilMethods.isSet( backupFolder.getInode() ) );
		List<Contentlet> results = contentletApi.search( "+conFolder:" + backupFolder.getInode(), 1, 0, "modDate desc", user, true );
		if ( results != null && results.size() > 0 ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "Errore di inizializzazione: trovate " );
			sb.append( results.size() );
			sb.append( " contentlets:[" );
			for ( Contentlet contentlet : results ) {
				if ( results.indexOf( contentlet ) > 0 ) {
					sb.append( ',' );
				}
				sb.append( contentlet.getTitle() );
			}
			sb.append( "] in path backup, possibile esecuzione precedente in errore" );
			throw new DotDataException( sb.toString() );
		}
		Logger.debug( ConsulWebImport.class, "Directory di backup OK" );
	}

	public void backupOldContentlet() throws Exception {
		Assert.assertTrue( "Not initialized", initialized );
		// Move old contentlet in backup hidden path
		List<Contentlet> dettagli = getContentlets( stDettaglio, null, consulwebFolder );
		List<Contentlet> links = null;
		List<Contentlet> allegati = null;
		links = checkoutContentlets( stLink, null, consulwebFolder );
		if ( links.isEmpty() ) {
			Logger.info( ConsulWebImport.class, "Directory " + consulwebFolder.getName() + " vuota, backup non necessario" );
		} else {

			allegati = checkoutContentlets( stAllegato, null, consulwebFolder );
			internalMoveContentlet( allegati, backupFolder, false, true, true );
			// contentletApi.publish( allegati, user, false );
			internalMoveContentlet( links, backupFolder, false, true, true );
			// contentletApi.publish( links, user, false );
			if ( dettagli != null && !dettagli.isEmpty() ) {

			}

			backup = true;
			Logger.info( ConsulWebImport.class, "Backup effettuato, contenuto directory " + consulwebFolder.getName() + " spostato in " + backupFolder.getName() );
		}
	}

	public void abort() throws Exception {
		rPublisher.abortPubblications();
	}

	public void rollBack() throws Exception {
		List<Contentlet> dettagli = getContentlets( stDettaglio, null, consulwebFolder );
		List<Contentlet> links = getContentlets( stLink, null, consulwebFolder );
		List<Contentlet> allegati = getContentlets( stAllegato, null, consulwebFolder );
		List<Contentlet> linksBackup = null;
		List<Contentlet> allegatiBackup = null;

		if ( !published ) {

			if ( backup ) {// Check backup
				linksBackup = checkoutContentlets( stLink, null, backupFolder );
				allegatiBackup = checkoutContentlets( stAllegato, null, backupFolder );
				if ( dettagli.size() != 2 || linksBackup.size() != 2 || allegatiBackup.size() != 3 ) {
					throw new Exception( "Contentlet di backup non trovate Dettagli:" + dettagli.size() + "Links:" + linksBackup.size() + " Allegati:" + allegatiBackup.size()
							+ " Impossibile effettuare il RollBack" );
				}

				if ( remoteBackup && remotePublication ) {
					if ( !rPublisher.remoteServerAvaiable() ) {
						throw new Exception( "nessun server remoto disponibile, impossibile effettuare pubblicazione remota" );
					}
					clear();

					for ( Contentlet link : links ) {
						contentToUnPublish.add( link.getIdentifier() );
					}
					for ( Contentlet allegato : allegati ) {
						contentToUnPublish.add( allegato.getIdentifier() );
					}
					String unpublishBundle = rPublisher.unPublish( contentToUnPublish );
					rPublisher.waitPubblication( unpublishBundle );
					Logger.info( ConsulWebImport.class, "Bundle(" + unpublishBundle + ") di rollback fase clear terminato con successo" );
				}
				// Detele partial import
				removeRelatedLink( dettagli.get( 0 ), links.get( 0 ) );

				for ( Contentlet link : links ) {
					removeContentlet( link );
				}
				for ( Contentlet allegato : allegati ) {
					removeContentlet( allegato );
				}
				// recover from backup
				internalMoveContentlet( allegatiBackup, consulwebFolder, true, false, false );
				internalMoveContentlet( linksBackup, consulwebFolder, true, false, false );
				APILocator.getRelationshipAPI().addRelationship( dettagli.get( 0 ).getIdentifier(), linksBackup.get( 0 ).getIdentifier(), relationName );
				if ( remoteBackup && remotePublication ) {
					String rollbackBundle = rPublisher.publish( contentToPublish );
					rPublisher.waitPubblication( rollbackBundle );
					Logger.info( ConsulWebImport.class, "Bundle(" + rollbackBundle + ") di rollback fase ripristino terminata con successo" );
				}
			} else {
				Logger.info( ConsulWebImport.class, "Nessun contenuto di backup da recuperare" );
			}
			if ( remotePublication ) {
				rPublisher.abortPubblications();
			}
		} else {
			Logger.warn(
					ConsulWebImport.class,
					"I files risultano correttamente pubblicati ma c'è stato un errore nella fase di finalizzazione, contattare l'assistenza per effettuare le verifiche e ripristinare lo stato di consistenza della transazione" );
		}
		clear();
		Logger.info( ConsulWebImport.class, "Rollback effettuato con successo" );
	}

	public void removeRelatedLink( Contentlet dettaglio, Contentlet link ) throws DotDataException {
		Relationship linkRelationship = RelationshipFactory.getRelationshipByRelationTypeValue( relationName );
		if ( dettaglio != null && link != null && linkRelationship != null ) {
			RelationshipFactory.deleteRelationships( dettaglio, linkRelationship, Collections.singletonList( link ) );
			Logger.debug( ConsulWebImport.class, "Cancellata relazioni a link" );
		}

	}

	public void removeOldContentlet() throws Exception {
		try {

			List<Contentlet> dettagli = getContentlets( stDettaglio, null, consulwebFolder );
			List<Contentlet> allegati = getContentlets( stAllegato, null, backupFolder );
			List<Contentlet> links = getContentlets( stLink, null, backupFolder );

			if ( links != null && !links.isEmpty() && dettagli != null && !dettagli.isEmpty() ) {
				removeRelatedLink( dettagli.get( 0 ), links.get( 0 ) );

				for ( Contentlet link : links ) {
					if ( !removeContentlet( link ) ) {
						Logger.warn( ConsulWebImport.class, "Cancellazione contentlet (inode:" + link.getInode() + ") non riuscita" );
					}
				}
				Logger.debug( ConsulWebImport.class, "Cancellati link" );
			}

			if ( allegati != null ) {// rimuovi allegato dettaglio
				for ( Contentlet allegato : allegati ) {
					if ( !removeContentlet( allegato ) ) {
						Logger.warn( ConsulWebImport.class, "Cancellazione contentlet (inode:" + allegato.getInode() + ") non riuscita" );
					}
				}
				Logger.debug( ConsulWebImport.class, "Cancellati allegati" );
			}

		} catch ( Exception e ) {
			throw e;
		}
	}

	private void internalMoveContentlet( List<Contentlet> contents, Folder folder, boolean publish, boolean unpublish, boolean backup ) throws DotStateException, DotDataException,
			DotSecurityException {
		for ( Contentlet contentlet : contents ) {
			// deleteOldVersions( contentlet.getIdentifier() );
			contentlet.setFolder( folder.getInode() );
			contentlet.setProperty( FileAssetAPI.HOST_FOLDER_FIELD, folder.getInode() );
			List<Permission> permissionList = permissionApi.getPermissions( contentlet.getStructure() );
			contentletApi.checkin( contentlet, permissionList, user, false );
			contentletApi.unlock( contentlet, user, false );

			if ( publish ) {// Put content in publish que
				contentToPublish.add( contentlet.getIdentifier() );
			}
			if ( unpublish ) {// Put content in unpublish que to delete
				contentToUnPublish.add( contentlet.getIdentifier() );
			}
			if ( backup ) {// Put content in backup que
				contentToBackup.add( contentlet.getIdentifier() );
			}
		}
		if ( publish ) {
			contentletApi.publish( contents, user, false );
		}
	}

	public boolean isRemotePublishEnabled() {
		return remotePublication;
	}

	public void setRemotePublication( boolean remotePublication ) {
		this.remotePublication = remotePublication;
	}
}
