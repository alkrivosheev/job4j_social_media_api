package jobforj.social.repository;

import jobforj.social.model.Friendship;
import jobforj.social.model.User;
import jobforj.social.model.Friendship.FriendshipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FriendshipRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @PersistenceContext
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private User user3;
    private User user4;
    private Friendship friendship1;
    private Friendship friendship2;
    private Friendship friendship3;
    private Friendship friendship4;
    private Friendship friendship5;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "2");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "2000");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeEach
    void setUp() {
        friendshipRepository.deleteAll();
        userRepository.deleteAll();

        user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        user3 = User.builder()
                .username("user3")
                .email("user3@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        user4 = User.builder()
                .username("user4")
                .email("user4@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        userRepository.saveAll(List.of(user1, user2, user3, user4));

        friendship1 = Friendship.builder()
                .requester(user1)
                .addressee(user2)
                .status(FriendshipStatus.PENDING)
                .build();

        friendship2 = Friendship.builder()
                .requester(user2)
                .addressee(user1)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        friendship3 = Friendship.builder()
                .requester(user1)
                .addressee(user3)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        friendship4 = Friendship.builder()
                .requester(user3)
                .addressee(user2)
                .status(FriendshipStatus.PENDING)
                .build();

        friendship5 = Friendship.builder()
                .requester(user4)
                .addressee(user1)
                .status(FriendshipStatus.REJECTED)
                .build();

        friendshipRepository.saveAll(List.of(friendship1, friendship2, friendship3, friendship4, friendship5));
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    void whenFindByRequesterAndAddresseeThenReturnFriendship() {
        Optional<Friendship> found = friendshipRepository.findByRequesterAndAddressee(user1, user2);

        assertThat(found).isPresent();
        assertThat(found.get().getRequester().getId()).isEqualTo(user1.getId());
        assertThat(found.get().getAddressee().getId()).isEqualTo(user2.getId());
        assertThat(found.get().getStatus()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void whenFindByRequesterAndAddresseeNotFoundThenReturnEmpty() {
        Optional<Friendship> found = friendshipRepository.findByRequesterAndAddressee(user2, user3);

        assertThat(found).isEmpty();
    }

    @Test
    void whenFindByUserIdsThenReturnFriendship() {
        Optional<Friendship> found = friendshipRepository.findByUserIds(
                Long.valueOf(user1.getId()),
                Long.valueOf(user2.getId()));

        assertThat(found).isPresent();
        assertThat(found.get().getRequester().getId()).isEqualTo(user1.getId());
        assertThat(found.get().getAddressee().getId()).isEqualTo(user2.getId());
    }

    @Test
    void whenFindByUserIdsNotFoundThenReturnEmpty() {
        Optional<Friendship> found = friendshipRepository.findByUserIds(
                Long.valueOf(user2.getId()),
                Long.valueOf(user3.getId()));

        assertThat(found).isEmpty();
    }

    @Test
    void whenFindByRequesterAndStatusThenReturnFriendships() {
        List<Friendship> pendingRequests = friendshipRepository.findByRequesterAndStatus(
                user1, FriendshipStatus.PENDING);
        List<Friendship> acceptedRequests = friendshipRepository.findByRequesterAndStatus(
                user1, FriendshipStatus.ACCEPTED);
        List<Friendship> rejectedRequests = friendshipRepository.findByRequesterAndStatus(
                user1, FriendshipStatus.REJECTED);

        assertThat(pendingRequests).hasSize(1);
        assertThat(pendingRequests.get(0).getAddressee().getId()).isEqualTo(user2.getId());

        assertThat(acceptedRequests).hasSize(1);
        assertThat(acceptedRequests.get(0).getAddressee().getId()).isEqualTo(user3.getId());

        assertThat(rejectedRequests).isEmpty();
    }

    @Test
    void whenFindByAddresseeAndStatusThenReturnFriendships() {
        List<Friendship> pendingRequests = friendshipRepository.findByAddresseeAndStatus(
                user2, FriendshipStatus.PENDING);
        List<Friendship> acceptedRequests = friendshipRepository.findByAddresseeAndStatus(
                user1, FriendshipStatus.ACCEPTED);
        List<Friendship> rejectedRequests = friendshipRepository.findByAddresseeAndStatus(
                user1, FriendshipStatus.REJECTED);

        assertThat(pendingRequests).hasSize(2);
        assertThat(pendingRequests)
                .extracting(f -> f.getRequester().getUsername())
                .containsExactlyInAnyOrder("user1", "user3");

        assertThat(acceptedRequests).hasSize(1);
        assertThat(acceptedRequests.get(0).getRequester().getId()).isEqualTo(user2.getId());

        assertThat(rejectedRequests).hasSize(1);
        assertThat(rejectedRequests.get(0).getRequester().getId()).isEqualTo(user4.getId());
    }

    @Test
    void whenFindSentRequestsByStatusThenReturnPage() {
        createMultipleFriendshipsForUser(user1, 15, FriendshipStatus.PENDING);
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable firstPage = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Pageable secondPage = PageRequest.of(1, 5, Sort.by("createdAt").descending());

        Page<Friendship> firstPageResults = friendshipRepository.findSentRequestsByStatus(
                Long.valueOf(user1.getId()), FriendshipStatus.PENDING, firstPage);
        Page<Friendship> secondPageResults = friendshipRepository.findSentRequestsByStatus(
                Long.valueOf(user1.getId()), FriendshipStatus.PENDING, secondPage);

        assertThat(firstPageResults.getContent()).hasSize(5);
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getTotalElements()).isEqualTo(16);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(4);
    }

    @Test
    void whenFindSentRequestsByStatusWithNoRequestsThenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Friendship> results = friendshipRepository.findSentRequestsByStatus(
                Long.valueOf(user4.getId()), FriendshipStatus.ACCEPTED, pageable);

        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isEqualTo(0);
    }

    @Test
    void whenFindReceivedRequestsByStatusThenReturnPage() {
        friendshipRepository.deleteAll(
                friendshipRepository.findByAddresseeAndStatus(user1, FriendshipStatus.PENDING)
        );
        testEntityManager.flush();

        createMultipleFriendshipsForReceiver(user1, 15, FriendshipStatus.PENDING);
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable firstPage = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Pageable secondPage = PageRequest.of(1, 5, Sort.by("createdAt").descending());

        Page<Friendship> firstPageResults = friendshipRepository.findReceivedRequestsByStatus(
                Long.valueOf(user1.getId()), FriendshipStatus.PENDING, firstPage);
        Page<Friendship> secondPageResults = friendshipRepository.findReceivedRequestsByStatus(
                Long.valueOf(user1.getId()), FriendshipStatus.PENDING, secondPage);

        assertThat(firstPageResults.getContent()).hasSize(5);
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getTotalElements()).isEqualTo(15);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(3);
    }

    @Test
    void whenAreFriendsThenReturnTrue() {
        boolean areFriends = friendshipRepository.areFriends(user1, user2);
        boolean areFriendsReverse = friendshipRepository.areFriends(user2, user1);
        boolean areNotFriends = friendshipRepository.areFriends(user1, user4);

        assertThat(areFriends).isTrue();
        assertThat(areFriendsReverse).isTrue();
        assertThat(areNotFriends).isFalse();
    }

    @Test
    void whenAreFriendsWithDifferentStatusesThenReturnCorrectResults() {
        boolean areFriendsAccepted = friendshipRepository.areFriends(user1, user2);
        assertThat(areFriendsAccepted).isTrue();

        boolean areFriendsPending = friendshipRepository.areFriends(user2, user3);
        assertThat(areFriendsPending).isFalse();

        boolean areFriendsRejected = friendshipRepository.areFriends(user1, user4);
        assertThat(areFriendsRejected).isFalse();

        boolean areFriendsNoRecord = friendshipRepository.areFriends(user3, user4);
        assertThat(areFriendsNoRecord).isFalse();
    }

    @Test
    void whenFindPendingRequestsFromUsersWithNoRequestsThenReturnEmptyList() {
        List<User> pendingRequesters = friendshipRepository.findPendingRequestsFromUsers(user4);

        assertThat(pendingRequesters).isEmpty();
    }

    @Test
    void whenDeleteFriendshipBetweenUsersThenRemoveFriendship() {
        boolean areFriends = friendshipRepository.areFriends(user1, user2);
        assertThat(areFriends).isTrue();

        friendshipRepository.deleteFriendshipBetweenUsers(user1, user2);
        testEntityManager.flush();
        testEntityManager.clear();

        boolean areFriendsAfterDelete = friendshipRepository.areFriends(user1, user2);
        assertThat(areFriendsAfterDelete).isFalse();

        Optional<Friendship> friendship = friendshipRepository.findByRequesterAndAddressee(user1, user2);
        assertThat(friendship).isEmpty();
    }

    @Test
    void whenDeleteFriendshipBetweenUsersReverseOrderThenRemoveFriendship() {
        boolean areFriends = friendshipRepository.areFriends(user1, user2);
        assertThat(areFriends).isTrue();

        friendshipRepository.deleteFriendshipBetweenUsers(user2, user1);
        testEntityManager.flush();
        testEntityManager.clear();

        boolean areFriendsAfterDelete = friendshipRepository.areFriends(user1, user2);
        assertThat(areFriendsAfterDelete).isFalse();
    }

    @Test
    void whenDeleteFriendshipBetweenNonExistentUsersThenNoException() {
        long initialCount = friendshipRepository.count();

        friendshipRepository.deleteFriendshipBetweenUsers(user3, user4);
        testEntityManager.flush();

        assertThat(friendshipRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void whenCreateDuplicateFriendshipThenConstraintViolation() {
        Friendship duplicate = Friendship.builder()
                .requester(user1)
                .addressee(user2)
                .status(FriendshipStatus.PENDING)
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> friendshipRepository.saveAndFlush(duplicate)
        );
    }

    @Test
    void whenUpdateFriendshipStatusThenChangesArePersisted() {
        Friendship friendship = friendshipRepository.findByRequesterAndAddressee(user1, user2).orElse(null);
        assertThat(friendship).isNotNull();
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        testEntityManager.flush();
        testEntityManager.clear();

        Friendship updatedFriendship = friendshipRepository.findByRequesterAndAddressee(user1, user2).orElse(null);
        assertThat(updatedFriendship).isNotNull();
        assertThat(updatedFriendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
        assertThat(updatedFriendship.getUpdatedAt()).isAfter(updatedFriendship.getCreatedAt());
    }

    @Test
    void whenFindByRequesterAndStatusWithMultipleStatusesThenReturnCorrect() {
        List<Friendship> user1Pending = friendshipRepository.findByRequesterAndStatus(
                user1, FriendshipStatus.PENDING);
        List<Friendship> user1Accepted = friendshipRepository.findByRequesterAndStatus(
                user1, FriendshipStatus.ACCEPTED);
        List<Friendship> user1Rejected = friendshipRepository.findByRequesterAndStatus(
                user1, FriendshipStatus.REJECTED);

        assertThat(user1Pending).hasSize(1);
        assertThat(user1Accepted).hasSize(1);
        assertThat(user1Rejected).isEmpty();
    }

    @Test
    void whenFindByAddresseeAndStatusWithMultipleStatusesThenReturnCorrect() {
        List<Friendship> user1ReceivedPending = friendshipRepository.findByAddresseeAndStatus(
                user1, FriendshipStatus.PENDING);
        List<Friendship> user1ReceivedAccepted = friendshipRepository.findByAddresseeAndStatus(
                user1, FriendshipStatus.ACCEPTED);
        List<Friendship> user1ReceivedRejected = friendshipRepository.findByAddresseeAndStatus(
                user1, FriendshipStatus.REJECTED);

        assertThat(user1ReceivedPending).isEmpty();
        assertThat(user1ReceivedAccepted).hasSize(1);
        assertThat(user1ReceivedAccepted.get(0).getRequester().getId()).isEqualTo(user2.getId());
        assertThat(user1ReceivedRejected).hasSize(1);
        assertThat(user1ReceivedRejected.get(0).getRequester().getId()).isEqualTo(user4.getId());
    }

    @Test
    void whenCountFriendshipsThenReturnCorrectNumber() {
        long count = friendshipRepository.count();
        assertThat(count).isEqualTo(5);
    }

    @Test
    void whenCreateFriendshipWithBuilderDefaultThenStatusIsPending() {
        Friendship newFriendship = Friendship.builder()
                .requester(user2)
                .addressee(user3)
                .build();

        friendshipRepository.save(newFriendship);
        testEntityManager.flush();

        assertThat(newFriendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(newFriendship.getCreatedAt()).isNotNull();
        assertThat(newFriendship.getUpdatedAt()).isNotNull();
    }

    @Test
    void whenFindByUserIdsWithRejectedStatusThenReturnFriendship() {
        Optional<Friendship> found = friendshipRepository.findByUserIds(
                Long.valueOf(user4.getId()),
                Long.valueOf(user1.getId()));

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(FriendshipStatus.REJECTED);
    }

    private void createMultipleFriendshipsForUser(User requester, int count, FriendshipStatus status) {
        IntStream.rangeClosed(1, count).forEach(i -> {
            User newUser = User.builder()
                    .username("tempuser" + i + "_" + System.currentTimeMillis())
                    .email("tempuser" + i + "@example.com")
                    .passwordHash("password123")
                    .isActive(true)
                    .build();
            userRepository.save(newUser);

            Friendship friendship = Friendship.builder()
                    .requester(requester)
                    .addressee(newUser)
                    .status(status)
                    .build();
            friendshipRepository.save(friendship);
        });
    }

    private void createMultipleFriendshipsForReceiver(User receiver, int count, FriendshipStatus status) {
        IntStream.rangeClosed(1, count).forEach(i -> {
            User newUser = User.builder()
                    .username("tempuser" + i + "_" + System.currentTimeMillis())
                    .email("tempuser" + i + "@example.com")
                    .passwordHash("password123")
                    .isActive(true)
                    .build();
            userRepository.save(newUser);

            Friendship friendship = Friendship.builder()
                    .requester(newUser)
                    .addressee(receiver)
                    .status(status)
                    .build();
            friendshipRepository.save(friendship);
        });
    }
}