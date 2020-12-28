package io.learnk8s.knotejava.service;

import io.learnk8s.knotejava.config.KNoteProperties;
import io.learnk8s.knotejava.document.Note;
import io.learnk8s.knotejava.repo.NotesRepository;
import io.minio.MinioClient;
import org.apache.commons.io.IOUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@EnableConfigurationProperties(KNoteProperties.class)
public class KNoteService {

  @Autowired
  private NotesRepository notesRepository;

  @Autowired
  private KNoteProperties properties;

  private MinioClient minioClient;
  private final Parser parser = Parser.builder().build();
  private final HtmlRenderer renderer = HtmlRenderer.builder().build();

  private void initMinio() {
    boolean success = false;
    while (!success) {
      try {
        minioClient = new MinioClient("http://" + properties.getMinioHost() + ":9000" ,
            properties.getMinioAccessKey(),
            properties.getMinioSecretKey(),
            false);
        // Check if the bucket already exists.
        boolean isExist = minioClient.bucketExists(properties.getMinioBucket());
        if (isExist) {
          System.out.println("> Bucket already exists.");
        } else {
          minioClient.makeBucket(properties.getMinioBucket());
        }
        success = true;
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("> Minio Reconnect: " + properties.isMinioReconnectEnabled());
        if (properties.isMinioReconnectEnabled()) {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException ex) {
            ex.printStackTrace();
          }
        } else {
          success = true;
        }
      }
    }
    System.out.println("> Minio initialized!");
  }

  @PostConstruct
  public void init() {
    initMinio();
  }

  public void getNotes(Model model) {
    List<Note> notes = notesRepository.findAll();
    Collections.reverse(notes);
    model.addAttribute("notes", notes);
  }

  public void createNote(String description, Model model) {
    if (description != null && !description.trim().isEmpty()) {
      Node document = parser.parse(description.trim());
      String html = renderer.render(document);
      notesRepository.save(new Note(null, html));
      model.addAttribute("description", "");
    }
  }

  public final void uploadImage(MultipartFile file, String description, Model model) throws Exception {
    String fileId = UUID.randomUUID().toString() + "."
        + Objects.requireNonNull(file.getOriginalFilename()).split("\\.")[1];
    minioClient.putObject(properties.getMinioBucket(), fileId, file.getInputStream(),
        file.getSize(), null, null, file.getContentType());
    model.addAttribute("description", description + " ![](/img/" + fileId + ")");
  }

  public final byte[] getImageByName(String name) throws Exception {
    InputStream imageStream = minioClient.getObject(properties.getMinioBucket(), name);
    return IOUtils.toByteArray(imageStream);
  }
}
