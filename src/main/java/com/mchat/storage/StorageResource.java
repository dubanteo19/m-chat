package com.mchat.storage;

import org.jboss.logging.Logger;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Path("/storage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StorageResource {
    private static final Logger LOG = Logger.getLogger(StorageResource.class);

    @Inject
    MinioClient minioClient;

    private static final String BUCKET_NAME = "mchat-public";
    private static final String MINIO_EXTERNAL_URL = "https://minio.dbt19.site";

    @GET
    @Path("/presigned-url")
    public Response getPresignedUrl(@QueryParam("filename") String filename) {
        LOG.info("request presign url for file: " + filename);
        if (filename == null || filename.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Query parameter 'filename' is required"))
                    .build();
        }
        try {
            // Generate a secure PUT URL for direct frontend client uploads
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(BUCKET_NAME)
                            .object(filename)
                            .expiry(5, TimeUnit.MINUTES)
                            .build());

            // Construct permanent path structure for asset rendering
            String downloadUrl = MINIO_EXTERNAL_URL + "/" + BUCKET_NAME + "/" + filename;
            // uploadUrl = uploadUrl.replace("http://192.168.1.81:9000", MINIO_EXTERNAL_URL);
            return Response.ok(Map.of(
                    "uploadUrl", uploadUrl,
                    "downloadUrl", downloadUrl)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Could not generate security token"))
                    .build();
        }
    }
}