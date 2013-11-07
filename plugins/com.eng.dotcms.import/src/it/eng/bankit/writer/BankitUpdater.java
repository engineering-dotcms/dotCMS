package it.eng.bankit.writer;

import it.eng.bankit.util.HyperwaveKey;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class BankitUpdater implements ItemWriter<Map<String, String>>, InitializingBean {
	protected static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat( "yyyy/MM/dd hh:mm:ss" );
	public static long updated=0;
	public static long missed=0;
	private ContentletAPI contentletAPI = null;
	private FolderAPI folderAPI = null;
	private User user = null;
	private Host host = null;
	private Map<String,Language> languages=new HashMap<String,Language>(2);
	private boolean initialized = false;

	@Override
	public void write( List<? extends Map<String, String>> listaContlets ) {
		if ( !initialized ) {
			init();
		}

		for ( Map<String, String> curContent : listaContlets ) {
			Folder curFolder = null;
			Date timeModified=null;
			try {
				if ( curContent.get( "path" ) != null ) {
					curFolder = folderAPI.findFolderByPath( curContent.get( "path" ), host, user, true );
					String timeModifiedProp=curContent.get( HyperwaveKey.TimeModified );
					if (UtilMethods.isSet(timeModifiedProp)){
						timeModified=dateTimeFormat.parse( timeModifiedProp );
					}
					Map<String, String> title = readMultilanguageProperty( HyperwaveKey.Title, curContent );
					if ( curFolder != null && InodeUtils.isSet( curFolder.getInode())  &&timeModified!=null && title!=null && !title.isEmpty()) {
						String goId = curContent.get( HyperwaveKey.GOid );
						StringBuilder query = new StringBuilder();
						query.append( " +conFolder:" );
						query.append( curFolder.getInode() );
						query.append( " +title:" );
						if(title.size()==1){
							query.append( title.values().iterator().next() );
						}else{
							query.append( "(" );
							query.append( title.values().iterator().next() );
							query.append( " OR " );
							query.append( title.values().iterator().next() );
							query.append( ")" );
						}
						
						query.append( " +working:true +live:true +deleted:false" );
						List<Contentlet> contentlets = contentletAPI.checkout( query.toString(), user, false, -1, -1 );
						if (!contentlets.isEmpty()){
							for(Contentlet curContentlet:contentlets){
								updateContentlet( curContentlet,timeModified );
							}
						/*Contentlet curContentlet=filterContentlets(contentlets,curContent);
						if (curContentlet!=null){
							updateContentlet( curContentlet,timeModified );*/
						}else {
							Logger.warn( this.getClass(), "No contentlet (" + goId + ") found in folder:" + curContent.get( "path" ) );
							missed++;
						}

					} else {
						Logger.warn( this.getClass(), "No folder:" + curContent.get( "path" ) + " found" );
						missed++;
					}
				} else {
					Logger.warn( this.getClass(), "Null path for" + curContent.toString() );
				}
			} catch ( Exception e ) {
				Logger.error( this.getClass(), "ERRORE update asset" + curContent.get( "fileName" ), e );
			}
		}
	}
	
	private Contentlet filterContentlets(List<Contentlet> contentlets,Map<String,String> properties){
		Contentlet contentlet=null;
		if (contentlets!=null && !contentlets.isEmpty()){
			contentlet=filterByGoId(contentlets,properties.get( HyperwaveKey.GOid ));
			if (contentlet==null){
				String format=properties.get( HyperwaveKey.Formato );
				if (format.equalsIgnoreCase( "D0" )){//Try File asset
					
				}
				
			}
		}
		return contentlet;
	}
	
	private Contentlet filterByGoId(List<Contentlet> contentlets,String goId){
		List<Contentlet> candidates=new ArrayList<Contentlet>(1);
		for(Contentlet curContentlet:contentlets){
			String contentGoId=(String) curContentlet.get( "hwgoid" );
			if (contentGoId!=null&&contentGoId.equalsIgnoreCase( goId )){
				candidates.add( curContentlet );
			}
		}
		if(candidates!=null&&!candidates.isEmpty()){
			if (candidates.size()==1){
				return candidates.get( 0 );
			}else{
				Logger.warn( getClass(), "Duplicate goId:"+goId+" found" );
				return null;
			}
		}else{
			return null;
		}
	}

	private boolean updateContentlet( Contentlet contentlet, Date date ) throws Exception {
		Field timeModifiedField=contentlet.getStructure().getFieldVar( "timeModified" );
		Field dataPubblicazioneField=contentlet.getStructure().getFieldVar( "dataPubblicazione" );
		if (timeModifiedField!=null && dataPubblicazioneField!=null){
			Date originalTimeModified = contentlet.getDateProperty( "timeModified" );
			Date originalPublicationTime = contentlet.getDateProperty( "dataPubblicazione" );
			if ( originalTimeModified == null || originalTimeModified.compareTo( date ) != 0 || originalPublicationTime == null || originalPublicationTime.compareTo( date ) != 0 ) {
				contentlet.setDateProperty( "timeModified", date );
				contentlet.setDateProperty( "dataPubblicazione", date );
				Contentlet contentletUpdated = contentletAPI.checkin( contentlet, user, false );
				contentletAPI.unlock( contentletUpdated, user, false );
				contentletAPI.publish( contentletUpdated, user, false );
				updated++;
				Logger.info( this.getClass(), "Update contentlet " + contentletUpdated.getInode() + " OK" );
				return true;
			} else {
				Logger.info( this.getClass(), "Update contentlet not needed " + contentlet.getInode() );
			}
		}else{
			Logger.warn( this.getClass(), "Impossibile aggiornare la contentlet("+contentlet.getInode()+") struttura "+contentlet.getStructure().getName() );
		}
		return false;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}
	
	protected Map<String, String> readMultilanguageProperty( String propertyName, Map<String, String> origin ) {
		Map<String, String> rValue = new HashMap<String, String>( 2 );
		for ( String curProperty : origin.keySet() ) {
			if ( curProperty.startsWith( propertyName ) ) {
				String[] languageProperty = curProperty.split( ":" );
				rValue.put( languageProperty[1], origin.get( curProperty ) );
			}
		}
		return rValue;
	}

	private void init() {
		updated=0;
		missed=0;
		contentletAPI = APILocator.getContentletAPI();
		folderAPI = APILocator.getFolderAPI();
		try {
			user = APILocator.getUserAPI().getSystemUser();
			host = APILocator.getHostAPI().findDefaultHost( user, false );
			languages.put( "en", APILocator.getLanguageAPI().getLanguage( "en", "US" ));
			languages.put( "it", APILocator.getLanguageAPI().getLanguage( "it", "IT" ));
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		initialized = true;
	}

}
