package com.mohamedbendali.sigc.repository;

import com.mohamedbendali.sigc.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByOwnerId(Long candidateId);

    List<Attachment> findByJobApplicationId(Long applicationId);

    Optional<Attachment> findByIdAndOwnerId(Long id, Long candidateId);

    Optional<Attachment> findByIdAndJobApplicationId(Long id, Long applicationId);
}