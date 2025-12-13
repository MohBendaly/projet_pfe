package com.mohamedbendali.sigc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class CandidateDTO {
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^\\+?[0-9.\\-\\s()]+$", message = "Format de téléphone invalide")
    @Size(max = 20)
    private String phone;

    private String resumePath;
    private Long userId; // ID de l'utilisateur associé
    private Set<String> skillNames; // Noms des compétences possédées
    private List<Long> applicationIds; // Juste les IDs des candidatures
    private LocalDateTime createdAt;
}