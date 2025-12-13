package com.mohamedbendali.sigc.service.impl;

import com.mohamedbendali.sigc.dto.JobOfferDTO;
import com.mohamedbendali.sigc.entity.JobOffer;
import com.mohamedbendali.sigc.entity.Skill;
import com.mohamedbendali.sigc.enums.OfferStatus;
import com.mohamedbendali.sigc.exception.ResourceNotFoundException;
import com.mohamedbendali.sigc.repository.JobOfferRepository;
import com.mohamedbendali.sigc.repository.SkillRepository;
import com.mohamedbendali.sigc.service.JobOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
// Importez un Mapper (ex: MapStruct) ou faites le mapping manuellement
// import com.mohamedbendali.sigc.mapper.JobOfferMapper;


@Service
@RequiredArgsConstructor // Injection via constructeur (Lombok)
@Transactional // Transactionnel par défaut pour les méthodes publiques
public class JobOfferServiceImpl implements JobOfferService {
    private final SkillRepository skillRepository;

    private final JobOfferRepository offerRepository;
    // private final JobOfferMapper offerMapper; // Si vous utilisez MapStruct

    @Override
    public JobOfferDTO createOffer(JobOfferDTO dto) {
        JobOffer offer = new JobOffer();
        offer.setTitle(dto.getTitle());
        offer.setDescription(dto.getDescription());
        offer.setStatus(dto.getStatus() != null ? dto.getStatus() : OfferStatus.DRAFT);
        offer.setSalaryRange(dto.getSalaryRange());
        offer.setRequirements(dto.getRequirements());
        offer.setPublicationDate(dto.getPublicationDate());
        offer.setExpirationDate(dto.getExpirationDate());

        Set<Skill> skills = dto.getRequiredSkillNames().stream()
                .map(name -> skillRepository.findByNameIgnoreCase(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Skill", "name", name)))
                .collect(Collectors.toSet());

        JobOffer savedOffer = offerRepository.save(offer);
        return convertToDto(savedOffer);
    }


    @Override
    public JobOfferDTO updateOffer(Long id, JobOfferDTO dto) {
        JobOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobOffer", "id", id));
        // Mapper les champs de dto vers offer
        offer.setTitle(dto.getTitle());
        offer.setDescription(dto.getDescription());
        // ... autres champs ...
        if (dto.getStatus() != null) { // S'assurer que le statut n'est pas null
            offer.setStatus(dto.getStatus());
        }
        // ... autres champs (salaryRange, requirements, requiredSkillNames, dates) ...
        // Si le statut passe à PUBLISHED et publicationDate est null, le mettre à jour ?
        if (dto.getStatus() == OfferStatus.PUBLISHED && offer.getPublicationDate() == null) {
            offer.setPublicationDate(LocalDateTime.now());
            //   log.info("Setting publicationDate for offer ID {} as it is now PUBLISHED.", id);
        }

        offer.setUpdatedAt(LocalDateTime.now()); // Assurer la mise à jour
        JobOffer updatedOffer = offerRepository.save(offer);
        return convertToDto(updatedOffer);
    }

    @Override
    @Transactional(readOnly = true) // Lecture seule, pas de transaction nécessaire
    public JobOfferDTO getOfferById(Long id) {
        JobOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobOffer", "id", id));
        return convertToDto(offer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getAllOffers(Pageable pageable) {
        return offerRepository.findAll(pageable).map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobOfferDTO> getOffersByStatus(OfferStatus status, Pageable pageable) {
        return offerRepository.findByStatus(status, pageable).map(this::convertToDto);
    }

    @Override
    public void deleteOffer(Long id) {
        JobOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobOffer", "id", id));
        // Ajouter logique: vérifier si des candidatures existent avant de supprimer? Archiver plutôt?
        offerRepository.delete(offer);
    }

    @Override
    public JobOfferDTO updateOfferStatus(Long id, OfferStatus newStatus) {
        JobOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobOffer", "id", id));
        offer.setStatus(newStatus);
        // Ajouter logique: si PUBLISHED, mettre à jour publicationDate?
        if (newStatus == OfferStatus.PUBLISHED && offer.getPublicationDate() == null) {
            // offer.setPublicationDate(java.time.LocalDateTime.now()); // Mise à jour date publication
        }
        JobOffer updatedOffer = offerRepository.save(offer);
        return convertToDto(updatedOffer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobOfferDTO> searchOffers(String keyword, OfferStatus status, Pageable pageable) {
        // Implémenter la recherche par mot clé + statut
        if (keyword != null && !keyword.isBlank()) {
            return offerRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatus(keyword, keyword, status, pageable)
                    .map(this::convertToDto);
        } else {
            return offerRepository.findByStatus(status, pageable).map(this::convertToDto);
        }
    }

    // Méthode de conversion Manuelle (à remplacer par MapStruct idéalement)
    private JobOfferDTO convertToDto(JobOffer offer) {
        JobOfferDTO dto = new JobOfferDTO();
        dto.setId(offer.getId());
        dto.setTitle(offer.getTitle());
        dto.setDescription(offer.getDescription());
        dto.setStatus(offer.getStatus());
        dto.setRequirements(offer.getRequirements());
        // Convertir Set<Skill> en Set<String> (noms)
        dto.setSalaryRange(offer.getSalaryRange());
        dto.setPublicationDate(offer.getPublicationDate());
        dto.setExpirationDate(offer.getExpirationDate());
        dto.setCreatedAt(offer.getCreatedAt());
        return dto;
    }
}