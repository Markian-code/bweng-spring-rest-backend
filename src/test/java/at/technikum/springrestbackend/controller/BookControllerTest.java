package at.technikum.springrestbackend.controller;

import at.technikum.springrestbackend.dto.BookCreateRequestDto;
import at.technikum.springrestbackend.dto.BookResponseDto;
import at.technikum.springrestbackend.dto.BookUpdateRequestDto;
import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.BookCondition;
import at.technikum.springrestbackend.entity.ExchangeType;
import at.technikum.springrestbackend.entity.ListingStatus;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.security.CustomUserDetails;
import at.technikum.springrestbackend.service.BookService;
import at.technikum.springrestbackend.service.FileStorageService;
import at.technikum.springrestbackend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookController")
class BookControllerTest {

    @Mock
    private BookService bookService;
    @Mock
    private UserService userService;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private BookController controller;

    // helpers

    private User buildUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@test.com");
        user.setUsername("user_" + id);
        user.setPasswordHash("hash");
        user.setCountryCode("AT");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private CustomUserDetails buildPrincipal(Long id, Role role) {
        return new CustomUserDetails(buildUser(id, role));
    }

    private Book buildBook(String imageObjectKey) {
        Book book = new Book();
        book.setTitle("Test Book");
        book.setAuthorName("Author");
        book.setDescription("Description");
        book.setCondition(BookCondition.GOOD);
        book.setExchangeType(ExchangeType.EXCHANGE_OR_GIVEAWAY);
        book.setStatus(ListingStatus.AVAILABLE);
        book.setImageObjectKey(imageObjectKey);
        return book;
    }

    private FileStorageService.StoredFileResult storedFile(String objectKey) {
        return new FileStorageService.StoredFileResult(
                objectKey, "http://minio/bucket/" + objectKey, "image/jpeg", 2048L
        );
    }

    // resolveCurrentUser guard (exercised via any endpoint)

    @Nested
    @DisplayName("resolveCurrentUser guard")
    class ResolveCurrentUser {

        @Test
        @DisplayName("throws BadRequestException when principal is null")
        void throwsForNullPrincipal() {
            assertThatThrownBy(() -> controller.getMyBooks(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("throws BadRequestException when principal id is null")
        void throwsForNullPrincipalId() {
            User userWithoutId = new User();
            userWithoutId.setEmail("no-id@test.com");
            userWithoutId.setPasswordHash("hash");
            userWithoutId.setRole(Role.USER);
            userWithoutId.setEnabled(true);
            // id remains null (Long default)
            CustomUserDetails principal = new CustomUserDetails(userWithoutId);

            assertThatThrownBy(() -> controller.getMyBooks(principal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("required");
        }
    }

    //GET /books

    @Nested
    @DisplayName("GET /books")
    class GetLatestPublicBooks {

        @Test
        @DisplayName("returns 200 with list of public books")
        void returns200WithPublicBooks() {
            BookResponseDto dto = new BookResponseDto();
            when(bookService.getLatestPublicBooks()).thenReturn(List.of(dto));

            ResponseEntity<List<BookResponseDto>> result = controller.getLatestPublicBooks();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsExactly(dto);
        }
    }

    // GET /books/{bookId}

    @Nested
    @DisplayName("GET /books/{bookId}")
    class GetPublicBookById {

        @Test
        @DisplayName("returns 200 with the requested book")
        void returns200WithBook() {
            BookResponseDto dto = new BookResponseDto();
            when(bookService.getPublicBookById(42L)).thenReturn(dto);

            ResponseEntity<BookResponseDto> result = controller.getPublicBookById(42L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(dto);
        }
    }

    // GET /books/me

    @Nested
    @DisplayName("GET /books/me")
    class GetMyBooks {

        @Test
        @DisplayName("returns 200 with books of authenticated user")
        void returns200WithMyBooks() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            BookResponseDto dto = new BookResponseDto();

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBooksOfUser(userEntity)).thenReturn(List.of(dto));

            ResponseEntity<List<BookResponseDto>> result = controller.getMyBooks(principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsExactly(dto);
        }
    }

    // POST /books

    @Nested
    @DisplayName("POST /books")
    class CreateBook {

        @Test
        @DisplayName("returns 201 with the created book")
        void returns201WithCreatedBook() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            BookCreateRequestDto request = new BookCreateRequestDto();
            BookResponseDto response = new BookResponseDto();

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.createBook(request, userEntity)).thenReturn(response);

            ResponseEntity<BookResponseDto> result = controller.createBook(request, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isSameAs(response);
        }
    }

    // PUT /books/{bookId}

    @Nested
    @DisplayName("PUT /books/{bookId}")
    class UpdateBook {

        @Test
        @DisplayName("returns 200 with the updated book")
        void returns200WithUpdatedBook() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            BookUpdateRequestDto request = new BookUpdateRequestDto();
            BookResponseDto response = new BookResponseDto();

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.updateBook(10L, request, userEntity)).thenReturn(response);

            ResponseEntity<BookResponseDto> result = controller.updateBook(10L, request, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
        }
    }

    // DELETE /books/{bookId}

    @Nested
    @DisplayName("DELETE /books/{bookId}")
    class DeleteBook {

        @Test
        @DisplayName("returns 204, deletes book and cleans up storage image")
        void returns204AndCleansUpImage() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            Book book = buildBook("books/cover.jpg");

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(5L)).thenReturn(book);

            ResponseEntity<Void> result = controller.deleteBook(5L, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(bookService).deleteBook(5L, userEntity);
            verify(fileStorageService).deleteObjectQuietly("books/cover.jpg");
        }

        @Test
        @DisplayName("returns 204 and still calls deleteObjectQuietly when image key is null")
        void returns204WhenImageKeyIsNull() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            Book book = buildBook(null);

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(5L)).thenReturn(book);

            ResponseEntity<Void> result = controller.deleteBook(5L, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(bookService).deleteBook(5L, userEntity);
            verify(fileStorageService).deleteObjectQuietly(null);
        }
    }

