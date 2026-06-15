package com.pfe.docextraction.repository;

import com.pfe.docextraction.entity.Reclamation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReclamationRepository extends JpaRepository<Reclamation, UUID> {
    Page<Reclamation> findByUserIdOrderByDateCreationDesc(UUID userId, Pageable pageable);
    Page<Reclamation> findAllByOrderByDateCreationDesc(Pageable pageable);
}
