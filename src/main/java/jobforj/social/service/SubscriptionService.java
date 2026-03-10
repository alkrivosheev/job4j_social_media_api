package jobforj.social.service;

import jobforj.social.model.Friendship;
import jobforj.social.model.Subscription;
import jobforj.social.model.User;
import jobforj.social.repository.FriendshipRepository;
import jobforj.social.repository.SubscriptionRepository;
import jobforj.social.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для управления подписками пользователей.
 * Предоставляет методы для подписки, отписки и получения информации о подписчиках.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    /**
     * Создает подписку одного пользователя на другого.
     *
     * @param followerId идентификатор подписчика
     * @param followingId идентификатор пользователя, на которого подписываются
     * @return созданная подписка
     * @throws IllegalArgumentException если пользователи не найдены или попытка подписаться на самого себя
     */
    @Transactional
    public Subscription subscribe(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Нельзя подписаться на самого себя");
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("Подписчик не найден"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь для подписки не найден"));

        if (subscriptionRepository.isFollowing(followerId, followingId)) {
            throw new IllegalArgumentException("Подписка уже существует");
        }

        Subscription subscription = new Subscription();
        subscription.setFollower(follower);
        subscription.setFollowing(following);
        subscription.setCreatedAt(LocalDateTime.now());

        return subscriptionRepository.save(subscription);
    }

    /**
     * Удаляет подписку.
     *
     * @param followerId идентификатор подписчика
     * @param followingId идентификатор пользователя, на которого были подписаны
     * @throws IllegalArgumentException если подписка не найдена
     */
    @Transactional
    public void unsubscribe(Long followerId, Long followingId) {
        Subscription subscription = subscriptionRepository.findByUserIds(followerId, followingId)
                .orElseThrow(() -> new IllegalArgumentException("Подписка не найдена"));

        subscriptionRepository.delete(subscription);
    }

    /**
     * Удаляет подписку напрямую через репозиторий.
     *
     * @param follower подписчик
     * @param following пользователь, на которого подписаны
     */
    @Transactional
    public void deleteSubscription(User follower, User following) {
        subscriptionRepository.deleteSubscription(follower, following);
    }

    /**
     * Проверяет, подписан ли один пользователь на другого.
     *
     * @param followerId идентификатор подписчика
     * @param followingId идентификатор пользователя для проверки
     * @return true если подписка существует
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return subscriptionRepository.isFollowing(followerId, followingId);
    }

    /**
     * Возвращает постраничный список подписок пользователя.
     *
     * @param follower пользователь-подписчик
     * @param pageable параметры пагинации
     * @return страница с подписками
     */
    @Transactional(readOnly = true)
    public Page<Subscription> findByFollower(User follower, Pageable pageable) {
        return subscriptionRepository.findByFollower(follower, pageable);
    }

    /**
     * Возвращает постраничный список подписчиков пользователя.
     *
     * @param following пользователь, чьих подписчиков получаем
     * @param pageable параметры пагинации
     * @return страница с подписками (где пользователь является объектом подписки)
     */
    @Transactional(readOnly = true)
    public Page<Subscription> findByFollowing(User following, Pageable pageable) {
        return subscriptionRepository.findByFollowing(following, pageable);
    }

    /**
     * Возвращает постраничный список пользователей, на которых подписан данный пользователь.
     *
     * @param followerId идентификатор подписчика
     * @param pageable параметры пагинации
     * @return страница с пользователями
     */
    @Transactional(readOnly = true)
    public Page<User> findFollowedUsers(Long followerId, Pageable pageable) {
        return subscriptionRepository.findFollowedUsers(followerId, pageable);
    }

    /**
     * Возвращает постраничный список подписчиков пользователя.
     *
     * @param followingId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница с подписчиками
     */
    @Transactional(readOnly = true)
    public Page<User> findFollowers(Long followingId, Pageable pageable) {
        return subscriptionRepository.findFollowers(followingId, pageable);
    }

    /**
     * Возвращает количество подписок пользователя.
     *
     * @param userId идентификатор пользователя
     * @return количество пользователей, на которых подписан данный пользователь
     */
    @Transactional(readOnly = true)
    public long countFollowing(Long userId) {
        return subscriptionRepository.countFollowing(userId);
    }

    /**
     * Возвращает количество подписчиков пользователя.
     *
     * @param userId идентификатор пользователя
     * @return количество подписчиков
     */
    @Transactional(readOnly = true)
    public long countFollowers(Long userId) {
        return subscriptionRepository.countFollowers(userId);
    }

    /**
     * Находит подписку по пользователям.
     *
     * @param follower подписчик
     * @param following пользователь, на которого подписаны
     * @return Optional с подпиской
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> findByFollowerAndFollowing(User follower, User following) {
        return subscriptionRepository.findByFollowerAndFollowing(follower, following);
    }

    /**
     * Находит подписку по идентификаторам пользователей.
     *
     * @param followerId идентификатор подписчика
     * @param followingId идентификатор пользователя, на которого подписаны
     * @return Optional с подпиской
     */
    @Transactional(readOnly = true)
    public Optional<Subscription> findByUserIds(Long followerId, Long followingId) {
        return subscriptionRepository.findByUserIds(followerId, followingId);
    }

    /**
     * Удаляет пользователя из друзей.
     * При удалении из друзей пользователь отписывается от второго пользователя,
     * но второй пользователь остается подписчиком (если он был подписан).
     *
     * @param userId идентификатор пользователя, который удаляет из друзей
     * @param friendId идентификатор пользователя, которого удаляют из друзей
     * @throws IllegalArgumentException если один из пользователей не найден
     */
    @Transactional
    public void removeFromFriends(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Нельзя удалить самого себя из друзей");
        }
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        userRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Друг не найден"));
        subscriptionRepository.findByUserIds(userId, friendId)
                .ifPresent(subscriptionRepository::delete);
    }

    /**
     * Отправляет заявку в друзья другому пользователю.
     * При отправке заявки пользователь автоматически подписывается на адресата.
     *
     * @param requesterId идентификатор пользователя, отправляющего заявку
     * @param addresseeId идентификатор пользователя, которому отправляют заявку
     * @return созданная заявка в друзья
     * @throws IllegalArgumentException если пользователи не найдены или попытка отправить заявку самому себе
     */
    @Transactional
    public Friendship sendFriendRequest(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new IllegalArgumentException("Нельзя отправить заявку в друзья самому себе");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Отправитель не найден"));
        User addressee = userRepository.findById(addresseeId)
                .orElseThrow(() -> new IllegalArgumentException("Получатель не найден"));

        if (friendshipRepository.findByRequesterAndAddressee(requester, addressee).isPresent()) {
            throw new IllegalArgumentException("Заявка уже существует");
        }

        if (friendshipRepository.areFriends(requester, addressee)) {
            throw new IllegalArgumentException("Пользователи уже являются друзьями");
        }

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(Friendship.FriendshipStatus.PENDING)
                .build();

        Friendship savedFriendship = friendshipRepository.save(friendship);

        if (!subscriptionRepository.isFollowing(requesterId, addresseeId)) {
            Subscription subscription = new Subscription();
            subscription.setFollower(requester);
            subscription.setFollowing(addressee);
            subscription.setCreatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
        }

        return savedFriendship;
    }

    /**
     * Принимает заявку в друзья.
     * После принятия оба пользователя становятся друзьями.
     *
     * @param requesterId идентификатор пользователя, отправившего заявку
     * @param addresseeId идентификатор пользователя, принимающего заявку
     * @throws IllegalArgumentException если заявка не найдена или не находится в статусе PENDING
     */
    @Transactional
    public void acceptFriendRequest(Long requesterId, Long addresseeId) {
        Friendship friendship = friendshipRepository.findByUserIds(requesterId, addresseeId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка в друзья не найдена"));

        if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Заявка уже обработана");
        }

        if (!friendship.getAddressee().getId().equals(addresseeId)) {
            throw new IllegalArgumentException("Только получатель может принять заявку");
        }

        // Обновляем статус заявки
        friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        User requester = friendship.getRequester();
        User addressee = friendship.getAddressee();

        // Подписываем получателя на отправителя (если еще не подписан)
        if (!subscriptionRepository.isFollowing(addresseeId, requesterId)) {
            Subscription subscription = new Subscription();
            subscription.setFollower(addressee);
            subscription.setFollowing(requester);
            subscription.setCreatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
        }
    }

    /**
     * Отклоняет заявку в друзья.
     * При отклонении заявки отправитель остается подписчиком получателя.
     *
     * @param requesterId идентификатор пользователя, отправившего заявку
     * @param addresseeId идентификатор пользователя, отклоняющего заявку
     * @throws IllegalArgumentException если заявка не найдена или не находится в статусе PENDING
     */
    @Transactional
    public void rejectFriendRequest(Long requesterId, Long addresseeId) {
        Friendship friendship = friendshipRepository.findByUserIds(requesterId, addresseeId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка в друзья не найдена"));

        if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Заявка уже обработана");
        }

        if (!friendship.getAddressee().getId().equals(addresseeId)) {
            throw new IllegalArgumentException("Только получатель может отклонить заявку");
        }

        // Обновляем статус заявки на REJECTED
        friendship.setStatus(Friendship.FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);

        // Отправитель остается подписчиком (подписка не удаляется)
    }

    /**
     * Отменяет отправленную заявку в друзья.
     * При отмене заявки отправитель остается подписчиком получателя.
     *
     * @param requesterId идентификатор пользователя, отправившего заявку
     * @param addresseeId идентификатор пользователя, которому была отправлена заявка
     * @throws IllegalArgumentException если заявка не найдена
     */
    @Transactional
    public void cancelFriendRequest(Long requesterId, Long addresseeId) {
        Friendship friendship = friendshipRepository.findByUserIds(requesterId, addresseeId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка в друзья не найдена"));

        if (!friendship.getRequester().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Только отправитель может отменить свою заявку");
        }

        // Удаляем заявку
        friendshipRepository.delete(friendship);

        // Отправитель остается подписчиком (подписка не удаляется)
    }

    /**
     * Проверяет, являются ли пользователи друзьями.
     *
     * @param user1 первый пользователь
     * @param user2 второй пользователь
     * @return true если пользователи являются друзьями
     */
    @Transactional(readOnly = true)
    public boolean areFriends(User user1, User user2) {
        return friendshipRepository.areFriends(user1, user2);
    }

    /**
     * Возвращает список входящих заявок в друзья со статусом PENDING.
     *
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница с входящими заявками
     */
    @Transactional(readOnly = true)
    public Page<Friendship> findPendingReceivedRequests(Long userId, Pageable pageable) {
        return friendshipRepository.findReceivedRequestsByStatus(
                userId, Friendship.FriendshipStatus.PENDING, pageable);
    }

    /**
     * Возвращает список исходящих заявок в друзья со статусом PENDING.
     *
     * @param userId идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница с исходящими заявками
     */
    @Transactional(readOnly = true)
    public Page<Friendship> findPendingSentRequests(Long userId, Pageable pageable) {
        return friendshipRepository.findSentRequestsByStatus(
                userId, Friendship.FriendshipStatus.PENDING, pageable);
    }

    /**
     * Находит заявку в друзья между двумя пользователями.
     *
     * @param requesterId идентификатор отправителя
     * @param addresseeId идентификатор получателя
     * @return Optional с заявкой в друзья
     */
    @Transactional(readOnly = true)
    public Optional<Friendship> findFriendshipByUserIds(Long requesterId, Long addresseeId) {
        return friendshipRepository.findByUserIds(requesterId, addresseeId);
    }

    /**
     * Проверяет, что пользователи являются друзьями и подписаны друг на друга.
     * Если друзья не подписаны друг на друга, восстанавливает подписки.
     *
     * @param userId идентификатор первого пользователя
     * @param friendId идентификатор второго пользователя
     * @return true если пользователи являются друзьями и подписаны друг на друга
     * @throws IllegalArgumentException если пользователи не найдены
     */
    @Transactional
    public boolean verifyAndFixFriendshipSubscriptions(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Пользователь не может быть другом самому себе");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Друг не найден"));

        boolean areFriends = friendshipRepository.areFriends(user, friend);

        if (!areFriends) {
            return false;
        }

        boolean userFollowsFriend = subscriptionRepository.isFollowing(userId, friendId);
        boolean friendFollowsUser = subscriptionRepository.isFollowing(friendId, userId);

        if (!userFollowsFriend || !friendFollowsUser) {
            if (!userFollowsFriend) {
                Subscription subscription = new Subscription();
                subscription.setFollower(user);
                subscription.setFollowing(friend);
                subscription.setCreatedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
            }

            if (!friendFollowsUser) {
                Subscription subscription = new Subscription();
                subscription.setFollower(friend);
                subscription.setFollowing(user);
                subscription.setCreatedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
            }

            return true;
        }

        return true;
    }

    /**
     * Проверяет, что все дружеские связи имеют обоюдные подписки.
     * При обнаружении несоответствий автоматически восстанавливает подписки.
     *
     * @return статистика по исправленным связям
     */
    @Transactional
    public Map<String, Long> verifyAllFriendships() {
        long fixedCount = 0;
        long checkedCount = 0;

        List<Friendship> acceptedFriendships = friendshipRepository.findAll().stream()
                .filter(f -> f.getStatus() == Friendship.FriendshipStatus.ACCEPTED)
                .toList();

        for (Friendship friendship : acceptedFriendships) {
            User user = friendship.getRequester();
            User friend = friendship.getAddressee();

            boolean userFollowsFriend = subscriptionRepository.isFollowing(user.getId().longValue(), friend.getId().longValue());
            boolean friendFollowsUser = subscriptionRepository.isFollowing(friend.getId().longValue(), user.getId().longValue());

            checkedCount++;

            if (!userFollowsFriend || !friendFollowsUser) {
                if (!userFollowsFriend) {
                    Subscription subscription = new Subscription();
                    subscription.setFollower(user);
                    subscription.setFollowing(friend);
                    subscription.setCreatedAt(LocalDateTime.now());
                    subscriptionRepository.save(subscription);
                    fixedCount++;
                }

                if (!friendFollowsUser) {
                    Subscription subscription = new Subscription();
                    subscription.setFollower(friend);
                    subscription.setFollowing(user);
                    subscription.setCreatedAt(LocalDateTime.now());
                    subscriptionRepository.save(subscription);
                    fixedCount++;
                }
            }
        }

        return Map.of(
                "checked", checkedCount,
                "fixed", fixedCount
        );
    }

    /**
     * Проверяет, что конкретные пользователи являются друзьями и подписаны друг на друга.
     *
     * @param userId идентификатор первого пользователя
     * @param friendId идентификатор второго пользователя
     * @return true если пользователи являются друзьями и подписаны друг на друга
     */
    @Transactional(readOnly = true)
    public boolean areFriendsWithSubscriptions(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        User friend = userRepository.findById(friendId).orElse(null);

        if (user == null || friend == null) {
            return false;
        }

        boolean areFriends = friendshipRepository.areFriends(user, friend);
        boolean userFollowsFriend = subscriptionRepository.isFollowing(userId, friendId);
        boolean friendFollowsUser = subscriptionRepository.isFollowing(friendId, userId);

        return areFriends && userFollowsFriend && friendFollowsUser;
    }
}