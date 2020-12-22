package io.learnk8s.knotejava.service;

import io.learnk8s.knotejava.config.KNoteProperties;
import io.learnk8s.knotejava.document.Note;
import io.learnk8s.knotejava.repo.NotesRepository;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class KNoteService {

  @Autowired
  private NotesRepository notesRepository;

  @Autowired
  private KNoteProperties properties;

  private final Parser parser = Parser.builder().build();
  private final HtmlRenderer renderer = HtmlRenderer.builder().build();

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
    File uploadsDir = new File(properties.getUploadDir());
    if (!uploadsDir.exists()) {
      uploadsDir.mkdir();
    }
    String fileId = UUID.randomUUID().toString() + "."
        + file.getOriginalFilename().split("\\.")[1];
    file.transferTo(new File(properties.getUploadDir() + fileId));
    model.addAttribute("description", description + " ![](/uploads/" + fileId + ")");
  }
}
