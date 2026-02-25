package jobforj.social.repository;

import jobforj.social.model.Post;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @PersistenceContext
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private User user3;
    private Post post1;
    private Post post2;
    private Post post3;
    private Post post4;

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
        postRepository.deleteAll();
        userRepository.deleteAll();
        subscriptionRepository.deleteAll();

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

        userRepository.saveAll(List.of(user1, user2, user3));

        post1 = Post.builder()
                .user(user1)
                .title("First Post")
                .content("Content of first post")
                .build();

        post2 = Post.builder()
                .user(user1)
                .title("Second Post")
                .content("Content of second post")
                .build();

        post3 = Post.builder()
                .user(user2)
                .title("Third Post")
                .content("Content of third post")
                .build();

        post4 = Post.builder()
                .user(user2)
                .title("Fourth Post")
                .content("Content of fourth post")
                .isDeleted(true)
                .build();

        postRepository.saveAll(List.of(post1, post2, post3, post4));
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    void whenFindByUserOrderByCreatedAtDescThenReturnPosts() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Post> result = postRepository.findByUserOrderByCreatedAtDesc(user1, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Second Post", "First Post");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void whenFindByUserOrderByCreatedAtDescWithPaginationThenReturnCorrectPage() {
        createMultiplePostsForUser(user1, 15);
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable firstPage = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Pageable secondPage = PageRequest.of(1, 5, Sort.by("createdAt").descending());

        Page<Post> firstPageResults = postRepository.findByUserOrderByCreatedAtDesc(user1, firstPage);
        Page<Post> secondPageResults = postRepository.findByUserOrderByCreatedAtDesc(user1, secondPage);

        assertThat(firstPageResults.getContent()).hasSize(5);
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getTotalElements()).isEqualTo(17);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(4);
    }

    @Test
    void whenFindByUserIdOrderByCreatedAtDescThenReturnPosts() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Post> result = postRepository.findByUserIdOrderByCreatedAtDesc(
                Long.valueOf(user1.getId()), pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Second Post", "First Post");
    }

    @Test
    void whenFindPostsByUserIdsThenReturnPosts() {
        List<Long> userIds = List.of(Long.valueOf(user1.getId()), Long.valueOf(user2.getId()));
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        Page<Post> result = postRepository.findPostsByUserIds(userIds, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Third Post", "Second Post", "First Post");
        assertThat(result.getContent())
                .allMatch(p -> !p.getIsDeleted());
    }

    @Test
    void whenFindPostsByUsersThenReturnPosts() {
        List<User> users = List.of(user1, user2);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        Page<Post> result = postRepository.findPostsByUsers(users, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Third Post", "Second Post", "First Post");
        assertThat(result.getContent())
                .allMatch(p -> !p.getIsDeleted());
    }

    @Test
    void whenGetFeedForUserByUserIdThenReturnFollowedUsersPosts() {
        subscriptionRepository.save(Subscription.builder()
                .follower(user3)
                .following(user1)
                .build());
        subscriptionRepository.save(Subscription.builder()
                .follower(user3)
                .following(user2)
                .build());
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Post> feed = postRepository.getFeedForUser(Long.valueOf(user3.getId()), pageable);

        assertThat(feed.getContent()).hasSize(3);
        assertThat(feed.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Third Post", "Second Post", "First Post");
        assertThat(feed.getContent())
                .allMatch(p -> !p.getIsDeleted());
    }

    @Test
    void whenGetFeedForUserByUserThenReturnFollowedUsersPosts() {
        subscriptionRepository.save(Subscription.builder()
                .follower(user3)
                .following(user1)
                .build());
        subscriptionRepository.save(Subscription.builder()
                .follower(user3)
                .following(user2)
                .build());
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Post> feed = postRepository.getFeedForUser(user3, pageable);

        assertThat(feed.getContent()).hasSize(3);
        assertThat(feed.getContent())
                .extracting(Post::getTitle)
                .containsExactly("Third Post", "Second Post", "First Post");
        assertThat(feed.getContent())
                .allMatch(p -> !p.getIsDeleted());
    }

    @Test
    void whenGetFeedForUserWithNoFollowedUsersThenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Post> feed = postRepository.getFeedForUser(Long.valueOf(user3.getId()), pageable);

        assertThat(feed.getContent()).isEmpty();
        assertThat(feed.getTotalElements()).isEqualTo(0);
    }

    @Test
    void whenCountActivePostsByUserIdThenReturnCorrectCount() {
        long count = postRepository.countActivePostsByUserId(Long.valueOf(user1.getId()));
        long countForUser2 = postRepository.countActivePostsByUserId(Long.valueOf(user2.getId()));
        long countForUser3 = postRepository.countActivePostsByUserId(Long.valueOf(user3.getId()));

        assertThat(count).isEqualTo(2);
        assertThat(countForUser2).isEqualTo(1);
        assertThat(countForUser3).isEqualTo(0);
    }

    @Test
    void whenSoftDeleteThenPostIsMarkedAsDeleted() {
        Integer postId = post1.getId();

        postRepository.softDelete(Long.valueOf(postId));
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> userPosts = postRepository.findByUserOrderByCreatedAtDesc(user1, pageable);
        assertThat(userPosts.getContent()).hasSize(1);
        assertThat(userPosts.getContent().get(0).getTitle()).isEqualTo("Second Post");
        assertThat(userPosts.getContent().get(0).getId()).isNotEqualTo(postId);

        Boolean isDeleted = (Boolean) entityManager.createNativeQuery(
                        "SELECT is_deleted FROM posts WHERE id = :id")
                .setParameter("id", postId)
                .getSingleResult();

        assertThat(isDeleted).isTrue();

        String title = (String) entityManager.createNativeQuery(
                        "SELECT title FROM posts WHERE id = :id")
                .setParameter("id", postId)
                .getSingleResult();

        assertThat(title).isEqualTo("First Post");
    }

    @Test
    void whenFindPostsInDateRangeThenReturnCorrectPosts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(1);
        LocalDateTime endDate = now.plusDays(1);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> postsInRange = postRepository.findPostsInDateRange(startDate, endDate, pageable);

        assertThat(postsInRange.getContent()).hasSize(3);
        assertThat(postsInRange.getContent())
                .extracting(Post::getTitle)
                .containsExactlyInAnyOrder("First Post", "Second Post", "Third Post");
    }

    @Test
    void whenFindPostsInDateRangeWithNoPostsThenReturnEmptyPage() {
        LocalDateTime startDate = LocalDateTime.now().minusYears(1);
        LocalDateTime endDate = LocalDateTime.now().minusYears(1).plusDays(1);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> postsInRange = postRepository.findPostsInDateRange(startDate, endDate, pageable);

        assertThat(postsInRange.getContent()).isEmpty();
    }

    @Test
    void whenFindByUserWithNoPostsThenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> result = postRepository.findByUserOrderByCreatedAtDesc(user3, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void whenFindPostsByUserIdsWithNoUsersThenReturnEmptyPage() {
        List<Long> userIds = List.of();
        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.findPostsByUserIds(userIds, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void whenSoftDeleteNonExistentPostThenNoException() {
        postRepository.softDelete(999L);
        testEntityManager.flush();

        assertThat(postRepository.count()).isEqualTo(3);
    }

    @Test
    void whenSoftDeleteAllByNonExistentUserIdThenNoException() {
        postRepository.softDeleteAllByUserId(999L);
        testEntityManager.flush();

        assertThat(postRepository.count()).isEqualTo(3);
    }

    @Test
    void whenCreatePostThenHelperMethodWorks() {
        Post post = Post.builder()
                .user(user1)
                .title("Test Post")
                .content("Test Content")
                .build();

        postRepository.save(post);
        testEntityManager.flush();

        assertThat(post.getImages()).isEmpty();

        Post foundPost = postRepository.findById(Long.valueOf(post.getId())).orElse(null);
        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getIsDeleted()).isFalse();
        assertThat(foundPost.getCreatedAt()).isNotNull();
        assertThat(foundPost.getUpdatedAt()).isNotNull();
    }

    @Test
    void whenSoftDeletedPostsAreExcludedFromQueries() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> allPosts = postRepository.findAll(pageable);
        assertThat(allPosts.getContent()).hasSize(3);

        long count = postRepository.count();
        assertThat(count).isEqualTo(3);

        Page<Post> user2Posts = postRepository.findByUserOrderByCreatedAtDesc(user2, pageable);
        assertThat(user2Posts.getContent()).hasSize(1);
        assertThat(user2Posts.getContent().get(0).getTitle()).isEqualTo("Third Post");
    }

    private void createMultiplePostsForUser(User user, int count) {
        IntStream.rangeClosed(1, count).forEach(i -> {
            Post post = Post.builder()
                    .user(user)
                    .title("Post " + i)
                    .content("Content " + i)
                    .build();
            postRepository.save(post);
        });
    }
}