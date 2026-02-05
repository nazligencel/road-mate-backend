package com.roadmate.service;

import com.roadmate.model.Notification;
import com.roadmate.model.User;
import com.roadmate.repository.NotificationRepository;
import com.roadmate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    public Notification sendMeetingRequest(User sender, Long targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        Notification notification = Notification.builder()
                .user(targetUser)
                .sender(sender)
                .type("MEETING_REQUEST")
                .title("Meeting Request")
                .message(sender.getName() + " wants to meet up with you!")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .data("{\"senderId\": " + sender.getId() + ", \"senderLat\": " + sender.getLatitude() + ", \"senderLng\": " + sender.getLongitude() + "}")
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public void createNotification(User targetUser, User sender, String type, String title, String message, String data) {
        Notification notification = Notification.builder()
                .user(targetUser)
                .sender(sender)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .data(data)
                .build();

        notificationRepository.save(notification);
    }
}
