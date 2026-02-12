package com.roadmate.service;

import com.roadmate.model.User;
import com.roadmate.repository.BlockedUserRepository;
import com.roadmate.repository.ConnectionRepository;
import com.roadmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteNotificationService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ExpoPushService expoPushService;
    private final BlockedUserRepository blockedUserRepository;

    @Async
    public void handleRouteChange(User user, String newRoute) {
        if (newRoute == null || newRoute.isBlank()) return;

        Set<Long> notifiedUserIds = new HashSet<>();

        // 1. Notify all accepted friends
        List<User> friends = connectionRepository.findAcceptedConnectionUsers(user.getId());
        List<String> friendPushTokens = new ArrayList<>();

        for (User friend : friends) {
            if (blockedUserRepository.existsBlockBetween(user.getId(), friend.getId())) continue;

            notifiedUserIds.add(friend.getId());

            notificationService.createNotification(
                    friend, user, "ROUTE_UPDATE",
                    "Route Update",
                    user.getName() + " is on the road: " + newRoute + "!",
                    "{\"senderId\": " + user.getId() + ", \"route\": \"" + newRoute.replace("\"", "\\\"") + "\"}"
            );

            if (friend.getExpoPushToken() != null) {
                friendPushTokens.add(friend.getExpoPushToken());
            }
        }

        if (!friendPushTokens.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "ROUTE_UPDATE");
            data.put("senderId", user.getId());
            expoPushService.sendBatchPushNotifications(
                    friendPushTokens,
                    "Route Update",
                    user.getName() + " is on the road: " + newRoute + "!",
                    data
            );
        }

        // 2. Notify users with matching route (non-friends)
        String destination = extractDestination(newRoute);
        if (destination == null || destination.isBlank()) return;

        List<User> matchingUsers = userRepository.findUsersWithMatchingRoute(destination, user.getId());
        List<String> matchPushTokens = new ArrayList<>();

        int count = 0;
        for (User matchUser : matchingUsers) {
            if (count >= 20) break;
            if (notifiedUserIds.contains(matchUser.getId())) continue;
            if (blockedUserRepository.existsBlockBetween(user.getId(), matchUser.getId())) continue;

            notifiedUserIds.add(matchUser.getId());
            count++;

            notificationService.createNotification(
                    matchUser, user, "ROUTE_UPDATE",
                    "Route Match",
                    user.getName() + " is also heading to " + destination + "!",
                    "{\"senderId\": " + user.getId() + ", \"route\": \"" + newRoute.replace("\"", "\\\"") + "\"}"
            );

            if (matchUser.getExpoPushToken() != null) {
                matchPushTokens.add(matchUser.getExpoPushToken());
            }
        }

        if (!matchPushTokens.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "ROUTE_UPDATE");
            data.put("senderId", user.getId());
            expoPushService.sendBatchPushNotifications(
                    matchPushTokens,
                    "Route Match",
                    user.getName() + " is also heading to " + destination + "!",
                    data
            );
        }
    }

    /**
     * Extract destination from route string.
     * Supports formats like "Istanbul → Antalya", "Istanbul - Antalya", "Istanbul > Antalya"
     */
    private String extractDestination(String route) {
        if (route == null) return null;

        // Try arrow separator first
        String[] separators = {"→", "->", "➔", "»", " - ", " > "};
        for (String sep : separators) {
            if (route.contains(sep)) {
                String[] parts = route.split(sep);
                if (parts.length >= 2) {
                    return parts[parts.length - 1].trim();
                }
            }
        }

        // If no separator found, use the whole route as keyword
        return route.trim();
    }
}
