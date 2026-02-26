package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.BookResponseDto;
import at.technikum.springrestbackend.dto.CommentResponseDto;
import at.technikum.springrestbackend.dto.UserResponseDto;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.security.CustomUserDetails;
import at.technikum.springrestbackend.service.BookService;
import at.technikum.springrestbackend.service.CommentService;
import at.technikum.springrestbackend.service.UserService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final BookService bookService;
    private final CommentService commentService;

    public AdminController(
            final UserService userService,
            final BookService bookService,
            final CommentService commentService
    ) {
        this.userService = userService;
        this.bookService = bookService;
        this.commentService = commentService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> getAllUsers(
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        List<UserResponseDto> response = userService.getAllUsersForAdmin(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponseDto> getUserById(
            @PathVariable final Long userId,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        UserResponseDto response = userService.getUserByIdForAdmin(userId, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{userId}/enabled")
    public ResponseEntity<UserResponseDto> setUserEnabled(
            @PathVariable final Long userId,
            @RequestParam final boolean enabled,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        UserResponseDto response = userService.setUserEnabled(userId, enabled, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{userId}/toggle-enabled")
    public ResponseEntity<UserResponseDto> toggleUserEnabled(
            @PathVariable final Long userId,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        UserResponseDto response = userService.toggleUserEnabled(userId, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/books")
    public ResponseEntity<List<BookResponseDto>> getAllBooks(
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        List<BookResponseDto> response = bookService.getAllBooksForAdmin(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comments")
    public ResponseEntity<List<CommentResponseDto>> getAllComments(
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        List<CommentResponseDto> response = commentService.getAllCommentsForAdmin(currentUser);
        return ResponseEntity.ok(response);
    }

    private User resolveCurrentUser(final CustomUserDetails principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Authenticated user is required");
        }

        return userService.getUserEntityById(principal.getId());
    }
}