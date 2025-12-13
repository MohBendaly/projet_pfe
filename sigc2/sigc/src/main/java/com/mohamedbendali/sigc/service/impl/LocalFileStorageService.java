package com.mohamedbendali.sigc.service.impl; // <- Est-ce le bon package ?

// Imports essentiels
import com.mohamedbendali.sigc.exception.FileStorageException;
import com.mohamedbendali.sigc.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service; // <- Annotation présente ?
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path; // <- L'import est là
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;


//@Service // <- Très important ! Est-elle bien là ?
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation; // Doit être final

    // LE SEUL constructeur public doit être celui-ci
    public LocalFileStorageService(Path rootLocation) { // <- Prend bien un Path
        System.out.println("--- LocalFileStorageService CONSTRUCTOR CALLED with Path: " + rootLocation + " ---");
        if (rootLocation == null) {
            System.err.println("ERREUR: rootLocation reçu est NULL dans le constructeur !");
            throw new IllegalArgumentException("Root location cannot be null");
        }
        this.rootLocation = rootLocation;
        System.out.println("rootLocation field set to: " + this.rootLocation);
    }

    // Pas d'autre constructeur (surtout pas de constructeur sans argument)

    @Override
    @PostConstruct
    public void init() {
        System.out.println("--- LocalFileStorageService init() method CALLED ---");
        try {
            // Cette vérification est redondante si on l'a faite dans FileStorageConfig, mais ne nuit pas
            Files.createDirectories(this.rootLocation);
            System.out.println("Storage location check/creation in init() successful for: " + this.rootLocation.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("ERREUR dans init(): Impossible de créer le répertoire: " + this.rootLocation.toAbsolutePath());
            throw new FileStorageException("Could not initialize storage location: " + this.rootLocation.toString(), e);
        }
    }
    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        try {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } catch (Exception e) {
            // Pas d'extension
            fileExtension = "";
        }
        // Générer un nom de fichier unique pour éviter les conflits
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        try {
            Path subDirPath = this.rootLocation.resolve(Paths.get(subDirectory)).normalize().toAbsolutePath();
            if (!subDirPath.startsWith(this.rootLocation.toAbsolutePath())) {
                throw new FileStorageException("Cannot store file outside current directory.");
            }
            Files.createDirectories(subDirPath); // Crée le sous-répertoire s'il n'existe pas

            Path destinationFile = subDirPath.resolve(uniqueFilename).normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(subDirPath)) {
                throw new FileStorageException("Cannot store file outside target directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Retourner le chemin relatif au dossier root pour stockage en BDD
            return Paths.get(subDirectory).resolve(uniqueFilename).toString().replace("\\", "/");

        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + originalFilename, e);
        }
    }


    private Path getFullPath(String filename, String subDirectory) {
        Path subDirPath = this.rootLocation.resolve(Paths.get(subDirectory)).normalize().toAbsolutePath();
        return subDirPath.resolve(filename).normalize();
    }

    @Override
    public Stream<Path> loadAll(String subDirectory) {
        Path subDirPath = this.rootLocation.resolve(Paths.get(subDirectory)).normalize().toAbsolutePath();
        try {
            return Files.walk(subDirPath, 1)
                    .filter(path -> !path.equals(subDirPath))
                    .map(subDirPath::relativize);
        } catch (IOException e) {
            // Si le sous-répertoire n'existe pas, retourner un stream vide
            if(Files.notExists(subDirPath)) {
                return Stream.empty();
            }
            throw new FileStorageException("Failed to read stored files from " + subDirectory, e);
        }
    }

    @Override
    public Path load(String filename, String subDirectory) {
        return getFullPath(filename, subDirectory);
    }

    @Override
    public Resource loadAsResource(String filename, String subDirectory) {
        try {
            Path file = load(filename, subDirectory);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Could not read file: " + filename + " in " + subDirectory);
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("Could not read file: " + filename + " in " + subDirectory, e);
        }
    }

    @Override
    public void delete(String filename, String subDirectory) {
        try {
            Path file = load(filename, subDirectory);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new FileStorageException("Could not delete file: " + filename + " in " + subDirectory, e);
        }
    }

    @Override
    public void deleteAll(String subDirectory) {
        Path subDirPath = this.rootLocation.resolve(Paths.get(subDirectory)).normalize().toAbsolutePath();
        FileSystemUtils.deleteRecursively(subDirPath.toFile());
        // Recréer le dossier après suppression
        try {
            Files.createDirectories(subDirPath);
        } catch (IOException e) {
            throw new FileStorageException("Could not recreate directory after deletion: " + subDirectory, e);
        }
    }
}