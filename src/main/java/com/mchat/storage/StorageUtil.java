package com.mchat.storage;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StorageUtil {

    private static String urlPrefix;

    @ConfigProperty(name = "app.bucket")
    String bucketName;
    @ConfigProperty(name = "quarkus.minio.host")
    String minioHost;

    @ConfigProperty(name = "quarkus.minio.port")
    int minioPort;

    @PostConstruct
    void init() {
        urlPrefix = "http://" + minioHost + ":" + minioPort
                + "/" + bucketName + "/";
    }

    public static String getUrlPrefix() {
        if (urlPrefix == null) {
            throw new IllegalStateException("StorageUrlUtil not initialized");
        }
        return urlPrefix;
    }
}