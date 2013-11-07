package it.eng.bankit.filereader;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class HWXmlReader extends AbstractHWFileReader  {

	private  DocumentBuilder db ;
	
	public HWXmlReader(File file) {
		super(file);
		initBuilder();
	}

	@Override
	public String getContentAsString( ) {
		String  contents = null;

		try{
			if( db != null && this.getFile() != null){
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(this.getFile());
				doc.getDocumentElement().normalize();	 
				NodeList lista   = doc.getElementsByTagName("body");
				if( lista != null && lista.getLength() >0 )
				{
					contents = lista.item(0).getTextContent();
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return contents;
	}

	private void initBuilder(){
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setValidating(false);
			dbf.setNamespaceAware(true);
			dbf.setIgnoringComments(false);
			dbf.setIgnoringElementContentWhitespace(false);
			dbf.setExpandEntityReferences(false);

			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

}
