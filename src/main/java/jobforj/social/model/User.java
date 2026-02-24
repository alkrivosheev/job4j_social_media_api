package jobforj.social.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username", name = "uk_users_username"),
                @UniqueConstraint(columnNames = "email", name = "uk_users_email")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"posts", "sentFriendRequests", "receivedFriendRequests",
        "followers", "following", "sentMessages", "receivedMessages"})
@ToString(exclude = {"posts", "sentFriendRequests", "receivedFriendRequests",
        "followers", "following", "sentMessages", "receivedMessages"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$",
            message = "Username must be 3-50 characters and contain only letters, numbers, and underscores")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "requester", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Friendship> sentFriendRequests = new ArrayList<>();

    @OneToMany(mappedBy = "addressee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Friendship> receivedFriendRequests = new ArrayList<>();

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Subscription> following = new HashSet<>();

    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Subscription> followers = new HashSet<>();

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Message> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Message> receivedMessages = new ArrayList<>();

    // Helper methods
    public void addPost(Post post) {
        posts.add(post);
        post.setUser(this);
    }

    public void removePost(Post post) {
        posts.remove(post);
        post.setUser(null);
    }

    public void addFollower(User follower) {
        Subscription subscription = Subscription.builder()
                .follower(follower)
                .following(this)
                .build();
        followers.add(subscription);
        follower.getFollowing().add(subscription);
    }

    public void removeFollower(User follower) {
        followers.removeIf(sub -> sub.getFollower().equals(follower));
        follower.getFollowing().removeIf(sub -> sub.getFollowing().equals(this));
    }

    public void follow(User userToFollow) {
        Subscription subscription = Subscription.builder()
                .follower(this)
                .following(userToFollow)
                .build();
        following.add(subscription);
        userToFollow.getFollowers().add(subscription);
    }

    public void unfollow(User userToUnfollow) {
        following.removeIf(sub -> sub.getFollowing().equals(userToUnfollow));
        userToUnfollow.getFollowers().removeIf(sub -> sub.getFollower().equals(this));
    }
}