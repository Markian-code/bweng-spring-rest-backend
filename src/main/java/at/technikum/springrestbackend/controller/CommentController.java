package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.CommentCreateRequestDto;
import at.technikum.springrestbackend.dto.CommentResponseDto;
import at.technikum.springrestbackend.dto.CommentUpdateRequestDto;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.security.CustomUserDetails;
import at.technikum.springrestbackend.service.CommentService;
import at.technikum.springrestbackend.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    public CommentController(
            final CommentService commentService,
            final UserService userService
    ) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentsForBook(
            @PathVariable final Long bookId
    ) {
        List<CommentResponseDto> response = commentService.getCommentsForPublicBook(bookId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<CommentResponseDto>> getMyComments(
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        List<CommentResponseDto> response = commentService.getCommentsOfUser(currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/book/{bookId}")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable final Long bookId,
            @Valid @RequestBody final CommentCreateRequestDto request,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);

        // sync path variable -> DTO (recommended if service expects request.getBookId())
        request.setBookId(bookId);

        CommentResponseDto response = commentService.createComment(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable final Long commentId,
            @Valid @RequestBody final CommentUpdateRequestDto request,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        CommentResponseDto response = commentService.updateComment(commentId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable final Long commentId,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    private User resolveCurrentUser(final CustomUserDetails principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Authenticated user is required");
        }

        return userService.getUserEntityById(principal.getId());
    }
}