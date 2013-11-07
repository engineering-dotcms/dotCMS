package it.eng.bankit.app.util;

import it.eng.bankit.app.ImportException;
import it.eng.bankit.servlet.CambiServlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.util.Logger;

public class CambiUtil {
	public static String filenamePrefix="cambi_rif_";
	public static String INDICI_FILENAME_IT="ind_cambi.htm";
	public static String INDICI_FILENAME_EN="ind_cambi_en.htm";
	public static String RIFERIMENTI_FILENAME_IT="cambi_rif.htm";
	public static String RIFERIMENTI_FILENAME_EN="cambi_rif_en.htm";
	public static long IMPORT_TIME_TOLLERANCE=60000;//1 minuto

	public static String generateCambiPath(Date date){
		Calendar calendar=Calendar.getInstance();
		if(date!=null){
			calendar.setTime( date );
		}
		int mese = calendar.get( Calendar.MONTH ) + 1;
		
		String meseString = String.valueOf( mese);
		if ( mese < 10 ) {
			meseString = "0" + mese;
		}
		return  calendar.get( Calendar.YEAR ) + "/" + meseString ;
	}
	
	public static String generateCambiOriginalFilename(Date date,String language){
		StringBuilder sb=internalGenerateCambiFile(date,language);
		sb.append( ".htm" );
		return sb.toString();
	}
	public static String generateCambiFilename(Date date,String language){
		StringBuilder sb=internalGenerateCambiFile(date,language);
		sb.append( '.');
		sb.append( com.dotmarketing.util.Config.getStringProperty( "VELOCITY_PAGE_EXTENSION" ) );
		return sb.toString();
	}
	
	private static StringBuilder internalGenerateCambiFile(Date date,String language){
		StringBuilder sb=new StringBuilder();
		sb.append( filenamePrefix );
		sb.append( generateCambiGiornoMese(date) );
		if(language.equalsIgnoreCase( "en" )){
			sb.append( "_en" );
		}
		return sb;
	}
	
	public static String generateCambiGiornoMese(Date date){
		Calendar calendar=Calendar.getInstance();
		if(date!=null){
			calendar.setTime( date );
		}
		int mese = calendar.get( Calendar.MONTH ) + 1;
		
		String meseString = String.valueOf( mese);
		if ( mese < 10 ) {
			meseString = "0" + mese;
		}
		int giorno = calendar.get( Calendar.DAY_OF_MONTH );
		String giornoString = String.valueOf(giorno);
		if ( giorno < 10 ) {
			giornoString = "0" + giorno;
		}
		
		StringBuilder sb=new StringBuilder();
		sb.append( giornoString );
		sb.append( meseString );
		return sb.toString();
	}
	
	public static String checkFiles(String username,File fileIndIta,File fileIndEng,File fileSelIta,File fileSelEng,File fileCambiIta,File fileCambiEng) throws ImportException{
		long importTimeCheck=0;
		Logger.info( CambiServlet.class, "Verifico i file dei cambi "  );

		Map<String,CheckFileBean> mapCheck=new HashMap<String,CheckFileBean>();
		Map<String,Date> sameTimeMap=new HashMap<String,Date>();
		Map<String,Date> diffTimeMap=new HashMap<String,Date>();
		Map<String,String> sameOwnerMap=new HashMap<String,String>();
		Map<String,String> diffOwnerMap=new HashMap<String,String>();
		List<String> missingFiles=new ArrayList<String>();
		StringBuilder description=new StringBuilder();
		
		String cambiFilenameIt=CambiUtil.generateCambiOriginalFilename(new Date(),"it");
		String cambiFilenameEn=CambiUtil.generateCambiOriginalFilename(new Date(),"en");
		
		mapCheck.put( INDICI_FILENAME_IT, checkFile(fileIndIta) );
		mapCheck.put( INDICI_FILENAME_EN, checkFile(fileIndEng) );
		mapCheck.put( RIFERIMENTI_FILENAME_IT, checkFile(fileSelIta) );
		mapCheck.put( RIFERIMENTI_FILENAME_EN, checkFile(fileSelEng) );
		mapCheck.put( cambiFilenameIt, checkFile(fileCambiIta) );
		mapCheck.put( cambiFilenameEn, checkFile(fileCambiEng) );
		
		for(String fileName:mapCheck.keySet()){
			CheckFileBean check=mapCheck.get( fileName );
			if(!check.exist){
				missingFiles.add( fileName );
			}else{
				if(importTimeCheck==0){
					importTimeCheck=check.getTimeModified().getTime();
					sameTimeMap.put( fileName, check.getTimeModified() );
				}else {
					if(Math.abs(check.getTimeModified().getTime()-importTimeCheck)<IMPORT_TIME_TOLLERANCE){
						sameTimeMap.put( fileName, check.getTimeModified() );
					}else{
						diffTimeMap.put( fileName, check.getTimeModified() );
					}
					if(check.getOwner().equals( username )){
						sameOwnerMap.put(fileName,check.getOwner());
					}else{
						diffOwnerMap.put(fileName,check.getOwner());
					}
				}
			}
		}
		
		if(!missingFiles.isEmpty()){
			if(missingFiles.size()==1){
				throw new ImportException("File "+missingFiles.get( 0 )+" mancante");
			}else{
				StringBuilder sb=new StringBuilder();
				sb.append( "Files " );
				for(String fileName:missingFiles){
					if(sb.length()>6){
						sb.append( ", " );
					}
					sb.append( fileName );
				}
				sb.append(" mancanti");
				throw new ImportException(sb.toString());
			}
		}
		if (!diffTimeMap.isEmpty()&&!diffOwnerMap.isEmpty()){
			description.append( "Files htm generati da utenti diversi ed in momenti diversi" );
			Logger.warn( CambiUtil.class,description.toString());
		}
		if(!diffTimeMap.isEmpty()){
			for(String curFile:diffTimeMap.keySet()){
				String curWarning="\nFile "+curFile+" data importazione differente("+diffTimeMap.get( curFile )+")";
				description.append(curWarning);
				Logger.warn( CambiUtil.class,curWarning) ;
			}
		}
		if(!diffOwnerMap.isEmpty()){
			for(String curFile:diffOwnerMap.keySet()){
				String curWarning="\nFile "+curFile+" owner differente("+diffOwnerMap.get( curFile )+")";
				description.append(curWarning);
				Logger.warn( CambiUtil.class, curWarning );
			}
		}
		return description.toString();
	}
	
	private static CheckFileBean checkFile(File file)throws ImportException{
		boolean exist=false;
		String owner=null;
		Date timeModified=null;
		
		if (file!=null&&file.exists()&&file.canRead()){
			exist=true;
			owner=FileReaderUtil.readFileOwner(file);
			timeModified=new Date(file.lastModified());	
		}
		return new CheckFileBean(exist,owner,timeModified);
	}
	
	private static class CheckFileBean{
		private boolean exist;
		private String owner;
		private Date timeModified;
		
		public CheckFileBean(boolean exist,String owner,Date timeModified){
			this.exist=exist;
			this.owner=owner;
			this.timeModified=timeModified;
		}
		public boolean exist(){
			return exist;
		}
		public String getOwner() {
			return owner;
		}
		public Date getTimeModified() {
			return timeModified;
		}
	}
}
