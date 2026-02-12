package com.roadmate.service;

import com.roadmate.dto.ConnectionDTO;
import com.roadmate.exception.BadRequestException;
import com.roadmate.exception.ConflictException;
import com.roadmate.exception.ResourceNotFoundException;
import com.roadmate.exception.UnauthorizedException;
import com.roadmate.model.Connection;
import com.roadmate.model.Connection.ConnectionStatus;
import com.roadmate.model.User;
import com.roadmate.repository.BlockedUserRepository;
import com.roadmate.repository.ConnectionRepository;
import com.roadmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ExpoPushService expoPushService;
    private final BlockedUserRepository blockedUserRepository;

    /**
     * QR tarandığında bağlantı isteği oluştur
     */
    @Transactional
    public ConnectionDTO connectByQR(Long scannerUserId, Long targetUserId) {
        // Kendine bağlantı isteği gönderilemez
        if (scannerUserId.equals(targetUserId)) {
            throw new BadRequestException("Kendinize bağlantı isteği gönderemezsiniz");
        }

        // Tarayan kullanıcıyı bul
        User scanner = userRepository.findById(scannerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", scannerUserId));

        // Hedef kullanıcıyı bul
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Hedef kullanıcı", targetUserId));

        // Zaten bağlantı var mı kontrol et
        Optional<Connection> existingConnection = connectionRepository
                .findConnectionBetweenUsers(scannerUserId, targetUserId);

        if (existingConnection.isPresent()) {
            Connection conn = existingConnection.get();
            if (conn.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new ConflictException("Bu kullanıcıyla zaten bağlısınız");
            } else if (conn.getStatus() == ConnectionStatus.PENDING) {
                // Eğer karşı taraf daha önce istek göndermişse, otomatik kabul et
                if (conn.getUser().getId().equals(targetUserId)) {
                    conn.setStatus(ConnectionStatus.ACCEPTED);
                    connectionRepository.save(conn);
                    return toDTO(conn);
                }
                throw new ConflictException("Zaten bekleyen bir bağlantı isteğiniz var");
            } else if (conn.getStatus() == ConnectionStatus.REJECTED) {
                // Reddedilmiş isteği yeniden gönder
                conn.setStatus(ConnectionStatus.PENDING);
                conn.setUser(scanner);
                conn.setConnectedUser(target);
                connectionRepository.save(conn);
                return toDTO(conn);
            }
        }

        // Yeni bağlantı oluştur
        Connection connection = Connection.builder()
                .user(scanner)
                .connectedUser(target)
                .status(ConnectionStatus.PENDING)
                .build();

        Connection saved = connectionRepository.save(connection);
        return toDTO(saved);
    }

    /**
     * Bağlantı isteğini kabul et
     */
    @Transactional
    public ConnectionDTO acceptConnection(Long connectionId, Long userId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bağlantı", connectionId));

        // Sadece hedef kullanıcı kabul edebilir
        if (!connection.getConnectedUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bu isteği kabul etme yetkiniz yok");
        }

        connection.setStatus(ConnectionStatus.ACCEPTED);
        Connection saved = connectionRepository.save(connection);

        // Send FRIEND_ACCEPTED notification to the requester
        User acceptor = connection.getConnectedUser();
        User requester = connection.getUser();
        notificationService.createNotification(
                requester, acceptor, "FRIEND_ACCEPTED",
                "Friend Request Accepted",
                acceptor.getName() + " accepted your friend request!",
                "{\"senderId\": " + acceptor.getId() + "}"
        );
        // Push notification
        if (requester.getExpoPushToken() != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "FRIEND_ACCEPTED");
            data.put("senderId", acceptor.getId());
            expoPushService.sendBatchPushNotifications(
                    Collections.singletonList(requester.getExpoPushToken()),
                    "Friend Request Accepted",
                    acceptor.getName() + " accepted your friend request!",
                    data
            );
        }

        return toDTO(saved);
    }

    /**
     * Bağlantı isteğini reddet
     */
    @Transactional
    public ConnectionDTO rejectConnection(Long connectionId, Long userId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Bağlantı", connectionId));

        // Sadece hedef kullanıcı reddedebilir
        if (!connection.getConnectedUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bu isteği reddetme yetkiniz yok");
        }

        connection.setStatus(ConnectionStatus.REJECTED);
        Connection saved = connectionRepository.save(connection);
        return toDTO(saved);
    }

    /**
     * Kullanıcının tüm kabul edilmiş bağlantılarını getir
     */
    public List<ConnectionDTO> getMyConnections(Long userId) {
        return connectionRepository
                .findAllByUserIdAndStatus(userId, ConnectionStatus.ACCEPTED)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Kullanıcıya gelen bekleyen istekleri getir
     */
    public List<ConnectionDTO> getPendingRequests(Long userId) {
        return connectionRepository
                .findPendingRequestsForUser(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Kullanıcının bağlantı sayısını getir
     */
    public Long getConnectionCount(Long userId) {
        return connectionRepository.countAcceptedConnections(userId);
    }

    /**
     * Profil/explore'dan arkadaş isteği gönder (bildirimli)
     */
    @Transactional
    public ConnectionDTO sendConnectionRequest(Long senderUserId, Long targetUserId) {
        // Block check
        if (blockedUserRepository.existsBlockBetween(senderUserId, targetUserId)) {
            throw new BadRequestException("Cannot send request to this user");
        }

        ConnectionDTO result = connectByQR(senderUserId, targetUserId);

        // Send FRIEND_REQUEST notification
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", senderUserId));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Hedef kullanıcı", targetUserId));

        // Only send notification if status is PENDING (not auto-accepted)
        if (result.getStatus() == ConnectionStatus.PENDING) {
            notificationService.createNotification(
                    target, sender, "FRIEND_REQUEST",
                    "Friend Request",
                    sender.getName() + " sent you a friend request!",
                    "{\"senderId\": " + sender.getId() + "}"
            );
            if (target.getExpoPushToken() != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("type", "FRIEND_REQUEST");
                data.put("senderId", sender.getId());
                expoPushService.sendBatchPushNotifications(
                        Collections.singletonList(target.getExpoPushToken()),
                        "Friend Request",
                        sender.getName() + " sent you a friend request!",
                        data
                );
            }
        }

        return result;
    }

    /**
     * İki kullanıcı arası bağlantı durumunu kontrol et
     */
    public String getConnectionStatus(Long currentUserId, Long targetUserId) {
        Optional<Connection> connection = connectionRepository
                .findConnectionBetweenUsers(currentUserId, targetUserId);

        if (connection.isEmpty()) {
            return "NONE";
        }

        Connection conn = connection.get();
        if (conn.getStatus() == ConnectionStatus.ACCEPTED) {
            return "FRIENDS";
        }
        if (conn.getStatus() == ConnectionStatus.PENDING) {
            if (conn.getUser().getId().equals(currentUserId)) {
                return "PENDING_SENT";
            } else {
                return "PENDING_RECEIVED";
            }
        }

        return "NONE";
    }

    /**
     * Arkadaşı sil veya bekleyen isteği iptal et
     */
    @Transactional
    public void removeConnection(Long currentUserId, Long targetUserId) {
        Connection connection = connectionRepository
                .findConnectionBetweenUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));

        connectionRepository.delete(connection);
    }

    /**
     * Connection'ı DTO'ya çevir
     */
    private ConnectionDTO toDTO(Connection connection) {
        return ConnectionDTO.builder()
                .id(connection.getId())
                .user(toUserDTO(connection.getUser()))
                .connectedUser(toUserDTO(connection.getConnectedUser()))
                .status(connection.getStatus())
                .createdAt(connection.getCreatedAt())
                .build();
    }

    private ConnectionDTO.UserDTO toUserDTO(User user) {
        return ConnectionDTO.UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .image(user.getImage())
                .vehicle(user.getVehicle())
                .vehicleModel(user.getVehicleModel())
                .status(user.getStatus())
                .route(user.getRoute())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
