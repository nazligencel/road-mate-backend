package com.roadmate.repository;

import com.roadmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query(value = "SELECT *, (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
                   "cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * " +
                   "sin(radians(latitude)))) AS distance FROM users " +
                   "WHERE latitude IS NOT NULL AND longitude IS NOT NULL " +
                   "ORDER BY distance LIMIT 20", nativeQuery = true)
    List<User> findNearbyNomads(@Param("lat") Double lat, @Param("lng") Double lng);
}
