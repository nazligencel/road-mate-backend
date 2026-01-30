package com.roadmate.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/places")
@CrossOrigin(origins = "*")
public class PlacesController {

    @Value("${google.places.api.key}")
    private String googlePlacesApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // Keywords for better search results in Turkey
    private static final Map<String, String> SEARCH_STRATEGY = Map.of(
        "mechanics", "keyword=oto+tamir",
        "markets", "keyword=market",
        "fuel", "type=gas_station"
    );

    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyPlaces(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam String category,
            @RequestParam(defaultValue = "10000") Integer radius) { // Increased radius to 10km
        
        String searchParam = SEARCH_STRATEGY.get(category);
        if (searchParam == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid category: " + category));
        }

        try {
            // Construct URL using the specific strategy (keyword or type)
            String url = String.format(
                java.util.Locale.US,
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=%d&%s&key=%s",
                lat, lng, radius, searchParam, googlePlacesApiKey
            );
            
            // Debug logging
            System.out.println("🔍 Places API Request:");
            System.out.println("   - Lat: " + lat + ", Lng: " + lng);
            System.out.println("   - Category: " + category + " -> Param: " + searchParam);
            System.out.println("   - URL: " + url.replace(googlePlacesApiKey, "***"));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            String status = (String) response.get("status");
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                System.err.println("Places API Error: " + status + " - " + response.get("error_message"));
                return ResponseEntity.ok(Collections.emptyList());
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getOrDefault("results", Collections.emptyList());
            
            List<Map<String, Object>> places = new ArrayList<>();
            int count = 0;
            
            for (Map<String, Object> place : results) {
                if (count >= 15) break; // Limit to 15 results
                
                @SuppressWarnings("unchecked")
                Map<String, Object> geometry = (Map<String, Object>) place.get("geometry");
                @SuppressWarnings("unchecked")
                Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                
                Double placeLat = ((Number) location.get("lat")).doubleValue();
                Double placeLng = ((Number) location.get("lng")).doubleValue();
                
                // Get photo reference if available
                String imageUrl = getDefaultImage(category);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> photos = (List<Map<String, Object>>) place.get("photos");
                if (photos != null && !photos.isEmpty()) {
                    String photoRef = (String) photos.get(0).get("photo_reference");
                    imageUrl = String.format(
                        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=200&photoreference=%s&key=%s",
                        photoRef, googlePlacesApiKey
                    );
                }
                
                // Get opening hours
                @SuppressWarnings("unchecked")
                Map<String, Object> openingHours = (Map<String, Object>) place.get("opening_hours");
                String openStatus = "Unknown";
                if (openingHours != null) {
                    Boolean openNow = (Boolean) openingHours.get("open_now");
                    openStatus = Boolean.TRUE.equals(openNow) ? "Open" : "Closed";
                }
                
                Map<String, Object> placeData = new HashMap<>();
                placeData.put("id", place.get("place_id"));
                placeData.put("name", place.get("name"));
                placeData.put("type", category);
                placeData.put("distance", calculateDistance(lat, lng, placeLat, placeLng));
                placeData.put("coordinate", Map.of("latitude", placeLat, "longitude", placeLng));
                placeData.put("image", imageUrl);
                placeData.put("status", openStatus);
                placeData.put("rating", place.getOrDefault("rating", 0));
                placeData.put("address", place.getOrDefault("vicinity", ""));
                
                // Only add if it has a name
                if (place.get("name") != null) {
                    places.add(placeData);
                    count++;
                }
            }
            
            System.out.println("✅ Found " + places.size() + " " + category + " near " + lat + "," + lng);
            return ResponseEntity.ok(places);
            
        } catch (Exception e) {
            System.err.println("Error fetching places: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round((R * c) * 10.0) / 10.0; // Round to 1 decimal
    }

    private String getDefaultImage(String category) {
        return switch (category) {
            case "mechanics" -> "https://images.unsplash.com/photo-1487754180477-db33d3d63b0a?w=200&q=80";
            case "markets" -> "https://images.unsplash.com/photo-1542838132-92c53300491e?w=200&q=80";
            case "fuel" -> "https://images.unsplash.com/photo-1545459720-aac3e5c2fa0c?w=200&q=80";
            default -> "https://via.placeholder.com/200";
        };
    }
}
