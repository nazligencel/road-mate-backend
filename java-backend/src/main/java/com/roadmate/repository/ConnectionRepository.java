package com.roadmate.repository;

import com.roadmate.model.Connection;
import com.roadmate.model.Connection.ConnectionStatus;
import com.roadmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    // İki kullanıcı arasında bağlantı var mı kontrol et (her iki yönde)
    @Query("SELECT c FROM Connection c WHERE " +
           "(c.user.id = :userId1 AND c.connectedUser.id = :userId2) OR " +
           "(c.user.id = :userId2 AND c.connectedUser.id = :userId1)")
    Optional<Connection> findConnectionBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // Kullanıcının tüm bağlantılarını getir (kabul edilmiş)
    @Query("SELECT c FROM Connection c WHERE " +
           "(c.user.id = :userId OR c.connectedUser.id = :userId) " +
           "AND c.status = :status")
    List<Connection> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ConnectionStatus status);

    // Kullanıcıya gelen bekleyen bağlantı isteklerini getir
    @Query("SELECT c FROM Connection c WHERE c.connectedUser.id = :userId AND c.status = 'PENDING'")
    List<Connection> findPendingRequestsForUser(@Param("userId") Long userId);

    // Kullanıcının gönderdiği bekleyen istekleri getir
    @Query("SELECT c FROM Connection c WHERE c.user.id = :userId AND c.status = 'PENDING'")
    List<Connection> findPendingRequestsByUser(@Param("userId") Long userId);

    // Kullanıcının kabul edilmiş bağlantı sayısını getir
    @Query("SELECT COUNT(c) FROM Connection c WHERE " +
           "(c.user.id = :userId OR c.connectedUser.id = :userId) " +
           "AND c.status = 'ACCEPTED'")
    Long countAcceptedConnections(@Param("userId") Long userId);

    // Kullanıcının tüm accepted arkadaşlarının User nesnelerini döndür
    @Query("SELECT CASE WHEN c.user.id = :userId THEN c.connectedUser ELSE c.user END " +
           "FROM Connection c WHERE (c.user.id = :userId OR c.connectedUser.id = :userId) " +
           "AND c.status = 'ACCEPTED'")
    List<User> findAcceptedConnectionUsers(@Param("userId") Long userId);
}
