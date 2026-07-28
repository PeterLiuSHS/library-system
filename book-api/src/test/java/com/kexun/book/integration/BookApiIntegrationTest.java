package com.kexun.book.integration;

import com.kexun.book.exception.DownstreamServiceException;
import com.kexun.book.model.Book;
import com.kexun.book.repository.BookRepository;
import com.kexun.book.client.LoanClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BookApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @MockBean
    private LoanClient loanClient;

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
                .andExpect(status().isCreated());

        assertEquals(1, bookRepository.count());

        Book savedBook = bookRepository.findAll().get(0);
        assertEquals("Python HandBook", savedBook.getTitle());
        assertEquals("David Lau", savedBook.getAuthor());
        assertEquals("1234567890", savedBook.getIsbn());
        assertNotNull(savedBook.getCreatedAt());
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

        assertEquals(1, bookRepository.count());
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
    void list_shouldReturn400_whenPageIsNegative()
            throws Exception {

        mockMvc.perform(get("/books")
                        .param("page", "-1")
                        .param("size", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"));

        assertEquals(0, bookRepository.count());
    }

    @Test
    void list_shouldReturn400_whenSizeIsZero()
            throws Exception {

        mockMvc.perform(get("/books")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"));

        assertEquals(0, bookRepository.count());
    }

    @Test
    void list_shouldReturn400_whenSizeExceedsMaximum()
            throws Exception {

        mockMvc.perform(get("/books")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"));

        assertEquals(0, bookRepository.count());
    }

    @Test
    void update_shouldModifyBookInDatabase_whenBookExists() throws Exception {
        Book book1 = new Book();
        book1.setTitle("Python HandBook");
        book1.setAuthor("David Lau");
        book1.setPublishedYear(2020);
        book1.setIsbn("1234567890");
        Book savedBook = bookRepository.save(book1);

        when(loanClient.hasActiveLoan(savedBook.getId())).thenReturn(false);

        String requestBody = """
                 {
                "title": "Python HandBook Revised Edition",
                "author": "David Lau and Team"
                 }
                """;

        mockMvc.perform(put("/books/{id}", savedBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Python HandBook Revised Edition"))
                .andExpect(jsonPath("$.author").value("David Lau and Team"));

        Book updatedBook = bookRepository.findById(savedBook.getId()).orElseThrow();

        assertEquals("Python HandBook Revised Edition", updatedBook.getTitle());
        assertEquals("David Lau and Team", updatedBook.getAuthor());
        assertEquals("1234567890", updatedBook.getIsbn());
        assertEquals(2020, updatedBook.getPublishedYear());
    }

    @Test
    void update_shouldReturn404_whenBookNotFound() throws Exception {
        String requestBody = """
                 {
                "title": "Python HandBook Revised Edition",
                "author": "David Lau and Team"
                 }
                """;

        mockMvc.perform(put("/books/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book 1 not found"));
    }

    @Test
    void update_shouldReturn409_whenBookHasActiveLoan() throws Exception {
        Book book = new Book();
        book.setTitle("Python HandBook");
        book.setAuthor("David Lau");
        book.setIsbn("1234567890");
        Book savedBook = bookRepository.save(book);

        when(loanClient.hasActiveLoan(savedBook.getId())).thenReturn(true);

        String requestBody = """
                {
                "title": "Python HandBook Revised Edition",
                "author": "David Lau and Team"
                }
                """;

        mockMvc.perform(put("/books/{id}", savedBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").
                        value("Book " + savedBook.getId() + " has an active loan and cannot be updated"));

        Book unchangedBook = bookRepository.findById(savedBook.getId()).orElseThrow();

        assertEquals("Python HandBook", unchangedBook.getTitle());
        assertEquals("David Lau", unchangedBook.getAuthor());
    }

    @Test
    void update_shouldReturn503_whenLoanServiceIsUnavailable()
            throws Exception {

        Book book = new Book();
        book.setTitle("Python HandBook");
        book.setAuthor("David Lau");
        book.setIsbn("1234567890");
        book.setPublishedYear(2020);

        Book savedBook = bookRepository.save(book);

        when(loanClient.hasActiveLoan(savedBook.getId()))
                .thenThrow(
                        new DownstreamServiceException(
                                "Loan service is unavailable"
                        )
                );

        String requestBody = """
            {
              "title": "Updated title",
              "author": "Updated author"
            }
            """;

        mockMvc.perform(put("/books/{id}", savedBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message")
                        .value("Loan service is unavailable"));

        Book unchangedBook = bookRepository
                .findById(savedBook.getId())
                .orElseThrow();

        assertEquals("Python HandBook", unchangedBook.getTitle());
        assertEquals("David Lau", unchangedBook.getAuthor());
        assertEquals("1234567890", unchangedBook.getIsbn());
        assertEquals(2020, unchangedBook.getPublishedYear());
    }

    @Test
    void delete_shouldRemoveBook_whenBookExistsAndHasNoActiveLoan() throws Exception {
        Book book = new Book();
        book.setTitle("Python HandBook");
        book.setAuthor("David Lau");
        book.setIsbn("1234567890");
        Book savedBook = bookRepository.save(book);

        when(loanClient.hasActiveLoan(savedBook.getId())).thenReturn(false);

        mockMvc.perform(delete("/books/{id}", savedBook.getId()))
                .andExpect(status().isNoContent());

        assertTrue(bookRepository.findById(savedBook.getId()).isEmpty());
    }

    @Test
    void delete_shouldReturn409_whenBookHasActiveLoan() throws Exception {
        Book book = new Book();
        book.setTitle("Python HandBook");
        book.setAuthor("David Lau");
        book.setIsbn("1234567890");
        Book savedBook = bookRepository.save(book);

        when(loanClient.hasActiveLoan(savedBook.getId())).thenReturn(true);

        mockMvc.perform(delete("/books/{id}", savedBook.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Book " + savedBook.getId() + " has an active loan and cannot be deleted"));

        assertTrue(bookRepository.findById(savedBook.getId()).isPresent());
    }

    @Test
    void delete_shouldReturn404_whenBookNotFound() throws Exception {
        mockMvc.perform(delete("/books/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book 1 not found"));
    }

    @Test
    void delete_shouldReturn503_whenLoanServiceIsUnavailable() throws Exception {
        Book book = new Book();
        book.setTitle("Python HandBook");
        book.setAuthor("David Lau");
        book.setIsbn("1234567890");

        Book savedBook = bookRepository.save(book);

        when(loanClient.hasActiveLoan(savedBook.getId())).thenThrow(new DownstreamServiceException("Loan service is unavailable"));

        mockMvc.perform(delete("/books/{id}", savedBook.getId()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message")
                        .value("Loan service is unavailable"));

        assertTrue(
                bookRepository.findById(savedBook.getId()).isPresent()
        );
    }

}
