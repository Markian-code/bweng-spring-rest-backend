package at.technikum.springrestbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentUpdateRequestDto {

    @NotBlank(message = "Comment content is required")
    @Size(max = 1000, message = "Comment content must not exceed 1000 characters")
    private String content;

    public CommentUpdateRequestDto() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }
}