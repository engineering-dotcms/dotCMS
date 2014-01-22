package com.eng.dotcms.additions.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.eng.dotcms.additions.jaxb.Album;
import com.eng.dotcms.additions.jaxb.Gallery;
import com.eng.dotcms.additions.jaxb.Img;
import com.eng.dotcms.additions.util.SortUtil;

/**
 * Servlet che sostituisce la pagina gallery.html adibita alla generazione di un XML da dare in pasto al player flash delle photogallery
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 */
public class GalleryServlet extends HttpServlet {

	private static final long serialVersionUID = 6195910556425895303L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String folderId = req.getParameter("id");
		Folder folder = null;
		resp.setContentType("text/xml");
		PrintWriter out = resp.getWriter();
		SortUtil sortUtil = new SortUtil();
		
		if(UtilMethods.isSet(folderId)){
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(Gallery.class);
				Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
				jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				
				folder = APILocator.getFolderAPI().find(folderId, APILocator.getUserAPI().getSystemUser(), true);
				String _languageId = (String)req.getSession().getAttribute(WebKeys.HTMLPAGE_LANGUAGE);
				if(!UtilMethods.isSet(_languageId))
					_languageId = (String)req.getAttribute(WebKeys.HTMLPAGE_LANGUAGE);
				
				List<Contentlet> list = APILocator.getContentletAPI().search(getQuery(folderId, _languageId).toString(), 1, -1, 
						"modDate desc", APILocator.getUserAPI().getSystemUser(), true);
				if(list.size()>0){
					Contentlet contentlet = list.get(0);
					Gallery gallery = new Gallery();
					Album album = new Album();
					album.setId("ssp");
					album.setTitle(contentlet.getStringProperty("titolo"));
					album.setDescription(contentlet.getStringProperty("descrizioneEstesa"));
					album.setTn("/contentAsset/raw-data/"+contentlet.getInode()+"/thumbnail");
					List<Img> imgs = new ArrayList<Img>();
					String sortOrder = UtilMethods.isSet(contentlet.getStringProperty("orderType"))?sortUtil.generateLuceneSortOrder(contentlet.getStringProperty("orderType"), "Foto"):"modDate desc";
					
					List<Contentlet> galleryPhotos = APILocator.getContentletAPI().search(getQueryPhotos(folder, _languageId).toString(),100,-1,
							sortOrder, APILocator.getUserAPI().getSystemUser(), true);
					
					for(Contentlet photo:galleryPhotos){
						Img img = new Img();
						String _photoFolder = photo.getFolder();
						Folder actualFolder = APILocator.getFolderAPI().find(_photoFolder, APILocator.getUserAPI().getSystemUser(), true);
						String bPath = actualFolder.getPath();
						bPath=bPath+photo.getStringProperty("fileName");
						if(UtilMethods.isSet(photo.get("thumbnail"))){
							File photoThumbnail = (File)photo.get("thumbnail");
							if(UtilMethods.isSet(photoThumbnail)){
								img.setSrc(bPath);
								img.setTn("/contentAsset/raw-data/"+photo.getInode()+"/thumbnail?w=150&amp;h=150");
								img.setTitle(photo.getTitle());
								img.setCaption("");
								img.setPause("3");
							}
						}else{
							FileAsset fileAsset = (FileAsset)photo.get("fileAsset");							
							img.setSrc(bPath);
							img.setTn("/contentAsset/image-thumbnail/"+fileAsset.getInode()+"/fileAsset?w=150&amp;h=150");
							img.setTitle(photo.getTitle());
							img.setCaption("");
							img.setPause("3");
						}
						imgs.add(img);
					}
					album.setImg(imgs);
					gallery.setAlbum(album);
					jaxbMarshaller.marshal(gallery, out);
				}
				
			} catch (DotHibernateException e) {
				printEmpty(out);
			} catch (DotRuntimeException e) {
				printEmpty(out);
			} catch (DotSecurityException e) {
				printEmpty(out);
			} catch (DotDataException e) {
				printEmpty(out);
			} catch (JAXBException e) {
				Logger.fatal(getClass(), e.getMessage());
			} catch (Exception e) {
				
			}
		}
			
	}

	private StringBuilder getQuery(String folderId, String _languageId) {
		StringBuilder sb = new StringBuilder();
		sb.append("+structureName:Fotogallery");
		sb.append(" +confolder:");
		sb.append(folderId);
		sb.append(" +languageId:");
		sb.append(_languageId);
		return sb;
	}
	
	private StringBuilder getQueryPhotos(Folder folder, String _languageId) {
		StringBuilder sb = new StringBuilder();
		sb.append("+structureName:Foto");
		sb.append(" +path:");
		sb.append(folder.getPath());
		sb.append("*");
		sb.append(" +languageId:");
		sb.append(_languageId);
		return sb;
	}
	
	private void printEmpty(PrintWriter out){
		Gallery gallery = new Gallery();				
		gallery.setAlbum(new Album());		
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Gallery.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(gallery, out);
		} catch (JAXBException e) {			
			Logger.fatal(getClass(), "Errore", e);
		}
		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {		
		doGet(req, resp);
	}
	
	
}
