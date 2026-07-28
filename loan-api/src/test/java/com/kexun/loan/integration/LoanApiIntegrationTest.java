package com.kexun.loan.integration;

import com.kexun.loan.client.BookClient;
import com.kexun.loan.client.UserClient;
import com.kexun.loan.exception.ConflictException;
import com.kexun.loan.exception.DownstreamServiceException;
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
        assertEquals(1L, savedLoan.getBookId());
        assertEquals(1L, savedLoan.getActiveBookId());
        assertNull(savedLoan.getReturnDate());
        assertEquals(1L, savedLoan.getUserId());
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
    void borrow_shouldReturn503_whenUserServiceIsUnavailable() throws Exception {
        doThrow(new DownstreamServiceException("User service is unavailable"))
                .when(userClient).assertUserExists(1L);

        String requestBody = """
                {"bookId": 1, "days": 8
                }
                """;

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("User service is unavailable"));

        assertEquals(0, loanRepository.count());
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
    void borrow_shouldReturn503_whenBookServiceIsUnavailable() throws Exception {

        doThrow(new DownstreamServiceException("Book service is unavailable"))
                .when(bookClient).assertBookExists(1L);

        String requestBody = """
                {
                "bookId": 1,
                "days": 8}
                """;

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Book service is unavailable"));

        assertEquals(0, loanRepository.count());
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
        loan.setActiveBookId(1L);
        loan.setUserId(2L);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(8));
        loan.setReturnDate(null);
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
        loan.setActiveBookId(1L);
        loan.setUserId(1L);
        loan.setLoanDate(LocalDate.of(2026, 3, 20));
        loan.setDueDate(LocalDate.of(2026, 3, 20).plusDays(8));
        loan.setReturnDate(null);

        Loan savedLoan = loanRepository.save(loan);

        mockMvc.perform(put("/users/{userId}/loans/{bookId}/return", 1L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()));

        Loan updatedLoan = loanRepository.findById(savedLoan.getId()).orElseThrow(() ->
                new ResourceNotFoundException("Book 1 not found"));

        assertNotNull(updatedLoan.getReturnDate());
        assertEquals(LocalDate.now(), updatedLoan.getReturnDate());
        assertNull(updatedLoan.getActiveBookId());
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
        activeLoan.setActiveBookId(1L);
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
        historyLoan.setReturnDate(LocalDate.of(2026, 3, 22));
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
    void getAllLoansByUser_shouldReturnEmptyList_whenUserExistsButHasNoLoans()
            throws Exception {

        mockMvc.perform(get("/users/{userId}/loans", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertEquals(0, loanRepository.count());
    }

    @Test
    void getAllLoansByUser_shouldReturn404_whenUserDoesNotExist()
            throws Exception {

        doThrow(new ResourceNotFoundException("User with id 999 not found"))
                .when(userClient)
                .assertUserExists(999L);

        mockMvc.perform(get("/users/{userId}/loans", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 999 not found"));
    }

    @Test
    void getActiveLoanByUser_shouldReturnOnlyActiveLoans() throws Exception {
        Loan activeLoan = new Loan();
        activeLoan.setBookId(1L);
        activeLoan.setActiveBookId(1L);
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
        historyLoan.setReturnDate(LocalDate.of(2026, 3, 22));
        Loan savedHistoryLoan = loanRepository.save(historyLoan);

        mockMvc.perform(get("/users/{userId}/loans/active", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].bookId").value(1))
                .andExpect(jsonPath("$[0].returnDate").value(Matchers.nullValue()));
    }

    @Test
    void getActiveLoansByUser_shouldReturnEmptyList_whenUserExistsButHasNoActiveLoans()
            throws Exception {

        mockMvc.perform(get("/users/{userId}/loans/active", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertEquals(0, loanRepository.count());
    }

    @Test
    void getActiveLoansByUser_shouldReturn404_whenUserDoesNotExist()
            throws Exception {

        doThrow(new ResourceNotFoundException("User with id 999 not found"))
                .when(userClient)
                .assertUserExists(999L);

        mockMvc.perform(get("/users/{userId}/loans/active", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 999 not found"));
    }

    @Test
    void getHistoryLoansByUser_shouldReturnOnlyReturnedLoans()
            throws Exception {

        Loan activeLoan = new Loan();
        activeLoan.setBookId(1L);
        activeLoan.setActiveBookId(1L);
        activeLoan.setUserId(1L);
        activeLoan.setLoanDate(LocalDate.of(2026, 3, 20));
        activeLoan.setDueDate(LocalDate.of(2026, 3, 28));
        activeLoan.setReturnDate(null);
        loanRepository.save(activeLoan);

        Loan historyLoan = new Loan();
        historyLoan.setBookId(2L);
        historyLoan.setUserId(1L);
        historyLoan.setLoanDate(LocalDate.of(2026, 3, 20));
        historyLoan.setDueDate(LocalDate.of(2026, 3, 28));
        historyLoan.setReturnDate(LocalDate.of(2026, 3, 25));
        loanRepository.save(historyLoan);

        mockMvc.perform(get("/users/{userId}/loans/history", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].bookId").value(2))
                .andExpect(jsonPath("$[0].returnDate")
                        .value("2026-03-25"));
    }

    @Test
    void getHistoryLoansByUser_shouldReturnEmptyList_whenUserExistsButHasNoHistory()
            throws Exception {

        mockMvc.perform(get("/users/{userId}/loans/history", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertEquals(0, loanRepository.count());
    }

    @Test
    void getHistoryLoansByUser_shouldReturn404_whenUserDoesNotExist()
            throws Exception {

        doThrow(new ResourceNotFoundException("User with id 999 not found"))
                .when(userClient)
                .assertUserExists(999L);

        mockMvc.perform(get("/users/{userId}/loans/history", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 999 not found"));
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
        activeLoan.setActiveBookId(1L);
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

    @Test
    void checkAvailability_shouldReturn404_whenBookDoesNotExist() throws Exception {

        doThrow(new ResourceNotFoundException("Book 999 not found"))
                .when(bookClient)
                .assertBookExists(999L);

        mockMvc.perform(get("/books/{bookId}/availability", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Book 999 not found"));

        assertTrue(loanRepository.findByBookIdAndReturnDateIsNull(999L).isEmpty());
    }

    @Test
    void checkAvailability_shouldReturn503_whenBookServiceIsUnavailable()
            throws Exception {

        doThrow(
                new DownstreamServiceException(
                        "Book service is unavailable"
                )
        )
                .when(bookClient)
                .assertBookExists(1L);

        mockMvc.perform(get("/books/{bookId}/availability", 1L))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message")
                        .value("Book service is unavailable"));

        assertEquals(0, loanRepository.count());
    }

    @Test
    void checkAvailability_shouldReturnNegativeRemainingDays_whenLoanIsOverdue()
            throws Exception {

        Loan overdueLoan = new Loan();
        overdueLoan.setBookId(1L);
        overdueLoan.setActiveBookId(1L);
        overdueLoan.setUserId(1L);
        overdueLoan.setLoanDate(LocalDate.now().minusDays(10));
        overdueLoan.setDueDate(LocalDate.now().minusDays(3));
        overdueLoan.setReturnDate(null);

        loanRepository.save(overdueLoan);

        mockMvc.perform(get("/books/{bookId}/availability", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.remainingDays").value(-3));
    }

    @Test
    void getAllLoans_shouldReturnAllLoansOrderedByLoanDateDesc() throws Exception {
        Loan olderLoan = new Loan();
        olderLoan.setUserId(1L);
        olderLoan.setBookId(1L);
        olderLoan.setActiveBookId(1L);
        olderLoan.setLoanDate(LocalDate.of(2026, 5, 20));
        olderLoan.setDueDate(LocalDate.of(2026, 5, 20).plusDays(15));
        olderLoan.setReturnDate(null);
        loanRepository.save(olderLoan);

        Loan newerLoan = new Loan();
        newerLoan.setUserId(2L);
        newerLoan.setBookId(2L);
        newerLoan.setLoanDate(LocalDate.of(2026, 5, 25));
        newerLoan.setDueDate(LocalDate.of(2026, 5, 25).plusDays(15));
        newerLoan.setReturnDate(LocalDate.of(2026, 5, 29));
        loanRepository.save(newerLoan);

        mockMvc.perform(
                        get("/loans")
                                .param("page", "0")
                                .param("size", "5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].userId").value(2))
                .andExpect(jsonPath("$.content[0].bookId").value(2))
                .andExpect(jsonPath("$.content[0].loanDate").value("2026-05-25"))
                .andExpect(jsonPath("$.content[1].userId").value(1))
                .andExpect(jsonPath("$.content[1].bookId").value(1))
                .andExpect(jsonPath("$.content[1].loanDate").value("2026-05-20"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllLoans_shouldReturn400_whenPageIsNegative()
            throws Exception {

        mockMvc.perform(get("/loans")
                        .param("page", "-1")
                        .param("size", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.errors.page")
                        .value("Page must be zero or greater"));

        assertEquals(0, loanRepository.count());
    }

    @Test
    void getAllLoansByUser_shouldReturn503_whenUserServiceIsUnavailable()
            throws Exception {

        doThrow(
                new DownstreamServiceException(
                        "User service is unavailable"
                )
        )
                .when(userClient)
                .assertUserExists(1L);

        mockMvc.perform(get("/users/{userId}/loans", 1L))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message")
                        .value("User service is unavailable"));

        assertEquals(0, loanRepository.count());
    }

    @Test
    void getAllActiveLoans_shouldReturnOnlyActiveLoans() throws Exception {
        Loan activeLoan = new Loan();
        activeLoan.setUserId(1L);
        activeLoan.setBookId(1L);
        activeLoan.setActiveBookId(1L);
        activeLoan.setLoanDate(LocalDate.of(2026, 7, 1));
        activeLoan.setDueDate(LocalDate.of(2026, 7, 10));
        activeLoan.setReturnDate(null);
        loanRepository.save(activeLoan);

        Loan historyLoan = new Loan();
        historyLoan.setUserId(2L);
        historyLoan.setBookId(2L);
        historyLoan.setLoanDate(LocalDate.of(2026, 7, 2));
        historyLoan.setDueDate(LocalDate.of(2026, 7, 11));
        historyLoan.setReturnDate(LocalDate.of(2026, 7, 8));
        loanRepository.save(historyLoan);

        mockMvc.perform(
                        get("/loans/active")
                                .param("page", "0")
                                .param("size", "5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(1))
                .andExpect(jsonPath("$.content[0].bookId").value(1))
                .andExpect(jsonPath("$.content[0].returnDate").value(nullValue()))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllActiveLoans_shouldReturn400_whenSizeIsZero()
            throws Exception {

        mockMvc.perform(get("/loans/active")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.errors.size")
                        .value("Size must be at least 1"));

        assertEquals(0, loanRepository.count());
    }

    @Test
    void getAllHistoryLoans_shouldReturnOnlyReturnedLoansOrderedByReturnDateDesc()
            throws Exception {

        Loan activeLoan = new Loan();
        activeLoan.setUserId(1L);
        activeLoan.setBookId(1L);
        activeLoan.setActiveBookId(1L);
        activeLoan.setLoanDate(LocalDate.of(2026, 7, 1));
        activeLoan.setDueDate(LocalDate.of(2026, 7, 10));
        activeLoan.setReturnDate(null);
        loanRepository.save(activeLoan);

        Loan olderHistoryLoan = new Loan();
        olderHistoryLoan.setUserId(2L);
        olderHistoryLoan.setBookId(2L);
        olderHistoryLoan.setLoanDate(LocalDate.of(2026, 6, 1));
        olderHistoryLoan.setDueDate(LocalDate.of(2026, 6, 10));
        olderHistoryLoan.setReturnDate(LocalDate.of(2026, 6, 8));
        loanRepository.save(olderHistoryLoan);

        Loan newerHistoryLoan = new Loan();
        newerHistoryLoan.setUserId(3L);
        newerHistoryLoan.setBookId(3L);
        newerHistoryLoan.setLoanDate(LocalDate.of(2026, 7, 1));
        newerHistoryLoan.setDueDate(LocalDate.of(2026, 7, 10));
        newerHistoryLoan.setReturnDate(LocalDate.of(2026, 7, 8));
        loanRepository.save(newerHistoryLoan);

        mockMvc.perform(
                        get("/loans/history")
                                .param("page", "0")
                                .param("size", "5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].userId").value(3))
                .andExpect(jsonPath("$.content[0].bookId").value(3))
                .andExpect(
                        jsonPath("$.content[0].returnDate")
                                .value("2026-07-08")
                )
                .andExpect(jsonPath("$.content[1].userId").value(2))
                .andExpect(jsonPath("$.content[1].bookId").value(2))
                .andExpect(
                        jsonPath("$.content[1].returnDate")
                                .value("2026-06-08")
                )
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllHistoryLoans_shouldReturn400_whenSizeExceedsMaximum()
            throws Exception {

        mockMvc.perform(get("/loans/history")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.errors.size")
                        .value("Size must not exceed 100"));

        assertEquals(0, loanRepository.count());
    }

    @Test
    void borrow_shouldReturn400_whenDaysExceedMaximum() throws Exception {
        String requestBody = """
                {
                "bookId": 1,
                "days": 16
                }
                """;

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.days").exists());

        assertEquals(0, loanRepository.count());
    }

    @Test
    void hasActiveLoans_shouldReturnTrue_whenUserHasActiveLoan()
            throws Exception {

        Loan activeLoan = new Loan();
        activeLoan.setBookId(1L);
        activeLoan.setActiveBookId(1L);
        activeLoan.setUserId(1L);
        activeLoan.setLoanDate(LocalDate.now());
        activeLoan.setDueDate(LocalDate.now().plusDays(8));
        activeLoan.setReturnDate(null);

        loanRepository.save(activeLoan);

        mockMvc.perform(
                        get("/users/{userId}/loans/active/exists", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void hasActiveLoans_shouldReturnFalse_whenUserHasNoActiveLoan()
            throws Exception {

        Loan historyLoan = new Loan();
        historyLoan.setBookId(1L);
        historyLoan.setUserId(1L);
        historyLoan.setLoanDate(LocalDate.now().minusDays(10));
        historyLoan.setDueDate(LocalDate.now().minusDays(2));
        historyLoan.setReturnDate(LocalDate.now());

        loanRepository.save(historyLoan);

        mockMvc.perform(
                        get("/users/{userId}/loans/active/exists", 1L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }
}
