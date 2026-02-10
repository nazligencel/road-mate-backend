package com.roadmate.controller;

import com.roadmate.dto.ConversationDto;
import com.roadmate.dto.MessageDto;
import com.roadmate.dto.SendMessageRequest;
import com.roadmate.model.Message;
import com.roadmate.model.Notification;
import com.roadmate.model.User;
import com.roadmate.repository.BlockedUserRepository;
import com.roadmate.repository.MessageRepository;
import com.roadmate.repository.NotificationRepository;
import com.roadmate.repository.UserRepository;
import com.roadmate.security.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private User getCurrentUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization required");
        }
        String token = authHeader.substring(7);
        String email = jwtUtils.getEmailFromJwtToken(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Get all conversations (chat list)
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);

        List<Message> latestMessages = messageRepository.findLatestMessagePerConversation(currentUser.getId());

        // Get blocked user IDs to filter conversations
        List<Long> blockedIds = blockedUserRepository.findBlockedUserIdsByBlockerId(currentUser.getId());

        List<ConversationDto> conversations = latestMessages.stream()
            .filter(msg -> {
                User otherUser = msg.getSender().getId().equals(currentUser.getId())
                        ? msg.getReceiver()
                        : msg.getSender();
                return !blockedIds.contains(otherUser.getId());
            })
            .map(msg -> {
            User otherUser = msg.getSender().getId().equals(currentUser.getId())
                    ? msg.getReceiver()
                    : msg.getSender();

            boolean isOnline = otherUser.getLastActive() != null &&
                    ChronoUnit.MINUTES.between(otherUser.getLastActive(), LocalDateTime.now()) < 5;

            Long unreadCount = messageRepository.countUnreadFromSender(currentUser.getId(), otherUser.getId());

            return ConversationDto.builder()
                    .odUserId(otherUser.getId())
                    .otherUserName(otherUser.getName())
                    .otherUserImage(otherUser.getProfileImageUrl() != null && !otherUser.getProfileImageUrl().isEmpty() ? otherUser.getProfileImageUrl() : otherUser.getImage())
                    .otherUserOnline(isOnline)
                    .lastMessage(msg.getContent())
                    .lastMessageTime(msg.getCreatedAt())
                    .unreadCount(unreadCount)
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(conversations);
    }

    // Get messages with a specific user
    @GetMapping("/conversation/{userId}")
    @Transactional
    public ResponseEntity<List<MessageDto>> getConversation(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);
        User otherUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Mark messages as read
        messageRepository.markAsRead(currentUser.getId(), userId);

        List<Message> messages = messageRepository.findConversation(currentUser.getId(), userId);

        List<MessageDto> messageDtos = messages.stream().map(msg -> MessageDto.builder()
                .id(msg.getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getName())
                .senderImage(msg.getSender().getImage())
                .receiverId(msg.getReceiver().getId())
                .receiverName(msg.getReceiver().getName())
                .receiverImage(msg.getReceiver().getImage())
                .content(msg.getContent())
                .isRead(msg.getIsRead())
                .createdAt(msg.getCreatedAt())
                .isMine(msg.getSender().getId().equals(currentUser.getId()))
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(messageDtos);
    }

    // Send a message
    @PostMapping("/send")
    public ResponseEntity<MessageDto> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @RequestHeader("Authorization") String authHeader) {
        User sender = getCurrentUser(authHeader);
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // Block check: either direction blocks messaging
        if (blockedUserRepository.existsBlockBetween(sender.getId(), receiver.getId())) {
            return ResponseEntity.status(403).body(null);
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Message savedMessage = messageRepository.save(message);

        // Create notification for receiver
        Notification notification = Notification.builder()
                .user(receiver)
                .sender(sender)
                .type("MESSAGE")
                .title("New message")
                .message(sender.getName() + " sent you a message")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);

        MessageDto dto = MessageDto.builder()
                .id(savedMessage.getId())
                .senderId(sender.getId())
                .senderName(sender.getName())
                .senderImage(sender.getImage())
                .receiverId(receiver.getId())
                .receiverName(receiver.getName())
                .receiverImage(receiver.getImage())
                .content(savedMessage.getContent())
                .isRead(savedMessage.getIsRead())
                .createdAt(savedMessage.getCreatedAt())
                .isMine(true)
                .build();

        return ResponseEntity.ok(dto);
    }

    // Get unread message count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {
        User currentUser = getCurrentUser(authHeader);
        Long count = messageRepository.countUnreadMessages(currentUser.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
