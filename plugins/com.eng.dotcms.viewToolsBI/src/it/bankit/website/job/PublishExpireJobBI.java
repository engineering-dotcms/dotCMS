/**
 * 
 */
package it.bankit.website.job;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.liferay.portal.model.User;

/**
 * This job seaches all content types for those with fields sysPublishDate and
 * sysExpireDate. If we have those, we will automatically publish/ unpublish the
 * content based on those dates. One caveat - the content to be
 * published/unpublished cannot be in a "Drafted" state, meaning, it you have a
 * published piece of content and have made changes to it without republihsing,
 * then the published version will not get unpublished,.
 * 
 * 
 */
public class PublishExpireJobBI implements Job {

	private final static String publishDateField = "sysPublishDate";
	private final static String expireDateField = "sysExpireDate";
	private final static String emailTitlePublishProperty = "publish.expiry.job.email.pubTitle";
	private final static String emailSubjectPublishProperty = "publish.expiry.job.email.pub.Subject";
	private final static String emailFromProperty = "publish.expiry.job.email.from";
	private final static String emailFromNameProperty = "publish.expiry.job.email.from.name";
	private final static String emailToProperty ="publish.expiry.job.email.to";
	private final static String emailTitleExpireProperty = "publish.expiry.job.email.expTitle";
	private final static String emailSubjectExpireProperty = "publish.expiry.job.email.exp.Subject";
	private static SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMddHHmmss", Locale.US );

	private static String pluginId = "com.dotcms.viewToolsBI-0.10.1-SNAPSHOT";

	private Map<String, List<Contentlet>> pubContent = new HashMap<String, List<Contentlet>>();
	private Map<String, List<Contentlet>> expContent = new HashMap<String, List<Contentlet>>();

	// Cache
	private static boolean cahceInitialized = false;
	private static final List<Structure> publishFieldCache = new Vector<Structure>();
	private static final List<Structure> expireFieldCache = new Vector<Structure>();
	private static PluginAPI pluginAPI = APILocator.getPluginAPI();
	private static ContentletAPI contentAPI = APILocator.getContentletAPI();
	private static User pubUser = null;
	private static User expireUSer = null;

	public void execute( JobExecutionContext ctx ) throws JobExecutionException {

		checkCache();

		Logger.info( PublishExpireJobBI.class, "--------------- Running Publish/Expiry Job -------------------" );
		Date now =  new Date() ;

		/*
		 * do publish first, where publish is in the past and expire is in the
		 * future and live = false
		 */
		for ( Structure s : publishFieldCache ) {
	
			String luceneQuery=getPublishContentsLuceneQuery(s.getVelocityVarName(),now);
			
			try {
				Logger.debug( PublishExpireJobBI.class, "Query contenuti da pubblicare struttura " + s.getVelocityVarName() + ": " + luceneQuery);
				List<Contentlet> contentsToPublish = contentAPI.search( luceneQuery, 0, -1, null, pubUser, false );

				if ( contentsToPublish.size() > 0 ) {
					List<Contentlet> listPublished=new ArrayList<Contentlet>(contentsToPublish.size());
					pubContent.put( s.getVelocityVarName(), listPublished );
					for(Contentlet curContent:contentsToPublish){
						try {
							contentAPI.publish( curContent, pubUser, true );
							listPublished.add( curContent );
						} catch ( DotRuntimeException e ) {
							Logger.error( PublishExpireJobBI.class,"Errore nella pubblicazione automatica del contenuto:"+curContent.getTitle(),e);
						} catch ( DotDataException e ) {
							Logger.error( PublishExpireJobBI.class,"Errore nella pubblicazione automatica del contenuto:"+curContent.getTitle(),e);
						} 
					}
					try {//Send Email
						notifyPubblications( pubContent );
					} catch ( DotDataException e1 ) {
						Logger.error( PublishExpireJobBI.class, "Error sending mail for publishing content", e1 );
					}
				}
			} catch ( Exception e ) {
				Logger.error( this.getClass(), "Errore irreversibile nella pubblicazione automatica dei contenuti", e );
				throw new JobExecutionException( e );
			}
		}

		/*
		 * do expire second, where expire is in the past and live = true
		 */
		for ( Structure s : expireFieldCache ) {
			
			String luceneQuery=getExpiredContentsLuceneQuery(s.getVelocityVarName(),now);
			
			try {
				Logger.debug( PublishExpireJobBI.class, "Query contenuti da spubblicare struttura " + s.getVelocityVarName() + ": " + luceneQuery );
				List<Contentlet> contentsToExpire = contentAPI.search( luceneQuery, 0, -1, null, expireUSer, false );

				if ( contentsToExpire.size() > 0 ) {
					List<Contentlet> listExpired=new ArrayList<Contentlet>(contentsToExpire.size());
					expContent.put( s.getVelocityVarName(), listExpired );
					for(Contentlet curContent:contentsToExpire){
						try {
							contentAPI.unpublish( curContent, pubUser, true );
							listExpired.add(curContent );
						} catch ( DotRuntimeException e ) {
							Logger.error( PublishExpireJobBI.class,"Errore nella spubblicazione automatica del contenuto:"+curContent.getTitle(),e);
						} catch ( DotDataException e ) {
							Logger.error( PublishExpireJobBI.class,"Errore nella spubblicazione automatica del contenuto:"+curContent.getTitle(),e);
						} 
					}
					try {//Send Email
						notifyExpirations( expContent );
					} catch ( DotDataException e1 ) {
						Logger.error( PublishExpireJobBI.class, "Error sending mail for spublishing content", e1 );
					}
				}
			} catch ( Exception e ) {
				Logger.error( this.getClass(), e.getMessage(), e );
				throw new JobExecutionException( e );
			}
		}

		Logger.info( PublishExpireJobBI.class, "--------------- Finished Publish/Expiry Job -------------------" );
	}

