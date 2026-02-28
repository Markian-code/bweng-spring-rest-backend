package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.BookCreateRequestDto;
import at.technikum.springrestbackend.dto.BookResponseDto;
import at.technikum.springrestbackend.dto.BookUpdateRequestDto;
import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.security.CustomUserDetails;
import at.technikum.springrestbackend.service.BookService;
import at.technikum.springrestbackend.service.FileStorageService;
import at.technikum.springrestbackend.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public BookController(
            final BookService bookService,
            final UserService userService,
            final FileStorageService fileStorageService
    ) {
        this.bookService = bookService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<List<BookResponseDto>> getLatestPublicBooks() {
        List<BookResponseDto> response = bookService.getLatestPublicBooks();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<BookResponseDto> getPublicBookById(
            @PathVariable final Long bookId
    ) {
        BookResponseDto response = bookService.getPublicBookById(bookId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<BookResponseDto>> getMyBooks(
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        List<BookResponseDto> response = bookService.getBooksOfUser(currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<BookResponseDto> createBook(
            @Valid @RequestBody final BookCreateRequestDto request,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        BookResponseDto response = bookService.createBook(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{bookId}")
    public ResponseEntity<BookResponseDto> updateBook(
            @PathVariable final Long bookId,
            @Valid @RequestBody final BookUpdateRequestDto request,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        BookResponseDto response = bookService.updateBook(bookId, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable final Long bookId,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        String imageObjectKey = bookService.getBookEntityById(bookId).getImageObjectKey();
        bookService.deleteBook(bookId, currentUser);
        fileStorageService.deleteObjectQuietly(imageObjectKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{bookId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookResponseDto> uploadBookImage(
            @PathVariable final Long bookId,
            @RequestPart("file") final MultipartFile file,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        bookService.verifyBookOwnerOrAdmin(bookId, currentUser);
        Book existingBook = bookService.getBookEntityById(bookId);
        String oldObjectKey = existingBook.getImageObjectKey();
        FileStorageService.StoredFileResult storedFile = null;

        try {
            storedFile = fileStorageService.uploadBookImage(file);
            BookResponseDto response = bookService.updateBookImageMetadata(
                    bookId, storedFile.fileUrl(), storedFile.objectKey(),
                    storedFile.contentType(), currentUser);

            if (oldObjectKey != null && !oldObjectKey.isBlank()
                    && !oldObjectKey.equals(storedFile.objectKey())) {
                fileStorageService.deleteObjectQuietly(oldObjectKey);
            }
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            if (storedFile != null && storedFile.objectKey() != null
                    && !storedFile.objectKey().isBlank()) {
                fileStorageService.deleteObjectQuietly(storedFile.objectKey());
            }
            throw ex;
        }
    }

    @DeleteMapping("/{bookId}/image")
    public ResponseEntity<BookResponseDto> deleteBookImage(
            @PathVariable final Long bookId,
            @AuthenticationPrincipal final CustomUserDetails principal
    ) {
        User currentUser = resolveCurrentUser(principal);
        Book existingBook = bookService.getBookEntityById(bookId);
        String oldObjectKey = existingBook.getImageObjectKey();

        BookResponseDto response = bookService.clearBookImageMetadata(bookId, currentUser);
        if (oldObjectKey != null && !oldObjectKey.isBlank()) {
            fileStorageService.deleteObjectQuietly(oldObjectKey);
        }
        return ResponseEntity.ok(response);
    }

    private User resolveCurrentUser(final CustomUserDetails principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadRequestException("Authenticated user is required");
        }
        return userService.getUserEntityById(principal.getId());
    }
}
