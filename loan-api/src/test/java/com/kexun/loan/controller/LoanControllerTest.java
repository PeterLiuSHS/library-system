package com.kexun.loan.controller;

import com.kexun.loan.exception.GlobalExceptionHandler;
import com.kexun.loan.model.Loan;
import com.kexun.loan.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.awt.print.Book;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

}
