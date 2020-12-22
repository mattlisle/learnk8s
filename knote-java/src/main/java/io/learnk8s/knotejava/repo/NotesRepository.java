package io.learnk8s.knotejava.repo;

import io.learnk8s.knotejava.document.Note;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotesRepository extends MongoRepository<Note, String> {

}

