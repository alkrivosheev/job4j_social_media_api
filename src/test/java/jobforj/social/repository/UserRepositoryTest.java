package jobforj.social.repository;

import jobforj.social.model.Post;
import jobforj.social.model.User;
import jobforj.social.model.Subscription;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @PersistenceContext
    private EntityManager entityManager;

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
        userRepository.deleteAll();
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    void whenFindByUsernameThenReturnUser() {
        User user = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .passwordHash("$2a$10$NVM0n8ElaRgg7zO1Y8vS7eF7e7e7e7e7e7e7e7e7e7e7e7e7e7e")
                .isActive(true)
                .build();
        userRepository.save(user);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> foundUser = userRepository.findByUsername("john_doe");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("john_doe");
        assertThat(foundUser.get().getEmail()).isEqualTo("john@example.com");
        assertThat(foundUser.get().getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(foundUser.get().getIsActive()).isTrue();
        assertThat(foundUser.get().getCreatedAt()).isNotNull();
    }

    @Test
    void whenFindByUsernameNotFoundThenReturnEmpty() {
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void whenFindByEmailThenReturnUser() {
        User user = User.builder()
                .username("jane_doe")
                .email("jane@example.com")
                .passwordHash("$2a$10$NVM0n8ElaRgg7zO1Y8vS7eF7e7e7e7e7e7e7e7e7e7e7e7e7e7e")
                .isActive(true)
                .build();
        userRepository.save(user);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> foundUser = userRepository.findByEmail("jane@example.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("jane_doe");
        assertThat(foundUser.get().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void whenFindByEmailNotFoundThenReturnEmpty() {
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void whenExistsByUsernameThenReturnTrue() {
        User user = User.builder()
                .username("existing_user")
                .email("existing@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.save(user);
        testEntityManager.flush();
        testEntityManager.clear();

        boolean exists = userRepository.existsByUsername("existing_user");
        boolean notExists = userRepository.existsByUsername("non_existing_user");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void whenExistsByEmailThenReturnTrue() {
        User user = User.builder()
                .username("user_with_email")
                .email("test@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.save(user);
        testEntityManager.flush();
        testEntityManager.clear();

        boolean exists = userRepository.existsByEmail("test@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void whenSearchUsersByUsernameKeywordThenReturnMatchingUsers() {
        User user1 = User.builder()
                .username("john_smith")
                .email("john@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User user2 = User.builder()
                .username("jane_smith")
                .email("jane@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User user3 = User.builder()
                .username("bob_johnson")
                .email("bob@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        userRepository.saveAll(List.of(user1, user2, user3));
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> searchResults = userRepository.searchUsers("smith", pageable);

        assertThat(searchResults.getContent()).hasSize(2);
        assertThat(searchResults.getContent())
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("john_smith", "jane_smith");
    }

    @Test
    void whenSearchUsersByEmailKeywordThenReturnMatchingUsers() {
        User user1 = User.builder()
                .username("user1")
                .email("john.test@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User user2 = User.builder()
                .username("user2")
                .email("jane.test@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User user3 = User.builder()
                .username("user3")
                .email("bob.other@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        userRepository.saveAll(List.of(user1, user2, user3));
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> searchResults = userRepository.searchUsers("test", pageable);

        assertThat(searchResults.getContent()).hasSize(2);
        assertThat(searchResults.getContent())
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("john.test@example.com", "jane.test@example.com");
    }

    @Test
    void whenSearchUsersWithPaginationThenReturnCorrectPage() {
        for (int i = 1; i <= 15; i++) {
            User user = User.builder()
                    .username("user" + i)
                    .email("user" + i + "@example.com")
                    .passwordHash("password" + i)
                    .isActive(true)
                    .build();
            userRepository.save(user);
        }
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable firstPage = PageRequest.of(0, 5);
        Pageable secondPage = PageRequest.of(1, 5);

        Page<User> firstPageResults = userRepository.searchUsers("user", firstPage);
        Page<User> secondPageResults = userRepository.searchUsers("user", secondPage);

        assertThat(firstPageResults.getContent()).hasSize(5);
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getTotalElements()).isEqualTo(15);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(3);
    }

    @Test
    void whenFindAllActiveUsersThenReturnOnlyActiveUsers() {
        User activeUser1 = User.builder()
                .username("active1")
                .email("active1@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User activeUser2 = User.builder()
                .username("active2")
                .email("active2@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User inactiveUser = User.builder()
                .username("inactive")
                .email("inactive@example.com")
                .passwordHash("password123")
                .isActive(false)
                .build();

        userRepository.saveAll(List.of(activeUser1, activeUser2, inactiveUser));
        testEntityManager.flush();
        testEntityManager.clear();

        List<User> activeUsers = userRepository.findAllActiveUsers();

        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("active1", "active2");
        assertThat(activeUsers)
                .allMatch(User::getIsActive);
    }

    @Test
    void whenFindUsersCreatedAfterDateThenReturnCorrectUsers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime twoDaysAgo = now.minusDays(2);

        // Создаем пользователя с двухдневной давности
        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        entityManager.persist(user1);
        entityManager.flush();

        entityManager.createNativeQuery("UPDATE users SET created_at = ? WHERE id = ?")
                .setParameter(1, twoDaysAgo)
                .setParameter(2, user1.getId())
                .executeUpdate();

        // Создаем пользователя с вчерашней давности
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        entityManager.persist(user2);
        entityManager.flush();

        entityManager.createNativeQuery("UPDATE users SET created_at = ? WHERE id = ?")
                .setParameter(1, yesterday)
                .setParameter(2, user2.getId())
                .executeUpdate();

        // Создаем пользователя с текущей датой
        User user3 = User.builder()
                .username("user3")
                .email("user3@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        entityManager.persist(user3);
        entityManager.flush();

        entityManager.clear();

        List<User> recentUsers = userRepository.findUsersCreatedAfter(yesterday.minusHours(1));

        assertThat(recentUsers).hasSize(2);
        assertThat(recentUsers)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("user2", "user3");
    }

    @Test
    void whenDeactivateUserThenUserIsInactive() {
        User user = User.builder()
                .username("to_deactivate")
                .email("deactivate@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        testEntityManager.flush();
        assertThat(savedUser.getIsActive()).isTrue();

        userRepository.deactivateUser(Long.valueOf(savedUser.getId()));
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> deactivatedUser = userRepository.findById(Long.valueOf(savedUser.getId()));
        assertThat(deactivatedUser).isPresent();
        assertThat(deactivatedUser.get().getIsActive()).isFalse();
    }

    @Test
    void whenDeactivateNonExistentUserThenNoException() {
        userRepository.deactivateUser(999L);
        testEntityManager.flush();

        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    void whenSaveUserWithHelperMethodsThenRelationshipsWork() {
        User user = User.builder()
                .username("helper_test")
                .email("helper@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        userRepository.save(user);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> foundUser = userRepository.findByUsername("helper_test");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getPosts()).isEmpty();
        assertThat(foundUser.get().getFollowers()).isEmpty();
        assertThat(foundUser.get().getFollowing()).isEmpty();
        assertThat(foundUser.get().getSentFriendRequests()).isEmpty();
        assertThat(foundUser.get().getReceivedFriendRequests()).isEmpty();
        assertThat(foundUser.get().getSentMessages()).isEmpty();
        assertThat(foundUser.get().getReceivedMessages()).isEmpty();
    }

    @Test
    void whenFindByUsernameAndPasswordHashWithValidCredentialsThenReturnUser() {
        String username = "john_doe";
        String passwordHash = "$2a$10$NVM0n8ElaRgg7zO1Y8vS7eF7e7e7e7e7e7e7e7e7e7e7e7e7e7e";

        User user = User.builder()
                .username(username)
                .email("john@example.com")
                .passwordHash(passwordHash)
                .isActive(true)
                .build();

        userRepository.save(user);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> foundUser = userRepository.findByUsernameAndPasswordHash(username, passwordHash);

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo(username);
        assertThat(foundUser.get().getPasswordHash()).isEqualTo(passwordHash);
        assertThat(foundUser.get().getEmail()).isEqualTo("john@example.com");
        assertThat(foundUser.get().getIsActive()).isTrue();
        assertThat(foundUser.get().getCreatedAt()).isNotNull();
    }

    @Test
    void whenFindByUsernameAndPasswordHashWithInvalidCredentialsThenReturnEmpty() {
        String username = "jane_doe";
        String correctPasswordHash = "$2a$10$correctHashValue12345678901234567890";
        String wrongPasswordHash = "$2a$10$wrongHashValue98765432109876543210";

        User user = User.builder()
                .username(username)
                .email("jane@example.com")
                .passwordHash(correctPasswordHash)
                .isActive(true)
                .build();

        userRepository.save(user);
        testEntityManager.flush();
        testEntityManager.clear();

        Optional<User> foundWithWrongPassword = userRepository.findByUsernameAndPasswordHash(
                username,
                wrongPasswordHash
        );

        Optional<User> foundWithWrongUsername = userRepository.findByUsernameAndPasswordHash(
                "wrong_username",
                correctPasswordHash
        );

        Optional<User> foundWithBothWrong = userRepository.findByUsernameAndPasswordHash(
                "wrong_username",
                wrongPasswordHash
        );
        assertThat(foundWithWrongPassword).isEmpty();
        assertThat(foundWithWrongUsername).isEmpty();
        assertThat(foundWithBothWrong).isEmpty();
        Optional<User> existingUser = userRepository.findByUsername(username);
        assertThat(existingUser).isPresent();
        assertThat(existingUser.get().getPasswordHash()).isEqualTo(correctPasswordHash);
    }

    @Test
    void whenFindFollowersByUserIdThenReturnAllFollowers() {
        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User user3 = User.builder()
                .username("user3")
                .email("user3@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        userRepository.saveAll(List.of(user1, user2, user3));
        testEntityManager.flush();

        Subscription subscription1 = Subscription.builder()
                .follower(user2)
                .following(user1)
                .build();

        Subscription subscription2 = Subscription.builder()
                .follower(user3)
                .following(user1)
                .build();

        entityManager.persist(subscription1);
        entityManager.persist(subscription2);
        testEntityManager.flush();
        testEntityManager.clear();

        List<User> followers = userRepository.findFollowersByUserId(Long.valueOf(user1.getId()));

        assertThat(followers).hasSize(2);
        assertThat(followers)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("user2", "user3");
        assertThat(followers)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("user2@example.com", "user3@example.com");
        assertThat(followers)
                .allMatch(User::getIsActive);
    }

    @Test
    void whenFindFollowersByUserIdWithNoFollowersThenReturnEmptyList() {
        User user = User.builder()
                .username("user_without_followers")
                .email("alone@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        userRepository.save(user);
        testEntityManager.flush();
        testEntityManager.clear();

        List<User> followers = userRepository.findFollowersByUserId(Long.valueOf(user.getId()));
        assertThat(followers).isEmpty();
    }

    @Test
    void whenFindFriendsByUserIdThenReturnMutualFollowers() {
        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User user3 = User.builder()
                .username("user3")
                .email("user3@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        User user4 = User.builder()
                .username("user4")
                .email("user4@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();

        userRepository.saveAll(List.of(user1, user2, user3, user4));
        testEntityManager.flush();
        Subscription friendShip1 = Subscription.builder()
                .follower(user1)
                .following(user2)
                .build();
        Subscription friendShip2 = Subscription.builder()
                .follower(user2)
                .following(user1)
                .build();
        Subscription friendShip3 = Subscription.builder()
                .follower(user1)
                .following(user3)
                .build();
        Subscription friendShip4 = Subscription.builder()
                .follower(user3)
                .following(user1)
                .build();
        Subscription oneWaySubscription = Subscription.builder()
                .follower(user4)
                .following(user1)
                .build();

        entityManager.persist(friendShip1);
        entityManager.persist(friendShip2);
        entityManager.persist(friendShip3);
        entityManager.persist(friendShip4);
        entityManager.persist(oneWaySubscription);
        testEntityManager.flush();
        testEntityManager.clear();
        List<User> friends = userRepository.findFriendsByUserId(Long.valueOf(user1.getId()));
        assertThat(friends).hasSize(2);
        assertThat(friends)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("user2", "user3");
        assertThat(friends)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("user2@example.com", "user3@example.com");
        assertThat(friends)
                .extracting(User::getUsername)
                .doesNotContain("user4");
        assertThat(friends)
                .allMatch(User::getIsActive);
    }

    @Test
    void whenFindFriendsByUserIdWithNoMutualSubscriptionsThenReturnEmptyList() {
        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        User user3 = User.builder()
                .username("user3")
                .email("user3@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.saveAll(List.of(user1, user2, user3));
        testEntityManager.flush();
        Subscription sub1 = Subscription.builder()
                .follower(user2)
                .following(user1)
                .build();
        Subscription sub2 = Subscription.builder()
                .follower(user1)
                .following(user3)
                .build();

        entityManager.persist(sub1);
        entityManager.persist(sub2);
        testEntityManager.flush();
        testEntityManager.clear();
        List<User> friends = userRepository.findFriendsByUserId(Long.valueOf(user1.getId()));
        assertThat(friends).isEmpty();
    }

    @Test
    void whenFindPostsByFollowingThenReturnAllPostsFromFollowedUsersSortedByDate() {
        User mainUser = User.builder()
                .username("main_user")
                .email("main@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.save(mainUser);
        User followedUser1 = User.builder()
                .username("followed1")
                .email("followed1@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        User followedUser2 = User.builder()
                .username("followed2")
                .email("followed2@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        User notFollowedUser = User.builder()
                .username("not_followed")
                .email("not_followed@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.saveAll(List.of(followedUser1, followedUser2, notFollowedUser));
        Subscription sub1 = Subscription.builder()
                .follower(mainUser)
                .following(followedUser1)
                .build();
        Subscription sub2 = Subscription.builder()
                .follower(mainUser)
                .following(followedUser2)
                .build();
        entityManager.persist(sub1);
        entityManager.persist(sub2);
        testEntityManager.flush();
        LocalDateTime now = LocalDateTime.now();
        Post post1 = Post.builder()
                .user(followedUser1)
                .title("First Post")
                .content("Content of first post")
                .build();
        entityManager.persist(post1);
        testEntityManager.flush();

        entityManager.createNativeQuery("UPDATE posts SET created_at = ? WHERE id = ?")
                .setParameter(1, now.minusDays(3))
                .setParameter(2, post1.getId())
                .executeUpdate();
        Post post2 = Post.builder()
                .user(followedUser2)
                .title("Second Post")
                .content("Content of second post")
                .build();
        entityManager.persist(post2);
        testEntityManager.flush();

        entityManager.createNativeQuery("UPDATE posts SET created_at = ? WHERE id = ?")
                .setParameter(1, now.minusDays(2))
                .setParameter(2, post2.getId())
                .executeUpdate();
        Post post3 = Post.builder()
                .user(followedUser1)
                .title("Third Post")
                .content("Content of third post")
                .build();
        entityManager.persist(post3);
        testEntityManager.flush();

        entityManager.createNativeQuery("UPDATE posts SET created_at = ? WHERE id = ?")
                .setParameter(1, now.minusDays(1))
                .setParameter(2, post3.getId())
                .executeUpdate();
        Post postFromNotFollowed = Post.builder()
                .user(notFollowedUser)
                .title("Not Followed Post")
                .content("Should not appear")
                .build();
        entityManager.persist(postFromNotFollowed);
        testEntityManager.flush();

        entityManager.createNativeQuery("UPDATE posts SET created_at = ? WHERE id = ?")
                .setParameter(1, now.minusDays(1))
                .setParameter(2, postFromNotFollowed.getId())
                .executeUpdate();

        testEntityManager.flush();
        testEntityManager.clear();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Post> posts = userRepository.findPostsByFollowing(
                Long.valueOf(mainUser.getId()), pageable);
        assertThat(posts.getContent()).hasSize(3);
        assertThat(posts.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Third Post", "Second Post", "First Post");
        assertThat(posts.getContent())
                .extracting(Post::getTitle)
                .doesNotContain("Not Followed Post");
        assertThat(posts.getContent())
                .extracting(post -> post.getUser().getUsername())
                .containsExactlyInAnyOrder("followed1", "followed2", "followed1");
        assertThat(posts.getTotalElements()).isEqualTo(3);
        assertThat(posts.getTotalPages()).isEqualTo(1);
    }

    @Test
    void whenFindPostsByFollowingWithPaginationThenReturnCorrectPages() {
        User mainUser = User.builder()
                .username("main_user")
                .email("main@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.save(mainUser);
        User followedUser = User.builder()
                .username("followed_user")
                .email("followed@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.save(followedUser);
        Subscription sub = Subscription.builder()
                .follower(mainUser)
                .following(followedUser)
                .build();
        entityManager.persist(sub);
        testEntityManager.flush();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 15; i++) {
            Post post = Post.builder()
                    .user(followedUser)
                    .title("Post " + i)
                    .content("Content of post " + i)
                    .build();
            entityManager.persist(post);
            testEntityManager.flush();
            entityManager.createNativeQuery("UPDATE posts SET created_at = ? WHERE id = ?")
                    .setParameter(1, now.minusDays(15 - i))
                    .setParameter(2, post.getId())
                    .executeUpdate();
        }
        testEntityManager.flush();
        testEntityManager.clear();
        Pageable firstPage = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Page<Post> firstPageResults = userRepository.findPostsByFollowing(
                Long.valueOf(mainUser.getId()), firstPage);
        Pageable secondPage = PageRequest.of(1, 5, Sort.by("createdAt").descending());
        Page<Post> secondPageResults = userRepository.findPostsByFollowing(
                Long.valueOf(mainUser.getId()), secondPage);
        Pageable thirdPage = PageRequest.of(2, 5, Sort.by("createdAt").descending());
        Page<Post> thirdPageResults = userRepository.findPostsByFollowing(
                Long.valueOf(mainUser.getId()), thirdPage);
        assertThat(firstPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Post 15", "Post 14", "Post 13", "Post 12", "Post 11");
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(secondPageResults.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Post 10", "Post 9", "Post 8", "Post 7", "Post 6");
        assertThat(thirdPageResults.getContent()).hasSize(5);
        assertThat(thirdPageResults.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Post 5", "Post 4", "Post 3", "Post 2", "Post 1");
        assertThat(firstPageResults.getTotalElements()).isEqualTo(15);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(3);
        assertThat(firstPageResults.getNumber()).isEqualTo(0);
        assertThat(secondPageResults.getNumber()).isEqualTo(1);
        assertThat(thirdPageResults.getNumber()).isEqualTo(2);
        assertThat(firstPageResults.getContent()).allMatch(p -> !p.getIsDeleted());
        assertThat(secondPageResults.getContent()).allMatch(p -> !p.getIsDeleted());
        assertThat(thirdPageResults.getContent()).allMatch(p -> !p.getIsDeleted());
    }
}