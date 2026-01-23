package com.roadmate.config;

import com.roadmate.model.User;
import com.roadmate.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                User user1 = User.builder()
                        .name("Selin")
                        .image("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&q=80")
                        .status("Şu an çevrimiçi")
                        .vehicle("4x4 Off-road")
                        .vehicleModel("VW Transporter T4")
                        .route("Kuzey (Akyaka)")
                        .latitude(37.0322)
                        .longitude(28.3242)
                        .build();

                User user2 = User.builder()
                        .name("Jax")
                        .image("https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300&q=80")
                        .status("Çevrimdışı")
                        .vehicle("Ford Transit")
                        .vehicleModel("Ford Transit Custom")
                        .route("Güney (Kaş)")
                        .latitude(37.0422)
                        .longitude(28.3142)
                        .build();

                User user3 = User.builder()
                        .name("Sage")
                        .image("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&q=80")
                        .status("Şu an çevrimiçi")
                        .vehicle("Vanagon")
                        .vehicleModel("VW Westfalia")
                        .route("Batı (Urla)")
                        .latitude(37.0222)
                        .longitude(28.3342)
                        .build();

                repository.saveAll(List.of(user1, user2, user3));
                System.out.println("🌱 Seed data initialized");
            }
        };
    }
}
