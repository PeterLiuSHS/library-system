package com.kexun.book.integration;

import com.kexun.book.model.Book;
import com.kexun.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestBody;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BookApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    void create_shouldPersistBookIntoDatabase() throws Exception {
        String requestBody = """
                {
                "title": "Python HandBook",
                "author": "David Lau",
                "isbn": "1234567890"
                }
                """;

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        assertEquals(1, bookRepository.count());

        Book savedBook = bookRepository.findAll().get(0);
        assertEquals("Python HandBook", savedBook.getTitle());
        assertEquals("David Lau", savedBook.getAuthor());
        assertEquals("1234567890", savedBook.getIsbn());
    }

    @Test
    void create_shouldReturn400_whenRequestBodyIsInvalid() throws Exception {
        String requestBody = """
                {
                "title": "Python HandBook",
                "author": "David Lau",
                "isbn": ""
                }
                """;

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn409_whenIsbnAlreadyExists() throws Exception {
        Book book1 = new Book();
        book1.setTitle("Python HandBook");
        book1.setAuthor("David Lau");
        book1.setIsbn("1234567890");
        Book savedBook1 = bookRepository.save(book1);

        String requestBody = """
                {
                "title": "Java HandBook",
                "author": "Peter Lau",
                "isbn": "1234567890"
                }
                """;

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("ISBN already exists"));
    }

    @Test
    void getById_shouldReturnBook_whenBookExists() throws Exception {
        Book book1 = new Book();
        book1.setTitle("Python HandBook");
        book1.setAuthor("David Lau");
        book1.setIsbn("1234567890");
        Book savedBook1 = bookRepository.save(book1);

        mockMvc.perform(get("/books/" + savedBook1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Python HandBook"))
                .andExpect(jsonPath("$.author").value("David Lau"))
                .andExpect(jsonPath("$.isbn").value("1234567890"));
    }

    @Test
    void getById_shouldReturn404_whenBookNotFound() throws Exception {
        mockMvc.perform(get("/books/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book 1 not found"));
    }

    @Test
    void list_shouldReturnPagedBooks_whenSearchIsNull() throws Exception {
        Book book1 = new Book();
        book1.setTitle("Python HandBook");
        book1.setAuthor("David Lau");
        book1.setIsbn("1234567890");
        bookRepository.save(book1);

        Book book2 = new Book();
        book2.setTitle("Java HandBook");
        book2.setAuthor("Peter Lau");
        book2.setIsbn("1234567889");
        bookRepository.save(book2);

        Book book3 = new Book();
        book3.setTitle("Python HandBook II");
        book3.setAuthor("Peter Lau");
        book3.setIsbn("1234567888");
        bookRepository.save(book3);

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Python HandBook"))
                .andExpect(jsonPath("$.content[1].title").value("Java HandBook"))
                .andExpect(jsonPath("$.content[2].title").value("Python HandBook II"))
                .andExpect(jsonPath("$.pageable.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.numberOfElements").value(3));
    }

    @Test
    void list_shouldReturnMatchedBooks_whenSearchKeywordProvided() throws Exception {
        Book book1 = new Book();
        book1.setTitle("Python HandBook");
        book1.setAuthor("David Lau");
        book1.setIsbn("1234567890");
        bookRepository.save(book1);

        Book book2 = new Book();
        book2.setTitle("Java HandBook");
        book2.setAuthor("Peter Lau");
        book2.setIsbn("1234567889");
        bookRepository.save(book2);

        mockMvc.perform(get("/books")
                        .param("search", "Python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Python HandBook"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Java"))));
    }

    @Test
    void update_shouldModifyBookInDatabase_whenBookExists() throws Exception {
        Book book1 = new Book();
        book1.setTitle("Python HandBook");
        book1.setAuthor("David Lau");
        book1.setIsbn("1234567890");
        Book savedBook = bookRepository.save(book1);

        String requestBody = """
                 {
                "title": "Python HandBook Revised Edition",
                "author": "David Lau and Team",
                "isbn": "1234567890"
                 }
                """;

        mockMvc.perform(put("/books/{id}", savedBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Python HandBook Revised Edition"))
                .andExpect(jsonPath("$.author").value("David Lau and Team"));
    }

    @Test
    void update_shouldReturn404_whenBookNotFound() throws Exception {
        String requestBody = """
                 {
                "title": "Python HandBook Revised Edition",
                "author": "David Lau and Team",
                "isbn": "1234567890"
                 }
                """;

        mockMvc.perform(put("/books/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book 1 not found"));
    }

    @Test
    void delete_shouldRemoveBookFromDatabase_whenBookExists() throws Exception {
        Book book1 = new Book();
        book1.setTitle("Python HandBook");
        book1.setAuthor("David Lau");
        book1.setIsbn("1234567890");
        Book savedBook = bookRepository.save(book1);

        mockMvc.perform(delete("/books/{id}", savedBook.getId()))
                .andExpect(status().isNoContent());

        assertTrue(bookRepository.findById(savedBook.getId()).isEmpty());
    }

    @Test
    void delete_shouldReturn404_whenBookNotFound() throws Exception {
        mockMvc.perform(delete("/books/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book 1 not found"));
    }

}
