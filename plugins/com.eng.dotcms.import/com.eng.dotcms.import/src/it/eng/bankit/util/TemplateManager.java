package it.eng.bankit.util;

import it.eng.bankit.bean.HmiStructure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;


public class TemplateManager {


	private static TemplateManager factory;
	private static String template_prefix = "TEMPLATE_";

	private	List<Template> listaTempls = null;
	private Map<String, Template> mappaTemplate = new HashMap<String, Template>();


	public static TemplateManager getInstance(){
		if( factory == null ){
			factory = new TemplateManager();
		}
		return factory;
	}

	private TemplateManager(){
		try {
			listaTempls =  APILocator.getTemplateAPI().findTemplatesAssignedTo( ImportUtil.getHost() );
		} catch (DotDataException e) {
			e.printStackTrace();
		}
	}

	public   Template findTemplate( HmiStructure struct ) throws Exception {
		String templateName = getTemplate( struct );
		Template template = getCachedTemplate(templateName);

		boolean foundT = false ;
		Iterator<Template> iter = listaTempls.iterator();
		if( UtilMethods.isSet( templateName )  && template == null ){

			while( iter.hasNext() && !foundT ) {
				Template tmpl = iter.next();
				if( tmpl.getFriendlyName()!= null && ( tmpl.getFriendlyName().equalsIgnoreCase( ImportConfig.getProperty( templateName )) 
						|| tmpl.getTitle().equalsIgnoreCase(ImportConfig.getProperty( templateName ) )) ){
					template = tmpl;
					mappaTemplate.put(templateName, tmpl );
					foundT = true;
				}
			}
		}
		return template;
	}


	private Template getCachedTemplate(String templateName) {
		Template  t = null;
		if( UtilMethods.isSet( templateName )){
			t = mappaTemplate.get(templateName);
		}
		return t;
	}

	public   String getTemplate(  HmiStructure structure ){
		String formato =  structure.getPropertyHmi( HyperwaveKey.Formato );
		String templateName = template_prefix+ "DETTAGLIO";
	 	
		if(structure.hasDocumentiCorrelati() ){
			HmiStructure hmiCorrelato =  structure.getDocumentoCorrelato();
			if( hmiCorrelato != null ){
				String posizione = hmiCorrelato.getPropertyHmi( HyperwaveKey.Posizione );
				if( UtilMethods.isSet(posizione) && ( posizione.equalsIgnoreCase("alto") || posizione.equalsIgnoreCase("sopra") )){
					templateName = template_prefix+"CORRELATI_ALTO";
				}else {
					templateName =template_prefix+"CORRELATI_BASSO";
				}
			}else {
				templateName =template_prefix+ "CORRELATI_ALTO";
			}
		} 
		else if( formato!= null && formato.equalsIgnoreCase("D5") ){
			templateName = template_prefix+"D5";
		}
		else if( formato!= null && formato.equalsIgnoreCase("HP")  ){
			templateName = template_prefix+"HOMEPAGE";
		}else if( formato!= null && ( formato.equalsIgnoreCase("L1") 
				|| formato.equalsIgnoreCase("L2")
				|| formato.equalsIgnoreCase("L3")
				|| formato.equalsIgnoreCase("L4")
				|| formato.equalsIgnoreCase("L5")
				|| formato.equalsIgnoreCase("L7")
				|| formato.equalsIgnoreCase("L8")
				|| formato.equalsIgnoreCase("L9") ) ){
			templateName = template_prefix+ formato ;
		}else {
			if(formato!= null &&  formato.equalsIgnoreCase("DG") ){
				if( structure.getFilePath().indexOf("footer")!= -1 ){
					templateName = template_prefix+formato;
				}
			}else if( structure.getFilePath().indexOf("RePEc")!= -1 ){
				templateName = template_prefix+"REPEC";
			}
			else if(  structure.isFullCollectionHead() ){
				templateName = template_prefix+ "FULL_HEAD";
			}else if( structure.isContainerCluster() ){
				templateName = template_prefix+"DETTALIO_CLUSTER";
			}			
			else if( formato == null ){
				templateName = 	getTemplateFormParent(structure.getParentStructure() , "" );
				String docType = structure.getPropertyHmi( HyperwaveKey.DocumentType ); 
				String bodyType = structure.getPropertyHmi( HyperwaveKey.BodyType ); 
				
				if(  docType != null && docType.equalsIgnoreCase("text") &&  
						bodyType != null && bodyType.equalsIgnoreCase("Body") ){
					templateName = template_prefix+"SIMPLE_HTML";
				}
				if( !UtilMethods.isSet(templateName ) ){
					templateName = template_prefix+"DETTAGLIO";
				}
			}
		}
		return templateName ;
	}


	private String getTemplateFormParent( HmiStructure parent , String templateName   ){	 
		
		if( parent != null ){
			String formatoP =  parent.getPropertyHmi( HyperwaveKey.Formato );

			if( formatoP!= null && ( formatoP.equalsIgnoreCase("L1")  
					|| formatoP.equalsIgnoreCase("L2")
					|| formatoP.equalsIgnoreCase("L3")
					|| formatoP.equalsIgnoreCase("L4")
					|| formatoP.equalsIgnoreCase("L5") 
					|| formatoP.equalsIgnoreCase("L7") 
					 ))
			{
				templateName = "TEMPLATE_"+formatoP ;
				
			}
		}if(parent!= null &&  parent.getParentStructure() != null && templateName.equalsIgnoreCase("") ){
			templateName =	getTemplateFormParent( parent.getParentStructure()  , templateName );
		}
		return templateName;
	}
}
