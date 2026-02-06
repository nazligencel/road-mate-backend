package com.roadmate.service;

import com.roadmate.dto.CreateDiscussionRequest;
import com.roadmate.dto.DiscussionCommentDto;
import com.roadmate.dto.DiscussionDto;
import com.roadmate.model.Discussion;
import com.roadmate.model.DiscussionBookmark;
import com.roadmate.model.DiscussionComment;
import com.roadmate.model.User;
import com.roadmate.repository.DiscussionBookmarkRepository;
import com.roadmate.repository.DiscussionCommentRepository;
import com.roadmate.repository.DiscussionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiscussionService {

    @Autowired
    private DiscussionRepository discussionRepository;

    @Autowired
    private DiscussionCommentRepository commentRepository;

    @Autowired
    private DiscussionBookmarkRepository bookmarkRepository;

    public List<DiscussionDto> getDiscussions(User currentUser) {
        List<Discussion> discussions = discussionRepository.findAllByOrderByCreatedAtDesc();
        return discussions.stream()
                .map(d -> mapToDto(d, currentUser))
                .collect(Collectors.toList());
    }

    public DiscussionDto getDiscussion(Long id, User currentUser) {
        Discussion discussion = discussionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));
        return mapToDto(discussion, currentUser);
    }

    @Transactional
    public DiscussionDto createDiscussion(User creator, CreateDiscussionRequest request) {
        Discussion discussion = Discussion.builder()
                .creator(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .tag(request.getTag())
                .image(request.getImage())
                .createdAt(LocalDateTime.now())
                .build();

        Discussion saved = discussionRepository.save(discussion);
        return mapToDto(saved, creator);
    }

    public List<DiscussionCommentDto> getComments(Long discussionId) {
        List<DiscussionComment> comments = commentRepository.findByDiscussionIdOrderByCreatedAtDesc(discussionId);
        return comments.stream()
                .map(this::mapCommentToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DiscussionCommentDto addComment(User author, Long discussionId, String text) {
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));

        DiscussionComment comment = DiscussionComment.builder()
                .discussion(discussion)
                .author(author)
                .text(text)
                .createdAt(LocalDateTime.now())
                .build();

        DiscussionComment saved = commentRepository.save(comment);
        return mapCommentToDto(saved);
    }

    @Transactional
    public boolean toggleBookmark(User user, Long discussionId) {
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));

        var existing = bookmarkRepository.findByUserIdAndDiscussionId(user.getId(), discussionId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return false; // unsaved
        } else {
            DiscussionBookmark bookmark = DiscussionBookmark.builder()
                    .user(user)
                    .discussion(discussion)
                    .createdAt(LocalDateTime.now())
                    .build();
            bookmarkRepository.save(bookmark);
            return true; // saved
        }
    }

    public List<DiscussionDto> getSavedDiscussions(User user) {
        List<DiscussionBookmark> bookmarks = bookmarkRepository.findByUserId(user.getId());
        return bookmarks.stream()
                .map(b -> mapToDto(b.getDiscussion(), user))
                .collect(Collectors.toList());
    }

    private DiscussionDto mapToDto(Discussion discussion, User currentUser) {
        long commentCount = commentRepository.countByDiscussionId(discussion.getId());
        boolean isSaved = bookmarkRepository.existsByUserIdAndDiscussionId(
                currentUser.getId(), discussion.getId());

        return DiscussionDto.builder()
                .id(discussion.getId())
                .title(discussion.getTitle())
                .description(discussion.getDescription())
                .tag(discussion.getTag())
                .image(discussion.getImage())
                .createdAt(discussion.getCreatedAt())
                .creatorId(discussion.getCreator().getId())
                .creatorName(discussion.getCreator().getName())
                .creatorImage(discussion.getCreator().getImage())
                .commentCount((int) commentCount)
                .isSaved(isSaved)
                .timeAgo(calculateTimeAgo(discussion.getCreatedAt()))
                .build();
    }

    private DiscussionCommentDto mapCommentToDto(DiscussionComment comment) {
        return DiscussionCommentDto.builder()
                .id(comment.getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .authorImage(comment.getAuthor().getImage())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .timeAgo(calculateTimeAgo(comment.getCreatedAt()))
                .build();
    }

    private String calculateTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = duration.toHours();
        if (hours < 24) return hours + "h ago";
        long days = duration.toDays();
        if (days < 30) return days + "d ago";
        long months = days / 30;
        if (months < 12) return months + "mo ago";
        return (days / 365) + "y ago";
    }
}
