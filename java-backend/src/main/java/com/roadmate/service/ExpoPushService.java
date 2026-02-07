package com.roadmate.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpoPushService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final int BATCH_SIZE = 100;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendBatchPushNotifications(List<String> tokens, String title, String body, Map<String, Object> data) {
        if (tokens == null || tokens.isEmpty()) return;

        List<Map<String, Object>> messages = new ArrayList<>();
        for (String token : tokens) {
            Map<String, Object> message = new HashMap<>();
            message.put("to", token);
            message.put("title", title);
            message.put("body", body);
            message.put("sound", "default");
            message.put("priority", "high");
            message.put("channelId", "sos-alerts");
            if (data != null) {
                message.put("data", data);
            }
            messages.add(message);
        }

        // Send in batches of 100
        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            List<Map<String, Object>> batch = messages.subList(i, Math.min(i + BATCH_SIZE, messages.size()));
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "application/json");

                HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(batch, headers);
                restTemplate.postForEntity(EXPO_PUSH_URL, request, String.class);
            } catch (Exception e) {
                System.err.println("Expo push notification batch failed: " + e.getMessage());
            }
        }
    }
}
