package com.dotcms.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import com.dotcms.rest.WebResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.util.Logger;
import com.eng.dotcms.polls.util.PollsConstants;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * REST service for copy the votes from receiver to sender
 * 
 * This file is part of Poll Management for dotCMS.
 * Poll Management for dotCMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Poll Management for dotCMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Poll Management for dotCMS.  If not, see <http://www.gnu.org/licenses/>
 *  
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Mar 7, 2013 - 4:15:43 PM
 */
@Path("/syncPolls")
public class SyncPollsResource extends WebResource {

	private PluginAPI pAPI = APILocator.getPluginAPI();

	@POST
	@Path("/sync")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response publish(
			@FormDataParam("csvZip") InputStream csvZip,
			@FormDataParam("csvZip") FormDataContentDisposition fileDetail,
			@Context HttpServletRequest req) {
		
		try {
			String csvPath = pAPI.loadProperty(
					PollsConstants.PLUGIN_ID, 
					PollsConstants.PROP_GET_CSV_JOB_SRC_PATH);
			
			//Scompatto il file
			untar(csvZip, csvPath, fileDetail.getFileName());
			
			return Response.status(HttpStatus.SC_OK).build();
			
		} catch (DotDataException e) {
			Logger.error(SyncPollsResource.class,e.getMessage(),e);
		}
		
		return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
	}

	
	private void untar(InputStream bundle, String path, String fileName) {
        TarEntry entry;
        TarInputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            // get a stream to tar file
            InputStream gstream = new GZIPInputStream(bundle);
            inputStream = new TarInputStream(gstream);

            // For each entry in the tar, extract and save the entry to the file
            // system
            while (null != (entry = inputStream.getNextEntry())) {
                // for each entry to be extracted
                int bytesRead;

                String pathWithoutName = path;

                // if the entry is a directory, create the directory
                if (entry.isDirectory()) {
                    File fileOrDir = new File(pathWithoutName + entry.getName());
                    fileOrDir.mkdir();
                    continue;
                }

                // write to file
                byte[] buf = new byte[1024];
                outputStream = new FileOutputStream(pathWithoutName
                        + entry.getName());
                while ((bytesRead = inputStream.read(buf, 0, 1024)) > -1)
                    outputStream.write(buf, 0, bytesRead);
                try {
                    if (null != outputStream)
                        outputStream.close();
                } catch (Exception e) {
                }
            }// while

        } catch (Exception e) {
            e.printStackTrace();
        } finally { // close your streams
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
