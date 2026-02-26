package at.technikum.springrestbackend.dto;

import at.technikum.springrestbackend.entity.BookCondition;
import at.technikum.springrestbackend.entity.ExchangeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BookCreateRequestDto {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Author name is required")
    @Size(max = 150, message = "Author name must not exceed 150 characters")
    private String authorName;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 50, message = "Language must not exceed 50 characters")
    private String language;

    @NotNull(message = "Book condition is required")
    private BookCondition condition;

    @NotNull(message = "Exchange type is required")
    private ExchangeType exchangeType;

    public BookCreateRequestDto() {
    }

    public String getTitle() {
        return title;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getDescription() {
        return description;
    }

    public String getLanguage() {
        return language;
    }

    public BookCondition getCondition() {
        return condition;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setAuthorName(final String authorName) {
        this.authorName = authorName;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public void setCondition(final BookCondition condition) {
        this.condition = condition;
    }

    public void setExchangeType(final ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }
}