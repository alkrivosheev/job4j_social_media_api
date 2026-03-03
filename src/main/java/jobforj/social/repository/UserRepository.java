package jobforj.social.repository;


import jobforj.social.model.User;
import jobforj.social.model.Post;
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
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Получает все посты подписчиков пользователя (тех, на кого подписан пользователь)
     * с сортировкой от новых к старым и пагинацией
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница с постами подписчиков
     */
    @Query("""
            SELECT p FROM Post p 
            WHERE p.user.id IN (
                SELECT s.following.id FROM Subscription s 
                WHERE s.follower.id = :userId
            )
            AND p.isDeleted = false
            ORDER BY p.createdAt DESC
            """)
    Page<Post> findPostsByFollowing(@Param("userId") Long userId, Pageable pageable);


    /**
     * Находит всех друзей пользователя (взаимные подписки/дружба)
     * Друзьями считаются пользователи, которые подписаны друг на друга
     * @param userId идентификатор пользователя
     * @return список друзей
     */
    @Query("""
            SELECT DISTINCT u FROM User u 
            WHERE u.id IN (
                SELECT s1.follower.id FROM Subscription s1 
                WHERE s1.following.id = :userId
            )
            AND u.id IN (
                SELECT s2.following.id FROM Subscription s2 
                WHERE s2.follower.id = :userId
            )
            """)
    List<User> findFriendsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT user FROM User AS user
            WHERE user.username = :username
                        AND user.passwordHash = :passwordHash
            """)
    Optional<User> findByUsernameAndPasswordHash(@Param("username") String username, @Param("passwordHash") String passwordHash);

    @Query("SELECT user FROM User user JOIN user.following f WHERE f.following.id = :userId")
    List<User> findFollowersByUserId(@Param("userId") Long userId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isActive = false WHERE u.id = :userId")
    void deactivateUser(@Param("userId") Long userId);
}