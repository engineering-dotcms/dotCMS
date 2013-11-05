package com.dotcms.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpStatus;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

@Path( "/blog" )
public class BlogResource extends WebResource {
	private static final String BLOG_STRUCTURENAME = "Blog";
	private static final String COMMENT_STRUCTURENAME = "Comments";
	private static final String RELATION_NAME = "Blog-Comments";

	@POST
	@Path( "/publish" )
	// @Consumes( MediaType.APPLICATION_FORM_URLENCODED )
	// @Produces(MediaType.TEXT_PLAIN)
	public Response publish( @FormParam( "inode" ) String inode, @Context HttpServletRequest req ) {
		try {
			// TODO Get current user &&check permission to publish
			User user = APILocator.getUserAPI().getSystemUser();
			Contentlet contentlet = APILocator.getContentletAPI().find( inode, user, true );
			if ( contentlet == null ) {
				Logger.warn( BlogResource.class, "No contentlet found inode:" + inode );
			} else {
				if ( contentlet.getStructure().getVelocityVarName().equalsIgnoreCase( BLOG_STRUCTURENAME ) ) {
					publishBlog( contentlet, user );
				} else if ( contentlet.getStructure().getVelocityVarName().equalsIgnoreCase( COMMENT_STRUCTURENAME ) ) {
					publishComment( contentlet, user );
				} else {
					throw new Exception( "Struttura " + contentlet.getStructure().getName() + " non supportata" );
				}
			}
		} catch ( Exception e ) {
			Logger.error( BlogResource.class, "Errore di pubblicazione, inode:"+inode,e );
			return Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR ).build();
		}
		Logger.info( BlogResource.class,"Pubblicazione("+inode+") avvenuta con successo");
		return Response.status( HttpStatus.SC_OK ).build();
	}

	@DELETE
	@Path( "/archive" )
	// @Consumes( MediaType.TEXT_PLAIN )
	public Response archive( @FormParam( "inode" ) String inode, @Context HttpServletRequest req ) {
		try {
			// TODO Get current user &&check permission to publish
			User user = APILocator.getUserAPI().getSystemUser();
			Contentlet contentlet = APILocator.getContentletAPI().find( inode, user, true );
			if ( contentlet == null ) {
				Logger.warn( BlogResource.class, "No contentlet found inode:" + inode );
			} else {
				if ( contentlet.getStructure().getVelocityVarName().equalsIgnoreCase( BLOG_STRUCTURENAME ) ) {
					unpublishBlog( contentlet, user );
				} else if ( contentlet.getStructure().getVelocityVarName().equalsIgnoreCase( COMMENT_STRUCTURENAME ) ) {
					unpublishComment( contentlet, user );
				} else {
					throw new Exception( "Struttura " + contentlet.getStructure().getName() + " non supportata" );
				}
			}
		} catch ( Exception e ) {
			Logger.error( BlogResource.class, "Errore di archiviazione, inode:"+inode,e );
			return Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR ).build();
		}
		Logger.info( BlogResource.class,"Archiviazione("+inode+") avvenuta con successo");
		return Response.status( HttpStatus.SC_OK ).build();
	}

	private void publishBlog( Contentlet blog, User user ) throws Exception {
		APILocator.getContentletAPI().publish( blog, user, true );
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable( blog );
	}

	private void unpublishBlog( Contentlet blog, User user ) throws Exception {
		APILocator.getContentletAPI().unpublish( blog, user, true );
		APILocator.getContentletAPI().archive( blog, user, true );
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable( blog );
	}

	private void publishComment( Contentlet comment, User user ) throws Exception {
		APILocator.getContentletAPI().publish( comment, user, true );
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable( comment );
		refreshCommentsCount( comment, user );
	}

	private void unpublishComment( Contentlet comment, User user ) throws Exception {
		APILocator.getContentletAPI().unpublish( comment, user, true );
		APILocator.getContentletAPI().archive( comment, user, true );
		CacheLocator.getIdentifierCache().removeFromCacheByVersionable( comment );
		refreshCommentsCount( comment, user );
	}

	private void refreshCommentsCount( Contentlet comment, User user ) throws Exception {
		Relationship blogRel = RelationshipFactory.getRelationshipByRelationTypeValue( RELATION_NAME );
		List<Contentlet> blogs = APILocator.getContentletAPI().getRelatedContent( comment, blogRel, user, true );
		if ( !blogs.isEmpty() ) {
			Contentlet blog = null;
			for ( Contentlet curBlog : blogs ) {
				if ( curBlog.getLanguageId() == comment.getLanguageId() ) {
					blog = curBlog;
					break;
				}
			}
			if ( blog != null ) {
				long comments = APILocator.getContentletAPI().indexCount(
						"+"+RELATION_NAME+":" + blog.getIdentifier() + " +languageId:" + comment.getLanguageId() + " +deleted:false +working:true +live:true", user, true );
				blog.setLongProperty( "commentscount", comments );
				APILocator.getContentletAPI().checkin( blog, user, true );
			}
		}
	}
}