    // POST /books/{bookId}/image

    @Nested
    @DisplayName("POST /books/{bookId}/image")
    class UploadBookImage {

        @Test
        @DisplayName("success: old key differs from new — old object is cleaned up")
        void uploadsImageAndCleansOldKey() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            MultipartFile file = mock(MultipartFile.class);
            Book existing = buildBook("books/old-key.jpg");
            FileStorageService.StoredFileResult stored = storedFile("books/new-key.jpg");
            BookResponseDto response = new BookResponseDto();

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(7L)).thenReturn(existing);
            when(fileStorageService.uploadBookImage(file)).thenReturn(stored);
            when(bookService.updateBookImageMetadata(
                    eq(7L), anyString(), anyString(), anyString(), eq(userEntity)))
                    .thenReturn(response);

            ResponseEntity<BookResponseDto> result = controller.uploadBookImage(7L, file, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
            verify(fileStorageService).deleteObjectQuietly("books/old-key.jpg");
        }

        @Test
        @DisplayName("success: old key is null — no old object cleanup")
        void noCleanupWhenOldKeyIsNull() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            MultipartFile file = mock(MultipartFile.class);
            Book existing = buildBook(null);
            FileStorageService.StoredFileResult stored = storedFile("books/new-key.jpg");

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(7L)).thenReturn(existing);
            when(fileStorageService.uploadBookImage(file)).thenReturn(stored);
            when(bookService.updateBookImageMetadata(
                    eq(7L), anyString(), anyString(), anyString(), eq(userEntity)))
                    .thenReturn(new BookResponseDto());

            controller.uploadBookImage(7L, file, principal);

