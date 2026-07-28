package com.kexun.loan.controller;

import com.kexun.loan.dto.AvailabilityResponse;
import com.kexun.loan.dto.BorrowRequest;
import com.kexun.loan.model.Loan;
import com.kexun.loan.service.LoanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping
@Validated
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/users/{userId}/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public Loan borrow(
            @PathVariable Long userId,
            @Valid @RequestBody BorrowRequest request
    ) {
        return loanService.borrow(userId, request.getBookId(), request.getDays());
    }

    @PutMapping("/users/{userId}/loans/{bookId}/return")
    public Loan returnBook(
            @PathVariable Long userId, @PathVariable Long bookId
    ) {
        return loanService.returnBook(userId, bookId);
    }

    @GetMapping("/users/{userId}/loans")
    public List<Loan> getAllLoansByUser(@PathVariable Long userId) {
        return loanService.getAllLoansForUser(userId);
    }

    @GetMapping("/users/{userId}/loans/active")
    public List<Loan> getActiveLoans(@PathVariable Long userId) {
        return loanService.getActiveLoansByUser(userId);
    }

    @GetMapping("/users/{userId}/loans/history")
    public List<Loan> getHistoryLoans(@PathVariable Long userId) {
        return loanService.getLoanHistoryByUser(userId);
    }

    @GetMapping("/books/{bookId}/availability")
    public AvailabilityResponse checkAvailability(@PathVariable Long bookId) {
        boolean available = loanService.isBookAvailable(bookId);

        AvailabilityResponse response = new AvailabilityResponse();
        response.setAvailable(available);

        if (!available) {
            response.setRemainingDays(loanService.getRemainingDays(bookId));
        } else {
            response.setRemainingDays(0);
        }

        return response;
    }

    @GetMapping("/loans")
    public Page<Loan> getAllLoans(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            int size
    ) {
        return loanService.getAllLoans(page, size);
    }

    @GetMapping("/loans/active")
    public Page<Loan> getAllActiveLoans(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            int size
    ) {
        return loanService.getAllActiveLoans(page, size);
    }

    @GetMapping("/loans/history")
    public Page<Loan> getHistoryLoans(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,


            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            int size
    ) {
        return loanService.getAllHistoryLoans(page, size);
    }


    @GetMapping("/users/{userId}/loans/active/exists")
    public boolean hasActiveLoans(@PathVariable Long userId) {
        return loanService.hasActiveLoans(userId);
    }
}


