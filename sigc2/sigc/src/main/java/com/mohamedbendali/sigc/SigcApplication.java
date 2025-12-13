package com.mohamedbendali.sigc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // Si vous utilisez @CreatedDate etc. non mentionné directement mais @CreationTimestamp oui

@SpringBootApplication
// @EnableJpaAuditing // Décommentez si vous utilisez @CreatedDate / @LastModifiedDate
public class SigcApplication {

    public static void main(String[] args) {
        SpringApplication.run(SigcApplication.class, args);
    }

}