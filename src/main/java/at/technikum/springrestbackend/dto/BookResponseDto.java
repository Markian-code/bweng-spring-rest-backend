package at.technikum.springrestbackend.dto;

import at.technikum.springrestbackend.entity.BookCondition;
import at.technikum.springrestbackend.entity.ExchangeType;
import at.technikum.springrestbackend.entity.ListingStatus;
import java.time.LocalDateTime;

public class BookResponseDto {

    private Long id;
    private String title;
    private String authorName;
    private String description;
    private String language;

    private BookCondition condition;
    private ExchangeType exchangeType;
    private ListingStatus status;

    private String imageUrl;
    private String imageContentType;

    private Long ownerId;
    private String ownerUsername;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookResponseDto() {
    }

    public Long getId() {
        return id;
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

    public ListingStatus getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
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

    public void setStatus(final ListingStatus status) {
        this.status = status;
    }

    public void setImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setImageContentType(final String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerUsername(final String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}