package com.mchat.storage.dto;

public record PresignedUrl(String uploadUrl, String downloadUrl, String objectKey) {
}
