package it.bankit.website.util;

import java.util.StringTokenizer;

public class StringUtil {

	private static String delimiter = "/";

	public static boolean checkFolderPathIsInURI(String uri, String folderPath, int level) {

		String subUri = delimiter;
		String subPath = delimiter;

		StringTokenizer st2 = new StringTokenizer(folderPath, "/");
		int tot = st2.countTokens();
		int count = 1;

		while ((count < tot) && st2.hasMoreElements()) {
			String token = (String) st2.nextElement();
			subPath = subPath.concat(token + delimiter);
			count += 1;
		}

		StringTokenizer st = new StringTokenizer(uri.substring(0, (uri.lastIndexOf(delimiter)) + 1), "/");

		int countToken = 1;

		while (countToken < tot && st.hasMoreElements()) {
			String token = (String) st.nextElement();
			subUri = subUri.concat(token + delimiter);
			countToken += 1;
		}

		subUri.concat(delimiter);

		if (subUri.equals(subPath)) {
			return true;
		} else
			return false;
	}


}
