package it.eng.bankit.app.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.dotmarketing.util.UtilMethods;

public class DisplayUtil {
	public static DateFormat standardDateFormatter = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
	public static DateFormat onlyTimeFormatter = new SimpleDateFormat( "HH:mm:ss" );
	public static DateFormat onlyDateFormatter = new SimpleDateFormat( "yyyy-MM-dd" );
	public static String printDiffTime(Date date1,Date date2){
		StringBuilder out=new StringBuilder();
		if (date1!=null&&date2!=null){
			long millisDiff=date2.getTime()-date1.getTime();
			int seconds = (int) ( millisDiff / 1000 % 60 );
			int minutes = (int) ( millisDiff / 60000 % 60 );
			int hours = (int) ( millisDiff / 3600000 % 24 );
			out.append( hours );
			out.append( ':' );
			out.append( minutes );
			out.append( ':' );
			out.append( seconds );
		}else{
			out.append( (date1==null?"DateTime parameter 1 == null ":"") );
			out.append( (date2==null?"DateTime parameter 2 == null":"") );
		}
		return out.toString();
	}
	public static String printErrorHtml( Throwable t ) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		pw.println( "<div>" );
		if (t!=null){
			t.printStackTrace( pw );
		}else{
			pw.println( "<p>Nessun dettaglio di errore disponibile</p>" );
		}
		pw.println( "</div>" );
		String stackTrace = sw.toString();
		return stackTrace.replace( System.getProperty( "line.separator" ), "<br/>\n" );
	}

	public static String printErrorText( Throwable t ) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		t.printStackTrace( pw );
		String stackTrace = sw.toString();
		return stackTrace;
	}
	
	public static String printAuditStatusDescription( String bundleid, String user, Date startTime, String esito ) {
		
		StringBuilder sb = new StringBuilder();
		sb.append( "(start:" );
		sb.append( startTime );
		if ( UtilMethods.isSet( user ) ) {
			sb.append( ", " + user );
		}
		sb.append( ", Bundle-id:" );
		sb.append( bundleid );
		sb.append( ", elapsed time: " );
		sb.append(printDiffTime(startTime,new Date(System.currentTimeMillis())));
		sb.append( ", esito: " );
		sb.append( esito );
		sb.append( ')' );
		return sb.toString();
	}
	
	public static String printOreGiorno( Date date ) {
		StringBuilder sb = new StringBuilder();
		if(date!=null){
			sb.append( "alle ore " );
			sb.append( DisplayUtil.onlyTimeFormatter.format( date ));
			sb.append( " del giorno ");
			sb.append( DisplayUtil.onlyDateFormatter.format( date ));
		}else{
			sb.append( "time null" );
		}
		return sb.toString();
	}
	
	public static String printConsulWebMailBody( String hostname, Date startTime, String esito ) {
		
		StringBuilder sb = new StringBuilder();
		sb.append( "La pubblicazione su " );
		sb.append( hostname );
		sb.append( " dei file PDF di Consulweb è iniziata " );
		sb.append( printOreGiorno(startTime));
		sb.append("\n");
		sb.append( esito );
		return sb.toString();
	}
	
	
}
