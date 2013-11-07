package it.eng.bankit.app.util;

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

	

	public void sendMail( String subject, String body) throws DotDataException {
		Mailer mailer = new Mailer();
		mailer.setFromEmail( from );
		mailer.setFromName( fromName );
		mailer.setToEmail( deliveryAddres );
		mailer.setSubject( subject );
		mailer.setHTMLAndTextBody( body );
		mailer.sendMessage();
	}

}
