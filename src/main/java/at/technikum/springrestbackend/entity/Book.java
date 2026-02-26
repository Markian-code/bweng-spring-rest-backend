package at.technikum.springrestbackend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "book_listing")
public class Book extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 150)
    private String authorName;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 50)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_condition", nullable = false, length = 20)
    private BookCondition condition = BookCondition.USED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExchangeType exchangeType = ExchangeType.EXCHANGE_OR_GIVEAWAY;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_status", nullable = false, length = 20)
    private ListingStatus status = ListingStatus.AVAILABLE;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 255)
    private String imageObjectKey;

    @Column(length = 100)
    private String imageContentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public Book() {
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

    public String getImageObjectKey() {
        return imageObjectKey;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public User getOwner() {
        return owner;
    }

    public List<Comment> getComments() {
        return comments;
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

    public void setImageObjectKey(final String imageObjectKey) {
        this.imageObjectKey = imageObjectKey;
    }

    public void setImageContentType(final String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public void setOwner(final User owner) {
        this.owner = owner;
    }

    public void setComments(final List<Comment> comments) {
        this.comments = comments;
    }
}