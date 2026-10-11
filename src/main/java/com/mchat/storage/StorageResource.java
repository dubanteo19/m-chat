package com.mchat.storage;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.eclipse.microprofile.jwt.Claim;
import org.jboss.logging.Logger;

@Path("/storage")
@RequestScoped
public class StorageResource {

  private static final Logger LOG = Logger.getLogger(StorageResource.class);

  @Inject
  @Claim("userId")
  Long currentUserId;

  @Inject StorageService storageService;

  @GET
  @Path("/presigned-url")
  public Response getPresignedUrl(
      @QueryParam("filename") String filename, @QueryParam("type") String type) {

    if (filename == null || filename.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(Map.of("error", "Query parameter 'filename' is required"))
          .build();
    }
    LOG.infof("Request presigned URL for file: %s", filename);
    var result = storageService.createPresignedUploadUrl(currentUserId, filename, type);
    return Response.ok(result).build();
  }
}
