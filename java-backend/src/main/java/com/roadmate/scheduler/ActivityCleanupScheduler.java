package com.roadmate.scheduler;

import com.roadmate.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ActivityCleanupScheduler {

    @Autowired
    private ActivityRepository activityRepository;

    // Run every 6 hours to clean up activities older than 1 week past their date
    @Scheduled(fixedRate = 21600000)
    @Transactional
    public void deleteExpiredActivities() {
        // First delete participant associations, then the activities
        activityRepository.deleteOldActivityParticipants();
        int count = activityRepository.deleteOldActivities();
        if (count > 0) {
            System.out.println("Cleaned up " + count + " expired activity/activities (older than 1 week).");
        }
    }
}
