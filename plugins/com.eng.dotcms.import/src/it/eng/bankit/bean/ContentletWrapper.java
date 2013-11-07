package it.eng.bankit.bean;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.links.model.Link;
 

public class ContentletWrapper   {

	
	private Contentlet contentlet;
	private List<ContentletContainer> allegati = new ArrayList<ContentletContainer>();
	private List<String> categories = new ArrayList<String>();
	//elenco dettagli figli di D5
	private List<ContentletWrapper> dettagliD5 = new ArrayList<ContentletWrapper>();

	//elenco oggetti link associati alla contentlet
	private List<ContentletWrapper> links = new ArrayList<ContentletWrapper>();
	private String query;
	
	private Contentlet allegato;
	private Contentlet linkAllegato;	
	//Nome della relazione della contentlet con la struttura link
	private String linksRelationShipName;
	
	//Elenco delle immaginiAllegati da caricare ( es 
	private List<ContentletWrapper> immaginiAllegati = new ArrayList<ContentletWrapper>();
	
	private List<Link> menuLinks = new ArrayList<Link>();
	
	private List<ContentletWrapper> listingLinks = new ArrayList<ContentletWrapper>();
	
	private List<ContentletWrapper> linkCorrelati = new ArrayList<ContentletWrapper>();
	private boolean archived = false; 
	private boolean singleAllegato = false; 
	private boolean translated  = false; 

	//elenco dettagli figli di dettagliCluster
	private List<ContentletWrapper> dettagliCluster = new ArrayList<ContentletWrapper>();

	
	public Contentlet getContentlet() {
		return contentlet;
	}
	public void setContentlet(Contentlet contentlet) {
		this.contentlet = contentlet;
	}
	public List<ContentletContainer> getAllegati() {
		return allegati;
	}
	public void setAllegati(List<ContentletContainer> allegati) {
		this.allegati = allegati;
	}	
	
	public List<ContentletWrapper> getDettagliD5() {
		return dettagliD5;
	}
	public void setDettagliD5(List<ContentletWrapper> dettagliD5 ) {
		this.dettagliD5 = dettagliD5;
	}
	public List<ContentletWrapper> getLinks() {
		return links;
	}
	public void setLinks(List<ContentletWrapper> links) {
		this.links = links;
	}
	public void addAllegato(ContentletContainer allegato ){
		allegati.add( allegato );
	}
	public void addCategories(String category ){
		categories.add( category );
	}
	
	public List<ContentletWrapper> getDettagliCluster() {
		return dettagliCluster;
	}
	public void setDettagliCluster(List<ContentletWrapper> dettagliCluster ) {
		this.dettagliCluster = dettagliCluster;
	}
	
	public List<String> getCategories() {
		return categories;
	}
	public void addDettaglio(ContentletWrapper dettaglio ){
		dettagliD5.add( dettaglio );
	}
	
	public void addLink(ContentletWrapper link ){
		links.add( link );
	}
	public String getLinksRelationShipName() {
		return linksRelationShipName;
	}
	public void setLinksRelationShipName(String linksRelationShipName) {
		this.linksRelationShipName = linksRelationShipName;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
		
	public Contentlet getLinkAllegato() {
		return linkAllegato;
	}
	public void setLinkAllegato(Contentlet linkAllegato) {
		this.linkAllegato = linkAllegato;
	}
	public Contentlet getAllegato() {
		return allegato;
	}
	public void setAllegato(Contentlet allegato) {
		this.allegato = allegato;
	}
	
	public void addMenuLink(Link menuLink ){
		menuLinks.add( menuLink );
	}
		
	public List<Link> getMenuLinks() {
		return menuLinks;
	}
	public void setMenuLinks(List<Link> menuLinks) {
		this.menuLinks = menuLinks;
	}
	
	public void addImmagineAllegato(ContentletWrapper immagine ){
		immaginiAllegati.add( immagine );
	}
	
	public List<ContentletWrapper> getImmaginiAllegati() {
		return immaginiAllegati;
	}
	public void setImmaginiAllegati(List<ContentletWrapper> immaginiAllegati) {
		this.immaginiAllegati = immaginiAllegati;
	}
	public void addListingLink(ContentletWrapper link ){
		listingLinks.add( link );
	}
	
	public List<ContentletWrapper> getListingLinks() {
		return listingLinks;
	}
	public void setListingLinks(List<ContentletWrapper> links) {
		this.listingLinks = links;
	}
	
	public void addLinkCorrelati(ContentletWrapper correlato ){
		linkCorrelati.add( correlato );
	}
	
	public List<ContentletWrapper> getLinkCorrelati() {
		return linkCorrelati;
	}
	public void setLinkCorrelati(List<ContentletWrapper> correlati) {
		this.linkCorrelati = correlati;
	}
	
	public boolean isSingleAllegato() {
		return singleAllegato;
	}
	public void setSingleAllegato(boolean singleAllegato) {
		this.singleAllegato = singleAllegato;
	}
	public boolean isArchived() {
		return archived;
	}
	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public void addDettaglioCluster(ContentletWrapper dettaglioC ){
		dettagliCluster.add( dettaglioC );
	}
	
 	public boolean isTranslated() {
		return translated;
	}
	public void setTranslated(boolean translated) {
		this.translated = translated;
	}
	@Override
	public String toString(){
		if (contentlet!=null){
			return (String) contentlet.getStringProperty( "titolo");
		}else{
			return "empty wrapper";
		}
	}
	
	 
	
}
