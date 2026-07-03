package com.kexun.loan.integration;

import com.kexun.loan.client.BookClient;
import com.kexun.loan.client.UserClient;
import com.kexun.loan.exception.ConflictException;
import com.kexun.loan.exception.ResourceNotFoundException;
import com.kexun.loan.model.Loan;
import com.kexun.loan.repository.LoanRepository;
import com.kexun.loan.service.LoanService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LoanApiIntegrationTest {
    @MockBean
    private UserClient userClient;

    @MockBean
    private BookClient bookClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
    }

    @Test
    void borrow_shouldPersistLoanIntoDatabase_whenUserAndBookExistAndBookAvailable() throws Exception {
        String requestBody = """
                {
                "bookId": 1,
                "days": 8
                }
                """;

        LocalDate today = LocalDate.now();
        LocalDate expectedDueDate = today.plusDays(8);

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.bookId").value(1L))
                .andExpect(jsonPath("$.loanDate").value(today.toString()))
                .andExpect(jsonPath("$.dueDate").value(expectedDueDate.toString()));

        assertEquals(1, loanRepository.count());

        Loan savedLoan = loanRepository.findAll().get(0);
        assertEquals(1, savedLoan.getBookId());
        assertEquals(1, savedLoan.getUserId());
        assertEquals(today, savedLoan.getLoanDate());
        assertEquals(expectedDueDate, savedLoan.getDueDate());
    }

    @Test
    void borrow_shouldReturn404_whenUserNotFound() throws Exception {
        String requestBody = """
                {
                "bookId": 1,
                "days": 8
                }
                """;

        doThrow(new ResourceNotFoundException("User 1 not found"))
                .when(userClient)
                .assertUserExists(1L);

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User 1 not found"));
    }

    @Test
    void borrow_shouldReturn404_whenBookNotFound() throws Exception {
        String requestBody = """
                {
                "bookId": 1,
                "days": 8
                }
                """;

        doThrow(new ResourceNotFoundException("Book 1 not found"))
                .when(bookClient)
                .assertBookExists(1L);

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Book 1 not found"));
    }

    @Test
    void borrow_shouldReturn409_whenBookAlreadyBorrowed() throws Exception {
        String requestBody = """
                {
                "bookId": 1,
                "days": 8
                }
                """;

        Loan loan = new Loan();
        loan.setBookId(1L);
        loan.setUserId(2L);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(8));
        loanRepository.save(loan);

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Book is not available"));
    }

    @Test
    void borrow_shouldReturn400_whenRequestBodyIsInvalid() throws Exception {
        String requestBody = """
                {
                "bookId": 1,
                "days": 0
                }
                """;

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void returnBook_shouldUpdateReturnDateInDatabase_whenActiveLoanExists() throws Exception {
        Loan loan = new Loan();
        loan.setBookId(1L);
        loan.setUserId(1L);
        loan.setLoanDate(LocalDate.of(2026, 3, 20));
        loan.setDueDate(LocalDate.of(2026, 3, 20).plusDays(8));
        Loan savedLoan = loanRepository.save(loan);

        mockMvc.perform(put("/users/{userId}/loans/{bookId}/return", 1L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()));

        Loan updatedLoan = loanRepository.findById(savedLoan.getId()).orElseThrow(() -> new ResourceNotFoundException("Book 1 not found"));

        assertNotNull(updatedLoan.getReturnDate());
        assertEquals(LocalDate.now(), updatedLoan.getReturnDate());

    }

    @Test
    void returnBook_shouldReturn404_whenActiveLoanNotFound() throws Exception {

        mockMvc.perform(put("/users/{userId}/loans/{bookId}/return", 1L, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Active loan not found for user 1 and book 1"));
    }

    @Test
    void getAllLoansByUser_shouldReturnActiveThenHistoryLoanExists() throws Exception {
        Loan activeLoan = new Loan();
        activeLoan.setBookId(1L);
        activeLoan.setUserId(1L);
        activeLoan.setLoanDate(LocalDate.of(2026, 3, 20));
        activeLoan.setDueDate(LocalDate.of(2026, 3, 20).plusDays(8));
        activeLoan.setReturnDate(null);
        Loan savedActiveLoan = loanRepository.save(activeLoan);

        Loan historyLoan = new Loan();
        historyLoan.setBookId(2L);
        historyLoan.setUserId(1L);
        historyLoan.setLoanDate(LocalDate.of(2026, 3, 20));
        historyLoan.setDueDate(LocalDate.of(2026, 3, 20).plusDays(8));
        historyLoan.setReturnDate(LocalDate.of(2026,3,22));
        Loan savedHistoryLoan = loanRepository.save(historyLoan);

        mockMvc.perform(get("/users/{userId}/loans", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].bookId").value(1))
                .andExpect(jsonPath("$[0].returnDate").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[1].userId").value(1))
                .andExpect(jsonPath("$[1].bookId").value(2))
                .andExpect(jsonPath("$[1].returnDate").value("2026-03-22"));
    }

    @Test
    void getActiveLoanByUser_shouldReturnOnlyActiveLoans() throws Exception {
        Loan activeLoan = new Loan();
        activeLoan.setBookId(1L);
        activeLoan.setUserId(1L);
        activeLoan.setLoanDate(LocalDate.of(2026, 3, 20));
        activeLoan.setDueDate(LocalDate.of(2026, 3, 20).plusDays(8));
        activeLoan.setReturnDate(null);
        Loan savedActiveLoan = loanRepository.save(activeLoan);

        Loan historyLoan = new Loan();
        historyLoan.setBookId(2L);
        historyLoan.setUserId(1L);
        historyLoan.setLoanDate(LocalDate.of(2026, 3, 20));
        historyLoan.setDueDate(LocalDate.of(2026, 3, 20).plusDays(8));
        historyLoan.setReturnDate(LocalDate.of(2026,3,22));
        Loan savedHistoryLoan = loanRepository.save(historyLoan);

        mockMvc.perform(get("/users/{userId}/loans/active", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].bookId").value(1))
                .andExpect(jsonPath("$[0].returnDate").value(Matchers.nullValue()));
    }

    @Test
    void checkAvailability_shouldReturnAvailableTrue_whenNoActiveLoanExists() throws Exception {

        mockMvc.perform(get("/books/{bookId}/availability", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.remainingDays").value(0));

        assertTrue(loanRepository.findByBookIdAndReturnDateIsNull(1L).isEmpty());
    }

    @Test
    void checkAvailability_shouldReturnAvailableFalse_whenActiveLoanExists() throws Exception {
        Loan activeLoan = new Loan();
        activeLoan.setBookId(1L);
        activeLoan.setUserId(1L);
        activeLoan.setLoanDate(LocalDate.now());
        activeLoan.setDueDate(LocalDate.now().plusDays(8));
        activeLoan.setReturnDate(null);
        Loan savedActiveLoan = loanRepository.save(activeLoan);

        mockMvc.perform(get("/books/{bookId}/availability", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.remainingDays").value(8));
    }
}
