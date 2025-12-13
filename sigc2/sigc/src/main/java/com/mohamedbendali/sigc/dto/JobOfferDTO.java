package com.mohamedbendali.sigc.dto;

import com.mohamedbendali.sigc.enums.OfferStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Data
public class JobOfferDTO {
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne peut dépasser 200 caractères")
    private String title;

    private String description;
    private OfferStatus status;
    private List<String> requirements;
    private Set<String> requiredSkillNames; // Noms des compétences requises
    private String salaryRange;
    private LocalDateTime publicationDate;
    private LocalDateTime expirationDate;
    private LocalDateTime createdAt;
}