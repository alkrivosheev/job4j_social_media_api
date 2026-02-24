package jobforj.social.repository;

import jobforj.social.model.Friendship;
import jobforj.social.model.User;
import jobforj.social.model.Friendship.FriendshipStatus;
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
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByRequesterAndAddressee(User requester, User addressee);

    @Query("SELECT f FROM Friendship f WHERE f.requester.id = :requesterId AND f.addressee.id = :addresseeId")
    Optional<Friendship> findByUserIds(@Param("requesterId") Long requesterId,
                                       @Param("addresseeId") Long addresseeId);

    List<Friendship> findByRequesterAndStatus(User requester, FriendshipStatus status);

    List<Friendship> findByAddresseeAndStatus(User addressee, FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE f.requester.id = :userId AND f.status = :status")
    Page<Friendship> findSentRequestsByStatus(@Param("userId") Long userId,
                                              @Param("status") FriendshipStatus status,
                                              Pageable pageable);

    @Query("SELECT f FROM Friendship f WHERE f.addressee.id = :userId AND f.status = :status")
    Page<Friendship> findReceivedRequestsByStatus(@Param("userId") Long userId,
                                                  @Param("status") FriendshipStatus status,
                                                  Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f "
            + "WHERE ((f.requester = :user1 AND f.addressee = :user2) OR "
            + "(f.requester = :user2 AND f.addressee = :user1)) AND f.status = 'ACCEPTED'")
    boolean areFriends(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT f.requester FROM Friendship f WHERE f.addressee = :user AND f.status = 'PENDING'")
    List<User> findPendingRequestsFromUsers(@Param("user") User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM Friendship f WHERE (f.requester = :user1 AND f.addressee = :user2) OR "
            + "(f.requester = :user2 AND f.addressee = :user1)")
    void deleteFriendshipBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);
}
