package com.kexun.book.service;
import com.kexun.book.model.Book;
import com.kexun.book.dto.BookUpdateRequest;
import org.springframework.data.domain.Page;

public interface BookService {
    Book create(Book book);
    Book getById(Long id);
    Page<Book> list(String search, int page, int size);
    void delete(Long id);
    Book update(Long id, BookUpdateRequest request);
}