	private String getExpiredContentsLuceneQuery( String structureName, Date date ) {
		StringWriter luceneQuery = new StringWriter();
		luceneQuery.append( " +structureName:" );
		luceneQuery.append( structureName );
		luceneQuery.append( " +" );
		luceneQuery.append( structureName );
		luceneQuery.append( '.' );
		luceneQuery.append( expireDateField );
		luceneQuery.append( ":[19990101010000 to " );
		luceneQuery.append( sdf.format( date ) );
		luceneQuery.append( ']' );
		luceneQuery.append( " +live:true " );
		luceneQuery.append( " +working:true " );
		luceneQuery.append( " +deleted:false " );
		return luceneQuery.toString();
	}
	
	private String getPublishContentsLuceneQuery( String structureName, Date date ) {
		StringWriter luceneQuery = new StringWriter();
		luceneQuery.append( " +structureName:" );
		luceneQuery.append( structureName );
		luceneQuery.append( " +" );
		luceneQuery.append( structureName );
		luceneQuery.append( '.' );
		luceneQuery.append( publishDateField );
		luceneQuery.append( ":[19990101010000 to " );
		luceneQuery.append( sdf.format( date ) );
		luceneQuery.append( ']' );
		luceneQuery.append( " +" );
		luceneQuery.append( structureName );
		luceneQuery.append( '.' );
		luceneQuery.append( expireDateField );
		luceneQuery.append( ":[" );
		luceneQuery.append( sdf.format( date ) );
		luceneQuery.append( " to 20990101010000]" );
		luceneQuery.append( " +live:false " );
		luceneQuery.append( " +working:true " );
		luceneQuery.append( " +deleted:false " );
		return luceneQuery.toString();
	}

