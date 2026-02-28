package at.technikum.springrestbackend.specification;

import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.BookCondition;
import at.technikum.springrestbackend.entity.ExchangeType;
import at.technikum.springrestbackend.entity.ListingStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> buildPublicFilter(
            final BookCondition condition,
            final ExchangeType exchangeType,
            final String language,
            final String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ListingStatus.AVAILABLE));
            if (condition != null) {
                predicates.add(cb.equal(root.get("condition"), condition));
            }
            if (exchangeType != null) {
                predicates.add(cb.equal(root.get("exchangeType"), exchangeType));
            }
            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("language")), language.trim().toLowerCase()));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(buildSearchPredicate(search, root, cb));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate buildSearchPredicate(
            final String search,
            final Root<Book> root,
            final CriteriaBuilder cb
    ) {
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("authorName")), pattern)
        );
    }
}
