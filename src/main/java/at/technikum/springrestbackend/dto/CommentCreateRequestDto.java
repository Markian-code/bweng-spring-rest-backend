package at.technikum.springrestbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CommentCreateRequestDto {

    @NotNull(message = "Book id is required")
    private Long bookId;

    @NotBlank(message = "Comment content is required")
    @Size(max = 1000, message = "Comment content must not exceed 1000 characters")
    private String content;

    public CommentCreateRequestDto() {
    }

    public Long getBookId() {
        return bookId;
    }

    public String getContent() {
        return content;
    }

    public void setBookId(final Long bookId) {
        this.bookId = bookId;
    }

    public void setContent(final String content) {
        this.content = content;
    }
}