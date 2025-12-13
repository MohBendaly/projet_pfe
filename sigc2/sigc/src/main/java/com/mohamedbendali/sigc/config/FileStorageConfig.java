package com.mohamedbendali.sigc.config;

import com.mohamedbendali.sigc.service.FileStorageService;
import com.mohamedbendali.sigc.service.impl.LocalFileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // N'oubliez pas cette implémentation

import java.nio.file.Path; // Vérifiez cet import
import java.nio.file.Paths; // Vérifiez cet import

@Configuration
public class FileStorageConfig implements WebMvcConfigurer { // Assurez-vous d'implémenter WebMvcConfigurer si vous avez addResourceHandlers

    @Value("${file.upload-dir}")
    private String uploadDir; // Le champ qui reçoit la valeur

    @Bean // Annotation pour déclarer le bean
    public FileStorageService fileStorageService() {
        System.out.println("--- Création du bean fileStorageService ---"); // Log de débogage
        System.out.println("Upload directory from property: " + this.uploadDir);

        // 1. Créer l'objet Path
        Path rootLocation = Paths.get(this.uploadDir);
        System.out.println("Resolved Path: " + rootLocation.toAbsolutePath());

        // 2. Passer le Path au constructeur
        LocalFileStorageService service = new LocalFileStorageService(rootLocation);

        System.out.println("--- Bean fileStorageService créé ---");
        return service; // Retourner l'instance créée
    }

    // Si vous avez cette méthode, gardez l'implémente WebMvcConfigurer
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(this.uploadDir);
        String uploadPathUri = uploadPath.toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPathUri); // Doit finir par '/' si c'est un répertoire? Essayez "file:" + uploadPath.toAbsolutePath().normalize() + "/"
        System.out.println("Resource Handler configured for /uploads/** pointing to " + uploadPathUri);
    }
}