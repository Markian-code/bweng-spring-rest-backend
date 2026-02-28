package at.technikum.springrestbackend.specification;

import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.BookCondition;
import at.technikum.springrestbackend.entity.ExchangeType;
import at.technikum.springrestbackend.entity.ListingStatus;
import at.technikum.springrestbackend.entity.Role;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.repository.BookRepository;
import at.technikum.springrestbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("BookSpecification.buildPublicFilter()")
class BookSpecificationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        User u = new User();
        u.setEmail("owner@test.com");
        u.setUsername("owner");
        u.setPasswordHash("hash");
        u.setCountryCode("AT");
        u.setRole(Role.USER);
        u.setEnabled(true);
        owner = userRepository.save(u);
    }

    private Book persist(String title, String authorName, BookCondition condition,
                         ExchangeType exchangeType, String language, ListingStatus status) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthorName(authorName);
        book.setDescription("A description");
        book.setCondition(condition);
        book.setExchangeType(exchangeType);
        book.setLanguage(language);
        book.setStatus(status);
        book.setOwner(owner);
        return bookRepository.save(book);
    }

    @Nested
    @DisplayName("status filter — always AVAILABLE")
    class StatusFilter {

        @Test
        @DisplayName("only returns AVAILABLE books when RESERVED and EXCHANGED also exist")
        void filtersToAvailableOnly() {
            persist("Available Book", "Author A", BookCondition.NEW,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("Reserved Book", "Author B", BookCondition.GOOD,
                    ExchangeType.EXCHANGE_ONLY, "en", ListingStatus.RESERVED);
            persist("Exchanged Book", "Author C", BookCondition.USED,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.EXCHANGED);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, null, null, null),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Available Book");
        }

        @Test
        @DisplayName("returns empty page when no AVAILABLE books exist")
        void returnsEmptyWhenNoAvailable() {
            persist("Reserved Book", "Author", BookCondition.NEW,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.RESERVED);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, null, null, null),
                    Pageable.unpaged());

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("condition filter")
    class ConditionFilter {

        @Test
        @DisplayName("filters by BookCondition when specified")
        void filtersByCondition() {
            persist("New Book", "Author", BookCondition.NEW,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("Used Book", "Author", BookCondition.USED,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(BookCondition.NEW, null, null, null),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("New Book");
        }

        @Test
        @DisplayName("returns all AVAILABLE books when condition filter is null")
        void ignoresConditionWhenNull() {
            persist("New Book", "Author", BookCondition.NEW,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("Used Book", "Author", BookCondition.USED,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, null, null, null),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("exchangeType filter")
    class ExchangeTypeFilter {

        @Test
        @DisplayName("filters by ExchangeType when specified")
        void filtersByExchangeType() {
            persist("Giveaway Book", "Author", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("Exchange Book", "Author", BookCondition.GOOD,
                    ExchangeType.EXCHANGE_ONLY, "en", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, ExchangeType.GIVEAWAY, null, null),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Giveaway Book");
        }
    }

    @Nested
    @DisplayName("language filter")
    class LanguageFilter {

        @Test
        @DisplayName("filters by language case-insensitively")
        void filtersByLanguageCaseInsensitive() {
            persist("English Book", "Author", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("German Book", "Author", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "de", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, null, "EN", null),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("English Book");
        }

        @Test
        @DisplayName("ignores blank language filter — returns all AVAILABLE books")
        void ignoresBlankLanguage() {
            persist("English Book", "Author", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("German Book", "Author", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "de", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, null, "   ", null),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("search filter (title OR authorName LIKE, case-insensitive)")
    class SearchFilter {

        @Test
        @DisplayName("matches by title using partial case-insensitive LIKE")
        void matchesByTitle() {
            persist("The Hobbit", "Tolkien", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("Clean Code", "Robert Martin", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, null, null, "hobbit"),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Hobbit");
        }

        @Test
        @DisplayName("matches by authorName using partial case-insensitive LIKE")
        void matchesByAuthorName() {
            persist("The Hobbit", "Tolkien", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("Clean Code", "Robert Martin", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, null, null, "TOLKIEN"),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Hobbit");
        }

        @Test
        @DisplayName("ignores blank search term — returns all AVAILABLE books")
        void ignoresBlankSearch() {
            persist("The Hobbit", "Tolkien", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("Clean Code", "Robert Martin", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, null, null, "   "),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("partial term matches both title and author across different books")
        void partialTermMatchesTitleAndAuthorAcrossBooks() {
            persist("The Hobbit", "Tolkien", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("Clean Code", "Robert Martin", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("Martin's World", "Unknown", BookCondition.GOOD,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(null, null, null, "martin"),
                    Pageable.unpaged());

            // "Clean Code" by Robert Martin + "Martin's World" by Unknown
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("combined filters")
    class CombinedFilters {

        @Test
        @DisplayName("applies all non-null filters with AND logic")
        void appliesAllFilters() {
            persist("English New Giveaway", "Author", BookCondition.NEW,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("English Used Giveaway", "Author", BookCondition.USED,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("German New Giveaway", "Author", BookCondition.NEW,
                    ExchangeType.GIVEAWAY, "de", ListingStatus.AVAILABLE);
            persist("English New Exchange", "Author", BookCondition.NEW,
                    ExchangeType.EXCHANGE_ONLY, "en", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(
                            BookCondition.NEW, ExchangeType.GIVEAWAY, "en", null),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("English New Giveaway");
        }

        @Test
        @DisplayName("search combined with condition filter narrows result correctly")
        void searchCombinedWithCondition() {
            persist("The Hobbit", "Tolkien", BookCondition.NEW,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);
            persist("The Hobbit (Used)", "Tolkien", BookCondition.USED,
                    ExchangeType.GIVEAWAY, "en", ListingStatus.AVAILABLE);

            Page<Book> result = bookRepository.findAll(
                    BookSpecification.buildPublicFilter(BookCondition.NEW, null, null, "hobbit"),
                    Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Hobbit");
        }
    }
}
