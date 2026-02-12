package job4j.social.repository;

import job4j.social.model.Message;
import job4j.social.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findBySenderOrderByCreatedAtDesc(User sender, Pageable pageable);

    Page<Message> findByReceiverOrderByCreatedAtDesc(User receiver, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR " +
            "(m.sender = :user2 AND m.receiver = :user1) ORDER BY m.createdAt DESC")
    Page<Message> findConversation(@Param("user1") User user1,
                                   @Param("user2") User user2,
                                   Pageable pageable);

    @Query("SELECT m FROM Message m WHERE (m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
            "(m.sender.id = :user2Id AND m.receiver.id = :user1Id) ORDER BY m.createdAt DESC")
    Page<Message> findConversation(@Param("user1Id") Long user1Id,
                                   @Param("user2Id") Long user2Id,
                                   Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false")
    long countUnreadMessages(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver.id = :receiverId " +
            "AND m.sender.id = :senderId AND m.isRead = false")
    int markMessagesAsRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.id IN :messageIds")
    void markMessagesAsRead(@Param("messageIds") List<Long> messageIds);

    @Query("SELECT m FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false " +
            "ORDER BY m.createdAt DESC")
    List<Message> findAllUnreadMessages(@Param("userId") Long userId);
}
