package it.bankit.website.util;

import it.bankit.website.deploy.IDeployConst;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.UtilMethods;

public class MediaXmlReader {
	protected static final DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US);
	private static String defLanguage = "it";
	public static final String TYPE_PROPERTY = "type";
	public static final String IMAGE_PROPERTY = "imageUrl";
	public static final String THUMBERNAIL_PROPERTY = "thumbernailUrl";
	public static final String VIDEO_TYPE   = "video";
	public static final String FOTO_TYPE    = "image";
	public static final String AUDIO_TYPE   = "audio";

//	private static String pluginId = "com.dotcms.viewToolsBI-0.10.1-SNAPSHOT";
	private PluginAPI pAPI = APILocator.getPluginAPI();

	public Map<String, Object> read(InputStream file) {
		return read(file, defLanguage);
	}
	public Map<String, Object> read(InputStream file, String lang) {
		Map<String, Object> contents = null;

		try {
			if (file != null) {
				Document doc = buildDocument(file);
				String type = getDocumentType(doc);
				if (type.equalsIgnoreCase(FOTO_TYPE)) {
					contents = readImage(doc, lang);
				} else if (type.equalsIgnoreCase(VIDEO_TYPE)) {
					contents = readVideo(doc, lang);
				}else if (type.equalsIgnoreCase(AUDIO_TYPE)) {
					contents = readVideo(doc, lang);
				}
				contents.put(TYPE_PROPERTY, type);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return contents;
	}
	public Map<String, Object> read(Document doc, String lang) {
		Map<String, Object> contents = null;

		try {
			String type = getDocumentType(doc);
			if (type.equalsIgnoreCase(FOTO_TYPE)) {
				contents = readImage(doc, lang);
			} else if (type.equalsIgnoreCase(VIDEO_TYPE)) {
				contents = readVideo(doc, lang);
			}
			contents.put(TYPE_PROPERTY, type);
		} catch (Exception e) {
			e.printStackTrace();			
		}
		return contents;
	}

	private Map<String, Object> readVideo(Document doc, String lang) {
		Map<String, Object> contents = new HashMap<String, Object>();
		String titolo = readXmlNode(doc, "titolo", lang);
		if (UtilMethods.isSet(titolo)) {
			contents.put("titolo", titolo);
		}

		String dataEmanazioneString = readXmlNode(doc, "data_emanazione", lang);
		if (UtilMethods.isSet(dataEmanazioneString)) {
			Date dataEmanazione = null;
			try {
				dataEmanazione = dateFormat.parse(dataEmanazioneString);
				if (dataEmanazione != null) {
					contents.put("dataEmanazione", dataEmanazione);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		String descrizione = readXmlNode(doc, "abstract", lang);
		if (UtilMethods.isSet(descrizione)) {
			contents.put("abstract", descrizione);
		}

		String luogo = readXmlNode(doc, "luogo", lang);
		if (UtilMethods.isSet(luogo)) {
			contents.put("luogo", luogo);
		}

		String autore = readXmlNode(doc, "autore", lang);
		if (UtilMethods.isSet(autore)) {
			contents.put("autori", autore);
		}

		String url_high263 = clearUrl(readXmlDomainNode(doc, "url_high263", "internet"));
		if (UtilMethods.isSet(url_high263)) {
			contents.put("urlH263", url_high263);
		}

		String url_low263 = clearUrl(readXmlDomainNode(doc, "url_low263", "internet"));
		if (UtilMethods.isSet(url_low263)) {
			contents.put("urlL263", url_low263);
		}

		String url_high264 = clearUrl(readXmlDomainNode(doc, "url_high264", "internet"));
		if (UtilMethods.isSet(url_high264)) {
			contents.put("urlH264", url_high264);
		}

		String url_low264 = clearUrl(readXmlDomainNode(doc, "url_low264", "internet"));
		if (UtilMethods.isSet(url_low264)) {
			contents.put("urlL264", url_low264);
		}
		return contents;
	}

	private Map<String, Object> readImage(Document doc, String lang) {
		Map<String, Object> contents = new HashMap<String, Object>();
		String titolo = readXmlNode(doc, "titolo", lang);
		if (UtilMethods.isSet(titolo)) {
			contents.put("titolo", titolo);
		}
		String autore = readXmlNode(doc, "autore", lang);
		if (UtilMethods.isSet(autore)) {
			contents.put("autore", autore);
		}

		String descrizione = readXmlNode(doc, "abstract", lang);
		if (UtilMethods.isSet(descrizione)) {
			contents.put("abstract", descrizione);
		}

		String imageUrl = findImageUrl(doc);
		System.out.println( "SONO NEL READ cerco imageurl " + imageUrl );
		if (UtilMethods.isSet(imageUrl)) {
			contents.put(IMAGE_PROPERTY, imageUrl);
			System.out.println( "SONO NEL READ cerco imageurl " + imageUrl );
		}

		String thumbernailUrl = findThumbernailUrl(doc);
		System.out.println( "SONO NEL READ cerco thumbernailUrl " + thumbernailUrl );
		
		if (UtilMethods.isSet(thumbernailUrl)) {
			contents.put(THUMBERNAIL_PROPERTY, thumbernailUrl);
		}

		return contents;
	}

	private String clearUrl(String url) {

		String from;
		
		try {
			from = pAPI.loadProperty(IDeployConst.PLUGIN_ID, "cutStartPath");
		} catch (DotDataException e) {
			from ="";
			e.printStackTrace();
		}

		if (url != null && url.trim().startsWith(from+File.separator)) {
			return url.substring(url.indexOf('/'));
		} else {
			return url;
		}

	}
	public Document buildDocument(InputStream file) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setValidating(false);
			dbf.setNamespaceAware(true);
			dbf.setIgnoringComments(true);
			dbf.setIgnoringElementContentWhitespace(false);
			dbf.setExpandEntityReferences(false);

			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getDocumentType(Document doc) {
		NodeList lista = doc.getElementsByTagName("asset");
		if (lista != null && lista.getLength() == 1) {
			Node asset = lista.item(0);
			Node type = asset.getAttributes().getNamedItem("type");
			return (type != null ? type.getTextContent() : "null");
		}
		return "unknow";
	}

	private String readXmlNode(Document doc, String tagName, String lang) {
		NodeList lNodes = doc.getElementsByTagName(tagName);
		String textValue = null;
		if (lNodes != null && lNodes.getLength() > 0) {
			for (int i = 0; i < lNodes.getLength(); i++) {
				Node node = lNodes.item(i);
				if (lang != null) {
					Node langNode = node.getAttributes().getNamedItem("lang");
					if (langNode != null && langNode.getNodeValue().equalsIgnoreCase(lang)) {
						String textContent = node.getTextContent().trim();
						if (!textContent.isEmpty()) {
							textValue = textContent;
						}
						break;
					}
				} else {// Get first
					String textContent = node.getTextContent().trim();
					if (!textContent.isEmpty()) {
						textValue = textContent;
					}
					break;
				}

			}
		}
		return textValue;
	}

	private String readXmlDomainNode(Document doc, String tagName, String domain) {
		NodeList lNodes = doc.getElementsByTagName(tagName);
		String textValue = null;
		if (lNodes != null && lNodes.getLength() > 0) {
			for (int i = 0; i < lNodes.getLength(); i++) {
				Node node = lNodes.item(i);
				Node langNode = node.getAttributes().getNamedItem("domain");
				if (langNode != null && langNode.getNodeValue().equalsIgnoreCase(domain)) {
					String textContent = node.getTextContent().trim();
					if (!textContent.isEmpty()) {
						textValue = textContent;
					}
					break;
				}
			}
		}
		return textValue;
	}
	private String findImageUrl(Document doc) {
		NodeList lMetadata = doc.getElementsByTagName("metadata");
		if (lMetadata != null && lMetadata.getLength() > 0) {
			Node metadata = lMetadata.item(0);
			NodeList metatati = metadata.getChildNodes();
			String nodeCandidate = null;
			String urlCandidate = null;
			for (int i = 0; i < metatati.getLength(); i++) {
				Node curMeta = metatati.item(i);
				if (curMeta.getNodeName().startsWith("url_") && curMeta.getAttributes().getNamedItem("domain").getNodeValue().equals("internet")) {
					if (curMeta.getNodeName().equalsIgnoreCase("url_big") | nodeCandidate == null) {
						nodeCandidate = curMeta.getNodeName();
						urlCandidate = curMeta.getTextContent().trim();
						if (nodeCandidate.equalsIgnoreCase("url_big"))
							break;
						else
							continue;
					}
					String candidateDimensions = nodeCandidate.substring(4);
					int candidateXindex = candidateDimensions.indexOf("x");
					int candidateH = Integer.parseInt(candidateDimensions.substring(0, candidateXindex));
					int candidateW = Integer.parseInt(candidateDimensions.substring(candidateXindex + 1));
					int candidateDefinition = candidateH * candidateW;

					String curDimensions = curMeta.getNodeName().substring(4);
					int curXindex = curDimensions.indexOf("x");
					int curH = Integer.parseInt(curDimensions.substring(0, curXindex));
					int curW = Integer.parseInt(curDimensions.substring(curXindex + 1));
					int curDefinition = curH * curW;
					if (curDefinition > candidateDefinition) {
						nodeCandidate = curMeta.getNodeName();
						urlCandidate = curMeta.getTextContent().trim();
					}

				}
			}
			return clearUrl(urlCandidate);
		}
		return null;
	}

	private String findThumbernailUrl(Document doc) {
		NodeList lMetadata = doc.getElementsByTagName("metadata");
		if (lMetadata != null && lMetadata.getLength() > 0) {
			Node metadata = lMetadata.item(0);
			NodeList metatati = metadata.getChildNodes();
			String nodeCandidate = null;
			String urlCandidate = null;
			for (int i = 0; i < metatati.getLength(); i++) {
				Node curMeta = metatati.item(i);
				if (curMeta.getNodeName().startsWith("url_") && curMeta.getAttributes().getNamedItem("domain").getNodeValue().equals("internet")) {
					if (curMeta.getNodeName().equalsIgnoreCase("url_thumb") || nodeCandidate == null) {
						nodeCandidate = curMeta.getNodeName();
						urlCandidate = curMeta.getTextContent();
						if (nodeCandidate.equalsIgnoreCase("url_thumb"))
							break;
						else
							continue;
					}
					String candidateDimensions = nodeCandidate.substring(4);
					int candidateXindex = candidateDimensions.indexOf("x");
					int candidateH = Integer.parseInt(candidateDimensions.substring(0, candidateXindex));
					int candidateW = Integer.parseInt(candidateDimensions.substring(candidateXindex + 1));
					int candidateDefinition = candidateH * candidateW;

					String curDimensions = curMeta.getNodeName().substring(4);
					int curXindex = curDimensions.indexOf("x");
					int curH = Integer.parseInt(curDimensions.substring(0, curXindex));
					int curW = Integer.parseInt(curDimensions.substring(curXindex + 1));
					int curDefinition = curH * curW;
					if (curDefinition < candidateDefinition) {
						nodeCandidate = curMeta.getNodeName();
						urlCandidate = curMeta.getTextContent().trim();
					}

				}
			}
			return clearUrl(urlCandidate);
		}
		return null;
	}
}
