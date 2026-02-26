package at.technikum.springrestbackend.repository;

import at.technikum.springrestbackend.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByBookIdOrderByCreatedAtAsc(Long bookId);

    List<Comment> findAllByAuthorIdOrderByCreatedAtDesc(Long authorId);
}