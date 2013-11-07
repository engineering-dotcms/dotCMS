package it.eng.bankit.processor;

import it.eng.bankit.bean.ContentletContainer;
import it.eng.bankit.bean.HmiStructure;
import it.eng.bankit.converter.ConverterFactory;
import it.eng.bankit.converter.FolderConverter;
import it.eng.bankit.converter.detail.GenericDetailConverter;
import it.eng.bankit.filereader.HmiFileReader;
import it.eng.bankit.util.HyperwaveKey;
import it.eng.bankit.writer.DotcmsSysDataWriter;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

public class HwItemProcessor implements ItemProcessor<HmiStructure, ContentletContainer>  {

	private static Logger LOG = Logger.getLogger( HwItemProcessor.class );
		@Autowired
	private DotcmsSysDataWriter sysWriter;	

	public ContentletContainer process(HmiStructure struct) throws Exception {
		ContentletContainer cons = null;
		try{
			File hmi = struct.getHmiFile();	
			Map<String, String> properties = HmiFileReader.processFile(hmi);
			String formato = properties.get(HyperwaveKey.Formato );
			String filePath =  struct.getFilePath();
			System.out.println("---------    ---------------"  );
			System.out.println("--------- INIT PROCESS ---------------"  );
			System.out.println("Formato -->  " +formato + " File : " + struct.getFilePath() );
			FolderConverter conv =  null;
			if(formato != null ) {
				LOG.debug("FILE " + struct.getFilePath()  + " FORMATO  " + formato );
				if( formato.startsWith("I3") || formato.startsWith("I2") 
						|| formato.startsWith("I1")  || formato.startsWith("I4") || formato.startsWith("I5") ){
					conv = 	ConverterFactory.getInstance().getFolderConverter( formato , struct );
				} else if( formato.equalsIgnoreCase( "PI")   ){
					//createFolderOnHost(parentPath, struct);
					conv = 	ConverterFactory.getInstance().getFolderConverter( formato , struct );
				} 
				else if( formato.equalsIgnoreCase( "NP")   ||  formato.equalsIgnoreCase( "NS")  ){
					//createFolderOnHost(parentPath, struct);
					conv = 	ConverterFactory.getInstance().getFolderConverter( formato , struct );
				}
				else if( formato.equalsIgnoreCase( "Ind")  ){
					conv = 	ConverterFactory.getInstance().getFolderConverter( formato , struct );
				}

				else if( formato.equalsIgnoreCase( "News")  ){
					conv = 	ConverterFactory.getInstance().getFolderConverter( formato , struct );
				}
				else if( formato.equalsIgnoreCase( "Box") ){
					String formatoBox =  "";
					if( filePath.indexOf("homepage") != -1 ){
						formatoBox = formato+"_HP" ;
						if( ( filePath.indexOf("calendario_prossime_pubblicazioni") != -1 ) ){
							formatoBox = formato+"_INFO_HP" ;
						}
						else if( ( filePath.indexOf("prossimi_appuntamenti") != -1 )  )	{
							formatoBox = formato+"_INFO_HP" ;
						}

					}else {
						formatoBox = formato+"_BoxMenu" ;
					}
					conv = 	ConverterFactory.getInstance().getFolderConverter( formatoBox , struct );
				

				}else if( formato.equalsIgnoreCase( "DV") || formato.equalsIgnoreCase( "LV")  ){
					//Formato VIDEO gallery
					conv = ConverterFactory.getInstance().getFolderConverter( formato , struct );
				 
				}
				else if( formato.equalsIgnoreCase( "DF") || formato.equalsIgnoreCase( "LF") || formato.equalsIgnoreCase( "LF2") || formato.equalsIgnoreCase( "IF") ){
					conv = ConverterFactory.getInstance().getFolderConverter( formato , struct );
				 
				}
				else if( formato.startsWith("D") ){
					conv = ConverterFactory.getInstance().getFolderConverter( formato , struct );
					if( conv ==  null ){
						if( formato.equalsIgnoreCase("D0")){
							boolean isDir = struct.getFile().isDirectory();
							if( isDir ){
								conv =(GenericDetailConverter)	ConverterFactory.getInstance().getFolderConverter( "D2" , struct );
 							}
							 
						}
					}
 				} else if ( formato.startsWith("L") ){
					if( formato.startsWith("L1") || formato.startsWith("L2") || formato.startsWith("L3")  
							|| formato.startsWith("L4") || formato.startsWith("L5") 	|| formato.startsWith("L7") ||  formato.startsWith("L9")){
						conv = ConverterFactory.getInstance().getFolderConverter( formato , struct );
 					}

				}
				else {					
					System.out.println( "FORMATO NON GESTITO !!! "  + formato  + " NOME FILE " + struct.getFilePath()  );
				}
			}else {
				conv = ConverterFactory.getInstance().getFolderConverter( formato , struct );

			}
			if( conv != null ){
				System.out.println(" FILE "+ struct.getFilePath() + " (Formato = " +formato   +") : ");
				System.out.println( "SubDirectories " +  struct.getSubDirectories() ) ;
				System.out.println( "SubFile " +  struct.getSubFile() ) ;
				System.out.println( "ChildrenLinks " +  struct.getChildrenLinks() ) ;		
				System.out.println( " CONVERTO i DATI  " ) ;				
 				cons =  conv.parseContent();		 
			 
				System.out.println("--------- PASSO AL WRITER::::::::::::::.   ---------------"  );
			}else {					
				System.err.println( "getFolderConverter NON TROVATE FILE " + struct.getFilePath()  + " NON PROCESSATO "); 
				//System.exit(-1);
			}
			return cons;
			//		return null;
		}catch (Exception e) {
			e.printStackTrace();
			LOG.error( e );
			throw e;
		}
	}

	public DotcmsSysDataWriter getSysWriter() {
		return sysWriter;
	}

	public void setSysWriter(DotcmsSysDataWriter sysWriter) {
		this.sysWriter = sysWriter;
	}



}

