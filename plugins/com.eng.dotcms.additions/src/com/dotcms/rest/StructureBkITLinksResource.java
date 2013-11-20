package com.dotcms.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

@Path("/structureLinks")
public class StructureBkITLinksResource extends WebResource {

	@GET
	@Path("/{path:.*}")
	@Produces("application/json")
	public String getStructuresWithWYSIWYGFields(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path, @QueryParam("name") String name) throws DotDataException, DotSecurityException, DotRuntimeException, PortalException, SystemException {
		Structure link = StructureFactory.getStructureByVelocityVarName("link");
		Structure linkSemplice = StructureFactory.getStructureByVelocityVarName("linkSemplice");
		StringBuilder structureDataStore = new StringBuilder();
		String EOL = System.getProperty("line.separator");
		structureDataStore.append("[");
		structureDataStore.append(EOL);
		structureDataStore.append("{id: \"");
		structureDataStore.append(link.getInode());
		structureDataStore.append("\", name: \"");
		structureDataStore.append(link.getName());
		structureDataStore.append("\"}");
		structureDataStore.append(",");
		structureDataStore.append(EOL);
		structureDataStore.append("{id: \"");
		structureDataStore.append(linkSemplice.getInode());
		structureDataStore.append("\", name: \"");
		structureDataStore.append(linkSemplice.getName());
		structureDataStore.append("\"}");
		structureDataStore.append(EOL);
		structureDataStore.append("]");
		return structureDataStore.toString();
	}
}
