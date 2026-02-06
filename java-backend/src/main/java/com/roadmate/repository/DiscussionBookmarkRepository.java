package com.roadmate.repository;

import com.roadmate.model.DiscussionBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscussionBookmarkRepository extends JpaRepository<DiscussionBookmark, Long> {
    Optional<DiscussionBookmark> findByUserIdAndDiscussionId(Long userId, Long discussionId);
    List<DiscussionBookmark> findByUserId(Long userId);
    void deleteByUserIdAndDiscussionId(Long userId, Long discussionId);
    boolean existsByUserIdAndDiscussionId(Long userId, Long discussionId);
}
