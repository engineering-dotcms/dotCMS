package com.eng.dotcms.contentlet.whatschanged;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.outerj.daisy.diff.DaisyDiff;
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

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;


@SuppressWarnings("deprecation")
public class WhatsChangedDaisyDiff {
	
	public static String diffByField(Contentlet newVersion, Contentlet oldVersion, Field field) throws TransformerConfigurationException, IOException, SAXException {		
		Writer string_writer = new StringWriter();
		StreamResult sr = new StreamResult(string_writer);
		if(field.getMap().get("fieldFieldType").equals(FieldType.WYSIWYG.toString())){			
			String newValue = String.valueOf(newVersion.getMap().get(field.getVelocityVarName()));
			String oldValue = String.valueOf(oldVersion.getMap().get(field.getVelocityVarName()));
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
		}else{			
			String newValue = String.valueOf(newVersion.getMap().get(field.getVelocityVarName()));
			String oldValue = String.valueOf(oldVersion.getMap().get(field.getVelocityVarName()));
			SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
	        TransformerHandler result = tf.newTransformerHandler();
	        Transformer serializer = result.getTransformer();
	        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
	        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF8");
	        result.setResult(sr);
			XslFilter filter = new XslFilter();
			InputStreamReader oldReader = null;
	        BufferedReader oldBuffer = null;
	        InputStreamReader newISReader = null;
	        BufferedReader newBuffer = null;		
			try{
				ContentHandler postProcess = filter.xsl(result, "org/outerj/daisy/diff/tagheader.xsl");
				postProcess.startDocument();
				postProcess.startElement("", "diffreport", "diffreport", new AttributesImpl());
				postProcess.startElement("", "diff", "diff", new AttributesImpl());
				oldReader = new InputStreamReader(new StringBufferInputStream(oldValue),"UTF-8");
				oldBuffer = new BufferedReader(oldReader);		
				newISReader = new InputStreamReader(new StringBufferInputStream(newValue),"UTF-8");
				newBuffer = new BufferedReader(newISReader);
				DaisyDiff.diffTag(oldBuffer, newBuffer, postProcess);
				postProcess.endElement("", "diff", "diff");
				postProcess.endElement("", "diffreport", "diffreport");
				postProcess.endDocument();
			} catch (Exception e) {
			    e.printStackTrace();
			} finally {
				if(null!=oldBuffer)
					oldBuffer.close();
				if(null!=newBuffer)
					newBuffer.close();
				if(null!=oldReader)
					oldReader.close();
				if(null!=newISReader)
					newISReader.close();
				if(null!=string_writer)
					string_writer.close();				
			}
		}
		StringWriter sw = (StringWriter) sr.getWriter();
		StringBuffer sb = sw.getBuffer();
		String sb_string = new String(sb.toString().getBytes("UTF-8"));
		Document doc = Jsoup.parse(sb_string);
		Element body = doc.body();
		Elements toRemove = body.select("div[class=diff-topbar]");
		Elements scriptToRemove = body.select("script");
		toRemove.remove();
		scriptToRemove.remove();
		String _result = "&nbsp;";
		if(doc.body().html()!=null && !"null".equals(doc.body().html()))
			_result = doc.body().html();
		return _result;
	}
}
