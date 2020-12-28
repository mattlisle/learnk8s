package io.learnk8s.knotejava.controller;

import io.learnk8s.knotejava.service.KNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class KNoteController {

  @Autowired
  KNoteService kNoteService;

  @GetMapping("/")
  public String index(Model model) {
    kNoteService.getNotes(model);
    return "index";
  }

  @PostMapping("/note")
  public String saveNotes(@RequestParam("image") MultipartFile file,
                          @RequestParam String description,
                          @RequestParam(required = false) String publish,
                          @RequestParam(required = false) String upload,
                          Model model) throws Exception {

    if (publish != null && publish.equals("Publish")) {
      kNoteService.createNote(description, model);
      kNoteService.getNotes(model);
      return "redirect:/";
    }
    if (upload != null && upload.equals("Upload")) {
      if (file != null && file.getOriginalFilename() != null
          && !file.getOriginalFilename().isEmpty()) {
        kNoteService.uploadImage(file, description, model);
      }
      kNoteService.getNotes(model);
      return "index";
    }
    // After save fetch all notes again
    return "index";
  }

  @GetMapping(value = "/img/{name}", produces = MediaType.IMAGE_PNG_VALUE)
  public @ResponseBody byte[] getImageByName(@PathVariable String name) throws Exception {
    System.out.println("> Getting image by name: " + name);
    return kNoteService.getImageByName(name);
  }
}