            verify(fileStorageService, never()).deleteObjectQuietly(anyString());
        }

        @Test
        @DisplayName("success: old key is blank — no old object cleanup")
        void noCleanupWhenOldKeyIsBlank() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            MultipartFile file = mock(MultipartFile.class);
            Book existing = buildBook("   ");
            FileStorageService.StoredFileResult stored = storedFile("books/new-key.jpg");

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(7L)).thenReturn(existing);
            when(fileStorageService.uploadBookImage(file)).thenReturn(stored);
            when(bookService.updateBookImageMetadata(
                    eq(7L), anyString(), anyString(), anyString(), eq(userEntity)))
                    .thenReturn(new BookResponseDto());

            controller.uploadBookImage(7L, file, principal);

            verify(fileStorageService, never()).deleteObjectQuietly(anyString());
        }

        @Test
        @DisplayName("success: old key equals new key — no old object cleanup")
        void noCleanupWhenOldKeyEqualsNewKey() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            MultipartFile file = mock(MultipartFile.class);
            String sameKey = "books/same-key.jpg";
            Book existing = buildBook(sameKey);
            FileStorageService.StoredFileResult stored = storedFile(sameKey);

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(7L)).thenReturn(existing);
            when(fileStorageService.uploadBookImage(file)).thenReturn(stored);
            when(bookService.updateBookImageMetadata(
                    eq(7L), anyString(), anyString(), anyString(), eq(userEntity)))
                    .thenReturn(new BookResponseDto());

            controller.uploadBookImage(7L, file, principal);

            verify(fileStorageService, never()).deleteObjectQuietly(anyString());
        }

        @Test
        @DisplayName("exception after upload: new file is cleaned up and exception is rethrown")
        void cleansUpNewFileWhenDbUpdateFails() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            MultipartFile file = mock(MultipartFile.class);
            Book existing = buildBook(null);
            FileStorageService.StoredFileResult stored = storedFile("books/new-key.jpg");

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(7L)).thenReturn(existing);
            when(fileStorageService.uploadBookImage(file)).thenReturn(stored);
            when(bookService.updateBookImageMetadata(any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("DB failure"));

            assertThatThrownBy(() -> controller.uploadBookImage(7L, file, principal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB failure");

            verify(fileStorageService).deleteObjectQuietly("books/new-key.jpg");
        }

        @Test
        @DisplayName("exception after upload: no cleanup when stored objectKey is null")
        void noCleanupWhenStoredObjectKeyIsNull() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            MultipartFile file = mock(MultipartFile.class);
            Book existing = buildBook(null);
            // Simulate a stored result where objectKey is null (defensive branch L143)
            FileStorageService.StoredFileResult storedWithNullKey =
                    new FileStorageService.StoredFileResult(null, "http://url", "image/jpeg", 1024L);

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(7L)).thenReturn(existing);
            when(fileStorageService.uploadBookImage(file)).thenReturn(storedWithNullKey);
            when(bookService.updateBookImageMetadata(any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("DB failure"));

            assertThatThrownBy(() -> controller.uploadBookImage(7L, file, principal))
                    .isInstanceOf(RuntimeException.class);

            verify(fileStorageService, never()).deleteObjectQuietly(anyString());
        }

        @Test
        @DisplayName("exception after upload: no cleanup when stored objectKey is blank")
        void noCleanupWhenStoredObjectKeyIsBlank() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            MultipartFile file = mock(MultipartFile.class);
            Book existing = buildBook(null);
            // Simulate a stored result where objectKey is blank (defensive branch L144)
            FileStorageService.StoredFileResult storedWithBlankKey =
                    new FileStorageService.StoredFileResult("   ", "http://url", "image/jpeg", 1024L);

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(7L)).thenReturn(existing);
            when(fileStorageService.uploadBookImage(file)).thenReturn(storedWithBlankKey);
            when(bookService.updateBookImageMetadata(any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("DB failure"));

            assertThatThrownBy(() -> controller.uploadBookImage(7L, file, principal))
                    .isInstanceOf(RuntimeException.class);

            verify(fileStorageService, never()).deleteObjectQuietly(anyString());
        }

        @Test
        @DisplayName("exception before upload: no cleanup is performed")
        void noCleanupWhenUploadItselffails() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            MultipartFile file = mock(MultipartFile.class);
            Book existing = buildBook(null);

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(7L)).thenReturn(existing);
            when(fileStorageService.uploadBookImage(file))
                    .thenThrow(new RuntimeException("MinIO unavailable"));

            assertThatThrownBy(() -> controller.uploadBookImage(7L, file, principal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("MinIO unavailable");

            // storedFile was never assigned — deleteObjectQuietly must not be called
            verify(fileStorageService, never()).deleteObjectQuietly(anyString());
        }
    }

    // DELETE /books/{bookId}/image

    @Nested
    @DisplayName("DELETE /books/{bookId}/image")
    class DeleteBookImage {

        @Test
        @DisplayName("returns 200, clears metadata and deletes image from storage")
        void returns200AndDeletesImage() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            Book existing = buildBook("books/cover.jpg");
            BookResponseDto response = new BookResponseDto();

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(9L)).thenReturn(existing);
            when(bookService.clearBookImageMetadata(9L, userEntity)).thenReturn(response);

            ResponseEntity<BookResponseDto> result = controller.deleteBookImage(9L, principal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isSameAs(response);
            verify(fileStorageService).deleteObjectQuietly("books/cover.jpg");
        }

        @Test
        @DisplayName("returns 200 but skips storage deletion when old key is null")
        void skipsStorageDeletionWhenKeyIsNull() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            Book existing = buildBook(null);

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(9L)).thenReturn(existing);
            when(bookService.clearBookImageMetadata(9L, userEntity)).thenReturn(new BookResponseDto());

            controller.deleteBookImage(9L, principal);

            verify(fileStorageService, never()).deleteObjectQuietly(anyString());
        }

        @Test
        @DisplayName("returns 200 but skips storage deletion when old key is blank")
        void skipsStorageDeletionWhenKeyIsBlank() {
            CustomUserDetails principal = buildPrincipal(1L, Role.USER);
            User userEntity = buildUser(1L, Role.USER);
            Book existing = buildBook("   ");

            when(userService.getUserEntityById(1L)).thenReturn(userEntity);
            when(bookService.getBookEntityById(9L)).thenReturn(existing);
            when(bookService.clearBookImageMetadata(9L, userEntity)).thenReturn(new BookResponseDto());

            controller.deleteBookImage(9L, principal);

            verify(fileStorageService, never()).deleteObjectQuietly(anyString());
        }
    }
}
