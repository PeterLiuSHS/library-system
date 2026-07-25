package com.kexun.loan.service;

import com.kexun.loan.model.Loan;

import java.util.List;

public interface LoanService {
    // action: borrow a book
    Loan borrow(Long userId, Long bookId, int days);
    // action: return a book
    Loan returnBook(Long userId, Long bookId);
    // get active loan records by userId
    List<Loan> getActiveLoansByUser(Long userId);
    // get historical loan records by userId
    List<Loan> getLoanHistoryByUser(Long userId);
    // get all loan records by userId = active + historical
    List<Loan> getAllLoansForUser(Long userId);
    // quick check the book's availability
    boolean isBookAvailable(Long bookId);
    // to return detailed info for book check
    long getRemainingDays(Long bookId);
    // check existence of active loan records
    boolean hasActiveLoans(Long userId);
    // get all active loans
    List<Loan> getAllActiveLoans();
    // get all loan records
    List<Loan> getAllLoans();
    // get all historical loan records
    List<Loan> getAllHistoryLoans();
}
