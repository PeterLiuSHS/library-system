package com.kexun.loan.controller;

import com.kexun.loan.exception.GlobalExceptionHandler;
import com.kexun.loan.exception.ResourceNotFoundException;
import com.kexun.loan.exception.DownstreamServiceException;
import com.kexun.loan.model.Loan;
import com.kexun.loan.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@Import(GlobalExceptionHandler.class)
public class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @Test
    void borrow_shouldReturnLoan_whenRequestIsValid() throws Exception {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setUserId(1L);
        loan.setBookId(5L);

        when(loanService.borrow(1L, 5L, 7)).thenReturn(loan);

        mockMvc.perform(post("/users/1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "bookId": 5,
                                "days": 7
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(5));

        verify(loanService).borrow(1L, 5L, 7);
    }

    @Test
    void borrow_shouldReturnBadRequest_whenRequestBodyIsInvalid() throws Exception {
        // no matter what the requestbody looks like, controller will regard it as invalid
        String requestBody = """
                {
                "bookId": null,
                "days": -100
                }
                """;

        mockMvc.perform(post("/users/1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loanService);
    }

    @Test
    void borrow_shouldReturnServiceUnavailable_whenBookServiceFails() throws Exception {

        doThrow(new DownstreamServiceException("Book service is unavailable")).when(loanService).borrow(1L, 5L, 7);

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "bookId": 5,
                                "days": 7
                                }"""))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Book service is unavailable"));
        verify(loanService).borrow(1L, 5L, 7);
    }

    @Test
    void borrow_shouldReturnServiceUnavailable_whenUserServiceFails() throws Exception {

        doThrow(new DownstreamServiceException("User service is unavailable")).when(loanService).borrow(1L, 5L, 7);

        mockMvc.perform(post("/users/{userId}/loans", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "bookId": 5,
                                "days": 7
                                }"""))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("User service is unavailable"));

        verify(loanService).borrow(1L, 5L, 7);
    }

    @Test
    void returnBook_shouldReturnLoan_whenReturnIsSuccessful() throws Exception {
        Loan loan = new Loan();
        loan.setId(1L);
        loan.setUserId(1L);
        loan.setBookId(5L);

        when(loanService.returnBook(1L, 5L)).thenReturn(loan);

        mockMvc.perform(put("/users/{userId}/loans/{bookId}/return", 1L, 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(5));

        verify(loanService).returnBook(1L, 5L);
    }

    @Test
    void getAllLoansByUser_shouldReturnLoans_whenUserHasLoans() throws Exception {
        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setUserId(1L);
        loan1.setBookId(5L);
        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setUserId(1L);
        loan2.setBookId(6L);
        List<Loan> loans = Arrays.asList(loan1, loan2);

        when(loanService.getAllLoansForUser(1L)).thenReturn(loans);

        mockMvc.perform(get("/users/{userId}/loans", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(1))
                .andExpect(jsonPath("$.[0].userId").value(1))
                .andExpect(jsonPath("$.[0].bookId").value(5));

        verify(loanService).getAllLoansForUser(1L);
    }

    @Test
    void getAllLoansByUser_shouldReturnEmptyList_whenUserHasNoLoans() throws Exception {
        List<Loan> loans = Collections.emptyList();
        when(loanService.getAllLoansForUser(1L)).thenReturn(loans);
        mockMvc.perform(get("/users/{userId}/loans", 1L))
                .andExpect(status().isOk())
//                .andDo(print());
                .andExpect(content().json("[]"));
        verify(loanService).getAllLoansForUser(1L);
    }

    @Test
    void getAllLoansByUser_shouldReturnNotFound_whenUserDoesNotExist()
            throws Exception {

        when(loanService.getAllLoansForUser(1L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "User with id 1 not found"
                        )
                );

        mockMvc.perform(get("/users/{userId}/loans", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 1 not found"));

        verify(loanService).getAllLoansForUser(1L);
    }

    @Test
    void getActiveLoans_shouldReturnActiveLoans_whenUserHasActiveLoans() throws Exception {
        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setUserId(1L);
        loan1.setBookId(5L);
        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setUserId(1L);
        loan2.setBookId(6L);
        List<Loan> loans = Arrays.asList(loan1, loan2);

        when(loanService.getActiveLoansByUser(1L)).thenReturn(loans);

        mockMvc.perform(get("/users/{userId}/loans/active", 1L))
                .andExpect(status().isOk())
//                .andDo(print());
                .andExpect(jsonPath("$.[0].id").value(1))
                .andExpect(jsonPath("$.[0].userId").value(1))
                .andExpect(jsonPath("$.[0].bookId").value(5))
                .andExpect(jsonPath("$.[1].id").value(2))
                .andExpect(jsonPath("$.[1].userId").value(1))
                .andExpect(jsonPath("$.[1].bookId").value(6));
        verify(loanService).getActiveLoansByUser(1L);
    }

    @Test
    void getActiveLoans_shouldReturnEmptyList_whenUserHasNoActiveLoans() throws Exception {
        List<Loan> loans = Collections.emptyList();
        when(loanService.getActiveLoansByUser(1L)).thenReturn(loans);
        mockMvc.perform(get("/users/{userId}/loans/active", 1L))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(loanService).getActiveLoansByUser(1L);
    }

    @Test
    void getActiveLoans_shouldReturnNotFound_whenUserDoesNotExist()
            throws Exception {

        when(loanService.getActiveLoansByUser(1L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "User with id 1 not found"
                        )
                );

        mockMvc.perform(get("/users/{userId}/loans/active", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 1 not found"));

        verify(loanService).getActiveLoansByUser(1L);
    }

    @Test
    void getHistoryLoans_shouldReturnHistoryLoans_whenUserHasLoanHistory() throws Exception {
        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setUserId(1L);
        loan1.setBookId(5L);
        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setUserId(1L);
        loan2.setBookId(6L);
        List<Loan> loans = Arrays.asList(loan1, loan2);

        when(loanService.getLoanHistoryByUser(1L)).thenReturn(loans);

        mockMvc.perform(get("/users/{userId}/loans/history", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(1))
                .andExpect(jsonPath("$.[0].userId").value(1))
                .andExpect(jsonPath("$.[0].bookId").value(5))
                .andExpect(jsonPath("$.[1].id").value(2))
                .andExpect(jsonPath("$.[1].userId").value(1))
                .andExpect(jsonPath("$.[1].bookId").value(6));

        verify(loanService).getLoanHistoryByUser(1L);
    }

    @Test
    void getHistoryLoans_shouldReturnNotFound_whenUserDoesNotExist()
            throws Exception {

        when(loanService.getLoanHistoryByUser(1L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "User with id 1 not found"
                        )
                );

        mockMvc.perform(get("/users/{userId}/loans/history", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 1 not found"));

        verify(loanService).getLoanHistoryByUser(1L);
    }

    @Test
    void checkAvailability_shouldReturnAvailableTrue_whenBookIsAvailable() throws Exception {
        when(loanService.isBookAvailable(1L)).thenReturn(true);

        mockMvc.perform(get("/books/{bookId}/availability", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.remainingDays").value(0));

        verify(loanService).isBookAvailable(1L);
    }

    @Test
    void checkAvailability_shouldReturnAvailableFalseAndRemainingDays_whenBookIsNotAvailable() throws Exception {
        when(loanService.isBookAvailable(1L)).thenReturn(false);
        when(loanService.getRemainingDays(1L)).thenReturn(5L);

        mockMvc.perform(get("/books/{bookId}/availability", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.remainingDays").value(5));

        verify(loanService).isBookAvailable(1L);
        verify(loanService).getRemainingDays(1L);
    }

    @Test
    void checkAvailability_shouldReturnNotFound_whenBookDoesNotExist() throws Exception {

        doThrow(new ResourceNotFoundException("Book 5 not found"))
                .when(loanService).isBookAvailable(5L);

        mockMvc.perform(get("/books/{bookId}/availability", 5L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Book 5 not found"));

        verify(loanService).isBookAvailable(5L);
        verify(loanService, never()).getRemainingDays(anyLong());
    }

    @Test
    void getAllLoans_shouldReturnPagedLoans() throws Exception {
        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setUserId(1L);
        loan1.setBookId(5L);

        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setUserId(2L);
        loan2.setBookId(6L);

        List<Loan> loans = Arrays.asList(loan1, loan2);

        Pageable pageable = PageRequest.of(0, 5);
        Page<Loan> loanPage =
                new PageImpl<>(loans, pageable, loans.size());

        when(loanService.getAllLoans(0, 5))
                .thenReturn(loanPage);

        mockMvc.perform(
                        get("/loans")
                                .param("page", "0")
                                .param("size", "5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(1))
                .andExpect(jsonPath("$.content[0].bookId").value(5))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].userId").value(2))
                .andExpect(jsonPath("$.content[1].bookId").value(6))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(loanService).getAllLoans(0, 5);
    }

    @Test
    void getAllLoans_shouldReturnBadRequest_whenPageIsNegative()
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

        verifyNoInteractions(loanService);
    }

    @Test
    void getAllActiveLoans_shouldReturnBadRequest_whenSizeIsZero()
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

        verifyNoInteractions(loanService);
    }

    @Test
    void getAllHistoryLoans_shouldReturnBadRequest_whenSizeExceedsMaximum()
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

        verifyNoInteractions(loanService);
    }

    @Test
    void getAllActiveLoans_shouldReturnPagedActiveLoans()
            throws Exception {

        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setUserId(1L);
        loan1.setBookId(5L);
        loan1.setActiveBookId(5L);
        loan1.setReturnDate(null);

        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setUserId(2L);
        loan2.setBookId(6L);
        loan2.setActiveBookId(6L);
        loan2.setReturnDate(null);

        List<Loan> activeLoans = Arrays.asList(loan1, loan2);

        Pageable pageable = PageRequest.of(0, 5);
        Page<Loan> activeLoanPage =
                new PageImpl<>(
                        activeLoans,
                        pageable,
                        activeLoans.size()
                );

        when(loanService.getAllActiveLoans(0, 5))
                .thenReturn(activeLoanPage);

        mockMvc.perform(
                        get("/loans/active")
                                .param("page", "0")
                                .param("size", "5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(1))
                .andExpect(jsonPath("$.content[0].bookId").value(5))
                .andExpect(jsonPath("$.content[0].returnDate").isEmpty())
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].userId").value(2))
                .andExpect(jsonPath("$.content[1].bookId").value(6))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(loanService).getAllActiveLoans(0, 5);
    }

    @Test
    void getAllHistoryLoans_shouldReturnPagedHistoryLoans()
            throws Exception {

        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setUserId(1L);
        loan1.setBookId(5L);
        loan1.setReturnDate(java.time.LocalDate.of(2026, 7, 10));

        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setUserId(2L);
        loan2.setBookId(6L);
        loan2.setReturnDate(java.time.LocalDate.of(2026, 7, 8));

        List<Loan> historyLoans = Arrays.asList(loan1, loan2);

        Pageable pageable = PageRequest.of(0, 5);
        Page<Loan> historyLoanPage =
                new PageImpl<>(
                        historyLoans,
                        pageable,
                        historyLoans.size()
                );

        when(loanService.getAllHistoryLoans(0, 5))
                .thenReturn(historyLoanPage);

        mockMvc.perform(
                        get("/loans/history")
                                .param("page", "0")
                                .param("size", "5")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(1))
                .andExpect(jsonPath("$.content[0].bookId").value(5))
                .andExpect(
                        jsonPath("$.content[0].returnDate")
                                .value("2026-07-10")
                )
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].userId").value(2))
                .andExpect(jsonPath("$.content[1].bookId").value(6))
                .andExpect(
                        jsonPath("$.content[1].returnDate")
                                .value("2026-07-08")
                )
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(loanService).getAllHistoryLoans(0, 5);
    }

}
