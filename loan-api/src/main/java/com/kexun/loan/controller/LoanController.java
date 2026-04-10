package com.kexun.loan.controller;

import com.kexun.loan.dto.AvailabilityResponse;
import com.kexun.loan.dto.BorrowRequest;
import com.kexun.loan.model.Loan;
import com.kexun.loan.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/users/{userId}/loans")
    public Loan borrow(
            @PathVariable Long userId,
            @Valid @RequestBody BorrowRequest request
    ){
        return loanService.borrow(userId, request.getBookId(), request.getDays());
    }

    @PutMapping("/users/{userId}/loans/{bookId}/return")
    public Loan returnBook(
            @PathVariable Long userId, @PathVariable Long bookId
    ){
        return loanService.returnBook(userId, bookId);
    }

    @GetMapping("/users/{userId}/loans")
    public List<Loan> getAllLoansByUser(@PathVariable Long userId){
        return loanService.getAllLoansForUser(userId);
    }

    @GetMapping("/users/{userId}/loans/active")
    public List<Loan> getActiveLoans(@PathVariable Long userId){
        return loanService.getActiveLoansByUser(userId);
    }

    @GetMapping("/users/{userId}/loans/history")
    public List<Loan> getHistoryLoans(@PathVariable Long userId){
        return loanService.getLoanHistoryByUser(userId);
    }

    @GetMapping("/books/{bookId}/availability")
    public AvailabilityResponse checkAvailability(@PathVariable Long bookId){
        boolean available = loanService.isBookAvailable(bookId);

        AvailabilityResponse response = new AvailabilityResponse();
        response.setAvailable(available);

        if(!available){
            response.setRemainingDays(loanService.getRemainingDays(bookId));
        } else {
            response.setRemainingDays(0);
        }

        return response;
    }
}


