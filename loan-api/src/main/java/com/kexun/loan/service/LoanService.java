package com.kexun.loan.service;

import com.kexun.loan.model.Loan;

import java.util.List;

public interface LoanService {
    // action: borrow a book
    Loan borrow(Long userId, Long bookId, int days);
    // action: return a book
    Loan returnBook(Long userId, Long bookId);
    // get active loan records
    List<Loan> getActiveLoansByUser(Long userId);
    // get historical loan records
    List<Loan> getLoanHistoryByUser(Long userId);
    // get all loan records = active + historical
    List<Loan> getAllLoansForUser(Long userId);
    // quick check the book's availability
    boolean isBookAvailable(Long bookId);
    // to return detailed info for book check
    long getRemainingDays(Long bookId);
    // check existence of active loan records
    boolean hasActiveLoans(Long userId);
}
