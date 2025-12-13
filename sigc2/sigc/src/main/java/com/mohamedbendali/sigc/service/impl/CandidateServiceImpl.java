package com.mohamedbendali.sigc.service.impl;

import com.mohamedbendali.sigc.dto.CandidateDTO;
import com.mohamedbendali.sigc.entity.Candidate;
import com.mohamedbendali.sigc.entity.JobApplication;
import com.mohamedbendali.sigc.entity.Skill;
import com.mohamedbendali.sigc.entity.User; // Supposons que le User est créé séparément ou lié
import com.mohamedbendali.sigc.exception.ResourceNotFoundException;
import com.mohamedbendali.sigc.repository.CandidateRepository;
import com.mohamedbendali.sigc.repository.UserRepository; // Pour lier User et Candidate
import com.mohamedbendali.sigc.service.CandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository; // Optionnel, si on lie ici

    @Override
    public CandidateDTO createCandidate(CandidateDTO dto) {
        log.debug("Attempting to create candidate with email: {}", dto.getEmail());
        // Idéalement, vérifier si l'email existe déjà via UserRepository si User est la source principale
        if (candidateRepository.findByEmailIgnoreCase(dto.getEmail()).isPresent()) {
            // Gérer le cas où un candidat avec cet email existe déjà
            // Peut-être lever une exception spécifique ou retourner l'existant ?
            // Pour l'instant, on suppose que la validation est faite avant
            log.warn("Candidate with email {} already exists.", dto.getEmail());
            // throw new DuplicateResourceException("Candidate", "email", dto.getEmail());
        }
        Candidate candidate = convertToEntity(dto);
        // Logique pour lier à un User existant si userId est fourni dans le DTO
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getUserId()));
            candidate.setUser(user);
            // Potentiellement mettre à jour le user.setCandidateProfile(candidate) si la relation est bidirectionnelle gérée ici
        }
        candidate.setCreatedAt(LocalDateTime.now()); // Assurer que la date est mise
        Candidate savedCandidate = candidateRepository.save(candidate);
        log.info("Candidate created successfully with ID: {}", savedCandidate.getId());
        return convertToDto(savedCandidate);
    }

    @Override
    public CandidateDTO updateCandidate(Long id, CandidateDTO dto) {
        log.debug("Attempting to update candidate with ID: {}", id);
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));

        // Mettre à jour les champs modifiables
        candidate.setFirstName(dto.getFirstName());
        candidate.setLastName(dto.getLastName());
        candidate.setPhone(dto.getPhone());
        // Ne pas permettre la modification de l'email ici ? Ou vérifier l'unicité si modifié.
        // Gérer la mise à jour des compétences et du CV (resumePath)
        // candidate.setResumePath(dto.getResumePath()); // Probablement géré par un endpoint de fichier séparé
        // Mettre à jour les compétences (logique plus complexe nécessaire)

        candidate.setUpdatedAt(LocalDateTime.now()); // Mise à jour manuelle si @PreUpdate n'est pas utilisé
        Candidate updatedCandidate = candidateRepository.save(candidate);
        log.info("Candidate updated successfully with ID: {}", updatedCandidate.getId());
        return convertToDto(updatedCandidate);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateDTO getCandidateById(Long id) {
        log.debug("Fetching candidate by ID: {}", id);
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));
        return convertToDto(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateDTO getCandidateByEmail(String email) {
        log.debug("Fetching candidate by email: {}", email);
        Candidate candidate = candidateRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "email", email));
        return convertToDto(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateDTO getCandidateByUserId(Long userId) {
        log.debug("Fetching candidate by user ID: {}", userId);
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "userId", userId));
        return convertToDto(candidate);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<CandidateDTO> getAllCandidates(Pageable pageable) {
        log.debug("Fetching all candidates, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return candidateRepository.findAll(pageable).map(this::convertToDto);
    }

    @Override
    public void deleteCandidate(Long id) {
        log.debug("Attempting to delete candidate with ID: {}", id);
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));
        // Ajouter logique de nettoyage si nécessaire (ex: anonymiser candidatures?)
        // Attention aux contraintes de clé étrangère et aux cascades.
        candidateRepository.delete(candidate);
        log.info("Candidate deleted successfully with ID: {}", id);
    }

    // --- Méthodes de Mapping (privées ou dans une classe Mapper) ---

    private CandidateDTO convertToDto(Candidate candidate) {
        if (candidate == null) return null;
        CandidateDTO dto = new CandidateDTO();
        dto.setId(candidate.getId());
        dto.setFirstName(candidate.getFirstName());
        dto.setLastName(candidate.getLastName());
        dto.setEmail(candidate.getEmail());
        dto.setPhone(candidate.getPhone());
        dto.setResumePath(candidate.getResumePath());
        dto.setUserId(candidate.getUser() != null ? candidate.getUser().getId() : null);
        dto.setCreatedAt(candidate.getCreatedAt());
        // Mapper les compétences (juste les noms par exemple)
        if (candidate.getSkills() != null) {
            dto.setSkillNames(candidate.getSkills().stream().map(Skill::getName).collect(Collectors.toSet()));
        }
        // Mapper les IDs des applications (simple liste d'IDs)
        if (candidate.getApplications() != null) {
            dto.setApplicationIds(candidate.getApplications().stream().map(JobApplication::getId).collect(Collectors.toList()));
        }
        return dto;
    }

    private Candidate convertToEntity(CandidateDTO dto) {
        if (dto == null) return null;
        Candidate candidate = new Candidate();
        // L'ID n'est généralement pas défini lors de la conversion DTO -> Entité pour la création
        candidate.setFirstName(dto.getFirstName());
        candidate.setLastName(dto.getLastName());
        candidate.setEmail(dto.getEmail());
        candidate.setPhone(dto.getPhone());
        candidate.setResumePath(dto.getResumePath()); // Peut être null initialement
        // La liaison User, Skills, Applications est gérée séparément dans la logique de service
        return candidate;
    }
}