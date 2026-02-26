package jobforj.social.repository;

import jobforj.social.model.Post;
import jobforj.social.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUserOrderByCreatedAtDesc(User user);

    List<Post> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findPostsByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user IN :users AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findPostsByUsers(@Param("users") List<User> users, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user IN "
            + "(SELECT s.following FROM Subscription s WHERE s.follower.id = :userId) "
            + "AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> getFeedForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user IN "
            + "(SELECT s.following FROM Subscription s WHERE s.follower = :user) "
            + "AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> getFeedForUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.isDeleted = false")
    long countActivePostsByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.isDeleted = true WHERE p.id = :postId")
    void softDelete(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.isDeleted = true WHERE p.user.id = :userId")
    void softDeleteAllByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Post p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.isDeleted = false")
    Page<Post> findPostsInDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    Pageable pageable);
}
