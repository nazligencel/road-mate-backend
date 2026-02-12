package com.roadmate.repository;

import com.roadmate.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // Get activities created by connected users (friends) + own activities
    @Query("SELECT a FROM Activity a WHERE a.status <> 'CANCELLED' AND (a.creator.id = :userId OR a.creator.id IN " +
           "(SELECT c.connectedUser.id FROM Connection c WHERE c.user.id = :userId AND c.status = 'ACCEPTED') OR " +
           "a.creator.id IN (SELECT c.user.id FROM Connection c WHERE c.connectedUser.id = :userId AND c.status = 'ACCEPTED')) " +
           "ORDER BY a.createdAt DESC")
    List<Activity> findActivitiesForUser(@Param("userId") Long userId);

    // Get all activities (for explore/discover)
    List<Activity> findAllByOrderByCreatedAtDesc();

    // Get activities by creator
    List<Activity> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    // Get activities user has joined
    @Query("SELECT a FROM Activity a JOIN a.participants p WHERE p.id = :userId ORDER BY a.createdAt DESC")
    List<Activity> findJoinedActivities(@Param("userId") Long userId);

    // Delete activities whose date is more than 7 days in the past
    @Modifying
    @Query(value = "DELETE FROM activity_participants WHERE activity_id IN (SELECT id FROM activities WHERE date IS NOT NULL AND CAST(date AS DATE) < CURRENT_DATE - INTERVAL '7 days')", nativeQuery = true)
    int deleteOldActivityParticipants();

    @Modifying
    @Query(value = "DELETE FROM activities WHERE date IS NOT NULL AND CAST(date AS DATE) < CURRENT_DATE - INTERVAL '7 days'", nativeQuery = true)
    int deleteOldActivities();
}
