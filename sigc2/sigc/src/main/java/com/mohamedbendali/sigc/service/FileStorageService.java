package com.mohamedbendali.sigc.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface FileStorageService {
    void init(); // Initialiser le stockage si nécessaire
    String store(MultipartFile file, String subDirectory); // Retourne le chemin relatif ou l'identifiant
    Stream<Path> loadAll(String subDirectory);
    Path load(String filename, String subDirectory);
    Resource loadAsResource(String filename, String subDirectory);
    void delete(String filename, String subDirectory);
    void deleteAll(String subDirectory);
}