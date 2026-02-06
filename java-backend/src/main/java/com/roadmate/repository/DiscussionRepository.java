package com.roadmate.repository;

import com.roadmate.model.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {
    List<Discussion> findAllByOrderByCreatedAtDesc();
    List<Discussion> findByTagOrderByCreatedAtDesc(String tag);
}
