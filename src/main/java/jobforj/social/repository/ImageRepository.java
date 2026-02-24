package jobforj.social.repository;

import jobforj.social.model.Image;
import jobforj.social.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByPost(Post post);

    List<Image> findByPostId(Long postId);

    @Query("SELECT i FROM Image i WHERE i.post.id IN :postIds")
    List<Image> findAllByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM Image i WHERE i.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Image i WHERE i.id IN :imageIds")
    void deleteAllByIds(@Param("imageIds") List<Long> imageIds);

    @Query("SELECT COUNT(i) FROM Image i WHERE i.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
}
