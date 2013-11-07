package it.eng.bankit.servlet;

import it.eng.bankit.bean.HmiStructure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

public class ImportServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	protected static User user;
	protected static FolderAPI fApi;
	private static HostAPI hostAPI;
	private String fileConfig ;
	private Properties props ;

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		fileConfig = config.getInitParameter("config_file");
		if( fileConfig != null ){
			//	props =  Properties properties = new Properties();
			try {
				props.load(new FileInputStream( fileConfig ));
			} catch (IOException e) {
				System.err.println( "ERRORE init servlet ");
			}


		}
	}







	private void createFolder( List<HmiStructure> listaCartelle, Host host , File parent) throws Exception 	{

	}

	private void createFolder( File[ ] listaFile , Host host , File parent) throws Exception 	{
		for( File f : listaFile  ){
			String folderName = "";
			if( parent != null ){
				folderName =  parent.getPath() +"/";
			}
			folderName += f.getName();
			folderName = folderName.replace("/home/roscigno/progetti/BANCADITALIA/workspace_git/EXPORT_IMPORT/new", "");
			if( f.isDirectory() ){
				System.out.println("Folder to create : "  + folderName );

				//				Folder fold = fApi.findFolderByPath( folderName, host, user, true);
				//				if( fold != null ){
				//					fApi.delete( fold,   user, true) ;
				//				}
				Folder 	fold = fApi.createFolders( folderName, host, user, true);
				File[] children  =  f.listFiles();
				createFolder(children , host ,f  );
				System.out.println("Folder  : "  + fold.getName()  );
			}
		}
	}
}
