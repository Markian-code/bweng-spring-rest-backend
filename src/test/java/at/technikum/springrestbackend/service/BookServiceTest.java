package at.technikum.springrestbackend.service;

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
import at.technikum.springrestbackend.exception.ForbiddenOperationException;
import at.technikum.springrestbackend.exception.ResourceNotFoundException;
import at.technikum.springrestbackend.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private User buildUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user_" + id);
        user.setEmail("user" + id + "@test.com");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private Book buildBook(Long id, User owner, ListingStatus status) {
        Book book = new Book();
        book.setId(id);
        book.setTitle("Title " + id);
        book.setAuthorName("Author " + id);
        book.setDescription("Description " + id);
        book.setLanguage("en");
        book.setCondition(BookCondition.GOOD);
        book.setExchangeType(ExchangeType.EXCHANGE_ONLY);
        book.setStatus(status);
        book.setOwner(owner);
        return book;
    }

    private BookCreateRequestDto buildCreateRequest(String language) {
        BookCreateRequestDto dto = new BookCreateRequestDto();
        dto.setTitle("Clean Code");
        dto.setAuthorName("Robert Martin");
        dto.setDescription("A great book about clean code.");
        dto.setLanguage(language);
        dto.setCondition(BookCondition.GOOD);
        dto.setExchangeType(ExchangeType.EXCHANGE_ONLY);
        return dto;
    }

    private BookUpdateRequestDto buildUpdateRequest() {
        BookUpdateRequestDto dto = new BookUpdateRequestDto();
        dto.setTitle("Updated Title");
        dto.setAuthorName("Updated Author");
        dto.setDescription("Updated Description");
        dto.setLanguage("de");
        dto.setCondition(BookCondition.USED);
        dto.setExchangeType(ExchangeType.GIVEAWAY);
        dto.setStatus(ListingStatus.RESERVED);
        return dto;
    }

    @Nested
    @DisplayName("getLatestPublicBooks()")
    class GetLatestPublicBooks {

        @Test
        @DisplayName("returns mapped DTOs ordered by date descending")
        void returnsMappedList() {
            User owner = buildUser(1L, Role.USER);
            List<Book> books = List.of(
                    buildBook(2L, owner, ListingStatus.AVAILABLE),
                    buildBook(1L, owner, ListingStatus.AVAILABLE)
            );
            when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(books));

            Page<BookResponseDto> result = bookService.getLatestPublicBooks(
                    Pageable.unpaged(), null, null, null, null);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getId()).isEqualTo(2L);
            assertThat(result.getContent().get(1).getId()).isEqualTo(1L);
            assertThat(result.getContent().get(0).getOwnerId()).isEqualTo(owner.getId());
            assertThat(result.getContent().get(0).getOwnerUsername()).isEqualTo(owner.getUsername());
        }

        @Test
        @DisplayName("returns empty page when no available books exist")
        void returnsEmptyListWhenNoBooksAvailable() {
            when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            assertThat(bookService.getLatestPublicBooks(
                    Pageable.unpaged(), null, null, null, null).getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPublicBookById(Long)")
    class GetPublicBookById {

        @Test
        @DisplayName("returns DTO for an AVAILABLE book")
        void returnsBookWhenAvailable() {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            BookResponseDto result = bookService.getPublicBookById(10L);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getTitle()).isEqualTo("Title 10");
        }

        @ParameterizedTest(name = "status={0} → ResourceNotFoundException")
        @ValueSource(strings = {"RESERVED", "EXCHANGED"})
        @DisplayName("throws ResourceNotFoundException for non-AVAILABLE status (security by obscurity)")
        void throwsForNonAvailableStatus(String statusName) {
            ListingStatus status = ListingStatus.valueOf(statusName);
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, status);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertThatThrownBy(() -> bookService.getPublicBookById(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("10");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when book does not exist")
        void throwsWhenBookNotFound() {
            when(bookRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.getPublicBookById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getBooksOfUser(User)")
    class GetBooksOfUser {

        @Test
        @DisplayName("returns books of the authenticated user")
        void returnsBooksForAuthenticatedUser() {
            User owner = buildUser(5L, Role.USER);
            List<Book> books = List.of(buildBook(1L, owner, ListingStatus.AVAILABLE));
            when(bookRepository.findAllByOwnerIdOrderByCreatedAtDesc(5L)).thenReturn(books);

            List<BookResponseDto> result = bookService.getBooksOfUser(owner);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("throws BadRequestException when user is null")
        void throwsWhenUserIsNull() {
            assertThatThrownBy(() -> bookService.getBooksOfUser(null))
                    .isInstanceOf(BadRequestException.class);
            verify(bookRepository, never()).findAllByOwnerIdOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("throws BadRequestException when user has null id (transient entity guard)")
        void throwsWhenUserHasNullId() {
            User userWithoutId = new User();

            assertThatThrownBy(() -> bookService.getBooksOfUser(userWithoutId))
                    .isInstanceOf(BadRequestException.class);
            verify(bookRepository, never()).findAllByOwnerIdOrderByCreatedAtDesc(any());
        }
    }

    @Nested
    @DisplayName("getAllBooksForAdmin(User)")
    class GetAllBooksForAdmin {

        @Test
        @DisplayName("returns all books when caller is ADMIN")
        void returnsAllBooksForAdmin() {
            User admin = buildUser(1L, Role.ADMIN);
            User owner = buildUser(2L, Role.USER);
            List<Book> books = List.of(
                    buildBook(1L, owner, ListingStatus.AVAILABLE),
                    buildBook(2L, owner, ListingStatus.RESERVED)
            );
            when(bookRepository.findAllByOrderByCreatedAtDesc()).thenReturn(books);

            List<BookResponseDto> result = bookService.getAllBooksForAdmin(admin);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for non-admin user")
        void throwsForNonAdminUser() {
            User normalUser = buildUser(1L, Role.USER);

            assertThatThrownBy(() -> bookService.getAllBooksForAdmin(normalUser))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("Admin");
            verify(bookRepository, never()).findAllByOrderByCreatedAtDesc();
        }

        @Test
        @DisplayName("throws BadRequestException when user is null (requireAuthenticatedUser guard)")
        void throwsWhenUserIsNull() {
            assertThatThrownBy(() -> bookService.getAllBooksForAdmin(null))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("createBook(BookCreateRequestDto, User)")
    class CreateBook {

        @Test
        @DisplayName("creates book with AVAILABLE status and maps result to DTO")
        void createsBookSuccessfully() {
            User owner = buildUser(1L, Role.USER);
            BookCreateRequestDto request = buildCreateRequest("  english  ");

            when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
                Book b = invocation.getArgument(0);
                b.setId(42L);
                return b;
            });

            BookResponseDto result = bookService.createBook(request, owner);

            assertThat(result.getId()).isEqualTo(42L);
            assertThat(result.getStatus()).isEqualTo(ListingStatus.AVAILABLE);

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(captor.capture());
            assertThat(captor.getValue().getLanguage()).isEqualTo("english");
            assertThat(captor.getValue().getOwner()).isEqualTo(owner);
        }

        @Test
        @DisplayName("creates book with null language — normalizeNullableString returns null")
        void createsBookWithNullLanguage() {
            User owner = buildUser(1L, Role.USER);
            BookCreateRequestDto request = buildCreateRequest(null);

            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
                Book b = inv.getArgument(0);
                b.setId(1L);
                return b;
            });

            bookService.createBook(request, owner);

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(captor.capture());
            assertThat(captor.getValue().getLanguage()).isNull();
        }

        @Test
        @DisplayName("normalizes blank language to null — prevents whitespace-only DB values")
        void createsBookWithBlankLanguageNormalizesToNull() {
            User owner = buildUser(1L, Role.USER);
            BookCreateRequestDto request = buildCreateRequest("   ");

            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
                Book b = inv.getArgument(0);
                b.setId(1L);
                return b;
            });

            bookService.createBook(request, owner);

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(captor.capture());
            assertThat(captor.getValue().getLanguage()).isNull();
        }

        @Test
        @DisplayName("throws BadRequestException for unauthenticated user")
        void throwsForNullUser() {
            assertThatThrownBy(() -> bookService.createBook(buildCreateRequest("en"), null))
                    .isInstanceOf(BadRequestException.class);
            verify(bookRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateBook(Long, BookUpdateRequestDto, User)")
    class UpdateBook {

        @Test
        @DisplayName("owner can update their own book")
        void ownerCanUpdateBook() {
            User owner = buildUser(1L, Role.USER);
            Book existing = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BookResponseDto result = bookService.updateBook(10L, buildUpdateRequest(), owner);

            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getStatus()).isEqualTo(ListingStatus.RESERVED);
        }

        @Test
        @DisplayName("admin can update any book regardless of ownership")
        void adminCanUpdateAnyBook() {
            User regularOwner = buildUser(2L, Role.USER);
            User admin = buildUser(99L, Role.ADMIN);
            Book existing = buildBook(10L, regularOwner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BookResponseDto result = bookService.updateBook(10L, buildUpdateRequest(), admin);

            assertThat(result.getTitle()).isEqualTo("Updated Title");
        }

        @Test
        @DisplayName("throws ForbiddenOperationException when non-owner non-admin tries to update")
        void throwsForUnauthorizedUser() {
            User owner = buildUser(1L, Role.USER);
            User stranger = buildUser(2L, Role.USER);
            Book existing = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> bookService.updateBook(10L, buildUpdateRequest(), stranger))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("not allowed");
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("EDGE: book with null owner — regular user gets Forbidden")
        void bookWithNullOwner_regularUserIsForbidden() {
            Book bookWithNoOwner = buildBook(10L, null, ListingStatus.AVAILABLE);
            User stranger = buildUser(5L, Role.USER);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(bookWithNoOwner));

            assertThatThrownBy(() -> bookService.updateBook(10L, buildUpdateRequest(), stranger))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("EDGE: book with null owner — admin can still update")
        void bookWithNullOwner_adminCanUpdate() {
            Book bookWithNoOwner = buildBook(10L, null, ListingStatus.AVAILABLE);
            User admin = buildUser(99L, Role.ADMIN);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(bookWithNoOwner));
            when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BookResponseDto result = bookService.updateBook(10L, buildUpdateRequest(), admin);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when book does not exist")
        void throwsWhenBookNotFound() {
            User user = buildUser(1L, Role.USER);
            when(bookRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.updateBook(99L, buildUpdateRequest(), user))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteBook(Long, User)")
    class DeleteBook {

        @Test
        @DisplayName("owner can delete their own book")
        void ownerCanDelete() {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            bookService.deleteBook(10L, owner);

            verify(bookRepository).delete(book);
        }

        @Test
        @DisplayName("admin can delete any book")
        void adminCanDelete() {
            User regularOwner = buildUser(1L, Role.USER);
            User admin = buildUser(99L, Role.ADMIN);
            Book book = buildBook(10L, regularOwner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            bookService.deleteBook(10L, admin);

            verify(bookRepository).delete(book);
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for non-owner")
        void throwsForNonOwner() {
            User owner = buildUser(1L, Role.USER);
            User stranger = buildUser(2L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertThatThrownBy(() -> bookService.deleteBook(10L, stranger))
                    .isInstanceOf(ForbiddenOperationException.class);
            verify(bookRepository, never()).delete(any(Book.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when book does not exist")
        void throwsWhenNotFound() {
            User user = buildUser(1L, Role.USER);
            when(bookRepository.findById(55L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.deleteBook(55L, user))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateBookImageMetadata()")
    class UpdateBookImageMetadata {

        @Test
        @DisplayName("updates image metadata fields and trims whitespace")
        void updatesImageMetadataSuccessfully() {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BookResponseDto result = bookService.updateBookImageMetadata(
                    10L, "  http://img.url/a.jpg  ", "  obj/key  ", "  image/jpeg  ", owner
            );

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(captor.capture());
            assertThat(captor.getValue().getImageUrl()).isEqualTo("http://img.url/a.jpg");
            assertThat(captor.getValue().getImageObjectKey()).isEqualTo("obj/key");
            assertThat(captor.getValue().getImageContentType()).isEqualTo("image/jpeg");
            assertThat(result.getImageUrl()).isEqualTo("http://img.url/a.jpg");
        }

        @ParameterizedTest(name = "imageUrl=\"{0}\" → BadRequestException")
        @ValueSource(strings = {"", "   "})
        @DisplayName("EDGE: throws BadRequestException for blank imageUrl")
        void throwsForBlankImageUrl(String blankUrl) {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertThatThrownBy(() ->
                    bookService.updateBookImageMetadata(10L, blankUrl, "key", "type", owner))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Image URL");
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BadRequestException for null imageUrl")
        void throwsForNullImageUrl() {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertThatThrownBy(() ->
                    bookService.updateBookImageMetadata(10L, null, "key", "type", owner))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Image URL");
        }

        @ParameterizedTest(name = "imageObjectKey=\"{0}\" → BadRequestException")
        @ValueSource(strings = {"", "   "})
        @DisplayName("EDGE: throws BadRequestException for blank imageObjectKey")
        void throwsForBlankImageObjectKey(String blankKey) {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertThatThrownBy(() ->
                    bookService.updateBookImageMetadata(10L, "http://url", blankKey, "type", owner))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("object key");
        }

        @ParameterizedTest(name = "imageContentType=\"{0}\" → BadRequestException")
        @ValueSource(strings = {"", "   "})
        @DisplayName("EDGE: throws BadRequestException for blank imageContentType")
        void throwsForBlankContentType(String blankType) {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertThatThrownBy(() ->
                    bookService.updateBookImageMetadata(10L, "http://url", "key", blankType, owner))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("content type");
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for non-owner")
        void throwsForNonOwner() {
            User owner = buildUser(1L, Role.USER);
            User stranger = buildUser(2L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertThatThrownBy(() ->
                    bookService.updateBookImageMetadata(10L, "url", "key", "type", stranger))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    @Nested
    @DisplayName("clearBookImageMetadata(Long, User)")
    class ClearBookImageMetadata {

        @Test
        @DisplayName("sets all image fields to null in the saved entity")
        void setsAllImageFieldsToNull() {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            book.setImageUrl("http://old-url.com");
            book.setImageObjectKey("old/key");
            book.setImageContentType("image/png");
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
            when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            bookService.clearBookImageMetadata(10L, owner);

            ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(captor.capture());
            assertThat(captor.getValue().getImageUrl()).isNull();
            assertThat(captor.getValue().getImageObjectKey()).isNull();
            assertThat(captor.getValue().getImageContentType()).isNull();
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for non-owner")
        void throwsForNonOwner() {
            User owner = buildUser(1L, Role.USER);
            User stranger = buildUser(2L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertThatThrownBy(() -> bookService.clearBookImageMetadata(10L, stranger))
                    .isInstanceOf(ForbiddenOperationException.class);
            verify(bookRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("verifyBookOwnerOrAdmin(Long, User)")
    class VerifyBookOwnerOrAdmin {

        @Test
        @DisplayName("does not throw for the book owner")
        void doesNotThrowForOwner() {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            bookService.verifyBookOwnerOrAdmin(10L, owner);
        }

        @Test
        @DisplayName("does not throw for admin user")
        void doesNotThrowForAdmin() {
            User owner = buildUser(1L, Role.USER);
            User admin = buildUser(99L, Role.ADMIN);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            bookService.verifyBookOwnerOrAdmin(10L, admin);
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for unrelated user")
        void throwsForUnrelatedUser() {
            User owner = buildUser(1L, Role.USER);
            User stranger = buildUser(2L, Role.USER);
            Book book = buildBook(10L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

            assertThatThrownBy(() -> bookService.verifyBookOwnerOrAdmin(10L, stranger))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("throws BadRequestException for null user (auth guard)")
        void throwsForNullUser() {
            assertThatThrownBy(() -> bookService.verifyBookOwnerOrAdmin(10L, null))
                    .isInstanceOf(BadRequestException.class);
            verify(bookRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("getBookEntityById(Long)")
    class GetBookEntityById {

        @Test
        @DisplayName("returns Book entity when found")
        void returnsEntity() {
            User owner = buildUser(1L, Role.USER);
            Book book = buildBook(7L, owner, ListingStatus.AVAILABLE);
            when(bookRepository.findById(7L)).thenReturn(Optional.of(book));

            Book result = bookService.getBookEntityById(7L);

            assertThat(result.getId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookService.getBookEntityById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("DTO mapping (toBookResponseDto)")
    class DtoMapping {

        @Test
        @DisplayName("EDGE: book without owner maps ownerId/ownerUsername to null (no NPE)")
        void bookWithoutOwnerMapsSafely() {
            Book book = buildBook(1L, null, ListingStatus.AVAILABLE);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            BookResponseDto result = bookService.getPublicBookById(1L);

            assertThat(result.getOwnerId()).isNull();
            assertThat(result.getOwnerUsername()).isNull();
        }
    }
}