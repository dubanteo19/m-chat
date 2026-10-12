package com.mchat.sticker;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class ImageProcessor {

  public record ProcessedImage(byte[] bytes, int width, int height, String contentType) {}

  private static final int MAX_DIMENSION = 500;
  private static final int WEBP_QUALITY = 80;

  public ProcessedImage process(InputStream imageStream) throws IOException {
    var image = ImmutableImage.loader().fromStream(imageStream);
    int originalWidth = image.width;
    int originalHeight = image.height;
    if (originalWidth > MAX_DIMENSION || originalHeight > MAX_DIMENSION) {
      double scale =
          Math.min((double) MAX_DIMENSION / originalWidth, (double) MAX_DIMENSION / originalHeight);
      int targetWidth = (int) (originalWidth * scale);
      int targetHeight = (int) (originalHeight * scale);
      image = image.scaleTo(targetWidth, targetHeight);
    }
    WebpWriter writer = new WebpWriter().withQ(WEBP_QUALITY).withM(6);
    byte[] output = image.bytes(writer);
    return new ProcessedImage(output, image.width, image.height, "image/webp");
  }
}
