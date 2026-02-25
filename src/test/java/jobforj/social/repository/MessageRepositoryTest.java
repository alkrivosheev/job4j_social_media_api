package jobforj.social.repository;

import jobforj.social.model.Message;
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
class MessageRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @PersistenceContext
    private EntityManager entityManager;

    private User user1;
    private User user2;
    private User user3;
    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Message message5;

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
        messageRepository.deleteAll();
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

        userRepository.saveAll(List.of(user1, user2, user3));

        message1 = Message.builder()
                .sender(user1)
                .receiver(user2)
                .content("Hello from user1 to user2")
                .isRead(false)
                .build();

        message2 = Message.builder()
                .sender(user2)
                .receiver(user1)
                .content("Reply from user2 to user1")
                .isRead(false)
                .build();

        message3 = Message.builder()
                .sender(user1)
                .receiver(user2)
                .content("Second message from user1 to user2")
                .isRead(true)
                .build();

        message4 = Message.builder()
                .sender(user1)
                .receiver(user3)
                .content("Message from user1 to user3")
                .isRead(false)
                .build();

        message5 = Message.builder()
                .sender(user3)
                .receiver(user1)
                .content("Reply from user3 to user1")
                .isRead(false)
                .build();

        messageRepository.saveAll(List.of(message1, message2, message3, message4, message5));
        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Test
    void whenFindBySenderOrderByCreatedAtDescThenReturnMessages() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Message> result = messageRepository.findBySenderOrderByCreatedAtDesc(user1, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(Message::getContent)
                .containsExactly(
                        "Message from user1 to user3",
                        "Second message from user1 to user2",
                        "Hello from user1 to user2"
                );
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void whenFindBySenderWithPaginationThenReturnCorrectPage() {
        createMultipleMessagesForUser(user1, user2, 15);
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable firstPage = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Pageable secondPage = PageRequest.of(1, 5, Sort.by("createdAt").descending());

        Page<Message> firstPageResults = messageRepository.findBySenderOrderByCreatedAtDesc(user1, firstPage);
        Page<Message> secondPageResults = messageRepository.findBySenderOrderByCreatedAtDesc(user1, secondPage);

        assertThat(firstPageResults.getContent()).hasSize(5);
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getTotalElements()).isEqualTo(18);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(4);
    }

    @Test
    void whenFindByReceiverOrderByCreatedAtDescThenReturnMessages() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Message> result = messageRepository.findByReceiverOrderByCreatedAtDesc(user2, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Message::getContent)
                .containsExactly(
                        "Second message from user1 to user2",
                        "Hello from user1 to user2"
                );
    }

    @Test
    void whenFindConversationByUsersThenReturnMessages() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Message> conversation = messageRepository.findConversation(user1, user2, pageable);

        assertThat(conversation.getContent()).hasSize(3);
        assertThat(conversation.getContent())
                .extracting(Message::getContent)
                .containsExactly(
                        "Second message from user1 to user2",
                        "Reply from user2 to user1",
                        "Hello from user1 to user2"
                );
    }

    @Test
    void whenFindConversationByUserIdsThenReturnMessages() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Message> conversation = messageRepository.findConversation(
                Long.valueOf(user1.getId()),
                Long.valueOf(user2.getId()),
                pageable);

        assertThat(conversation.getContent()).hasSize(3);
        assertThat(conversation.getContent())
                .extracting(Message::getContent)
                .containsExactly(
                        "Second message from user1 to user2",
                        "Reply from user2 to user1",
                        "Hello from user1 to user2"
                );
    }

    @Test
    void whenFindConversationWithNoMessagesThenReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> conversation = messageRepository.findConversation(user2, user3, pageable);

        assertThat(conversation.getContent()).isEmpty();
        assertThat(conversation.getTotalElements()).isEqualTo(0);
    }

    @Test
    void whenFindConversationWithPaginationThenReturnCorrectPage() {
        createMultipleMessagesForConversation(user1, user2, 15);
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable firstPage = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Pageable secondPage = PageRequest.of(1, 5, Sort.by("createdAt").descending());

        Page<Message> firstPageResults = messageRepository.findConversation(user1, user2, firstPage);
        Page<Message> secondPageResults = messageRepository.findConversation(user1, user2, secondPage);

        assertThat(firstPageResults.getContent()).hasSize(5);
        assertThat(secondPageResults.getContent()).hasSize(5);
        assertThat(firstPageResults.getTotalElements()).isEqualTo(18);
        assertThat(firstPageResults.getTotalPages()).isEqualTo(4);
    }

    @Test
    void whenCountUnreadMessagesThenReturnCorrectCount() {
        long unreadForUser1 = messageRepository.countUnreadMessages(Long.valueOf(user1.getId()));
        long unreadForUser2 = messageRepository.countUnreadMessages(Long.valueOf(user2.getId()));
        long unreadForUser3 = messageRepository.countUnreadMessages(Long.valueOf(user3.getId()));

        assertThat(unreadForUser1).isEqualTo(2); // от user2 и от user3
        assertThat(unreadForUser2).isEqualTo(1); // от user1 (message1 не прочитано)
        assertThat(unreadForUser3).isEqualTo(1); // от user1
    }

    @Test
    void whenMarkMessagesAsReadByReceiverAndSenderThenUpdateStatus() {
        int updatedCount = messageRepository.markMessagesAsRead(
                Long.valueOf(user2.getId()),
                Long.valueOf(user1.getId())
        );

        testEntityManager.flush();
        testEntityManager.clear();

        assertThat(updatedCount).isEqualTo(1); // только message1 было не прочитано

        Message message = messageRepository.findById(Long.valueOf(message1.getId())).orElse(null);
        assertThat(message).isNotNull();
        assertThat(message.getIsRead()).isTrue();

        Message otherMessage = messageRepository.findById(Long.valueOf(message2.getId())).orElse(null);
        assertThat(otherMessage).isNotNull();
        assertThat(otherMessage.getIsRead()).isFalse(); // это сообщение от user2, оно не должно измениться
    }

    @Test
    void whenMarkMessagesAsReadByMessageIdsThenUpdateStatus() {
        List<Long> messageIds = List.of(
                Long.valueOf(message1.getId()),
                Long.valueOf(message4.getId())
        );

        messageRepository.markMessagesAsRead(messageIds);
        testEntityManager.flush();
        testEntityManager.clear();

        Message msg1 = messageRepository.findById(Long.valueOf(message1.getId())).orElse(null);
        Message msg4 = messageRepository.findById(Long.valueOf(message4.getId())).orElse(null);
        Message msg2 = messageRepository.findById(Long.valueOf(message2.getId())).orElse(null);

        assertThat(msg1).isNotNull();
        assertThat(msg1.getIsRead()).isTrue();

        assertThat(msg4).isNotNull();
        assertThat(msg4.getIsRead()).isTrue();

        assertThat(msg2).isNotNull();
        assertThat(msg2.getIsRead()).isFalse();
    }

    @Test
    void whenMarkMessagesAsReadWithEmptyListThenNoChanges() {
        messageRepository.markMessagesAsRead(List.of());
        testEntityManager.flush();
        testEntityManager.clear();

        long unreadCount = messageRepository.countUnreadMessages(Long.valueOf(user2.getId()));
        assertThat(unreadCount).isEqualTo(1);
    }

    @Test
    void whenFindAllUnreadMessagesThenReturnUnreadMessages() {
        List<Message> unreadMessages = messageRepository.findAllUnreadMessages(
                Long.valueOf(user1.getId()));

        assertThat(unreadMessages).hasSize(2);
        assertThat(unreadMessages)
                .extracting(Message::getSender)
                .extracting(User::getUsername)
                .containsExactlyInAnyOrder("user2", "user3");
        assertThat(unreadMessages)
                .allMatch(m -> !m.getIsRead());
    }

    @Test
    void whenFindAllUnreadMessagesWithNoUnreadThenReturnEmptyList() {
        messageRepository.markMessagesAsRead(List.of(
                Long.valueOf(message1.getId()),
                Long.valueOf(message2.getId()),
                Long.valueOf(message4.getId()),
                Long.valueOf(message5.getId())
        ));
        testEntityManager.flush();
        testEntityManager.clear();

        List<Message> unreadMessages = messageRepository.findAllUnreadMessages(
                Long.valueOf(user1.getId()));

        assertThat(unreadMessages).isEmpty();
    }

    @Test
    void whenFindBySenderWithNoMessagesThenReturnEmptyPage() {
        // Создаем нового пользователя без сообщений
        User newUser = User.builder()
                .username("newuser_" + System.currentTimeMillis())
                .email("newuser@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.save(newUser);
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> result = messageRepository.findBySenderOrderByCreatedAtDesc(newUser, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void whenFindByReceiverWithNoMessagesThenReturnEmptyPage() {
        User newUser = User.builder()
                .username("newreceiver_" + System.currentTimeMillis())
                .email("newreceiver@example.com")
                .passwordHash("password123")
                .isActive(true)
                .build();
        userRepository.save(newUser);
        testEntityManager.flush();
        testEntityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> result = messageRepository.findByReceiverOrderByCreatedAtDesc(newUser, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void whenMarkMessagesAsReadWithNonExistentIdsThenReturnZero() {
        int updatedCount = messageRepository.markMessagesAsRead(999L, 888L);

        assertThat(updatedCount).isEqualTo(0);
    }

    @Test
    void whenMarkMessagesAsReadByMessageIdsWithNonExistentIdsThenNoException() {
        messageRepository.markMessagesAsRead(List.of(999L, 888L));
        testEntityManager.flush();

        assertThat(messageRepository.count()).isEqualTo(5);
    }

    @Test
    void whenCreateMessageThenFieldsAreSetCorrectly() {
        Message newMessage = Message.builder()
                .sender(user1)
                .receiver(user3)
                .content("Test message content")
                .build();

        messageRepository.save(newMessage);
        testEntityManager.flush();

        assertThat(newMessage.getIsRead()).isFalse();
        assertThat(newMessage.getCreatedAt()).isNotNull();

        Message foundMessage = messageRepository.findById(Long.valueOf(newMessage.getId())).orElse(null);
        assertThat(foundMessage).isNotNull();
        assertThat(foundMessage.getContent()).isEqualTo("Test message content");
    }

    @Test
    void whenFindConversationOrderIsCorrect() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> conversation = messageRepository.findConversation(user1, user2, pageable);

        List<LocalDateTime> dates = conversation.getContent().stream()
                .map(Message::getCreatedAt)
                .collect(java.util.stream.Collectors.toList());

        assertThat(dates).isSortedAccordingTo((d1, d2) -> d2.compareTo(d1));
    }

    private void createMultipleMessagesForUser(User sender, User receiver, int count) {
        IntStream.rangeClosed(1, count).forEach(i -> {
            Message message = Message.builder()
                    .sender(sender)
                    .receiver(receiver)
                    .content("Test message " + i)
                    .isRead(false)
                    .build();
            messageRepository.save(message);
        });
    }

    private void createMultipleMessagesForConversation(User user1, User user2, int count) {
        IntStream.rangeClosed(1, count).forEach(i -> {
            Message message;
            if (i % 2 == 0) {
                message = Message.builder()
                        .sender(user1)
                        .receiver(user2)
                        .content("Message from user1 " + i)
                        .isRead(false)
                        .build();
            } else {
                message = Message.builder()
                        .sender(user2)
                        .receiver(user1)
                        .content("Message from user2 " + i)
                        .isRead(false)
                        .build();
            }
            messageRepository.save(message);
        });
    }
}