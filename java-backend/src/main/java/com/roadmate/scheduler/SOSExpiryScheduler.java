package com.roadmate.scheduler;

import com.roadmate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SOSExpiryScheduler {

    @Autowired
    private UserRepository userRepository;

    @Scheduled(fixedRate = 900000) // Every 15 minutes
    public void deactivateExpiredSOS() {
        int count = userRepository.deactivateExpiredSOS();
        if (count > 0) {
            System.out.println("Auto-expired " + count + " SOS alert(s).");
        }
    }
}
