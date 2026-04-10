package com.kexun.book.service.impl;

import com.kexun.book.exception.ResourceNotFoundException;
import com.kexun.book.model.Book;
import com.kexun.book.repository.BookRepository;
import com.kexun.book.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookServiceImpl implements BookService {

    private BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public Book create(Book book) {
        return bookRepository.save(book);
    }

    @Override
    public Book getById(Long id) {
        return bookRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Book "+id+" not found"));
    }

    @Override
    public Page<Book> list(String search, int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        if (search == null || search.isBlank()){
            return bookRepository.findAll(pageable);
        }
        return bookRepository.findByTitleContainingOrAuthorContaining(search, search, pageable);
    }

    @Override
    public void delete(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book "+id+" not found"));
        bookRepository.delete(book);
    }

    @Override
    public Book update(Long id, Book book) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book "+id+" not found"));

        existing.setTitle(book.getTitle());
        existing.setAuthor(book.getAuthor());

        return bookRepository.save(existing);
    }
}
