package com.eng.dotcms.test.publishing.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.outerj.daisy.diff.HtmlCleaner;
import org.outerj.daisy.diff.XslFilter;
import org.outerj.daisy.diff.html.HTMLDiffer;
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput;
import org.outerj.daisy.diff.html.TextNodeComparator;
import org.outerj.daisy.diff.html.dom.DomTreeBuilder;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Logger;

public class Utils {
	
	private static String pluginId = "com.eng.dotcms.test";
	private static PluginAPI pluginAPI = APILocator.getPluginAPI();
	private static SimpleDateFormat SDF = new SimpleDateFormat("ddMMyyyy_hhmmss");

	/**
	 * Questo metodo restituisce il path in cui verranno salvati i files relativi all'esecuzione corrente.
	 * 
	 * Viene creata anche la cartella css con il file diff.css al suo interno.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 23, 2013 - 3:35:56 PM
	 */
	public static File getCurrentExecutionRootPathWithCSS() throws DotDataException, IOException {
		File rootDir = getRootPath();
		String dir = SDF.format(new GregorianCalendar().getTime());
		File currentDir = new File(rootDir, dir);
		// add the diff.css under the path
		File cssFolder = new File(currentDir, "css");		
		if(!cssFolder.exists())
			cssFolder.mkdirs();
		
		// copy the file
		copyCSSDiffFile(new File(cssFolder,"diff.css"));
		return currentDir;
	}
	
	/**
	 * Data la query Lucene eseguita per recuperare i contenuti da pubblicare, questo metodo restituisce la lista dei percorsi pubblicati.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 23, 2013 - 3:36:52 PM
	 */
	public static List<String> getQueryPaths() throws DotDataException{
		String query = pluginAPI.loadProperty(pluginId, "remotepublishing.query");
		// la query deve essere del tipo "parentPath" per funzionare...
		if(!query.startsWith("+parentPath") || query.indexOf("+parentPath")<0)
			return null;
		else {
			List<String> paths = new ArrayList<String>();
			// vediamo se è stato inserito piu di un parentPath oppure no
			if(query.indexOf("+parentPath:(")>0){ //multiplo
				String[] _temp = query.split("[(]");
				String[] _paths = _temp[1].replace(")","").split("[,]");
				for(int i=0; i<_paths.length; i++)
					paths.add(_paths[i].replace("*", ""));				
			}else{ //singola
				String[] _temp = query.split("[:]");
				paths.add(_temp[1].replace("*", ""));
			}
			return paths;
		}
	}
	
	/**
	 * Restituisce una URL well formed relativa al sender.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 23, 2013 - 3:37:40 PM
	 */
	public static String getSenderURL(String appendPath) throws DotDataException{
		StringBuffer sb = new StringBuffer();
		sb.append("http://");
		sb.append(pluginAPI.loadProperty(pluginId, "remotepublishing.sender.address"));
		sb.append(":");
		sb.append(pluginAPI.loadProperty(pluginId, "remotepublishing.sender.port"));
		if(null!=appendPath){
			if(!appendPath.startsWith("/"))
				sb.append("/");
			sb.append(appendPath);
		}
		return sb.toString();
	}

	/**
	 * Restituisce una URL well formed relativa al receiver.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 23, 2013 - 3:37:57 PM
	 */
	public static String getReceiverURL(String appendPath) throws DotDataException{
		StringBuffer sb = new StringBuffer();
		sb.append("http://");
		sb.append(pluginAPI.loadProperty(pluginId, "remotepublishing.receiver.address"));
		sb.append(":");
		sb.append(pluginAPI.loadProperty(pluginId, "remotepublishing.receiver.port"));
		if(null!=appendPath){
			if(!appendPath.startsWith("/"))
				sb.append("/");
			sb.append(appendPath);
		}
		return sb.toString();
	}
	
