package com.mohamedbendali.sigc.service;

import com.mohamedbendali.sigc.dto.CandidateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CandidateService {
    CandidateDTO createCandidate(CandidateDTO dto);
    CandidateDTO updateCandidate(Long id, CandidateDTO dto);
    CandidateDTO getCandidateById(Long id);
    CandidateDTO getCandidateByEmail(String email);
    Page<CandidateDTO> getAllCandidates(Pageable pageable);
    void deleteCandidate(Long id);

    // Ajoutez cette ligne :
    CandidateDTO getCandidateByUserId(Long userId); // Méthode pour trouver par l'ID de l'utilisateur lié
}