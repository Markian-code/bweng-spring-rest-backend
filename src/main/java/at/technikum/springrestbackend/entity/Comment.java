package at.technikum.springrestbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "comment")
public class Comment extends BaseEntity {

    @Column(nullable = false, length = 1000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    public Comment() {
    }

    public String getContent() {
        return content;
    }

    public Book getBook() {
        return book;
    }

    public User getAuthor() {
        return author;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public void setBook(final Book book) {
        this.book = book;
    }

    public void setAuthor(final User author) {
        this.author = author;
    }
}