package it.eng.bankit;

import it.eng.bankit.util.ImportConfig;
import it.eng.bankit.util.ImportUtil;

import org.apache.log4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.Assert;

import com.dotmarketing.cache.StructureCache;

public class InitTasklet extends StepExecutionListenerSupport implements Tasklet {
	private static Logger logger = Logger.getLogger( InitTasklet.class );

	@Override
	public RepeatStatus execute( StepContribution contribution, ChunkContext chunkContext ) throws Exception {
		checkImportContext();
		return RepeatStatus.FINISHED;
	}

	private void checkImportContext() throws Exception {
		Assert.notNull( ImportUtil.getHost(), "Host di import mancante" );
		logger.debug( "Trovato host" + ImportUtil.getHost().getHostname() );
		Assert.notNull( ImportUtil.getUser(), "Utente di import mancante" );
		logger.debug( "Caricato utente di import:" + ImportUtil.getUser().getFullName() );
		Assert.hasText( ImportConfig.getProperty( "STRUCTURE_LINK" ), "Nome Struttura Link non definito" );
		Assert.notNull( StructureCache.getStructureByVelocityVarName( ImportConfig.getProperty( "STRUCTURE_LINK" ) ), "Struttura Link non accessibile" );
		logger.debug( "Struttura Link(" + ImportConfig.getProperty( "STRUCTURE_LINK" ) + ") OK" );
	}
	
}
