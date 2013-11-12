package it.eng.dotcms.sitemap.api;

import it.eng.dotcms.sitemap.wrapper.HtmlLinkWrapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

public class SortLinkUtil {
	public static void sortHtmlLinksList(List<HtmlLinkWrapper> input, Set<HtmlLinkWrapper> output, Map<String, Integer> foldersSortedMap) {
		if(output == null)
			output = new TreeSet<HtmlLinkWrapper>();
		
		//Imposto l'ordinamento assoluto come concatenazione delle cartelle che compongono il path
		for (HtmlLinkWrapper link : input) {
			String [] foldersPath = link.getPath().split("/");
			StringBuilder absOrder = new StringBuilder();
			StringBuilder relativePath = new StringBuilder("/");
			boolean showLink = true;
			for (String folderPath : foldersPath) {
				if(folderPath != null && folderPath.length() > 0) {
					relativePath.append(folderPath+"/");
					
					Integer sortOrder = foldersSortedMap.get(relativePath.toString());
					if(sortOrder == null)
						sortOrder = foldersSortedMap.get(StringUtils.stripEnd(relativePath.toString(),"/"));
					
					if(sortOrder == null) {
						showLink = false;
						break;
					}
					
					absOrder.append(sortOrder);
					
					absOrder.append("!!");
					absOrder.append(folderPath);
					absOrder.append("::");
				}
			}
			
			if(showLink) {
				link.setAbsoluteOrder(StringUtils.stripEnd(absOrder.toString(), "::"));
				output.add(link);
			}
		}
	}
}
