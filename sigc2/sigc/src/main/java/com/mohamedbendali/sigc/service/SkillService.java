package com.mohamedbendali.sigc.service;

import com.mohamedbendali.sigc.dto.SkillDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SkillService {

    SkillDTO createSkill(SkillDTO skillDto);

    SkillDTO updateSkill(Long id, SkillDTO skillDto);

    Optional<SkillDTO> findById(Long id);

    Optional<SkillDTO> findByName(String name);

    Page<SkillDTO> findAll(Pageable pageable);

    List<SkillDTO> searchSkills(String name);

    void deleteSkill(Long id);
    List<SkillDTO> findAllSkillsAsList(); // Nouvelle méthode

    // Logique de liaison potentiellement dans CandidateService/JobOfferService
//     void addSkillToCandidate(Long candidateId, Long skillId);
//     void removeSkillFromCandidate(Long candidateId, Long skillId);
//     void addSkillToJobOffer(Long offerId, Long skillId);
//     void removeSkillFromJobOffer(Long offerId, Long skillId);
}