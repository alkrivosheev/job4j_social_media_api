package jobforj.social.repository;

import jobforj.social.model.Subscription;
import jobforj.social.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscriptionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private SubscriptionRepository subscriptionRepository;

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
        subscriptionRepository.deleteAll();
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
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    void whenFindByFollowerAndFollowingThenReturnSubscription() {
        Subscription subscription = Subscription.builder()
                .follower(user1)
                .following(user2)
                .build();
        subscriptionRepository.save(subscription);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<Subscription> found = subscriptionRepository.findByFollowerAndFollowing(user1, user2);

        assertThat(found).isPresent();
        assertThat(found.get().getFollower().getId()).isEqualTo(user1.getId());
        assertThat(found.get().getFollowing().getId()).isEqualTo(user2.getId());
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void whenFindByFollowerAndFollowingNotFoundThenReturnEmpty() {
        Optional<Subscription> found = subscriptionRepository.findByFollowerAndFollowing(user1, user2);

        assertThat(found).isEmpty();
    }

    @Test
    void whenFindByUserIdsThenReturnSubscription() {
        Subscription subscription = Subscription.builder()
                .follower(user1)
                .following(user2)
                .build();
        subscriptionRepository.save(subscription);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<Subscription> found = subscriptionRepository.findByUserIds(
                Long.valueOf(user1.getId()),
                Long.valueOf(user2.getId()));

        assertThat(found).isPresent();
        assertThat(found.get().getFollower().getId()).isEqualTo(user1.getId());
        assertThat(found.get().getFollowing().getId()).isEqualTo(user2.getId());
    }

    @Test
    void whenFindByUserIdsNotFoundThenReturnEmpty() {
        Optional<Subscription> found = subscriptionRepository.findByUserIds(
                Long.valueOf(user1.getId()),
                Long.valueOf(user2.getId()));

        assertThat(found).isEmpty();
    }

    @Test
    void whenFindByFollowerThenReturnSubscriptions() {
        Subscription sub1 = Subscription.builder().follower(user1).following(user2).build();
        Subscription sub2 = Subscription.builder().follower(user1).following(user3).build();
        Subscription sub3 = Subscription.builder().follower(user2).following(user1).build();

        subscriptionRepository.saveAll(List.of(sub1, sub2, sub3));
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Subscription> result = subscriptionRepository.findByFollower(user1, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(s -> s.getFollowing().getUsername())
                .containsExactlyInAnyOrder("user2", "user3");
    }

    @Test
    void whenFindByFollowerWithPaginationThenReturnCorrectPage() {
        for (int i = 1; i <= 15; i++) {
            User following = User.builder()
                    .username("following" + i)
                    .email("following" + i + "@example.com")
                    .passwordHash("password123")
                    .isActive(true)
                    .build();
            userRepository.save(following);

            Subscription sub = Subscription.builder()
                    .follower(user1)
                    .following(following)
                    .build();
            subscriptionRepository.save(sub);
        }
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable firstPage = PageRequest.of(0, 5);
        Pageable secondPage = PageRequest.of(1, 5);

        Page<Subscription> firstPageResults = subscriptionRepository.findByFollower(user1, firstPage);
        Page<Subscription> secondPageResults = subscriptionRepository.findByFollower(user1, secondPage);

        assertThat(firstPageResults.getContent()).hasSize(5);
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getTotalElements()).isEqualTo(15);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(3);
    }

    @Test
    void whenFindByFollowingThenReturnSubscriptions() {
        Subscription sub1 = Subscription.builder().follower(user2).following(user1).build();
        Subscription sub2 = Subscription.builder().follower(user3).following(user1).build();
        Subscription sub3 = Subscription.builder().follower(user1).following(user2).build();

        subscriptionRepository.saveAll(List.of(sub1, sub2, sub3));
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Subscription> result = subscriptionRepository.findByFollowing(user1, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(s -> s.getFollower().getUsername())
                .containsExactlyInAnyOrder("user2", "user3");
    }

    @Test
    void whenFindFollowedUsersThenReturnUsers() {
        Subscription sub1 = Subscription.builder().follower(user1).following(user2).build();
        Subscription sub2 = Subscription.builder().follower(user1).following(user3).build();
        Subscription sub3 = Subscription.builder().follower(user1).following(user4).build();

        subscriptionRepository.saveAll(List.of(sub1, sub2, sub3));
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = subscriptionRepository.findFollowedUsers(
                Long.valueOf(user1.getId()), pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("user2", "user3", "user4");
    }

    @Test
    void whenFindFollowersThenReturnUsers() {
        Subscription sub1 = Subscription.builder().follower(user2).following(user1).build();
        Subscription sub2 = Subscription.builder().follower(user3).following(user1).build();
        Subscription sub3 = Subscription.builder().follower(user4).following(user1).build();

        subscriptionRepository.saveAll(List.of(sub1, sub2, sub3));
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = subscriptionRepository.findFollowers(
                Long.valueOf(user1.getId()), pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("user2", "user3", "user4");
    }

    @Test
    void whenCountFollowingThenReturnCorrectNumber() {
        Subscription sub1 = Subscription.builder().follower(user1).following(user2).build();
        Subscription sub2 = Subscription.builder().follower(user1).following(user3).build();
        Subscription sub3 = Subscription.builder().follower(user2).following(user1).build();

        subscriptionRepository.saveAll(List.of(sub1, sub2, sub3));
        testEntityManager.flush();
        testEntityManager.clear();

        long count = subscriptionRepository.countFollowing(Long.valueOf(user1.getId()));
        long countForUser2 = subscriptionRepository.countFollowing(Long.valueOf(user2.getId()));

        assertThat(count).isEqualTo(2);
        assertThat(countForUser2).isEqualTo(1);
    }

    @Test
    void whenCountFollowersThenReturnCorrectNumber() {
        Subscription sub1 = Subscription.builder().follower(user2).following(user1).build();
        Subscription sub2 = Subscription.builder().follower(user3).following(user1).build();
        Subscription sub3 = Subscription.builder().follower(user1).following(user2).build();

        subscriptionRepository.saveAll(List.of(sub1, sub2, sub3));
        testEntityManager.flush();
        testEntityManager.clear();

        long count = subscriptionRepository.countFollowers(Long.valueOf(user1.getId()));
        long countForUser2 = subscriptionRepository.countFollowers(Long.valueOf(user2.getId()));

        assertThat(count).isEqualTo(2);
        assertThat(countForUser2).isEqualTo(1);
    }

    @Test
    void whenIsFollowingThenReturnTrue() {
        Subscription subscription = Subscription.builder()
                .follower(user1)
                .following(user2)
                .build();
        subscriptionRepository.save(subscription);
        testEntityManager.flush();
        testEntityManager.clear();

        boolean isFollowing = subscriptionRepository.isFollowing(
                Long.valueOf(user1.getId()),
                Long.valueOf(user2.getId()));
        boolean isNotFollowing = subscriptionRepository.isFollowing(
                Long.valueOf(user1.getId()),
                Long.valueOf(user3.getId()));

        assertThat(isFollowing).isTrue();
        assertThat(isNotFollowing).isFalse();
    }

    @Test
    void whenDeleteSubscriptionThenSubscriptionRemoved() {
        Subscription subscription = Subscription.builder()
                .follower(user1)
                .following(user2)
                .build();
        subscriptionRepository.save(subscription);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<Subscription> found = subscriptionRepository.findByFollowerAndFollowing(user1, user2);
        assertThat(found).isPresent();

        subscriptionRepository.deleteSubscription(user1, user2);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<Subscription> afterDelete = subscriptionRepository.findByFollowerAndFollowing(user1, user2);
        assertThat(afterDelete).isEmpty();
    }

    @Test
    void whenDeleteNonExistentSubscriptionThenNoException() {
        subscriptionRepository.deleteSubscription(user1, user2);
        testEntityManager.flush();

        assertThat(subscriptionRepository.count()).isEqualTo(0);
    }

    @Test
    void whenFindByFollowerWithNoSubscriptionsThenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Subscription> result = subscriptionRepository.findByFollower(user1, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void whenCountFollowingWithNoSubscriptionsThenReturnZero() {
        long count = subscriptionRepository.countFollowing(Long.valueOf(user1.getId()));

        assertThat(count).isEqualTo(0);
    }

    @Test
    void whenCountFollowersWithNoSubscriptionsThenReturnZero() {
        long count = subscriptionRepository.countFollowers(Long.valueOf(user1.getId()));

        assertThat(count).isEqualTo(0);
    }

    @Test
    void whenFindFollowedUsersWithNoSubscriptionsThenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = subscriptionRepository.findFollowedUsers(
                Long.valueOf(user1.getId()), pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void whenFindFollowersWithNoSubscriptionsThenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = subscriptionRepository.findFollowers(
                Long.valueOf(user1.getId()), pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void whenCreateDuplicateSubscriptionThenConstraintViolation() {
        Subscription sub1 = Subscription.builder()
                .follower(user1)
                .following(user2)
                .build();
        subscriptionRepository.save(sub1);
        testEntityManager.flush();

        Subscription sub2 = Subscription.builder()
                .follower(user1)
                .following(user2)
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> subscriptionRepository.saveAndFlush(sub2)
        );
    }
}