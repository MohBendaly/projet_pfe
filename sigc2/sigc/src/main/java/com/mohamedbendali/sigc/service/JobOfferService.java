package com.mohamedbendali.sigc.service;

import com.mohamedbendali.sigc.dto.JobOfferDTO;
import com.mohamedbendali.sigc.entity.JobOffer;
import com.mohamedbendali.sigc.enums.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface JobOfferService {
    JobOfferDTO createOffer(JobOfferDTO dto);
    JobOfferDTO updateOffer(Long id, JobOfferDTO dto);
    JobOfferDTO getOfferById(Long id);
    Page<JobOfferDTO> getAllOffers(Pageable pageable);
    Page<JobOfferDTO> getOffersByStatus(OfferStatus status, Pageable pageable);
    void deleteOffer(Long id);
    JobOfferDTO updateOfferStatus(Long id, OfferStatus newStatus);
    Page<JobOfferDTO> searchOffers(String keyword, OfferStatus status, Pageable pageable);
}