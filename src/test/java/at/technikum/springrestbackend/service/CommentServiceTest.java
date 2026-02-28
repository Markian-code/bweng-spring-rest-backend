package at.technikum.springrestbackend.service;

import at.technikum.springrestbackend.dto.CommentCreateRequestDto;
import at.technikum.springrestbackend.dto.CommentResponseDto;
import at.technikum.springrestbackend.dto.CommentUpdateRequestDto;
import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.BookCondition;
import at.technikum.springrestbackend.entity.Comment;
import at.technikum.springrestbackend.entity.ExchangeType;
import at.technikum.springrestbackend.entity.ListingStatus;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.ForbiddenOperationException;
import at.technikum.springrestbackend.exception.ResourceNotFoundException;
import at.technikum.springrestbackend.repository.CommentRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private BookService bookService;

    @InjectMocks
    private CommentService commentService;

    //  fixture helpers

    private User buildUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user_" + id);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private Book buildBook(Long id, ListingStatus status) {
        Book book = new Book();
        book.setId(id);
        book.setTitle("Book " + id);
        book.setAuthorName("Author");
        book.setDescription("Desc");
        book.setCondition(BookCondition.GOOD);
        book.setExchangeType(ExchangeType.EXCHANGE_ONLY);
        book.setStatus(status);
        return book;
    }

    private Comment buildComment(Long id, String content, User author, Book book) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setContent(content);
        comment.setAuthor(author);
        comment.setBook(book);
        return comment;
    }

    private CommentCreateRequestDto buildCreateRequest(Long bookId, String content) {
        CommentCreateRequestDto dto = new CommentCreateRequestDto();
        dto.setBookId(bookId);
        dto.setContent(content);
        return dto;
    }

    private CommentUpdateRequestDto buildUpdateRequest(String content) {
        CommentUpdateRequestDto dto = new CommentUpdateRequestDto();
        dto.setContent(content);
        return dto;
    }

   //  getCommentsForPublicBook

    @Nested
    @DisplayName("getCommentsForPublicBook(Long)")
    class GetCommentsForPublicBook {

        @Test
        @DisplayName("returns comments ordered by date ascending for AVAILABLE book")
        void returnsCommentsForAvailableBook() {
            User author = buildUser(1L, Role.USER);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            List<Comment> comments = List.of(
                    buildComment(1L, "First", author, book),
                    buildComment(2L, "Second", author, book)
            );
            when(bookService.getBookEntityById(10L)).thenReturn(book);
            when(commentRepository.findAllByBookIdOrderByCreatedAtAsc(10L)).thenReturn(comments);

            List<CommentResponseDto> result = commentService.getCommentsForPublicBook(10L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getContent()).isEqualTo("First");
            assertThat(result.get(0).getBookId()).isEqualTo(10L);
            assertThat(result.get(0).getAuthorId()).isEqualTo(1L);
            assertThat(result.get(0).getAuthorUsername()).isEqualTo("user_1");
        }

        @Test
        @DisplayName("returns empty list when book has no comments")
        void returnsEmptyListWhenNoComments() {
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            when(bookService.getBookEntityById(10L)).thenReturn(book);
            when(commentRepository.findAllByBookIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

            assertThat(commentService.getCommentsForPublicBook(10L)).isEmpty();
        }

        @ParameterizedTest(name = "status={0} → ResourceNotFoundException")
        @ValueSource(strings = {"RESERVED", "EXCHANGED"})
        @DisplayName("throws ResourceNotFoundException for non-AVAILABLE book status")
        void throwsForNonAvailableBook(String statusName) {
            Book book = buildBook(10L, ListingStatus.valueOf(statusName));
            when(bookService.getBookEntityById(10L)).thenReturn(book);

            assertThatThrownBy(() -> commentService.getCommentsForPublicBook(10L))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(commentRepository, never()).findAllByBookIdOrderByCreatedAtAsc(any());
        }
    }

   //  getCommentsOfUser

    @Nested
    @DisplayName("getCommentsOfUser(User)")
    class GetCommentsOfUser {

        @Test
        @DisplayName("returns comments of the given user ordered by date descending")
        void returnsUserComments() {
            User author = buildUser(5L, Role.USER);
            Book book = buildBook(1L, ListingStatus.AVAILABLE);
            List<Comment> comments = List.of(buildComment(1L, "Great book!", author, book));
            when(commentRepository.findAllByAuthorIdOrderByCreatedAtDesc(5L)).thenReturn(comments);

            List<CommentResponseDto> result = commentService.getCommentsOfUser(author);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("Great book!");
        }

        @Test
        @DisplayName("returns empty list when user has no comments")
        void returnsEmptyListForUserWithNoComments() {
            User author = buildUser(5L, Role.USER);
            when(commentRepository.findAllByAuthorIdOrderByCreatedAtDesc(5L))
                    .thenReturn(List.of());

            assertThat(commentService.getCommentsOfUser(author)).isEmpty();
        }
    }

    //  getAllCommentsForAdmin

    @Nested
    @DisplayName("getAllCommentsForAdmin(User)")
    class GetAllCommentsForAdmin {

        @Test
        @DisplayName("returns all comments when caller is ADMIN")
        void returnsAllCommentsForAdmin() {
            User admin = buildUser(1L, Role.ADMIN);
            User author = buildUser(2L, Role.USER);
            Book book = buildBook(1L, ListingStatus.AVAILABLE);
            when(commentRepository.findAll()).thenReturn(List.of(
                    buildComment(1L, "Hello", author, book),
                    buildComment(2L, "World", author, book)
            ));

            List<CommentResponseDto> result = commentService.getAllCommentsForAdmin(admin);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for non-admin user")
        void throwsForNonAdmin() {
            User regularUser = buildUser(1L, Role.USER);

            assertThatThrownBy(() -> commentService.getAllCommentsForAdmin(regularUser))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("Admin");
            verify(commentRepository, never()).findAll();
        }

        @Test
        @DisplayName("returns empty list when no comments exist")
        void returnsEmptyList() {
            User admin = buildUser(1L, Role.ADMIN);
            when(commentRepository.findAll()).thenReturn(List.of());

            assertThat(commentService.getAllCommentsForAdmin(admin)).isEmpty();
        }

        @Test
        @DisplayName("EDGE: throws NullPointerException when currentUser is null (missing null guard — inconsistent with UserService)")
        void throwsNPEWhenCurrentUserIsNull() {
            assertThatThrownBy(() -> commentService.getAllCommentsForAdmin(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

   //  createComment

    @Nested
    @DisplayName("createComment(CommentCreateRequestDto, User)")
    class CreateComment {

        @Test
        @DisplayName("creates comment and links it to the book and author")
        void createsCommentSuccessfully() {
            User author = buildUser(1L, Role.USER);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            when(bookService.getBookEntityById(10L)).thenReturn(book);
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(99L);
                return c;
            });

            CommentResponseDto result = commentService.createComment(
                    buildCreateRequest(10L, "Great listing!"), author
            );

            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getContent()).isEqualTo("Great listing!");

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue().getBook()).isEqualTo(book);
            assertThat(captor.getValue().getAuthor()).isEqualTo(author);
            assertThat(captor.getValue().getContent()).isEqualTo("Great listing!");
        }

        @ParameterizedTest(name = "status={0} → ForbiddenOperationException")
        @ValueSource(strings = {"RESERVED", "EXCHANGED"})
        @DisplayName("throws ForbiddenOperationException when book is not AVAILABLE")
        void throwsWhenBookNotAvailable(String statusName) {
            Book book = buildBook(10L, ListingStatus.valueOf(statusName));
            when(bookService.getBookEntityById(10L)).thenReturn(book);

            assertThatThrownBy(() ->
                    commentService.createComment(buildCreateRequest(10L, "text"), buildUser(1L, Role.USER)))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("available");
            verify(commentRepository, never()).save(any());
        }
    }

    //  updateComment

    @Nested
    @DisplayName("updateComment(Long, CommentUpdateRequestDto, User)")
    class UpdateComment {

        @Test
        @DisplayName("author can update their own comment")
        void authorCanUpdate() {
            User author = buildUser(1L, Role.USER);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment comment = buildComment(5L, "old content", author, book);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));
            when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CommentResponseDto result = commentService.updateComment(
                    5L, buildUpdateRequest("new content"), author
            );

            assertThat(result.getContent()).isEqualTo("new content");
        }

        @Test
        @DisplayName("admin can update any comment regardless of authorship")
        void adminCanUpdateAnyComment() {
            User author = buildUser(1L, Role.USER);
            User admin = buildUser(99L, Role.ADMIN);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment comment = buildComment(5L, "old content", author, book);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));
            when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CommentResponseDto result = commentService.updateComment(
                    5L, buildUpdateRequest("admin edit"), admin
            );

            assertThat(result.getContent()).isEqualTo("admin edit");
        }

        @Test
        @DisplayName("throws ForbiddenOperationException when caller is neither author nor admin")
        void throwsForUnrelatedUser() {
            User author = buildUser(1L, Role.USER);
            User stranger = buildUser(2L, Role.USER);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment comment = buildComment(5L, "text", author, book);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() ->
                    commentService.updateComment(5L, buildUpdateRequest("hacked"), stranger))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("not allowed");
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when comment does not exist")
        void throwsWhenCommentNotFound() {
            when(commentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    commentService.updateComment(999L, buildUpdateRequest("x"), buildUser(1L, Role.USER)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("EDGE: comment with null author — regular user gets Forbidden, admin succeeds")
        void commentWithNullAuthor_regularUserIsForbidden() {
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment orphanComment = buildComment(5L, "text", null, book);
            User stranger = buildUser(2L, Role.USER);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(orphanComment));

            assertThatThrownBy(() ->
                    commentService.updateComment(5L, buildUpdateRequest("x"), stranger))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("EDGE: comment with null author — admin can still update")
        void commentWithNullAuthor_adminCanUpdate() {
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment orphanComment = buildComment(5L, "text", null, book);
            User admin = buildUser(99L, Role.ADMIN);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(orphanComment));
            when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CommentResponseDto result = commentService.updateComment(
                    5L, buildUpdateRequest("fixed"), admin
            );

            assertThat(result.getContent()).isEqualTo("fixed");
        }

        @Test
        @DisplayName("EDGE: throws NullPointerException when currentUser is null (missing null guard)")
        void throwsNPEWhenCurrentUserIsNull() {
            User author = buildUser(1L, Role.USER);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment comment = buildComment(5L, "text", author, book);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() ->
                    commentService.updateComment(5L, buildUpdateRequest("x"), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    //  deleteComment

    @Nested
    @DisplayName("deleteComment(Long, User)")
    class DeleteComment {

        @Test
        @DisplayName("author can delete their own comment")
        void authorCanDelete() {
            User author = buildUser(1L, Role.USER);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment comment = buildComment(5L, "text", author, book);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));

            commentService.deleteComment(5L, author);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("admin can delete any comment")
        void adminCanDelete() {
            User author = buildUser(1L, Role.USER);
            User admin = buildUser(99L, Role.ADMIN);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment comment = buildComment(5L, "text", author, book);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));

            commentService.deleteComment(5L, admin);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("throws ForbiddenOperationException for unrelated user")
        void throwsForStranger() {
            User author = buildUser(1L, Role.USER);
            User stranger = buildUser(2L, Role.USER);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment comment = buildComment(5L, "text", author, book);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.deleteComment(5L, stranger))
                    .isInstanceOf(ForbiddenOperationException.class);
            verify(commentRepository, never()).delete(any(Comment.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when comment does not exist")
        void throwsWhenNotFound() {
            when(commentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(404L, buildUser(1L, Role.USER)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("EDGE: throws NullPointerException when currentUser is null (missing null guard)")
        void throwsNPEWhenCurrentUserIsNull() {
            User author = buildUser(1L, Role.USER);
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment comment = buildComment(5L, "text", author, book);
            when(commentRepository.findById(5L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.deleteComment(5L, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    //  getCommentEntityById

    @Nested
    @DisplayName("getCommentEntityById(Long)")
    class GetCommentEntityById {

        @Test
        @DisplayName("returns Comment entity when found")
        void returnsEntity() {
            User author = buildUser(1L, Role.USER);
            Comment comment = buildComment(7L, "hello", author, buildBook(1L, ListingStatus.AVAILABLE));
            when(commentRepository.findById(7L)).thenReturn(Optional.of(comment));

            Comment result = commentService.getCommentEntityById(7L);

            assertThat(result.getId()).isEqualTo(7L);
            assertThat(result.getContent()).isEqualTo("hello");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when comment does not exist")
        void throwsWhenNotFound() {
            when(commentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.getCommentEntityById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    //  DTO mapping (toCommentResponseDto)

    @Nested
    @DisplayName("DTO mapping (toCommentResponseDto)")
    class DtoMapping {

        @Test
        @DisplayName("EDGE: comment without book sets bookId to null (no NPE)")
        void commentWithoutBookMapsSafely() {
            User author = buildUser(1L, Role.USER);
            Comment comment = buildComment(1L, "text", author, null);
            when(commentRepository.findAllByAuthorIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(comment));

            List<CommentResponseDto> result = commentService.getCommentsOfUser(author);

            assertThat(result.get(0).getBookId()).isNull();
        }

        @Test
        @DisplayName("EDGE: comment without author sets authorId/authorUsername to null (no NPE)")
        void commentWithoutAuthorMapsSafely() {
            Book book = buildBook(10L, ListingStatus.AVAILABLE);
            Comment comment = buildComment(1L, "text", null, book);
            when(bookService.getBookEntityById(10L)).thenReturn(book);
            when(commentRepository.findAllByBookIdOrderByCreatedAtAsc(10L))
                    .thenReturn(List.of(comment));

            List<CommentResponseDto> result = commentService.getCommentsForPublicBook(10L);

            assertThat(result.get(0).getAuthorId()).isNull();
            assertThat(result.get(0).getAuthorUsername()).isNull();
        }
    }
}
