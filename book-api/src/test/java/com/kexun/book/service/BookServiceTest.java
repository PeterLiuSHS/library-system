package com.kexun.book.service;

import com.kexun.book.client.LoanClient;
import com.kexun.book.exception.DownstreamServiceException;
import com.kexun.book.exception.ResourceNotFoundException;
import com.kexun.book.exception.ConflictException;
import com.kexun.book.model.Book;
import com.kexun.book.repository.BookRepository;
import com.kexun.book.service.impl.BookServiceImpl;
import com.kexun.book.dto.BookUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BookServiceTest {
    private BookRepository bookRepository;
    private BookService bookService;
    private LoanClient loanClient;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        loanClient = mock(LoanClient.class);
        bookService = new BookServiceImpl(bookRepository, loanClient);
    }

    @Test
    void create_shouldSaveBook_whenBookProvided() {
        Book book = new Book();
        book.setIsbn("123456789");

        when(bookRepository.existsByIsbn("123456789")).thenReturn(false);

        when(bookRepository.save(book))
                .thenReturn(book);

        Book result = bookService.create(book);

        assertEquals(book, result);
        verify(bookRepository).existsByIsbn("123456789");
        verify(bookRepository).save(book);
    }

    @Test
    void create_shouldThrowConflictException_whenIsbnAlreadyExists() {
        Book book = new Book();
        book.setIsbn("123456789");

        when(bookRepository.existsByIsbn("123456789"))
                .thenReturn(true);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> bookService.create(book)
        );

        assertEquals("ISBN already exists", ex.getMessage());

        verify(bookRepository).existsByIsbn("123456789");
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void getById_shouldReturnBook_whenBookExists() {
        Book book = new Book();
        book.setId(1L);
        when(bookRepository.findById(1L))
                .thenReturn(Optional.of(book));

        Book result = bookService.getById(1L);
        assertEquals(book, result);
        verify(bookRepository).findById(1L);
    }

    @Test
    void getById_shouldThrowException_whenBookNotFound() {
        when(bookRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.getById(1L)
        );

        assertEquals("Book 1 not found", ex.getMessage());
        verify(bookRepository).findById(1L);
    }

    @Test
    void list_shouldReturnAllBooks_whenSearchIsNull() {
        Book book1 = new Book();
        book1.setId(1L);
        Book book2 = new Book();
        book2.setId(2L);
        List<Book> books = Arrays.asList(book1, book2);
        Page<Book> page = new PageImpl<>(books);
        Pageable pageable = PageRequest.of(0, 5);

        when(bookRepository.findAll(pageable))
                .thenReturn(page);

        Page<Book> result = bookService.list(null, 0, 5);

        assertEquals(page.getContent(), result.getContent());
        verify(bookRepository).findAll(pageable);
    }

    @Test
    void list_shouldReturnAllBooks_whenSearchIsBlank() {
        Book book1 = new Book();
        book1.setId(1L);
        Book book2 = new Book();
        book2.setId(2L);
        List<Book> books = Arrays.asList(book1, book2);
        Page<Book> page = new PageImpl<>(books);
        Pageable pageable = PageRequest.of(0, 5);

        when(bookRepository.findAll(pageable))
                .thenReturn(page);

        Page<Book> result = bookService.list(" ", 0, 5);

        assertEquals(page.getContent(), result.getContent());
        verify(bookRepository).findAll(pageable);
    }

    @Test
    void list_shouldSearchBooks_whenKeywordProvided() {
        Book book1 = new Book();
        book1.setId(1L);
        Book book2 = new Book();
        book2.setId(2L);
        List<Book> books = Arrays.asList(book1, book2);
        Page<Book> page = new PageImpl<>(books);
        Pageable pageable = PageRequest.of(0, 5);
        String keyword = "test";
        when(bookRepository.findByTitleContainingOrAuthorContaining(keyword, keyword, pageable))
                .thenReturn(page);

        Page<Book> result = bookService.list(keyword, 0, 5);

        assertNotNull(result);
        assertEquals(page.getContent(), result.getContent());

        verify(bookRepository).findByTitleContainingOrAuthorContaining(keyword, keyword, pageable);
    }

    @Test
    void list_shouldUseCorrectPaginationParameters() {
        Page<Book> emptyPage = new PageImpl<>(List.of());

        when(bookRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        bookService.list(null, 0, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookRepository).findAll(pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();

        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(5, capturedPageable.getPageSize());
    }

    @Test
    void update_shouldUpdateBook_whenBookExistsAndHasNoActiveLoan() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Python II");
        book.setAuthor("David White");

        when(bookRepository.findById(1L))
                .thenReturn(Optional.of(book));

        when(loanClient.hasActiveLoan(1L)).thenReturn(false);

        BookUpdateRequest updateRequest = new BookUpdateRequest();
        updateRequest.setTitle("Java 8");
        updateRequest.setAuthor("Joe Smith");

        when(bookRepository.save(any(Book.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Book result = bookService.update(1L, updateRequest);

        assertEquals("Java 8", result.getTitle());
        assertEquals("Joe Smith", result.getAuthor());
        verify(bookRepository).findById(1L);
        verify(loanClient).hasActiveLoan(1L);
        verify(bookRepository).save(book);
    }

    @Test
    void update_shouldThrowConflictException_whenBookHasActiveLoan() {
        Book existingBook = new Book();
        existingBook.setId(1L);
        existingBook.setTitle("Original title");
        existingBook.setAuthor("Original author");
        existingBook.setIsbn("123456789");

        BookUpdateRequest updateRequest = new BookUpdateRequest();
        updateRequest.setTitle("Updated title");
        updateRequest.setAuthor("Updated author");

        when(bookRepository.findById(1L))
                .thenReturn(Optional.of(existingBook));

        when(loanClient.hasActiveLoan(1L)).thenReturn(true);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> bookService.update(1L, updateRequest)
        );

        assertEquals("Book 1 has an active loan and cannot be updated", ex.getMessage());

        assertEquals("Original title", existingBook.getTitle());
        assertEquals("Original author", existingBook.getAuthor());

        verify(bookRepository).findById(1L);
        verify(loanClient).hasActiveLoan(1L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void update_shouldThrowException_whenBookNotFound() {

        BookUpdateRequest request = new BookUpdateRequest();
        request.setTitle("Updated title");
        request.setAuthor("Updated author");

        when(bookRepository.findById(1L))
                .thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.update(1L, request)
        );
        assertEquals("Book 1 not found", ex.getMessage());
        verify(bookRepository).findById(1L);
    }

    @Test
    void update_shouldThrowDownstreamException_whenLoanServiceIsUnavailable() {
        Book existingBook = new Book();
        existingBook.setId(1L);
        existingBook.setTitle("Old title");
        existingBook.setAuthor("Old author");
        existingBook.setIsbn("123456789");

        BookUpdateRequest request = new BookUpdateRequest();
        request.setTitle("Updated title");
        request.setAuthor("Updated author");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));

        when(loanClient.hasActiveLoan(1L)).thenThrow(new DownstreamServiceException("Loan service is unavailable"));

        DownstreamServiceException ex = assertThrows(
                DownstreamServiceException.class,
                () -> bookService.update(1L, request)
        );

        assertEquals("Loan service is unavailable", ex.getMessage());

        assertEquals("Old title", existingBook.getTitle());
        assertEquals("Old author", existingBook.getAuthor());

        verify(bookRepository).findById(1L);
        verify(loanClient).hasActiveLoan(1L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void delete_shouldDeleteBook_whenBookExistsAndHasNoActiveLoan() {
        Book book = new Book();
        book.setId(1L);

        when(bookRepository.findById(1L))
                .thenReturn(Optional.of(book));

        when(loanClient.hasActiveLoan(1L)).thenReturn(false);

        bookService.delete(1L);

        verify(bookRepository).findById(1L);
        verify(loanClient).hasActiveLoan(1L);
        verify(bookRepository).delete(book);
    }

    @Test
    void delete_shouldThrowConflictException_whenBookHasActiveLoan() {
        Book book = new Book();
        book.setId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        when(loanClient.hasActiveLoan(1L)).thenReturn(true);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> bookService.delete(1L)
        );

        assertEquals("Book 1 has an active loan and cannot be deleted", ex.getMessage());

        verify(bookRepository).findById(1L);
        verify(loanClient).hasActiveLoan(1L);
        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    void delete_shouldThrowException_whenBookNotFound() {
        when(bookRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> bookService.delete(1L)
        );

        assertEquals("Book 1 not found", ex.getMessage());
        verify(bookRepository).findById(1L);
    }

    @Test
    void delete_shouldThrowDownstreamException_whenLoanServiceIsUnavailable() {
        Book book = new Book();
        book.setId(1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        when(loanClient.hasActiveLoan(1L)).thenThrow(
                new DownstreamServiceException("Loan service is unavailable")
        );

        DownstreamServiceException ex = assertThrows(
                DownstreamServiceException.class,
                () -> bookService.delete(1L)
        );

        assertEquals("Loan service is unavailable", ex.getMessage());

        verify(bookRepository).findById(1L);
        verify(loanClient).hasActiveLoan(1L);
        verify(bookRepository, never()).delete(any(Book.class));
    }
}
