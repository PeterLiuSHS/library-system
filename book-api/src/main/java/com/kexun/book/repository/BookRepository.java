package com.kexun.book.repository;

import com.kexun.book.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

    Page<Book> findByTitleContainingOrAuthorContaining(
            String titleKeyword,
            String authorKeyword,
            Pageable pageable
    );
}


