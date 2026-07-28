package com.kexun.book.service.impl;

import com.kexun.book.client.LoanClient;
import com.kexun.book.exception.ResourceNotFoundException;
import com.kexun.book.exception.ConflictException;
import com.kexun.book.model.Book;
import com.kexun.book.repository.BookRepository;
import com.kexun.book.service.BookService;
import com.kexun.book.dto.BookUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final LoanClient loanClient;

    public BookServiceImpl(BookRepository bookRepository, LoanClient loanClient) {
        this.bookRepository = bookRepository;
        this.loanClient = loanClient;
    }

    @Override
    public Book create(Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new ConflictException("ISBN already exists");
        }
        return bookRepository.save(book);
    }

    @Override
    public Book getById(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book " + id + " not found"));
    }

    @Override
    public Page<Book> list(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (search == null || search.isBlank()) {
            return bookRepository.findAll(pageable);
        }
        return bookRepository.findByTitleContainingOrAuthorContaining(search, search, pageable);
    }

    @Override
    public void delete(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book " + id + " not found"));

        if (loanClient.hasActiveLoan(id)) {
            throw new ConflictException(
                    "Book " + id + " has an active loan and cannot be deleted"
            );
        }
        bookRepository.delete(book);
    }

    @Override
    public Book update(Long id, BookUpdateRequest request) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book " + id + " not found"));

        if (loanClient.hasActiveLoan(id)) {
            throw new ConflictException(
                    "Book " + id + " has an active loan and cannot be updated"
            );
        }

        existing.setTitle(request.getTitle());
        existing.setAuthor(request.getAuthor());

        return bookRepository.save(existing);
    }
}
