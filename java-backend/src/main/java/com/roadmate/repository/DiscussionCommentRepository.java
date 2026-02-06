package com.roadmate.repository;

import com.roadmate.model.DiscussionComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscussionCommentRepository extends JpaRepository<DiscussionComment, Long> {
    List<DiscussionComment> findByDiscussionIdOrderByCreatedAtDesc(Long discussionId);
    long countByDiscussionId(Long discussionId);
}
