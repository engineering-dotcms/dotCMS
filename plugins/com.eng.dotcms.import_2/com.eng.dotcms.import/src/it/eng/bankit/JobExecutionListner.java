/**
 * 
 */
package it.eng.bankit;

import it.eng.bankit.util.FolderUtil;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.Logger;

/**
 * @author cesare
 * 
 */
public class JobExecutionListner implements JobExecutionListener {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.JobExecutionListener#beforeJob(org.
	 * springframework.batch.core.JobExecution)
	 */
	@Override
	public void beforeJob( JobExecution jobExecution ) {
		Logger.info( JobExecutionListner.class, "Starting Job " + jobExecution.getJobInstance().getJobName() );

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.JobExecutionListener#afterJob(org.
	 * springframework.batch.core.JobExecution)
	 */
	@Override
	public void afterJob( JobExecution jobExecution ) {
		try {
 			createRSSPages();
		} catch ( IOException e ) {
			Logger.error( JobExecutionListner.class, e.getMessage(),e );
		}
		logStatistics( jobExecution );
	}

	private void logStatistics( JobExecution jobExecution ) {
		Iterator<StepExecution> stepIterator = jobExecution.getStepExecutions().iterator();
		StepExecution initStep = stepIterator.next();
		StepExecution mainStep = stepIterator.next();
		StringBuilder message = new StringBuilder();
		message.append( "Job " + jobExecution.getJobInstance().getJobName() );
		message.append( " Inizializzazione:[" );
		message.append( initStep.getExitStatus() );
		message.append( "]" );
		message.append( " Stato:[" + jobExecution.getExitStatus() );
		message.append( "]" );
		message.append( " DurataTotale:" );
		long startTime = jobExecution.getStartTime().getTime();
		long endTime = jobExecution.getEndTime().getTime();
		long time = ( endTime - startTime ) / 1000;
		if ( time < 60 ) {
			message.append( time + " secondi" );
		} else if ( ( time / 60 ) < 60 ) {
			message.append( ( time / 60 ) + " minuti, " );
			message.append( ( time % 60 ) + " secondi" );
		} else {
			message.append( ( time / 3600 ) + " ore, " );
			message.append( ( ( time % 3600 ) / 60 ) + " minuti, " );
			message.append( ( ( time % 3600 ) % 60 ) + " secondi" );
		}

		int writeCount = mainStep.getWriteCount();
		int rollbackCount = mainStep.getRollbackCount();
		message.append( " Totale contenuti importati:" + writeCount );
		if ( rollbackCount > 0 ) {
			message.append( " Elementi in errore:" + rollbackCount );
		}
		Logger.info( JobExecutionListner.class, message.toString() );
	}

	private void createRSSPages() throws IOException {
		InputStream resourceStream = null;
		DataInputStream in = null;
		try {
			if(!FolderUtil.isInitialized()){
				FolderUtil.init();
			}
			FolderUtil.init();
			
			resourceStream = this.getClass().getResourceAsStream( File.separatorChar + "rss-path.lst" );
			in = new DataInputStream( resourceStream );
			BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
			String curPath = null;
			while ( ( curPath = br.readLine() ) != null ) {
				Folder folder = FolderUtil.findFolder( curPath );
				if ( folder != null ) {
					HTMLPage workingRssPage = APILocator.getHTMLPageAPI().getWorkingHTMLPageByPageURL( FolderUtil.RSS_PAGE, folder );
					if ( ( workingRssPage == null ) ) {
						if ( FolderUtil.createPageOnFolder( FolderUtil.RSS_PAGE, FolderUtil.escapePath( curPath ) + ".rss", "RSS", folder,new Long(60) ) != null ) {
							Logger.info( JobExecutionListner.class, "Creata pagina rss nel path:" + curPath );
						} else {
							Logger.info( JobExecutionListner.class, "Errore generico nel creare pagina rss nel path:" + curPath );
						}
					} else {
						Logger.debug( JobExecutionListner.class, "Pagina rss già presente nel path:" + curPath );
					}
				} else {
					Logger.warn( JobExecutionListner.class, "Impossibile creare pagina rss path:" + curPath + " non inizializzato" );
				}
			}
		} catch ( Exception e ) {
			Logger.error( JobExecutionListner.class, "Impossibile creare pagina rss ", e );
		} finally {
			in.close();
		}
	}

}