	private void notifyPubblications(Map<String, List<Contentlet>> contents) throws DotDataException{
		String title=pluginAPI.loadProperty( pluginId, emailTitlePublishProperty );
		String subject=pluginAPI.loadProperty( pluginId, emailSubjectPublishProperty );
		String from= pluginAPI.loadProperty( pluginId, emailFromProperty);
		String fromName= pluginAPI.loadProperty( pluginId, emailFromNameProperty);
		String deliveryAddres= pluginAPI.loadProperty( pluginId, emailToProperty );
		String body=generatePublishEmailBody(contents,title);
		sendMail(title,subject,body,from,fromName,deliveryAddres);
	}
	private void notifyExpirations(Map<String, List<Contentlet>> contents) throws DotDataException{
		String title=pluginAPI.loadProperty( pluginId, emailTitleExpireProperty );
		String subject=pluginAPI.loadProperty( pluginId, emailSubjectExpireProperty );
		String from= pluginAPI.loadProperty( pluginId, emailFromProperty);
		String fromName= pluginAPI.loadProperty( pluginId, emailFromNameProperty);
		String deliveryAddres= pluginAPI.loadProperty( pluginId, emailToProperty );
		String body=generatePublishEmailBody(contents,title);
		sendMail(title,subject,body,from,fromName,deliveryAddres);
	}
	private void sendMail( String title,String subject,String body,String from,String fromName,String deliveryAddres) throws DotDataException {
		
		Mailer mail = new Mailer();
		mail.setFromEmail(from);
		mail.setFromName(fromName);
		mail.setToEmail(deliveryAddres);
		mail.setSubject( subject );
		mail.setHTMLAndTextBody( body );
		Logger.debug( PublishExpireJobBI.class, body );
		mail.sendMessage();
	}
	
	private String generatePublishEmailBody(Map<String, List<Contentlet>> content,String title) throws DotDataException{

		
		String emailBody = "<h1><strong>" + title + "</strong></h1><br/><br/>";

		Set<String> keySet = content.keySet();
		for ( String key : keySet ) {

			Structure s = StructureCache.getStructureByVelocityVarName( key );
			emailBody.concat( "<strong>" + key.toString() + "</strong><br/>" );

			List<Contentlet> conts = content.get( key );

			if ( conts.size() > 0 ) {
				emailBody = emailBody.concat( "<ul>" );

				for ( Contentlet contentlet : conts ) {

					emailBody = emailBody.concat( "<li>" );
					if ( StructureCache.getStructureByInode( s.getInode() ).getFieldVar( "path" ) != null ) {
						emailBody = emailBody.concat( contentlet.getTitle() + " - Folder: "
								+ StructureCache.getStructureByInode( s.getInode() ).getFieldVar( "path" ).getFieldContentlet() );
					} else {
						emailBody = emailBody.concat( contentlet.getTitle() + " - Inode: " + contentlet.getInode() );
					}
					emailBody = emailBody.concat( "</li>" );
				}
				emailBody = emailBody.concat( "</ul>" );
			}
		}
		return emailBody;
	}
	
	private static synchronized void checkCache() throws JobExecutionException {
		if ( !cahceInitialized ) {
			try {
				pubUser = APILocator.getUserAPI().getSystemUser();
				expireUSer = APILocator.getUserAPI().getSystemUser();
			} catch ( DotDataException e ) {
				Logger.error( PublishExpireJobBI.class, "Error init cache", e );
				throw new JobExecutionException( e );
			}
			
			publishFieldCache.addAll( getStructWithField( publishDateField ) );
			expireFieldCache.addAll( getStructWithField( expireDateField ) );
			cahceInitialized = true;
		}
	}

	private static List<Structure> getStructWithField( String filedVelocityVarName ) {
		List<Structure> structs = StructureFactory.getStructures();
		List<Structure> ret = new ArrayList<Structure>();

		for ( Structure s : structs ) {
			List<Field> fields = FieldsCache.getFieldsByStructureInode( s.getInode() );
			for ( Field f : fields ) {
				if ( filedVelocityVarName.equals( f.getVelocityVarName() ) && f.isIndexed() ) {
					ret.add( s );
				} else if ( filedVelocityVarName.equals( f.getVelocityVarName() ) & !f.isIndexed() ) {
					Logger.warn( PublishExpireJobBI.class, "Found " + filedVelocityVarName + " field on " + s.getName()
							+ "  but it is not indexed.  This won't work" );
				}
			}
		}
		return ret;
	}
}