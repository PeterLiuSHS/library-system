package com.kexun.book.controller;

import com.kexun.book.exception.GlobalExceptionHandler;
import com.kexun.book.exception.ResourceNotFoundException;
import com.kexun.book.model.Book;
import com.kexun.book.service.BookService;
import org.apache.catalina.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import(GlobalExceptionHandler.class)
public class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Test
    void create_shouldReturn_whenRequestIsValid() throws Exception {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Java Handbook");
        book.setAuthor("Joe Smith");
        book.setIsbn("123456789");

        when(bookService.create(any(Book.class))).thenReturn(book);

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                 "title": "Java Handbook",
                                 "author": "Joe Smith",
                                 "isbn": "123456789"
                                                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Java Handbook"))
                .andExpect(jsonPath("$.author").value("Joe Smith"));

        verify(bookService, times(1)).create(any(Book.class));
    }

    @Test
    void getById_shouldReturnBook_whenBookExists() throws Exception {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Java Handbook");
        book.setAuthor("Joe Smith");
        book.setIsbn("123456789");

        when(bookService.getById(1L)).thenReturn(book);

        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Java Handbook"))
                .andExpect(jsonPath("$.author").value("Joe Smith"))
                .andExpect(jsonPath("$.isbn").value("123456789"));

        verify(bookService, times(1)).getById(1L);
    }

    @Test
    void getById_shouldReturnNotFound_whenBookDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Book 1 not found")).when(bookService).getById(1L);

        mockMvc.perform(get("/books/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Book 1 not found"));

        verify(bookService, times(1)).getById(1L);
    }

    @Test
    void list_shouldReturnBooks_whenBooksExist() throws Exception {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Java Handbook");
        book.setAuthor("Joe Smith");
        book.setIsbn("123456789");
        List<Book> books = Arrays.asList(book);
        Page<Book> bookPage = new PageImpl<>(books);

        when(bookService.list("Java", 0, 5)).thenReturn(bookPage);

        mockMvc.perform(get("/books")
                        .param("search", "Java")
                        .param("page", "0")
                        .param("size", "5"))
//                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Java Handbook"))
                .andExpect(jsonPath("$.content[0].author").value("Joe Smith"))
                .andExpect(jsonPath("$.content[0].isbn").value("123456789"))
                .andExpect(jsonPath("$.size").value(1));

        verify(bookService, times(1)).list("Java", 0, 5);
    }

    @Test
    void list_shouldReturnEmptyPage_whenNoBooksExist() throws Exception {
        Page<Book> emptyBookPage = new PageImpl<>(Collections.emptyList());

        when(bookService.list(null, 0, 5)).thenReturn(emptyBookPage);

        mockMvc.perform(get("/books"))
//                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(bookService).list(null, 0, 5);
    }

    @Test
    void list_shouldReturnBooks_whenSearchKeywordProvided() throws Exception {
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Java Handbook");
        book1.setAuthor("Joe Smith");
        book1.setIsbn("123456789");

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Java Masterbook");
        book2.setAuthor("Joe Brown");
        book2.setIsbn("123456788");

        List<Book> javaBooks = Arrays.asList(book1, book2);
        Page<Book> javaPage = new PageImpl<>(javaBooks);

        when(bookService.list("Java", 0, 5)).thenReturn(javaPage);

        mockMvc.perform(get("/books")
                        .param("search", "Java")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Java Handbook"))
                .andExpect(jsonPath("$.content[0].author").value("Joe Smith"));

        verify(bookService, times(1)).list("Java", 0, 5);
    }

    @Test
    void delete_shouldReturnNoContent_whenBookExists() throws Exception {

        mockMvc.perform(delete("/books/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(bookService, times(1)).delete(1L);
    }

    @Test
    void delete_shouldReturnNotFound_whenBookDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Book 1 not found"))
                .when(bookService).delete(1L);

        mockMvc.perform(delete("/books/{id}", 1L))
                .andExpect(status().isNotFound());

        verify(bookService, times(1)).delete(1L);
    }

    @Test
    void update_shouldReturnBook_whenRequestIsValid() throws Exception {
        Book updatedBook = new Book();
        updatedBook.setId(1L);
        updatedBook.setTitle("Java Handbook");
        updatedBook.setAuthor("Joe Smith");
        updatedBook.setIsbn("123456789");

        when(bookService.update(eq(1L), any(Book.class))).thenReturn(updatedBook);

        mockMvc.perform(put("/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "title": "Java Handbook",
                                "author": "Joe Smith",
                                "isbn": "123456789"
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Java Handbook"))
                .andExpect(jsonPath("$.author").value("Joe Smith"))
                .andExpect(jsonPath("$.isbn").value("123456789"));

        verify(bookService, times(1)).update(eq(1L), any(Book.class));
    }

    @Test
    void update_shouldReturnNotFound_whenBookDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Book 1 not found"))
                .when(bookService).update(eq(1L), any(Book.class));

        mockMvc.perform(put("/books/{id}",1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "title": "Java Handbook",
                        "author": "Joe Smith", 
                        "isbn": "123456789"
                        }"""))
                .andExpect(status().isNotFound());

        verify(bookService, times(1)).update(eq(1L), any(Book.class));


    }
}
