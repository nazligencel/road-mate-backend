package com.roadmate.repository;

import com.roadmate.model.AssistRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssistRequestRepository extends JpaRepository<AssistRequest, Long> {

    @Query(value = "SELECT * FROM assist_requests WHERE status = :status " +
                   "ORDER BY created_at DESC LIMIT 50", nativeQuery = true)
    List<AssistRequest> findByStatus(@Param("status") String status);

    @Query(value = "SELECT * FROM assist_requests " +
                   "ORDER BY created_at DESC LIMIT 50", nativeQuery = true)
    List<AssistRequest> findAllRecent();

    List<AssistRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
