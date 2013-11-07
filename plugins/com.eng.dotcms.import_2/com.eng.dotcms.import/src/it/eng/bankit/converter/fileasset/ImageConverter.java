package it.eng.bankit.converter.fileasset;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.ContentletWrapper;
import it.eng.bankit.bean.HmiStructure;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

public class ImageConverter extends AllegatoConverter {

	public ImageConverter(HmiStructure struttura) {
		super(struttura);
	}

	@Override
	public ContentletContainer parseContent() throws Exception {
		ContentletContainer c = new ContentletContainer();
		String correctLang = getHmiStructure().getLanguageFile(); 
		Contentlet content = super.creaAllegato( correctLang );
		ContentletWrapper wrapper = new ContentletWrapper();
		wrapper.setContentlet(content);
		c.add(wrapper);
		return c;
	}


	public ContentletWrapper getContentlet( String lang ) throws Exception {
		Contentlet content = super.creaAllegato( lang );
		ContentletWrapper wrapper = new ContentletWrapper();
		wrapper.setContentlet(content);
		return wrapper;
	}
}
