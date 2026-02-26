package at.technikum.springrestbackend.repository;

import at.technikum.springrestbackend.entity.Book;
import at.technikum.springrestbackend.entity.ListingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    List<Book> findAllByOrderByCreatedAtDesc();

    List<Book> findAllByStatusOrderByCreatedAtDesc(ListingStatus status);

    List<Book> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}