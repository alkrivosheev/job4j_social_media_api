package jobforj.social.service;

import jobforj.social.model.Post;
import jobforj.social.model.User;
import jobforj.social.repository.PostRepository;
import jobforj.social.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для управления постами.
 * Предоставляет методы для создания, обновления, удаления и получения постов.
 */
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;

    /**
     * Создает новый пост.
     *
     * @param post пост для создания
     * @return созданный пост
     */
    @Transactional
    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    /**
     * Обновляет существующий пост.
     *
     * @param id      идентификатор поста
     * @param title   новый заголовок
     * @param content новое содержание
     * @return количество обновленных записей
     */
    @Transactional
    public int updatePost(Long id, String title, String content) {
        return postRepository.updatePost(title, content, id);
    }

    /**
     * Удаляет изображение из поста.
     *
     * @param imageId идентификатор изображения
     * @param postId  идентификатор поста
     * @return количество удаленных записей
     */
    @Transactional
    public int deleteImageByIdAndPostId(Long imageId, Long postId) {
        return postRepository.deleteImageByIdAndPostId(imageId, postId);
    }

    /**
     * Полностью удаляет пост.
     *
     * @param id идентификатор поста
     * @return количество удаленных записей
     */
    @Transactional
    public int deletePost(Long id) {
        return postRepository.deletePost(id);
    }

    /**
     * Мягко удаляет пост (устанавливает флаг isDeleted = true).
     *
     * @param postId идентификатор поста
     */
    @Transactional
    public void softDelete(Long postId) {
        postRepository.softDelete(postId);
    }

    /**
     * Мягко удаляет все посты пользователя.
     *
     * @param userId идентификатор пользователя
     */
    @Transactional
    public void softDeleteAllByUserId(Long userId) {
        postRepository.softDeleteAllByUserId(userId);
    }

    /**
     * Находит все посты пользователя.
     *
     * @param user пользователь
     * @return список постов пользователя
     */
    @Transactional(readOnly = true)
    public List<Post> findByUserOrderByCreatedAtDesc(User user) {
        return postRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Находит посты за указанный период.
     *
     * @param startDate начальная дата
     * @param endDate   конечная дата
     * @return список постов за период
     */
    @Transactional(readOnly = true)
    public List<Post> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate) {
        return postRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
    }

    /**
     * Возвращает постраничный список всех постов.
     *
     * @param pageable параметры пагинации
     * @return страница с постами
     */
    @Transactional(readOnly = true)
    public Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Возвращает постраничный список постов пользователя.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return страница с постами пользователя
     */
    @Transactional(readOnly = true)
    public Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable) {
        return postRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Возвращает постраничный список постов пользователя по его идентификатору.
     *
     * @param userId   идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница с постами пользователя
     */
    @Transactional(readOnly = true)
    public Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Находит посты по списку идентификаторов пользователей.
     *
     * @param userIds список идентификаторов пользователей
     * @param pageable параметры пагинации
     * @return страница с постами
     */
    @Transactional(readOnly = true)
    public Page<Post> findPostsByUserIds(List<Long> userIds, Pageable pageable) {
        return postRepository.findPostsByUserIds(userIds, pageable);
    }

    /**
     * Находит посты по списку пользователей.
     *
     * @param users    список пользователей
     * @param pageable параметры пагинации
     * @return страница с постами
     */
    @Transactional(readOnly = true)
    public Page<Post> findPostsByUsers(List<User> users, Pageable pageable) {
        return postRepository.findPostsByUsers(users, pageable);
    }

    /**
     * Возвращает ленту постов для пользователя.
     *
     * @param userId   идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница с постами из ленты
     */
    @Transactional(readOnly = true)
    public Page<Post> getFeedForUser(Long userId, Pageable pageable) {
        return postRepository.getFeedForUser(userId, pageable);
    }

    /**
     * Возвращает ленту постов для пользователя.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return страница с постами из ленты
     */
    @Transactional(readOnly = true)
    public Page<Post> getFeedForUser(User user, Pageable pageable) {
        return postRepository.getFeedForUser(user, pageable);
    }

    /**
     * Подсчитывает количество активных постов пользователя.
     *
     * @param userId идентификатор пользователя
     * @return количество активных постов
     */
    @Transactional(readOnly = true)
    public long countActivePostsByUserId(Long userId) {
        return postRepository.countActivePostsByUserId(userId);
    }

    /**
     * Находит посты в указанном диапазоне дат.
     *
     * @param startDate начальная дата
     * @param endDate   конечная дата
     * @param pageable  параметры пагинации
     * @return страница с постами
     */
    @Transactional(readOnly = true)
    public Page<Post> findPostsInDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return postRepository.findPostsInDateRange(startDate, endDate, pageable);
    }

    /**
     * Создает новый пост с указанным заголовком, текстом и изображениями.
     *
     * @param user         пользователь, создающий пост
     * @param title        заголовок поста
     * @param content      текст поста
     * @param imageIds     список идентификаторов изображений (может быть пустым)
     * @return созданный пост
     */
    @Transactional
    public Post createPostWithImages(User user, String title, String content, List<Long> imageIds) {
        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setIsDeleted(false);

        Post savedPost = postRepository.save(post);

        if (imageIds != null && !imageIds.isEmpty()) {
            imageRepository.attachImagesToPost(imageIds, Long.valueOf(savedPost.getId()));
        }

        return savedPost;
    }

    /**
     * Обновляет пост, если пользователь является его автором.
     *
     * @param userId  идентификатор пользователя
     * @param postId  идентификатор поста
     * @param title   новый заголовок
     * @param content новое содержание
     * @return количество обновленных записей (0 или 1)
     */
    @Transactional
    public int updateUserPost(Long userId, Long postId, String title, String content) {
        return postRepository.updateUserPost(userId, postId, title, content);
    }

    /**
     * Мягко удаляет пост, если пользователь является его автором.
     *
     * @param userId идентификатор пользователя
     * @param postId идентификатор поста
     * @return true если пост был удален, false если пост не найден или пользователь не автор
     */
    @Transactional
    public boolean softDeleteUserPost(Long userId, Long postId) {
        return postRepository.softDeleteUserPost(userId, postId) > 0;
    }

    /**
     * Полностью удаляет пост, если пользователь является его автором.
     *
     * @param userId идентификатор пользователя
     * @param postId идентификатор поста
     * @return true если пост был удален, false если пост не найден или пользователь не автор
     */
    @Transactional
    public boolean deleteUserPost(Long userId, Long postId) {
        return postRepository.deleteUserPost(userId, postId) > 0;
    }
}