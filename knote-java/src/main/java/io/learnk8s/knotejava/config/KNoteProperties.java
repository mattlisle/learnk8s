package io.learnk8s.knotejava.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knote")
public class KNoteProperties {

  @Value("${uploadDir:/tmp/uploads/}")
  private String uploadDir;

  public String getUploadDir() {
    return uploadDir;
  }
}

