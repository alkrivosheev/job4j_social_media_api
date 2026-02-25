package jobforj.social.repository;

import jobforj.social.model.Image;
import jobforj.social.model.Post;
import jobforj.social.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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
class ImageRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;
    private Post post1;
    private Post post2;
    private Image image1;
    private Image image2;
    private Image image3;
    private Image image4;
    private Image image5;

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
        imageRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.save(user);

        post1 = Post.builder()
                .user(user)
                .title("Post 1")
                .content("Content 1")
                .build();

        post2 = Post.builder()
                .user(user)
                .title("Post 2")
                .content("Content 2")
                .build();

        postRepository.saveAll(List.of(post1, post2));

        image1 = Image.builder()
                .post(post1)
                .url("https://example.com/image1.jpg")
                .fileName("image1.jpg")
                .fileSize(1024)
                .build();

        image2 = Image.builder()
                .post(post1)
                .url("https://example.com/image2.jpg")
                .fileName("image2.jpg")
                .fileSize(2048)
                .build();

        image3 = Image.builder()
                .post(post1)
                .url("https://example.com/image3.jpg")
                .fileName("image3.jpg")
                .fileSize(3072)
                .build();

        image4 = Image.builder()
                .post(post2)
                .url("https://example.com/image4.jpg")
                .fileName("image4.jpg")
                .fileSize(4096)
                .build();

        image5 = Image.builder()
                .post(post2)
                .url("https://example.com/image5.jpg")
                .fileName("image5.jpg")
                .fileSize(5120)
                .build();

        imageRepository.saveAll(List.of(image1, image2, image3, image4, image5));
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    void whenFindByPostThenReturnImages() {
        List<Image> images = imageRepository.findByPost(post1);

        assertThat(images).hasSize(3);
        assertThat(images)
                .extracting(Image::getFileName)
                .containsExactlyInAnyOrder("image1.jpg", "image2.jpg", "image3.jpg");
        assertThat(images)
                .allMatch(image -> image.getPost().getId().equals(post1.getId()));
    }

    @Test
    void whenFindByPostWithNoImagesThenReturnEmptyList() {
        Post newPost = Post.builder()
                .user(user)
                .title("New Post")
                .content("New Content")
                .build();
        postRepository.save(newPost);
        testEntityManager.flush();
        testEntityManager.clear();

        List<Image> images = imageRepository.findByPost(newPost);

        assertThat(images).isEmpty();
    }

    @Test
    void whenFindByPostIdThenReturnImages() {
        List<Image> images = imageRepository.findByPostId(Long.valueOf(post1.getId()));

        assertThat(images).hasSize(3);
        assertThat(images)
                .extracting(Image::getFileName)
                .containsExactlyInAnyOrder("image1.jpg", "image2.jpg", "image3.jpg");
    }

    @Test
    void whenFindByPostIdWithNoImagesThenReturnEmptyList() {
        List<Image> images = imageRepository.findByPostId(999L);

        assertThat(images).isEmpty();
    }

    @Test
    void whenFindAllByPostIdsThenReturnImages() {
        List<Long> postIds = List.of(Long.valueOf(post1.getId()), Long.valueOf(post2.getId()));
        List<Image> images = imageRepository.findAllByPostIds(postIds);

        assertThat(images).hasSize(5);
        assertThat(images)
                .extracting(Image::getFileName)
                .containsExactlyInAnyOrder(
                        "image1.jpg", "image2.jpg", "image3.jpg", "image4.jpg", "image5.jpg"
                );
    }

    @Test
    void whenFindAllByPostIdsWithEmptyListThenReturnEmptyList() {
        List<Image> images = imageRepository.findAllByPostIds(List.of());

        assertThat(images).isEmpty();
    }

    @Test
    void whenFindAllByPostIdsWithNonExistentIdsThenReturnEmptyList() {
        List<Image> images = imageRepository.findAllByPostIds(List.of(999L, 888L));

        assertThat(images).isEmpty();
    }

    @Test
    void whenDeleteAllByPostIdThenAllImagesForPostAreDeleted() {
        imageRepository.deleteAllByPostId(Long.valueOf(post1.getId()));
        testEntityManager.flush();
        testEntityManager.clear();

        List<Image> imagesForPost1 = imageRepository.findByPost(post1);
        List<Image> imagesForPost2 = imageRepository.findByPost(post2);

        assertThat(imagesForPost1).isEmpty();
        assertThat(imagesForPost2).hasSize(2);
        assertThat(imagesForPost2)
                .extracting(Image::getFileName)
                .containsExactlyInAnyOrder("image4.jpg", "image5.jpg");
    }

    @Test
    void whenDeleteAllByPostIdWithNonExistentIdThenNoChanges() {
        long initialCount = imageRepository.count();

        imageRepository.deleteAllByPostId(999L);
        testEntityManager.flush();

        assertThat(imageRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void whenDeleteAllByIdsThenSpecifiedImagesAreDeleted() {
        List<Long> imageIds = List.of(
                Long.valueOf(image1.getId()),
                Long.valueOf(image3.getId()),
                Long.valueOf(image5.getId())
        );

        imageRepository.deleteAllByIds(imageIds);
        testEntityManager.flush();
        testEntityManager.clear();

        List<Image> remainingImages = imageRepository.findAll();

        assertThat(remainingImages).hasSize(2);
        assertThat(remainingImages)
                .extracting(Image::getFileName)
                .containsExactlyInAnyOrder("image2.jpg", "image4.jpg");
    }

    @Test
    void whenDeleteAllByIdsWithEmptyListThenNoChanges() {
        long initialCount = imageRepository.count();

        imageRepository.deleteAllByIds(List.of());
        testEntityManager.flush();

        assertThat(imageRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void whenDeleteAllByIdsWithNonExistentIdsThenNoChanges() {
        long initialCount = imageRepository.count();

        imageRepository.deleteAllByIds(List.of(999L, 888L, 777L));
        testEntityManager.flush();

        assertThat(imageRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void whenCountByPostIdThenReturnCorrectCount() {
        long countForPost1 = imageRepository.countByPostId(Long.valueOf(post1.getId()));
        long countForPost2 = imageRepository.countByPostId(Long.valueOf(post2.getId()));
        long countForNonExistentPost = imageRepository.countByPostId(999L);

        assertThat(countForPost1).isEqualTo(3);
        assertThat(countForPost2).isEqualTo(2);
        assertThat(countForNonExistentPost).isEqualTo(0);
    }

    @Test
    void whenSaveImageThenFieldsAreSetCorrectly() {
        Image newImage = Image.builder()
                .post(post1)
                .url("https://example.com/new-image.jpg")
                .fileName("new-image.jpg")
                .fileSize(6144)
                .build();

        imageRepository.save(newImage);
        testEntityManager.flush();

        assertThat(newImage.getId()).isNotNull();
        assertThat(newImage.getUploadDate()).isNotNull();

        Image foundImage = imageRepository.findById(Long.valueOf(newImage.getId())).orElse(null);
        assertThat(foundImage).isNotNull();
        assertThat(foundImage.getUrl()).isEqualTo("https://example.com/new-image.jpg");
        assertThat(foundImage.getFileName()).isEqualTo("new-image.jpg");
        assertThat(foundImage.getFileSize()).isEqualTo(6144);
    }

    @Test
    void whenFindByIdThenReturnImage() {
        Optional<Image> foundImage = imageRepository.findById(Long.valueOf(image1.getId()));

        assertThat(foundImage).isPresent();
        assertThat(foundImage.get().getFileName()).isEqualTo("image1.jpg");
        assertThat(foundImage.get().getUrl()).isEqualTo("https://example.com/image1.jpg");
        assertThat(foundImage.get().getFileSize()).isEqualTo(1024);
        assertThat(foundImage.get().getPost().getId()).isEqualTo(post1.getId());
    }

    @Test
    void whenFindByIdNotFoundThenReturnEmpty() {
        Optional<Image> foundImage = imageRepository.findById(999L);

        assertThat(foundImage).isEmpty();
    }

    @Test
    void whenFindAllThenReturnAllImages() {
        List<Image> allImages = imageRepository.findAll();

        assertThat(allImages).hasSize(5);
        assertThat(allImages)
                .extracting(Image::getFileName)
                .containsExactlyInAnyOrder(
                        "image1.jpg", "image2.jpg", "image3.jpg", "image4.jpg", "image5.jpg"
                );
    }

    @Test
    void whenDeleteImageThenImageIsRemoved() {
        imageRepository.delete(image1);
        testEntityManager.flush();

        List<Image> remainingImages = imageRepository.findAll();

        assertThat(remainingImages).hasSize(4);
        assertThat(remainingImages)
                .extracting(Image::getFileName)
                .containsExactlyInAnyOrder("image2.jpg", "image3.jpg", "image4.jpg", "image5.jpg");
    }

    @Test
    void whenCreateMultipleImagesForPostThenAllAreSaved() {
        Post newPost = Post.builder()
                .user(user)
                .title("Post with many images")
                .content("Content with many images")
                .build();
        postRepository.save(newPost);

        List<Image> images = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> Image.builder()
                        .post(newPost)
                        .url("https://example.com/image" + i + ".jpg")
                        .fileName("image" + i + ".jpg")
                        .fileSize(i * 1024)
                        .build())
                .toList();

        imageRepository.saveAll(images);
        testEntityManager.flush();
        testEntityManager.clear();

        List<Image> savedImages = imageRepository.findByPost(newPost);

        assertThat(savedImages).hasSize(10);
        assertThat(savedImages)
                .extracting(Image::getFileSize)
                .containsExactlyInAnyOrderElementsOf(
                        IntStream.rangeClosed(1, 10).map(i -> i * 1024).boxed().toList()
                );
    }

    @Test
    void whenDeletePostThenImagesAreCascadeDeleted() {
        Post postWithImages = postRepository.findById(Long.valueOf(post1.getId())).orElse(null);
        assertThat(postWithImages).isNotNull();

        postWithImages.getImages().size();

        postRepository.delete(postWithImages);
        testEntityManager.flush();
        testEntityManager.clear();

        List<Image> imagesForPost1 = imageRepository.findByPostId(Long.valueOf(post1.getId()));
        List<Image> allImages = imageRepository.findAll();

        assertThat(imagesForPost1).isEmpty();
        assertThat(allImages).hasSize(2);
        assertThat(allImages)
                .extracting(Image::getFileName)
                .containsExactlyInAnyOrder("image4.jpg", "image5.jpg");
    }

    @Test
    void whenUpdateImageThenChangesArePersisted() {
        Image image = imageRepository.findById(Long.valueOf(image1.getId())).orElse(null);
        assertThat(image).isNotNull();

        image.setUrl("https://example.com/updated-image.jpg");
        image.setFileSize(9999);

        imageRepository.save(image);
        testEntityManager.flush();
        testEntityManager.clear();

        Image updatedImage = imageRepository.findById(Long.valueOf(image1.getId())).orElse(null);
        assertThat(updatedImage).isNotNull();
        assertThat(updatedImage.getUrl()).isEqualTo("https://example.com/updated-image.jpg");
        assertThat(updatedImage.getFileSize()).isEqualTo(9999);
        assertThat(updatedImage.getFileName()).isEqualTo("image1.jpg");
    }

    @Test
    void whenCountByPostIdAfterDeletionThenCountUpdates() {
        long initialCount = imageRepository.countByPostId(Long.valueOf(post1.getId()));
        assertThat(initialCount).isEqualTo(3);

        imageRepository.deleteAllByPostId(Long.valueOf(post1.getId()));
        testEntityManager.flush();
        testEntityManager.clear();

        long finalCount = imageRepository.countByPostId(Long.valueOf(post1.getId()));
        assertThat(finalCount).isEqualTo(0);
    }
}