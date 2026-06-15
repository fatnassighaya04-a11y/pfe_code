package com.pfe.docextraction.repository;

import com.pfe.docextraction.entity.ReclamationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReclamationMessageRepository extends JpaRepository<ReclamationMessage, UUID> {
    List<ReclamationMessage> findByReclamationIdOrderByDateMessageAsc(UUID reclamationId);
}
