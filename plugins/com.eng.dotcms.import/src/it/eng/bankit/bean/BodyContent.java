	package it.eng.bankit.bean;

import it.eng.bankit.filereader.HWHtmlReader;
import it.eng.bankit.util.FileUtil;
import it.eng.bankit.util.HyperwaveKey;

import java.io.File;
import java.util.Map;

public class BodyContent {
	private String language;
	private String mimeType;
	private String content;

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public static BodyContent read(HmiStructure childStruct) throws Exception {
		BodyContent bodyContent = new BodyContent();
		File fileCorrente = childStruct.getFile();
		Map<String, String> properties = childStruct.getPropertiesHmi();
		bodyContent.setLanguage(properties.get(HyperwaveKey.HW_Language));
		String mimeType = properties.get(HyperwaveKey.MimeType);
		bodyContent.setMimeType(mimeType);
		if (mimeType.equals("text/plain")) {
			bodyContent.setContent(FileUtil.readFile(fileCorrente));
		} else if (mimeType.equals("text/html")) {
			bodyContent.setContent(HWHtmlReader
					.getContentAsString(fileCorrente));
		} else {
			throw new Exception("MymeType:" + mimeType + " non supportato");
		}
		return bodyContent;
	}

}
