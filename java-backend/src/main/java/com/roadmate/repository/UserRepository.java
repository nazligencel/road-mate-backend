package com.roadmate.repository;

import com.roadmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Haversine Formülü kullanılarak kullanıcının mevcut
     * koordinatlarına göre en yakın 20 kişiyi bulur
     */
    @Query(value = "SELECT *, (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
                   "cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * " +
                   "sin(radians(latitude)))) AS distance FROM users " +
                   "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                   "ORDER BY distance LIMIT 20", nativeQuery = true)
    List<User> findNearbyNomads(@Param("lat") Double lat, @Param("lng") Double lng);

    @Query(value = "SELECT *, (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
                   "cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * " +
                   "sin(radians(latitude)))) AS distance FROM users " +
                   "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                   "AND expo_push_token IS NOT NULL " +
                   "AND id != :excludeUserId " +
                   "AND (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
                   "cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * " +
                   "sin(radians(latitude)))) <= :radiusKm " +
                   "ORDER BY distance", nativeQuery = true)
    List<User> findUsersWithPushTokenWithinRadius(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm,
            @Param("excludeUserId") Long excludeUserId);

    @Query(value = "SELECT *, (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
                   "cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * " +
                   "sin(radians(latitude)))) AS distance FROM users " +
                   "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                   "AND sos_active = true " +
                   "AND sos_activated_at IS NOT NULL " +
                   "AND sos_activated_at > NOW() - INTERVAL '2 hours' " +
                   "ORDER BY distance", nativeQuery = true)
    List<User> findActiveSOSUsersNearby(@Param("lat") Double lat, @Param("lng") Double lng);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET sos_active = false, sos_activated_at = NULL " +
                   "WHERE sos_active = true AND sos_activated_at IS NOT NULL " +
                   "AND sos_activated_at < NOW() - INTERVAL '2 hours'", nativeQuery = true)
    int deactivateExpiredSOS();

    java.util.Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);

    @Query(value = "SELECT * FROM users WHERE route IS NOT NULL AND route != '' " +
                   "AND LOWER(route) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "AND id != :excludeUserId " +
                   "AND expo_push_token IS NOT NULL " +
                   "LIMIT 20", nativeQuery = true)
    List<User> findUsersWithMatchingRoute(@Param("keyword") String keyword, @Param("excludeUserId") Long excludeUserId);
}
