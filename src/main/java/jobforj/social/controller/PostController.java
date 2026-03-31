package jobforj.social.controller;

import jobforj.social.model.Post;
import jobforj.social.service.PostService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/post")
public class PostController {

    PostService postService;

    @PostMapping
    public ResponseEntity<Post> save(@RequestBody Post post) {
        postService.save(post);
        var uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(post.getId())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(uri)
                .body(post);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Post> get(@PathVariable Long postId) {
        return postService.findById(postId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping({ "", "/" })
    public ResponseEntity<List<Post>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending()
        );
        Page<Post> postPage = postService.findAllByOrderByCreatedAtDesc(sortedPageable);
        List<Post> posts = postPage.getContent();
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(postPage.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(postPage.getTotalPages()))
                .header("X-Current-Page", String.valueOf(postPage.getNumber()))
                .header("X-Page-Size", String.valueOf(postPage.getSize()))
                .body(posts);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> update(@RequestBody Post post) {
        postService.updatePost(Long.valueOf(post.getId()), post.getTitle(), post.getContent());
        if (postService.update(post)){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> removeById(@PathVariable Long postId) {
        if (postService.deletePost(postId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
