package com.roadmate.repository;

import com.roadmate.model.VehiclePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, Long> {
    List<VehiclePhoto> findByUserId(Long userId);
    long countByUserId(Long userId);
}