	/**
	 * Metodo che salva un file html
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 23, 2013 - 3:38:12 PM
	 */
	public static String storeFile(File parent, String fileName, Document doc) throws IOException {		
		File singleFile = new File(parent, fileName);
		if(!singleFile.exists()){
			Logger.info(Utils.class, "************* creo il file "+fileName+" al path "+parent.getAbsolutePath());
			singleFile.createNewFile();
		}
		FileWriter fw = new FileWriter(singleFile.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		Logger.info(Utils.class, "************* numero di lettere nel file "+doc.html().length());
		bw.write(doc.html());
		bw.close();	
		return singleFile.getAbsolutePath();
	}
	
	/**
	 * Metodo che effettua le differenze tra due file dati in input e salva il file ottenuto attraverso DaisyDiff nella medesima folder.
	 * 
	 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
	 *
	 * May 23, 2013 - 3:38:24 PM
	 */
	@SuppressWarnings("deprecation")
	public static void storeDifference(File parent, String file1, String file2, String uuid) throws IOException, TransformerConfigurationException, SAXException {
		Writer string_writer = new StringWriter();
		StreamResult sr = new StreamResult(string_writer);
		FileInputStream fisFile1 = new FileInputStream(new File(file1));
		FileInputStream fisFile2 = new FileInputStream(new File(file2));
		String newValue = IOUtils.toString(fisFile1, "UTF-8");
		String oldValue = IOUtils.toString(fisFile2, "UTF-8");
		SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
        TransformerHandler result = tf.newTransformerHandler();
        Transformer serializer = result.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        result.setResult(sr);
		XslFilter filter = new XslFilter();
		ContentHandler postProcess = filter.xsl(result, "org/outerj/daisy/diff/htmlheader.xsl");

        Locale locale = Locale.getDefault();
        String prefix = "diff";

        HtmlCleaner cleaner = new HtmlCleaner();

        InputSource oldSource = new InputSource(new StringBufferInputStream(oldValue));
        InputSource newSource = new InputSource(new StringBufferInputStream(newValue));

        DomTreeBuilder oldHandler = new DomTreeBuilder();
        cleaner.cleanAndParse(oldSource, oldHandler);

        TextNodeComparator leftComparator = new TextNodeComparator(oldHandler, locale);

        DomTreeBuilder newHandler = new DomTreeBuilder();
        cleaner.cleanAndParse(newSource, newHandler);

        TextNodeComparator rightComparator = new TextNodeComparator(
                newHandler, locale);

        postProcess.startDocument();
        postProcess.startElement("", "diffreport", "diffreport",
                new AttributesImpl());
        
        postProcess.startElement("", "diff", "diff",
                new AttributesImpl());
        HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess,
                prefix);
        
        HTMLDiffer differ = new HTMLDiffer(output);
        differ.diff(leftComparator, rightComparator);
        postProcess.endElement("", "diff", "diff");
        postProcess.endElement("", "diffreport", "diffreport");
        postProcess.endDocument();
        StringWriter sw = (StringWriter) sr.getWriter();
		StringBuffer sb = sw.getBuffer();
		String sb_string = new String(sb.toString().getBytes("UTF-8"));
		Document doc = Jsoup.parse(sb_string);
		Element body = doc.body();
		Elements toRemove = body.select("div[class=diff-topbar]");
		Elements scriptToRemove = body.select("script");
		toRemove.remove();
		scriptToRemove.remove();		
		storeFile(parent, "diff_"+uuid+".html", doc);
	}
	
	public static String getCleanPath(String absoluteUrl) throws MalformedURLException {
		return new URL(absoluteUrl).getPath();
	}
	
	private static File getRootPath() throws DotDataException{
		File rootPath = new File(pluginAPI.loadProperty(pluginId, "remotepublishing.root"));
		if(!rootPath.exists())
			rootPath.mkdirs();
		return rootPath;
	}
	
	private static void copyCSSDiffFile(File dest) throws IOException {
		if(!dest.exists())
			dest.createNewFile();
		InputStream in = Utils.class.getClassLoader().getResourceAsStream("com/eng/dotcms/test/publishing/util/diff.css");
		OutputStream out = new FileOutputStream(dest);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0){
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}
	
	
}
