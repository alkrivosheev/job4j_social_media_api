package job4j.social.repository;

import job4j.social.model.Subscription;
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
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByFollowerAndFollowing(User follower, User following);

    @Query("SELECT s FROM Subscription s WHERE s.follower.id = :followerId AND s.following.id = :followingId")
    Optional<Subscription> findByUserIds(@Param("followerId") Long followerId,
                                         @Param("followingId") Long followingId);

    Page<Subscription> findByFollower(User follower, Pageable pageable);

    Page<Subscription> findByFollowing(User following, Pageable pageable);

    @Query("SELECT s.following FROM Subscription s WHERE s.follower.id = :followerId")
    Page<User> findFollowedUsers(@Param("followerId") Long followerId, Pageable pageable);

    @Query("SELECT s.follower FROM Subscription s WHERE s.following.id = :followingId")
    Page<User> findFollowers(@Param("followingId") Long followingId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.follower.id = :userId")
    long countFollowing(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.following.id = :userId")
    long countFollowers(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscription s " +
            "WHERE s.follower.id = :followerId AND s.following.id = :followingId")
    boolean isFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Subscription s WHERE s.follower = :follower AND s.following = :following")
    void deleteSubscription(@Param("follower") User follower, @Param("following") User following);
}
