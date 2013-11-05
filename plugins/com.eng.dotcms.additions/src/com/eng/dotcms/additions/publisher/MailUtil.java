package com.eng.dotcms.additions.publisher;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Mailer;

public class MailUtil {
	private String from;
	private String fromName;
	private String deliveryAddres;

	public MailUtil(String from, String fromName, String deliveryAddres) {
		this.from=from;
		this.fromName=fromName;
		this.deliveryAddres=deliveryAddres;
	}

	

	public void sendMail(List<String> contentlets) throws DotDataException {
		Mailer mailer = new Mailer();
		mailer.setFromEmail( from );
		mailer.setFromName( fromName );
		mailer.setToEmail( deliveryAddres );
		StringBuffer sb = new StringBuffer();
		sb.append("[SITIDOT Alert Contenuti Depubblicati alla data ");
		sb.append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new GregorianCalendar().getTime()));
		sb.append("]");
		mailer.setSubject(sb.toString());
		StringBuffer html = new StringBuffer();
		html.append("<p>Di seguito la lista dei contenuti non visibili alla data in oggetto</p>");
		html.append("<br />");
		html.append("<ul>");
		for(String c:contentlets){
			html.append("<li>");
			html.append(c);
			html.append("</li>");
		}
		html.append("<br />");
		html.append("<br />");
		html.append("Cordialmente");
		mailer.setHTMLAndTextBody(html.toString());
		mailer.sendMessage();
	}
	
	
	
	
}
