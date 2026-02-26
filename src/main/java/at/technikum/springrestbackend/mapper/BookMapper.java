package at.technikum.springrestbackend.mapper;

import at.technikum.springrestbackend.dto.BookCreateRequestDto;
import at.technikum.springrestbackend.dto.BookResponseDto;
import at.technikum.springrestbackend.dto.BookUpdateRequestDto;
import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.User;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public Book toEntity(final BookCreateRequestDto dto, final User owner) {
        if (dto == null) {
            return null;
        }

        Book book = new Book();
        book.setTitle(dto.getTitle().trim());
        book.setAuthorName(dto.getAuthorName().trim());
        book.setDescription(dto.getDescription().trim());
        book.setLanguage(dto.getLanguage() == null ? null : dto.getLanguage().trim());
        book.setCondition(dto.getCondition());
        book.setExchangeType(dto.getExchangeType());
        book.setOwner(owner);

        return book;
    }

    public void updateEntityFromDto(final BookUpdateRequestDto dto, final Book book) {
        if (dto == null || book == null) {
            return;
        }

        book.setTitle(dto.getTitle().trim());
        book.setAuthorName(dto.getAuthorName().trim());
        book.setDescription(dto.getDescription().trim());
        book.setLanguage(dto.getLanguage() == null ? null : dto.getLanguage().trim());
        book.setCondition(dto.getCondition());
        book.setExchangeType(dto.getExchangeType());
        book.setStatus(dto.getStatus());
    }

    public BookResponseDto toResponseDto(final Book book) {
        if (book == null) {
            return null;
        }

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

    public List<BookResponseDto> toResponseDtoList(final List<Book> books) {
        if (books == null) {
            return List.of();
        }

        return books.stream()
                .map(this::toResponseDto)
                .toList();
    }
}