package com.roadmate.service;

import com.roadmate.dto.ActivityDto;
import com.roadmate.dto.CreateActivityRequest;
import com.roadmate.dto.UpdateActivityRequest;
import com.roadmate.model.Activity;
import com.roadmate.model.Connection;
import com.roadmate.model.User;
import com.roadmate.repository.ActivityRepository;
import com.roadmate.repository.ConnectionRepository;
import com.roadmate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    public List<ActivityDto> getActivities(User currentUser) {
        List<Activity> activities = activityRepository.findActivitiesForUser(currentUser.getId());

        return activities.stream().map(activity -> mapToDto(activity, currentUser)).collect(Collectors.toList());
    }

    public ActivityDto getActivity(Long activityId, User currentUser) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
        return mapToDto(activity, currentUser);
    }

    @Transactional
    public ActivityDto createActivity(User creator, CreateActivityRequest request) {
        Activity activity = Activity.builder()
                .creator(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .date(request.getDate())
                .time(request.getTime())
                .type(request.getType())
                .image(request.getImage())
                .createdAt(LocalDateTime.now())
                .build();

        Activity savedActivity = activityRepository.save(activity);

        // Notify all connected users
        notifyConnectedUsers(creator, savedActivity);

        return mapToDto(savedActivity, creator);
    }

    @Transactional
    public ActivityDto updateActivity(User user, Long activityId, UpdateActivityRequest request) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        if (!activity.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Only the creator can edit this activity");
        }

        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setLocation(request.getLocation());
        activity.setDate(request.getDate());
        activity.setTime(request.getTime());
        activity.setType(request.getType());
        activity.setImage(request.getImage());

        Activity savedActivity = activityRepository.save(activity);
        return mapToDto(savedActivity, user);
    }

    @Transactional
    public void joinActivity(User user, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        if (activity.getParticipants().contains(user)) {
            throw new RuntimeException("You have already joined this activity");
        }

        activity.getParticipants().add(user);
        activityRepository.save(activity);

        // Notify activity creator
        if (!activity.getCreator().getId().equals(user.getId())) {
            notificationService.createNotification(
                    activity.getCreator(),
                    user,
                    "ACTIVITY_JOIN",
                    "New Participant",
                    user.getName() + " joined your activity: " + activity.getTitle(),
                    "{\"activityId\": " + activityId + "}"
            );
        }
    }

    @Transactional
    public void leaveActivity(User user, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        activity.getParticipants().remove(user);
        activityRepository.save(activity);
    }

    @Transactional
    public void cancelActivity(User user, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
        
        if (!activity.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Only the creator can cancel this activity");
        }

        activity.setStatus("CANCELLED");
        activityRepository.save(activity);

        // Notify all participants about cancellation
        for (User participant : activity.getParticipants()) {
            if (!participant.getId().equals(user.getId())) {
                notificationService.createNotification(
                        participant,
                        user,
                        "ACTIVITY_CANCELLED",
                        "Activity Cancelled",
                        user.getName() + " cancelled the activity: " + activity.getTitle(),
                        "{\"activityId\": " + activityId + "}"
                );
            }
        }
    }

    private void notifyConnectedUsers(User creator, Activity activity) {
        List<Connection> connections = connectionRepository.findAllByUserIdAndStatus(
                creator.getId(),
                Connection.ConnectionStatus.ACCEPTED
        );

        for (Connection connection : connections) {
            User otherUser = connection.getUser().getId().equals(creator.getId())
                    ? connection.getConnectedUser()
                    : connection.getUser();

            notificationService.createNotification(
                    otherUser,
                    creator,
                    "NEW_ACTIVITY",
                    "New Activity",
                    creator.getName() + " created a new activity: " + activity.getTitle(),
                    "{\"activityId\": " + activity.getId() + "}"
            );
        }
    }

    private ActivityDto mapToDto(Activity activity, User currentUser) {
        boolean hasJoined = activity.getParticipants().stream()
                .anyMatch(p -> p.getId().equals(currentUser.getId()));

        return ActivityDto.builder()
                .id(activity.getId())
                .title(activity.getTitle())
                .description(activity.getDescription())
                .location(activity.getLocation())
                .date(activity.getDate())
                .time(activity.getTime())
                .type(activity.getType())
                .image(activity.getImage())
                .status(activity.getStatus())
                .createdAt(activity.getCreatedAt())
                .creatorId(activity.getCreator().getId())
                .creatorName(activity.getCreator().getName())
                .creatorImage(activity.getCreator().getProfileImageUrl() != null && !activity.getCreator().getProfileImageUrl().isEmpty() ? activity.getCreator().getProfileImageUrl() : activity.getCreator().getImage())
                .participantCount(activity.getParticipants().size())
                .hasJoined(hasJoined)
                .build();
    }
}
