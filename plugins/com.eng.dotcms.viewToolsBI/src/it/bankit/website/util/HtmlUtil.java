package it.bankit.website.util;

import com.dotmarketing.util.UtilMethods;

public class HtmlUtil {

	public static String cleanText(String testo) {
		
		if (UtilMethods.isSet( testo ) ) {
			testo = testo.replaceAll("\"", "&rdquo;");
			return testo;
		}
		return testo;
	}

}
