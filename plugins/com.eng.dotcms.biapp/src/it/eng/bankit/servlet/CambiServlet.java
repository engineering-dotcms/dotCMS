package it.eng.bankit.servlet;

import it.eng.bankit.app.util.DisplayUtil;
import it.eng.bankit.app.util.MailUtil;
import it.eng.bankit.deploy.ApplicationPluginDeployer;
import it.eng.bankit.deploy.IDeployConst;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class CambiServlet extends HttpServlet {
	private static final long serialVersionUID = -1121512696725560621L;
	private static final String IMPORT_NO_STARTED = "100 Nessuna importazione avviata";
	private static final String IMPORT_RUNNING_MSG = "202 Importazione già avviata";
	private static final String IMPORT_STARTING_MSG = "201 Importazione avviata";
	private static final String IMPORT_REMOTE_MSG = "200 Remote Publisging avviato";
	private static final String IMPORT_FINALIZING_MSG = "204 Finalizing import";
	private static final String IMPORT_END_MSG = "205 Importazione conclusa con successo";
	private static final String IMPORT_METHOD_DISABLED = "405 Moetodo %s disabilitato";
	private static final String IMPORT_ERROR_MSG = "501 Errore Importazione: ";
	private static final String IMPORT_ERROR_END_MSG = "502 Importazione conclusa con errore";

	private static final String USER_PARAMETER = "UserID";
	private static final String OPERATION_PARAMETER = "op";

	private static enum Operation {
		getPublishingStatus, startPublish, abortPublish
	};

	private static CambiThread action = null;
	private User user;
	private Host host;
	private boolean getEnabled = false;
	private boolean postEnabled = false;
	private String importDir;
	private String selettorePath;
	private String cambiPath;
	private String indicatoriPath;
	private boolean remotePublishing;
	private String emailFrom;
	private String emailFromName;
	private String emailAddres;
	private MailUtil mailer;
	private String localHostName;
	private boolean updateMode = false;

	@Override
	public void init( ServletConfig config ) throws ServletException {
		Logger.info( CambiServlet.class, "INIT della servlet Importazione Cambi" );
		try {
			ApplicationPluginDeployer.initialize();
			user = APILocator.getUserAPI().getSystemUser();
			String httpGetProp = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.http.get" );
			String httpPostProp = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.http.post" );
			if ( UtilMethods.isSet( httpGetProp ) ) {
				getEnabled = Boolean.parseBoolean( httpGetProp );
			}
			if ( UtilMethods.isSet( httpPostProp ) ) {
				postEnabled = Boolean.parseBoolean( httpPostProp );
			}
			Assert.isTrue( getEnabled || postEnabled, "Nessuna interfaccia di rete abilitata, inpossibile avviare la servlet" );

			String hostName = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "bankit.host_name" );
			importDir = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.import_dir" );
			selettorePath = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.selettore.path" );
			cambiPath = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.path" );
			indicatoriPath = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "indicatori.path" );			
			String remotePubblishingProp = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.remotePubblishing" );
			if ( UtilMethods.isSet( remotePubblishingProp ) ) {
				remotePublishing = Boolean.parseBoolean( remotePubblishingProp );
			}
			String updateModeProp = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.updateMode" );
			if ( UtilMethods.isSet( updateModeProp ) ) {
				updateMode = Boolean.parseBoolean( updateModeProp );
			}

			//Assert.hasText( importDir, "Parametro cambi.import_dir non impostato" );
			//Assert.hasText( selettorePath, "Parametro cambi.selettore.path non impostato" );
			//Assert.hasText( cambiPath, "Parametro cambi.path non impostato" );

			if ( UtilMethods.isSet( hostName ) ) {
				host = APILocator.getHostAPI().find( hostName, user, true );
			}
			if ( host == null ) {
				host = APILocator.getHostAPI().findDefaultHost( user, true );
			}
			Assert.notNull( host, "Errore nel recuperare l'host:" + hostName );

			emailFrom = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.email.from" );
			emailFromName = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.email.from.name" );
			emailAddres = APILocator.getPluginAPI().loadProperty( IDeployConst.pluginId, "cambi.email.addres" );
			mailer = new MailUtil( emailFrom, emailFromName, emailAddres );

		} catch ( DotDataException e ) {
			throw new ServletException( "Errore dati di inizializzazione servlet Importazione Cambi", e );
		} catch ( DotSecurityException e ) {
			throw new ServletException( "Errore sicurezza di inizializzazione servlet Importazione Cambi", e );
		} catch ( Exception e ) {
			throw new ServletException( "Errore generico di inizializzazione servlet Importazione Cambi", e );
		}
	}

	@Override
	public void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		if ( getEnabled ) {
			internalProcess( req, resp );
		} else {
			methodNotEnabled( "Get", resp );
		}
	}

	@Override
	protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
		if ( postEnabled ) {
			internalProcess( req, resp );
		} else {
			methodNotEnabled( "Post", resp );
		}
	}

	protected void internalProcess( HttpServletRequest request, HttpServletResponse response ) throws IOException {
		// String user = request.getRemoteUser();
		Logger.info( CambiServlet.class, "internalProcess " );
		localHostName = request.getServerName();
		String address = request.getRemoteAddr();
		String host = request.getRemoteHost();
		int port = request.getRemotePort();
		String userIdParameter = request.getParameter( USER_PARAMETER );
		String operationParameter = request.getParameter( OPERATION_PARAMETER );
		Logger.info( CambiServlet.class, "Importazione Cambi interrogata da:" + userIdParameter + "(" + address + ") from " + host + ":" + port );
		PrintWriter out = response.getWriter();
		response.setCharacterEncoding( "UTF-8" );
		response.setContentType( "text/plain" );
		response.setStatus( HttpServletResponse.SC_OK );// Fake ok for wget

		if ( !UtilMethods.isSet( userIdParameter ) ) {
			Logger.info( CambiServlet.class, USER_PARAMETER + " mancante" );
			out.print( IMPORT_ERROR_MSG );
			out.print( USER_PARAMETER + " mancante, " );
			out.print( "impossibile procedere" );
			return;
		}
//		else {
//			try{
//				User usr = APILocator.getUserAPI().loadUserById(userIdParameter);
//			}catch (Exception e) {
//				 	Logger.info( CambiServlet.class, "Utente non presente in DOTCMS " );
//					out.print( IMPORT_ERROR_MSG );
//					out.print( USER_PARAMETER + "  "+ userIdParameter+ " non presente. " );
//					out.print( "impossibile procedere" );
//					return;
//			}
//			
//			
//		}
		Logger.info( CambiServlet.class, "operationParameter value : " +  operationParameter );

		if ( !UtilMethods.isSet( operationParameter ) || operationParameter.equalsIgnoreCase( Operation.getPublishingStatus.name() ) ) {
			printStatus( out );
		} else if ( operationParameter.equalsIgnoreCase( Operation.startPublish.name() ) ) {
			try {

				startImport( userIdParameter, out );
			} catch ( InterruptedException e ) {
				out.print( IMPORT_ERROR_MSG );
				out.print( e );
			}
		} else if ( operationParameter.equalsIgnoreCase( Operation.abortPublish.name() ) ) {
			abortImport( out );
		} else {
			out.print( operationParameter );
			out.println( " operazione non supportata" );
			out.println( "Operazioni supportate:" );
			for ( Operation operation : Operation.values() ) {
				out.println( operation );
			}
		}
	}

	private void methodNotEnabled( String method, HttpServletResponse response ) throws IOException {
		PrintWriter out = response.getWriter();
		response.setCharacterEncoding( "UTF-8" );
		response.setContentType( "text/plain" );
		response.setStatus( HttpServletResponse.SC_OK );
		out.printf( IMPORT_METHOD_DISABLED, method );
	}

	private void printStatus( PrintWriter out ) {
		if ( action == null ) {
			out.println( IMPORT_NO_STARTED );
		} else {
			if ( action.isAlive() ) {
				if ( action.getStatus() == CambiThread.Status.RemotePublishing ) {
					out.println( IMPORT_REMOTE_MSG + " il:" + DisplayUtil.standardDateFormatter.format( action.getRemotePublishStartTime() ) + " Bundle-id:" + action.getBundlets().get( 0 ) );
				} else if ( action.getStatus() == CambiThread.Status.Finalizing ) {
					out.println( IMPORT_FINALIZING_MSG );
				} else {
					out.println( IMPORT_STARTING_MSG + " alle ore: " + DisplayUtil.standardDateFormatter.format( action.getStartTime() ) + " da:" + action.getUsername() );
				}
			} else if ( action.getLastException() == null ) {
				out.println( IMPORT_END_MSG + " alle ore: " + DisplayUtil.standardDateFormatter.format( action.getEndTime() ) + " richiesta da:" + action.getUsername() );
				if ( action.getRemotePublishStartTime() != null && action.getRemotePublishEndTime() != null ) {
					out.println( "Remote Publisging avviato il :" + DisplayUtil.standardDateFormatter.format( action.getRemotePublishStartTime() ) + " terminato il:"
							+ DisplayUtil.standardDateFormatter.format( action.getRemotePublishEndTime() ) + " Bundle-id:" + action.getBundlets().get( 0 ) );
				}
			} else {
				out.println( IMPORT_ERROR_END_MSG + " il: " + DisplayUtil.standardDateFormatter.format( action.getEndTime() ) + " richiesta da:" + action.getUsername() );
				out.println( DisplayUtil.printErrorText( action.getLastException() ) );
			}
		}
	}

	private void startImport( String username, PrintWriter out ) throws InterruptedException {
		if ( action != null
				&& action.isAlive()
				&& ( action.getStatus().equals( CambiThread.Status.Started ) || action.getStatus().equals( CambiThread.Status.RemotePublishing ) || action.getStatus().equals(
						CambiThread.Status.Finalizing ) ) ) {
			out.println( IMPORT_RUNNING_MSG + " alle ore: " + DisplayUtil.standardDateFormatter.format( action.getStartTime() ) + " da:" + action.getUsername() );
			return;
		}
		Logger.info( CambiServlet.class, "Comincia -> startImport "  );

		action = new CambiThread( user, host, importDir, selettorePath, cambiPath, indicatoriPath, username, localHostName );
		action.setRemotePublishing( remotePublishing );
		action.setUpdateMode( updateMode );
		action.setMailer( mailer );
		action.start();

		do {
			Thread.sleep( 3000 );
		} while ( action.isAlive() && action.getStatus() == CambiThread.Status.Started );
		Logger.debug( CambiServlet.class, "Thread live:" + action.isAlive() + " status:" + action.getStatus() );
		if ( action.isAlive() && action.getStatus() == CambiThread.Status.RemotePublishing ) {
			printStatus( out );
		} else {
			printStatus( out );
		}
	}

	private void abortImport( PrintWriter out ) {
		out.println( "205 Richiesta cancellazione dell'importazione" );
		if ( action != null && action.isAlive() && !action.getStatus().equals( CambiThread.Status.Abort ) ) {
			try {
				action.abort();
			} catch ( Exception e ) {
				out.print( IMPORT_ERROR_MSG );
				out.println( DisplayUtil.printErrorText( e ) );
			}
			action = null;
		} else {
			out.print( IMPORT_NO_STARTED );
		}
	}
}
