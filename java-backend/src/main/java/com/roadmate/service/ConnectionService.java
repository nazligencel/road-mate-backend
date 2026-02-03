package com.roadmate.service;

import com.roadmate.dto.ConnectionDTO;
import com.roadmate.model.Connection;
import com.roadmate.model.Connection.ConnectionStatus;
import com.roadmate.model.User;
import com.roadmate.repository.ConnectionRepository;
import com.roadmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;

    /**
     * QR tarandığında bağlantı isteği oluştur
     */
    @Transactional
    public ConnectionDTO connectByQR(Long scannerUserId, Long targetUserId) {
        // Kendine bağlantı isteği gönderilemez
        if (scannerUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Kendinize bağlantı isteği gönderemezsiniz");
        }

        // Tarayan kullanıcıyı bul
        User scanner = userRepository.findById(scannerUserId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + scannerUserId));

        // Hedef kullanıcıyı bul
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Hedef kullanıcı bulunamadı: " + targetUserId));

        // Zaten bağlantı var mı kontrol et
        Optional<Connection> existingConnection = connectionRepository
                .findConnectionBetweenUsers(scannerUserId, targetUserId);

        if (existingConnection.isPresent()) {
            Connection conn = existingConnection.get();
            if (conn.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new IllegalStateException("Bu kullanıcıyla zaten bağlısınız");
            } else if (conn.getStatus() == ConnectionStatus.PENDING) {
                // Eğer karşı taraf daha önce istek göndermişse, otomatik kabul et
                if (conn.getUser().getId().equals(targetUserId)) {
                    conn.setStatus(ConnectionStatus.ACCEPTED);
                    connectionRepository.save(conn);
                    return toDTO(conn);
                }
                throw new IllegalStateException("Zaten bekleyen bir bağlantı isteğiniz var");
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
                .orElseThrow(() -> new RuntimeException("Bağlantı bulunamadı"));

        // Sadece hedef kullanıcı kabul edebilir
        if (!connection.getConnectedUser().getId().equals(userId)) {
            throw new IllegalStateException("Bu isteği kabul etme yetkiniz yok");
        }

        connection.setStatus(ConnectionStatus.ACCEPTED);
        Connection saved = connectionRepository.save(connection);
        return toDTO(saved);
    }

    /**
     * Bağlantı isteğini reddet
     */
    @Transactional
    public ConnectionDTO rejectConnection(Long connectionId, Long userId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("Bağlantı bulunamadı"));

        // Sadece hedef kullanıcı reddedebilir
        if (!connection.getConnectedUser().getId().equals(userId)) {
            throw new IllegalStateException("Bu isteği reddetme yetkiniz yok");
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
                .build();
    }
}
