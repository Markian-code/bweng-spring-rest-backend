package at.technikum.springrestbackend.service;

import at.technikum.springrestbackend.dto.CommentCreateRequestDto;
import at.technikum.springrestbackend.dto.CommentResponseDto;
import at.technikum.springrestbackend.dto.CommentUpdateRequestDto;
import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.Comment;
import at.technikum.springrestbackend.entity.ListingStatus;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.ForbiddenOperationException;
import at.technikum.springrestbackend.exception.ResourceNotFoundException;
import at.technikum.springrestbackend.repository.CommentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final BookService bookService;

    public CommentService(
            final CommentRepository commentRepository,
            final BookService bookService
    ) {
        this.commentRepository = commentRepository;
        this.bookService = bookService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsForPublicBook(final Long bookId) {
        Book book = bookService.getBookEntityById(bookId);

        if (book.getStatus() != ListingStatus.AVAILABLE) {
            throw new ResourceNotFoundException("Public book listing not found");
        }

        return commentRepository.findAllByBookIdOrderByCreatedAtAsc(bookId)
                .stream()
                .map(this::toCommentResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsOfUser(final User currentUser) {
        return commentRepository.findAllByAuthorIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toCommentResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getAllCommentsForAdmin(final User currentUser) {
        requireAdmin(currentUser);

        return commentRepository.findAll()
                .stream()
                .map(this::toCommentResponseDto)
                .toList();
    }

    public CommentResponseDto createComment(
            final CommentCreateRequestDto request,
            final User currentUser
    ) {
        Book book = bookService.getBookEntityById(request.getBookId());

        if (book.getStatus() != ListingStatus.AVAILABLE) {
            throw new ForbiddenOperationException(
                    "Comments can only be added to available book listings"
            );
        }

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setBook(book);
        comment.setAuthor(currentUser);

        Comment saved = commentRepository.save(comment);
        return toCommentResponseDto(saved);
    }

    public CommentResponseDto updateComment(
            final Long commentId,
            final CommentUpdateRequestDto request,
            final User currentUser
    ) {
        Comment comment = getCommentEntityById(commentId);
        requireCommentAuthorOrAdmin(comment, currentUser);

        comment.setContent(request.getContent());

        Comment saved = commentRepository.save(comment);
        return toCommentResponseDto(saved);
    }

    public void deleteComment(final Long commentId, final User currentUser) {
        Comment comment = getCommentEntityById(commentId);
        requireCommentAuthorOrAdmin(comment, currentUser);

        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public Comment getCommentEntityById(final Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comment not found with id: " + commentId
                ));
    }

    private void requireCommentAuthorOrAdmin(
            final Comment comment,
            final User currentUser
    ) {
        boolean isAuthor = comment.getAuthor() != null
                && comment.getAuthor().getId() != null
                && comment.getAuthor().getId().equals(currentUser.getId());

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new ForbiddenOperationException(
                    "You are not allowed to modify this comment"
            );
        }
    }

    private void requireAdmin(final User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Admin access required");
        }
    }

    private CommentResponseDto toCommentResponseDto(final Comment comment) {
        CommentResponseDto dto = new CommentResponseDto();

        dto.setId(comment.getId());
        dto.setContent(comment.getContent());

        if (comment.getBook() != null) {
            dto.setBookId(comment.getBook().getId());
        }

        if (comment.getAuthor() != null) {
            dto.setAuthorId(comment.getAuthor().getId());
            dto.setAuthorUsername(comment.getAuthor().getUsername());
        }

        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());

        return dto;
    }
}