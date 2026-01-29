package com.roadmate;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoadMateApplication {
    public static void main(String[] args) {
        // .env dosyasını farklı dizinlerde ara
        Dotenv dotenv = Dotenv.configure()
                .directory("./java-backend") // Projenin bir üst dizinden çalıştırılması durumu için
                .ignoreIfMissing()
                .load();

        if (dotenv.entries().isEmpty()) {
            dotenv = Dotenv.configure()
                    .directory("./") // Mevcut dizin için
                    .ignoreIfMissing()
                    .load();
        }

        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        System.out.println("✅ Environment variables loaded: " + !dotenv.entries().isEmpty());
        
        SpringApplication.run(RoadMateApplication.class, args);
    }
}
