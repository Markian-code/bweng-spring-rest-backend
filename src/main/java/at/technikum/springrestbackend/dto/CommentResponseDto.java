package at.technikum.springrestbackend.dto;

import java.time.LocalDateTime;

public class CommentResponseDto {

    private Long id;
    private String content;

    private Long bookId;

    private Long authorId;
    private String authorUsername;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CommentResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Long getBookId() {
        return bookId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public void setBookId(final Long bookId) {
        this.bookId = bookId;
    }

    public void setAuthorId(final Long authorId) {
        this.authorId = authorId;
    }

    public void setAuthorUsername(final String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}