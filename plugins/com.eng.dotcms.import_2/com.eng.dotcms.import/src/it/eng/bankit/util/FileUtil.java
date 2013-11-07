package it.eng.bankit.util;

import it.eng.bankit.converter.GenericMultilanguageConverter;

import java.io.BufferedInputStream;
import java.io.CharConversionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.Date;

import org.apache.commons.lang.ArrayUtils;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.liferay.portal.model.User;

public class FileUtil {
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	public static boolean isFileDescriptor( File file, String extension ) {
		boolean isDescriptor = false;
		String ext = getFileExtension( file );
		if ( ext != null ) {
			if ( ext.equals( extension ) ) {
				isDescriptor = true;
			}
		}
		return isDescriptor;
	}

	public static String getFileExtension( File file ) {
		String ext = null;
		if ( file.isFile() ) {
			String fileName = file.getName();
			int index = fileName.lastIndexOf( "." );
			if ( index != -1 ) {
				ext = fileName.substring( index + 1 );
			}
		}
		return ext;
	}

	public static String getFileNoExtension( File file ) {
		String nameNoExt = null;
		if ( file.isFile() ) {
			String fileName = file.getName();
			int index = fileName.lastIndexOf( "." );
			if ( index != -1 ) {
				nameNoExt = fileName.substring( 0, index );
			}
		}
		return nameNoExt;
	}

	public static boolean createDirectory( String basePath, String dirPath, boolean create ) {
		if ( create ) {
			return new File( basePath + dirPath ).mkdir();
		}
		return false;

	}

	public static boolean isTextFile( File fileCorrente ) {
		String ext = getFileExtension( fileCorrente );
		String[] textExt = ImportConfig.getProperties( "TEXT_EXTENSION" );
		ext = ext.toLowerCase();
		boolean isText = false;
		if ( ArrayUtils.contains( textExt, ext ) ) {
			isText = true;
		}
		return isText;
	}

	public static boolean isImageFile( File fileCorrente ) {
		String ext = getFileExtension( fileCorrente );
		String[] imgExt = ImportConfig.getProperties( "IMAGE_EXTENSION" );
		ext = ext.toLowerCase();
		boolean isImage = false;
		if ( ArrayUtils.contains( imgExt, ext ) ) {
			isImage = true;
		}
		return isImage;
	}

	public static boolean isAttachFile( File fileCorrente ) {
		String ext = getFileExtension( fileCorrente );
		String[] textExt = ImportConfig.getProperties( "ATTACH_EXTENSION" );
		ext = ext.toLowerCase();
		boolean isText = false;
		if ( ArrayUtils.contains( textExt, ext ) ) {
			isText = true;
		}
		return isText;
	}

	public static String readFile( File file ) throws IOException {

		BufferedInputStream bufferedStream = new BufferedInputStream( new FileInputStream( file ), DEFAULT_BUFFER_SIZE );
		CharsetDetector detector = new CharsetDetector();
		detector.setText( bufferedStream );
		CharsetMatch match = detector.detect();
		if ( match != null ) {
			Logger.debug( FileUtil.class, "Detected " + match.getName() + " with confidence of " + match.getConfidence() + "%" );
			Reader encoderReader = match.getReader();
			CharBuffer charBuffer = CharBuffer.allocate( DEFAULT_BUFFER_SIZE );
			StringWriter stringWriter = new StringWriter();
			int read = 0;
			do {
				read = encoderReader.read( charBuffer );
				if ( read > 0 ) {
					charBuffer.flip();
					stringWriter.write( charBuffer.toString() );
					charBuffer.clear();
				}
			} while ( read > 0 );

			return stringWriter.toString();
		} else {
			throw new CharConversionException( "Unknow charset for file:" + file.getAbsolutePath() );
		}
	}

	public static File createTempFile( File file ) throws Exception {
		File tempDir;
		try {// Copy file in temp directory preventing after checkin deletion
			//*see com.dotmarketing.servlets.AjaxFileUploadServlet
			tempDir = new File(APILocator.getFileAPI().getRealAssetPath()+ File.separator + "tmp_"+ ImportUtil.getUser().getFirstName() );
			boolean created = false;
			boolean exists = tempDir.exists();
			if(!exists){
				created = tempDir.mkdir();
			}
			 
			if ( exists || created ) {
				File tempFile = new File( tempDir.getPath(), ImportUtil.encodePathToURL( System.currentTimeMillis()+file.getName() ) );
				com.liferay.util.FileUtil.copyFile( file, tempFile , false );
				return tempFile;
			}
		} catch ( IOException e ) {
			Logger.error( FileUtil.class, e.getMessage(), e );
		}
		return null;
	}

	public static com.dotmarketing.portlets.files.model.File convertAndSaveFile( File file, Folder folder ) throws Exception {
		com.dotmarketing.portlets.files.model.File dotFile = new com.dotmarketing.portlets.files.model.File();
		Date now = new Date();
		User user = ImportUtil.getUser();
		
		String fileName = FileUtil.getFileNoExtension( file );
		dotFile.setTitle( fileName );
		dotFile.setFriendlyName( fileName );
		dotFile.setParent( folder.getInode() );
		dotFile.setModDate( now );
		dotFile.setPublishDate( now );
		dotFile.setFileName( file.getName() );
		String mimeType = APILocator.getFileAPI().getMimeType( file.getPath() );
		dotFile.setMimeType( mimeType );
		dotFile.setType( "file_asset" );
		dotFile.setModUser( user.getUserId() );
		com.dotmarketing.portlets.files.model.File workingFile = null;
		File temp = FileUtil.createTempFile( file );
		if ( APILocator.getFileAPI().fileNameExists( folder, file.getName() ) ) {
			Logger.error(FileUtil.class, " Esiste un file nel folder " + folder.getName() + " con il nome " + file.getName() );
		} else {
			workingFile = APILocator.getFileAPI().saveFile( dotFile, temp, folder, user, false );
			APILocator.getVersionableAPI().setWorking( workingFile );
			Logger.info(FileUtil.class, " File " + file.getName() + " salvato nel folder " + folder.getName() );
		}
		return dotFile;
	}

}
