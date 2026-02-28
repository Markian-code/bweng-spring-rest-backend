package at.technikum.springrestbackend.service;

import at.technikum.springrestbackend.dto.BookCreateRequestDto;
import at.technikum.springrestbackend.dto.BookResponseDto;
import at.technikum.springrestbackend.dto.BookUpdateRequestDto;
import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.BookCondition;
import at.technikum.springrestbackend.entity.ExchangeType;
import at.technikum.springrestbackend.entity.ListingStatus;
import at.technikum.springrestbackend.entity.User;
import at.technikum.springrestbackend.exception.BadRequestException;
import at.technikum.springrestbackend.exception.ForbiddenOperationException;
import at.technikum.springrestbackend.exception.ResourceNotFoundException;
import at.technikum.springrestbackend.repository.BookRepository;
import at.technikum.springrestbackend.specification.BookSpecification;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(final BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<BookResponseDto> getLatestPublicBooks(
            final Pageable pageable,
            final BookCondition condition,
            final ExchangeType exchangeType,
            final String language,
            final String search
    ) {
        Specification<Book> spec = BookSpecification.buildPublicFilter(
                condition, exchangeType, language, search);
        return bookRepository.findAll(spec, pageable).map(this::toBookResponseDto);
    }

    public BookResponseDto getPublicBookById(final Long bookId) {
        Book book = getBookEntityById(bookId);
        if (book.getStatus() != ListingStatus.AVAILABLE) {
            throw new ResourceNotFoundException("Book listing not found with id: " + bookId);
        }
        return toBookResponseDto(book);
    }

    public List<BookResponseDto> getBooksOfUser(final User currentUser) {
        requireAuthenticatedUser(currentUser);
        return bookRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toBookResponseDto)
                .toList();
    }

    public List<BookResponseDto> getAllBooksForAdmin(final User currentUser) {
        requireAdmin(currentUser);
        return bookRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toBookResponseDto)
                .toList();
    }

    @Transactional
    public BookResponseDto createBook(
            final BookCreateRequestDto request,
            final User currentUser
    ) {
        requireAuthenticatedUser(currentUser);
        Book book = new Book();
        applyCreateFields(book, request);
        book.setOwner(currentUser);
        book.setStatus(ListingStatus.AVAILABLE);
        Book saved = bookRepository.save(book);
        return toBookResponseDto(saved);
    }

    @Transactional
    public BookResponseDto updateBook(
            final Long bookId,
            final BookUpdateRequestDto request,
            final User currentUser
    ) {
        requireAuthenticatedUser(currentUser);
        Book book = getBookEntityById(bookId);
        requireOwnerOrAdmin(book, currentUser);
        applyUpdateFields(book, request);
        Book saved = bookRepository.save(book);
        return toBookResponseDto(saved);
    }

    @Transactional
    public void deleteBook(final Long bookId, final User currentUser) {
        requireAuthenticatedUser(currentUser);
        Book book = getBookEntityById(bookId);
        requireOwnerOrAdmin(book, currentUser);
        bookRepository.delete(book);
    }

    public Book getBookEntityById(final Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book listing not found with id: " + bookId));
    }

    @Transactional
    public BookResponseDto updateBookImageMetadata(
            final Long bookId,
            final String imageUrl,
            final String imageObjectKey,
            final String imageContentType,
            final User currentUser
    ) {
        requireAuthenticatedUser(currentUser);
        Book book = getBookEntityById(bookId);
        requireOwnerOrAdmin(book, currentUser);

        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BadRequestException("Image URL is required");
        }
        if (imageObjectKey == null || imageObjectKey.isBlank()) {
            throw new BadRequestException("Image object key is required");
        }
        if (imageContentType == null || imageContentType.isBlank()) {
            throw new BadRequestException("Image content type is required");
        }

        book.setImageUrl(imageUrl.trim());
        book.setImageObjectKey(imageObjectKey.trim());
        book.setImageContentType(imageContentType.trim());
        Book saved = bookRepository.save(book);
        return toBookResponseDto(saved);
    }

    @Transactional
    public BookResponseDto clearBookImageMetadata(
            final Long bookId,
            final User currentUser
    ) {
        requireAuthenticatedUser(currentUser);
        Book book = getBookEntityById(bookId);
        requireOwnerOrAdmin(book, currentUser);

        book.setImageUrl(null);
        book.setImageObjectKey(null);
        book.setImageContentType(null);
        Book saved = bookRepository.save(book);
        return toBookResponseDto(saved);
    }

    public void verifyBookOwnerOrAdmin(final Long bookId, final User currentUser) {
        requireAuthenticatedUser(currentUser);
        Book book = getBookEntityById(bookId);
        requireOwnerOrAdmin(book, currentUser);
    }

    private void applyCreateFields(final Book book, final BookCreateRequestDto request) {
        book.setTitle(request.getTitle().trim());
        book.setAuthorName(request.getAuthorName().trim());
        book.setDescription(request.getDescription().trim());
        book.setLanguage(normalizeNullableString(request.getLanguage()));
        book.setCondition(request.getCondition());
        book.setExchangeType(request.getExchangeType());
    }

    private void applyUpdateFields(final Book book, final BookUpdateRequestDto request) {
        book.setTitle(request.getTitle().trim());
        book.setAuthorName(request.getAuthorName().trim());
        book.setDescription(request.getDescription().trim());
        book.setLanguage(normalizeNullableString(request.getLanguage()));
        book.setCondition(request.getCondition());
        book.setExchangeType(request.getExchangeType());
        book.setStatus(request.getStatus());
    }

    private String normalizeNullableString(final String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private void requireAuthenticatedUser(final User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new BadRequestException("Authenticated user is required");
        }
    }

    private void requireOwnerOrAdmin(final Book book, final User currentUser) {
        boolean isOwner = book.getOwner() != null
                && book.getOwner().getId() != null
                && book.getOwner().getId().equals(currentUser.getId());

        if (isOwner || isAdmin(currentUser)) {
            return;
        }
        throw new ForbiddenOperationException(
                "You are not allowed to modify this book listing");
    }

    private void requireAdmin(final User currentUser) {
        requireAuthenticatedUser(currentUser);
        if (!isAdmin(currentUser)) {
            throw new ForbiddenOperationException("Admin privileges required");
        }
    }

    private boolean isAdmin(final User user) {
        return user != null
                && user.getRole() != null
                && "ADMIN".equals(user.getRole().name());
    }

    private BookResponseDto toBookResponseDto(final Book book) {
        BookResponseDto dto = new BookResponseDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthorName(book.getAuthorName());
        dto.setDescription(book.getDescription());
        dto.setLanguage(book.getLanguage());
        dto.setCondition(book.getCondition());
        dto.setExchangeType(book.getExchangeType());
        dto.setStatus(book.getStatus());
        dto.setImageUrl(book.getImageUrl());
        dto.setImageContentType(book.getImageContentType());

        if (book.getOwner() != null) {
            dto.setOwnerId(book.getOwner().getId());
            dto.setOwnerUsername(book.getOwner().getUsername());
        }
        dto.setCreatedAt(book.getCreatedAt());
        dto.setUpdatedAt(book.getUpdatedAt());
        return dto;
    }
}
