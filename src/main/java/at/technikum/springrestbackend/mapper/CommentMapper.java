package at.technikum.springrestbackend.mapper;

import at.technikum.springrestbackend.dto.CommentCreateRequestDto;
import at.technikum.springrestbackend.dto.CommentResponseDto;
import at.technikum.springrestbackend.dto.CommentUpdateRequestDto;
import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.Comment;
import at.technikum.springrestbackend.entity.User;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public Comment toEntity(final CommentCreateRequestDto dto, final Book book, final User author) {
        if (dto == null) {
            return null;
        }

        Comment comment = new Comment();
        comment.setContent(dto.getContent().trim());
        comment.setBook(book);
        comment.setAuthor(author);

        return comment;
    }

    public void updateEntityFromDto(final CommentUpdateRequestDto dto, final Comment comment) {
        if (dto == null || comment == null) {
            return;
        }

        comment.setContent(dto.getContent().trim());
    }

    public CommentResponseDto toResponseDto(final Comment comment) {
        if (comment == null) {
            return null;
        }

        CommentResponseDto dto = new CommentResponseDto();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());

        if (comment.getBook() != null) {
            dto.setBookId(comment.getBook().getId());
        }

        if (comment.getAuthor() != null) {
            dto.setAuthorId(comment.getAuthor().getId());
            dto.setAuthorUsername(comment.getAuthor().getUsername());
        }

        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());

        return dto;
    }

    public List<CommentResponseDto> toResponseDtoList(final List<Comment> comments) {
        if (comments == null) {
            return List.of();
        }

        return comments.stream()
                .map(this::toResponseDto)
                .toList();
    }
}