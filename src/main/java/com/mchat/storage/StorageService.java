package com.mchat.storage;

import com.mchat.storage.dto.PresignedUrl;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class StorageService {

  private static final String BUCKET_NAME = "mchat-public";
  private static final int UPLOAD_URL_EXPIRY_MINUTES = 5;

  @Inject MinioClient minioClient;

  @ConfigProperty(name = "quarkus.minio.host")
  String minioHost;

  @ConfigProperty(name = "quarkus.minio.port")
  String minioPort;

  public void uploadObject(String objectKey, byte[] bytes) {
    try {
      var putObjectArgs =
          PutObjectArgs.builder().bucket(BUCKET_NAME).object(objectKey).stream(
                  new ByteArrayInputStream(bytes), bytes.length, -1)
              .build();
      minioClient.putObject(putObjectArgs);
    } catch (Exception e) {
      throw new StorageException("Could not upload object" + objectKey, e);
    }
  }

  public InputStream downloadObject(String objectKey) {
    try {
      return minioClient.getObject(
          GetObjectArgs.builder().bucket(BUCKET_NAME).object(objectKey).build());
    } catch (Exception e) {
      throw new StorageException("Could not download object" + objectKey, e);
    }
  }

  public PresignedUrl createPresignedUploadUrl(Long userId, String filename, String type) {

    String prefix = resolvePrefix(userId, type);
    String extension = extractExtension(filename);
    String objectKey = prefix + UUID.randomUUID() + extension;

    try {
      String uploadUrl =
          minioClient.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .method(Method.PUT)
                  .bucket(BUCKET_NAME)
                  .object(objectKey)
                  .expiry(UPLOAD_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
                  .build());

      String downloadUrl =
          "http://" + minioHost + ":" + minioPort + "/" + BUCKET_NAME + "/" + objectKey;

      return new PresignedUrl(uploadUrl, downloadUrl, objectKey);

    } catch (Exception e) {
      throw new StorageException("Could not generate presigned URL", e.getCause());
    }
  }

  private String resolvePrefix(Long userId, String type) {
    if (type == null) {
      return "images/";
    }

    return switch (type) {
      case "AVATAR" -> "avatars/" + userId + "/";
      case "IMAGE" -> "images/";
      case "STICKER" -> "draft/stickers/" + userId + "/";
      default -> "images/";
    };
  }

  private String extractExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex >= 0 ? filename.substring(dotIndex) : "";
  }

  public static class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
