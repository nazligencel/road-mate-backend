package com.roadmate.repository;

import com.roadmate.model.AssistMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssistMessageRepository extends JpaRepository<AssistMessage, Long> {
    List<AssistMessage> findByAssistRequestIdOrderByCreatedAtAsc(Long assistRequestId);
    long countByAssistRequestId(Long assistRequestId);
}
