package at.technikum.springrestbackend.mapper;

import at.technikum.springrestbackend.dto.BookCreateRequestDto;
import at.technikum.springrestbackend.dto.BookResponseDto;
import at.technikum.springrestbackend.dto.BookUpdateRequestDto;
import at.technikum.springrestbackend.dto.CommentCreateRequestDto;
import at.technikum.springrestbackend.dto.CommentResponseDto;
import at.technikum.springrestbackend.dto.CommentUpdateRequestDto;
import at.technikum.springrestbackend.dto.UserResponseDto;
import at.technikum.springrestbackend.dto.UserUpdateRequestDto;
import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.BookCondition;
import at.technikum.springrestbackend.entity.Comment;
import at.technikum.springrestbackend.entity.ExchangeType;
import at.technikum.springrestbackend.entity.ListingStatus;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mappers")
class MapperTest {

    //  fixture helpers

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername("user_" + id);
        user.setPasswordHash("hash");
        user.setCountryCode("AT");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return user;
    }

    private Book buildBook(Long id, User owner) {
        Book book = new Book();
        book.setId(id);
        book.setTitle("Book " + id);
        book.setAuthorName("Author");
        book.setDescription("Description");
        book.setCondition(BookCondition.GOOD);
        book.setExchangeType(ExchangeType.EXCHANGE_ONLY);
        book.setStatus(ListingStatus.AVAILABLE);
        book.setOwner(owner);
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

    //  BookMapper

    @Nested
    @DisplayName("BookMapper")
    class BookMapperTests {

        private final BookMapper mapper = new BookMapper();

        @Test
        @DisplayName("toEntity: maps all fields including trimming and null language")
        void toEntity_mapsAllFields() {
            User owner = buildUser(1L, "owner@test.com");
            BookCreateRequestDto dto = new BookCreateRequestDto();
            dto.setTitle("  Clean Code  ");
            dto.setAuthorName("  Robert Martin  ");
            dto.setDescription("  A great book  ");
            dto.setLanguage(null);
            dto.setCondition(BookCondition.NEW);
            dto.setExchangeType(ExchangeType.GIVEAWAY);

            Book book = mapper.toEntity(dto, owner);

            assertThat(book.getTitle()).isEqualTo("Clean Code");
            assertThat(book.getAuthorName()).isEqualTo("Robert Martin");
            assertThat(book.getDescription()).isEqualTo("A great book");
            assertThat(book.getLanguage()).isNull();
            assertThat(book.getCondition()).isEqualTo(BookCondition.NEW);
            assertThat(book.getExchangeType()).isEqualTo(ExchangeType.GIVEAWAY);
            assertThat(book.getOwner()).isEqualTo(owner);
        }

        @Test
        @DisplayName("toEntity: trims language when provided")
        void toEntity_trimsLanguage() {
            User owner = buildUser(1L, "owner@test.com");
            BookCreateRequestDto dto = new BookCreateRequestDto();
            dto.setTitle("Title");
            dto.setAuthorName("Author");
            dto.setDescription("Desc");
            dto.setLanguage("  English  ");
            dto.setCondition(BookCondition.GOOD);
            dto.setExchangeType(ExchangeType.EXCHANGE_ONLY);

            Book book = mapper.toEntity(dto, owner);

            assertThat(book.getLanguage()).isEqualTo("English");
        }

        @Test
        @DisplayName("toEntity: returns null when dto is null")
        void toEntity_returnsNullForNullDto() {
            assertThat(mapper.toEntity(null, buildUser(1L, "a@b.com"))).isNull();
        }

        @Test
        @DisplayName("updateEntityFromDto: updates all mutable fields")
        void updateEntityFromDto_updatesFields() {
            Book book = buildBook(1L, buildUser(1L, "a@b.com"));
            BookUpdateRequestDto dto = new BookUpdateRequestDto();
            dto.setTitle("  New Title  ");
            dto.setAuthorName("  New Author  ");
            dto.setDescription("  New Desc  ");
            dto.setLanguage(null);
            dto.setCondition(BookCondition.USED);
            dto.setExchangeType(ExchangeType.GIVEAWAY);
            dto.setStatus(ListingStatus.RESERVED);

            mapper.updateEntityFromDto(dto, book);

            assertThat(book.getTitle()).isEqualTo("New Title");
            assertThat(book.getAuthorName()).isEqualTo("New Author");
            assertThat(book.getDescription()).isEqualTo("New Desc");
            assertThat(book.getLanguage()).isNull();
            assertThat(book.getCondition()).isEqualTo(BookCondition.USED);
            assertThat(book.getStatus()).isEqualTo(ListingStatus.RESERVED);
        }

        @Test
        @DisplayName("updateEntityFromDto: no-op when dto is null")
        void updateEntityFromDto_noOpForNullDto() {
            Book book = buildBook(1L, buildUser(1L, "a@b.com"));
            String originalTitle = book.getTitle();

            mapper.updateEntityFromDto(null, book);

            assertThat(book.getTitle()).isEqualTo(originalTitle);
        }

        @Test
        @DisplayName("updateEntityFromDto: no-op when book is null")
        void updateEntityFromDto_noOpForNullBook() {
            BookUpdateRequestDto dto = new BookUpdateRequestDto();
            dto.setTitle("Title");
            dto.setAuthorName("Author");
            dto.setDescription("Desc");
            dto.setCondition(BookCondition.GOOD);
            dto.setExchangeType(ExchangeType.EXCHANGE_ONLY);
            dto.setStatus(ListingStatus.AVAILABLE);

            mapper.updateEntityFromDto(dto, null);
        }

        @Test
        @DisplayName("toResponseDto: maps all fields including owner")
        void toResponseDto_mapsAllFields() {
            User owner = buildUser(2L, "owner@test.com");
            Book book = buildBook(10L, owner);
            book.setLanguage("English");
            book.setImageUrl("http://example.com/img.jpg");
            book.setImageContentType("image/jpeg");

            BookResponseDto dto = mapper.toResponseDto(book);

            assertThat(dto.getId()).isEqualTo(10L);
            assertThat(dto.getTitle()).isEqualTo("Book 10");
            assertThat(dto.getLanguage()).isEqualTo("English");
            assertThat(dto.getImageUrl()).isEqualTo("http://example.com/img.jpg");
            assertThat(dto.getOwnerId()).isEqualTo(2L);
            assertThat(dto.getOwnerUsername()).isEqualTo("user_2");
        }

        @Test
        @DisplayName("toResponseDto: returns null for null book")
        void toResponseDto_returnsNullForNullBook() {
            assertThat(mapper.toResponseDto(null)).isNull();
        }

        @Test
        @DisplayName("toResponseDto: null owner fields when book has no owner")
        void toResponseDto_nullOwnerFields() {
            Book book = buildBook(5L, null);

            BookResponseDto dto = mapper.toResponseDto(book);

            assertThat(dto.getOwnerId()).isNull();
            assertThat(dto.getOwnerUsername()).isNull();
        }

        @Test
        @DisplayName("toResponseDtoList: maps all items in the list")
        void toResponseDtoList_mapsAllItems() {
            User owner = buildUser(1L, "a@b.com");
            List<Book> books = List.of(buildBook(1L, owner), buildBook(2L, owner));

            List<BookResponseDto> result = mapper.toResponseDtoList(books);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(1).getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("toResponseDtoList: returns empty list for null input")
        void toResponseDtoList_emptyForNull() {
            assertThat(mapper.toResponseDtoList(null)).isEmpty();
        }
    }

    //  UserMapper

    @Nested
    @DisplayName("UserMapper")
    class UserMapperTests {

        private final UserMapper mapper = new UserMapper();

        @Test
        @DisplayName("toResponseDto: maps all fields")
        void toResponseDto_mapsAllFields() {
            User user = buildUser(3L, "alice@test.com");
            user.setRole(Role.ADMIN);
            user.setEnabled(false);
            user.setProfilePictureUrl("http://example.com/pic.jpg");

            UserResponseDto dto = mapper.toResponseDto(user);

            assertThat(dto.getId()).isEqualTo(3L);
            assertThat(dto.getEmail()).isEqualTo("alice@test.com");
            assertThat(dto.getUsername()).isEqualTo("user_3");
            assertThat(dto.getCountryCode()).isEqualTo("AT");
            assertThat(dto.getRole()).isEqualTo(Role.ADMIN);
            assertThat(dto.isEnabled()).isFalse();
            assertThat(dto.getProfilePictureUrl()).isEqualTo("http://example.com/pic.jpg");
        }

        @Test
        @DisplayName("toResponseDto: returns null for null user")
        void toResponseDto_returnsNullForNull() {
            assertThat(mapper.toResponseDto(null)).isNull();
        }

        @Test
        @DisplayName("updateEntityFromDto: updates all non-null fields with trimming")
        void updateEntityFromDto_updatesFields() {
            User user = buildUser(1L, "a@test.com");
            UserUpdateRequestDto dto = new UserUpdateRequestDto();
            dto.setUsername("  newname  ");
            dto.setCountryCode("  at  ");
            dto.setProfilePictureUrl("  http://pic.jpg  ");

            mapper.updateEntityFromDto(dto, user);

            assertThat(user.getUsername()).isEqualTo("newname");
            assertThat(user.getCountryCode()).isEqualTo("AT");
            assertThat(user.getProfilePictureUrl()).isEqualTo("http://pic.jpg");
        }

        @Test
        @DisplayName("updateEntityFromDto: skips null fields")
        void updateEntityFromDto_skipsNullFields() {
            User user = buildUser(1L, "a@test.com");
            user.setUsername("original");
            UserUpdateRequestDto dto = new UserUpdateRequestDto();
            // all fields null

            mapper.updateEntityFromDto(dto, user);

            assertThat(user.getUsername()).isEqualTo("original");
        }

        @Test
        @DisplayName("updateEntityFromDto: no-op when dto is null")
        void updateEntityFromDto_noOpForNullDto() {
            User user = buildUser(1L, "a@test.com");
            String originalUsername = user.getUsername();

            mapper.updateEntityFromDto(null, user);

            assertThat(user.getUsername()).isEqualTo(originalUsername);
        }

        @Test
        @DisplayName("updateEntityFromDto: no-op when user is null")
        void updateEntityFromDto_noOpForNullUser() {
            UserUpdateRequestDto dto = new UserUpdateRequestDto();
            dto.setUsername("name");

            mapper.updateEntityFromDto(dto, null);
        }
    }

   //  CommentMapper

    @Nested
    @DisplayName("CommentMapper")
    class CommentMapperTests {

        private final CommentMapper mapper = new CommentMapper();

        @Test
        @DisplayName("toEntity: maps content (trimmed), book, author")
        void toEntity_mapsAllFields() {
            User author = buildUser(1L, "a@test.com");
            Book book = buildBook(10L, author);
            CommentCreateRequestDto dto = new CommentCreateRequestDto();
            dto.setBookId(10L);
            dto.setContent("  Great book!  ");

            Comment comment = mapper.toEntity(dto, book, author);

            assertThat(comment.getContent()).isEqualTo("Great book!");
            assertThat(comment.getBook()).isEqualTo(book);
            assertThat(comment.getAuthor()).isEqualTo(author);
        }

        @Test
        @DisplayName("toEntity: returns null when dto is null")
        void toEntity_returnsNullForNullDto() {
            assertThat(mapper.toEntity(null, buildBook(1L, null), buildUser(1L, "a@b.com"))).isNull();
        }

        @Test
        @DisplayName("updateEntityFromDto: updates content with trimming")
        void updateEntityFromDto_updatesContent() {
            User author = buildUser(1L, "a@test.com");
            Comment comment = buildComment(1L, "old", author, buildBook(1L, author));
            CommentUpdateRequestDto dto = new CommentUpdateRequestDto();
            dto.setContent("  updated content  ");

            mapper.updateEntityFromDto(dto, comment);

            assertThat(comment.getContent()).isEqualTo("updated content");
        }

        @Test
        @DisplayName("updateEntityFromDto: no-op when dto is null")
        void updateEntityFromDto_noOpForNullDto() {
            User author = buildUser(1L, "a@test.com");
            Comment comment = buildComment(1L, "original", author, buildBook(1L, author));

            mapper.updateEntityFromDto(null, comment);

            assertThat(comment.getContent()).isEqualTo("original");
        }

        @Test
        @DisplayName("updateEntityFromDto: no-op when comment is null")
        void updateEntityFromDto_noOpForNullComment() {
            CommentUpdateRequestDto dto = new CommentUpdateRequestDto();
            dto.setContent("content");

            mapper.updateEntityFromDto(dto, null);
        }

        @Test
        @DisplayName("toResponseDto: maps all fields including book and author")
        void toResponseDto_mapsAllFields() {
            User author = buildUser(5L, "author@test.com");
            Book book = buildBook(10L, author);
            Comment comment = buildComment(1L, "Nice!", author, book);

            CommentResponseDto dto = mapper.toResponseDto(comment);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getContent()).isEqualTo("Nice!");
            assertThat(dto.getBookId()).isEqualTo(10L);
            assertThat(dto.getAuthorId()).isEqualTo(5L);
            assertThat(dto.getAuthorUsername()).isEqualTo("user_5");
        }

        @Test
        @DisplayName("toResponseDto: returns null for null comment")
        void toResponseDto_returnsNullForNull() {
            assertThat(mapper.toResponseDto(null)).isNull();
        }

        @Test
        @DisplayName("toResponseDto: null bookId when comment has no book")
        void toResponseDto_nullBookId() {
            User author = buildUser(1L, "a@test.com");
            Comment comment = buildComment(1L, "text", author, null);

            CommentResponseDto dto = mapper.toResponseDto(comment);

            assertThat(dto.getBookId()).isNull();
        }

        @Test
        @DisplayName("toResponseDto: null authorId/username when comment has no author")
        void toResponseDto_nullAuthorFields() {
            Book book = buildBook(1L, null);
            Comment comment = buildComment(1L, "text", null, book);

            CommentResponseDto dto = mapper.toResponseDto(comment);

            assertThat(dto.getAuthorId()).isNull();
            assertThat(dto.getAuthorUsername()).isNull();
        }

        @Test
        @DisplayName("toResponseDtoList: maps all items")
        void toResponseDtoList_mapsAllItems() {
            User author = buildUser(1L, "a@b.com");
            Book book = buildBook(1L, author);
            List<Comment> comments = List.of(
                    buildComment(1L, "First", author, book),
                    buildComment(2L, "Second", author, book)
            );

            List<CommentResponseDto> result = mapper.toResponseDtoList(comments);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("toResponseDtoList: returns empty list for null input")
        void toResponseDtoList_emptyForNull() {
            assertThat(mapper.toResponseDtoList(null)).isEmpty();
        }
    }
}