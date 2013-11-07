package it.eng.bankit.converter;

import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.detail.BoxMenuConverter;
import it.eng.bankit.converter.detail.CambiConverter;
import it.eng.bankit.converter.detail.D2Converter;
import it.eng.bankit.converter.detail.D5Converter;
import it.eng.bankit.converter.detail.DGConverter;
import it.eng.bankit.converter.detail.DXConverter;
import it.eng.bankit.converter.detail.FooterConverter;
import it.eng.bankit.converter.fileasset.RePEcConverter;
import it.eng.bankit.converter.index.CollectionClusterConverter;
import it.eng.bankit.converter.index.DocumentCollectionConverter;
import it.eng.bankit.converter.index.IMenuConverter;
import it.eng.bankit.converter.listing.L0Converter;
import it.eng.bankit.converter.listing.LinksCorrelatiConverter;
import it.eng.bankit.converter.listing.Listing1Converter;
import it.eng.bankit.converter.listing.Listing3Converter;
import it.eng.bankit.converter.listing.Listing5Converter;
import it.eng.bankit.converter.listing.Listing7Converter;
import it.eng.bankit.converter.listing.Listing9Converter;
import it.eng.bankit.converter.media.FotoConverter;
import it.eng.bankit.converter.media.FotoGalleryConverter;
import it.eng.bankit.converter.media.VideoConverter;
import it.eng.bankit.converter.media.VideoGalleryConverter;
import it.eng.bankit.converter.media.VideoStreamingConverter;
import it.eng.bankit.converter.news.BoxSidebarConverter;
import it.eng.bankit.converter.news.IndicatoreConverter;
import it.eng.bankit.converter.news.InfoConverter;
import it.eng.bankit.converter.news.NotiziaSecConverter;
import it.eng.bankit.converter.news.PInternaConverter;
import it.eng.bankit.converter.news.SelettoreConverter;
import it.eng.bankit.converter.news.StrilloConverter;

import com.dotmarketing.util.UtilMethods;


public class ConverterFactory {


	private static ConverterFactory factory;

	public static ConverterFactory getInstance(){
		if( factory == null ){
			factory = new ConverterFactory();
		}
		return factory;
	}

	public FolderConverter getFolderConverter(String formatoDettaglio , HmiStructure structure ){

		String formatoCorretto = checkNullFormat( formatoDettaglio , structure );

		if( UtilMethods.isSet( formatoCorretto ) )
		{

			if( formatoCorretto.equalsIgnoreCase("D1") ){
				return new D2Converter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase("PI") ){
				return new PInternaConverter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase("D0") ){
				return new D2Converter( structure );
			}	
			if( formatoCorretto.equalsIgnoreCase("D2") ){
				return new D2Converter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase("DG") ){
				return new DGConverter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase("D3") ){
				return new D2Converter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase("D4") ){
				return new D2Converter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase("D5") ){
				return new D5Converter( structure);
			} 
			if( formatoCorretto.equalsIgnoreCase("D6") ){
				return new D5Converter( structure );
			}			 

			if(formatoCorretto.equalsIgnoreCase("I1") || 
					formatoCorretto.equalsIgnoreCase("I2") || 
					formatoCorretto.equalsIgnoreCase("I3")|| 
					formatoCorretto.equalsIgnoreCase("I4")|| 
					formatoCorretto.equalsIgnoreCase("I5") ){
				return new IMenuConverter(structure);
			}

			/* AGGIUNTE NEV INIZIO */


			if(FotoGalleryConverter.accept(structure)){
				return new FotoGalleryConverter(structure);
			}
			if(FotoConverter.accept(structure)){
				return new FotoConverter(structure);
			}
			if(VideoGalleryConverter.accept(structure)){
				return new VideoGalleryConverter(structure);
			}
			if(VideoConverter.accept(structure)){
				return new VideoConverter(structure);
			}
			if(VideoStreamingConverter.accept(structure)){
				return new VideoStreamingConverter(structure);
			}

			if( formatoCorretto.equalsIgnoreCase("NP") ){
				return new StrilloConverter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase("NS") ){
				return new NotiziaSecConverter( structure );
			}

			if( formatoCorretto.equalsIgnoreCase("News") ){
				return  new SelettoreConverter( structure ); 
			}

			if( formatoCorretto.equalsIgnoreCase("Ind") ){
				return  new IndicatoreConverter( structure ); 
			}

			if( formatoCorretto.equalsIgnoreCase("BOX_HP") ){ //BOX HOMEPAGE
				return new BoxSidebarConverter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase("BOX_INFO_HP") ){ //BOX HOMEPAGE - PROX APP - COMUN 
				return new InfoConverter( structure );
			}		 
			if(formatoCorretto.equalsIgnoreCase("L1") || 
					formatoCorretto.equalsIgnoreCase("L2") ){
				return new Listing1Converter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("L3") ||
					formatoCorretto.equalsIgnoreCase("L4")  || formatoCorretto.equalsIgnoreCase("L8")    ){
				return new Listing3Converter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("L7")   ){
				return new Listing7Converter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("L5")   ){
				return new Listing5Converter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("Cambi")   ){
				return new CambiConverter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("L0") ){ 
				return new D2Converter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("Notizie") ){
				return new L0Converter(structure);
			}
			if( formatoCorretto.endsWith( "_BoxMenu")){
				return new BoxMenuConverter( structure );
			}
			if(formatoCorretto.equalsIgnoreCase("L9")  ){
				return new Listing9Converter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("links")  ){
				return new LinksCorrelatiConverter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("DX") ){				 
				return new DXConverter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("testotit") ){
				return new IMenuConverter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("ClusterCollection") ){
				return new CollectionClusterConverter(structure);
			}
			if(formatoCorretto.equalsIgnoreCase("DocumentCollection") ){
				return new DocumentCollectionConverter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase( "Footer")){
				return new FooterConverter( structure );
			}
			if( formatoCorretto.equalsIgnoreCase( "RePEc")){
				return new RePEcConverter( structure );
			}
		} 
		return null;

	}

	private String checkNullFormat(String formatoDettaglio,  HmiStructure structure) {
		String formato = formatoDettaglio;
		if( !UtilMethods.isSet(formatoDettaglio ) ){			
			String filePath =  structure.getFilePath();

			if( structure.getFile().isDirectory() ) { 
				String filename =structure.getFilePath();
				
				if(filename.indexOf("banca_centrale/cambi/rif") !=-1 ){
					formato = "Cambi";
				}else if( filename.equalsIgnoreCase("footer") ){
					formato = "Footer";
				}else if( filename.equalsIgnoreCase("RePEc") ){
					formato = "RePEc";
				}
				else if(  filePath.indexOf("homepage") != -1 ){
						formato = "News";
					}
					else if( filename.endsWith("links")){
						formato = "links";
					}
					else if(  structure.isContainerCluster())	{
						formato = "ClusterCollection" ;
					} else if  (  structure.isCollectionDocumentType()	){
						formato = "DocumentCollection" ;
					} else if(filename.indexOf("footer") !=-1 ){
						formato = "Footer";
					}
					else {
						formato = "DX";
					}
				}			 
		}
		return formato;
	}
}
