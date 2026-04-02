package jobforj.social.dto;

import jobforj.social.model.Post;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPostsDto {

    /** Идентификатор пользователя. */
    private Long userId;

    /** Имя пользователя. */
    private String username;

    /** Список публикаций пользователя. */
    private List<Post> posts;
}