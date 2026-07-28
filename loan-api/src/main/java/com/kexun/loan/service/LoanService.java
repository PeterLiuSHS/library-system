package com.kexun.loan.service;

import com.kexun.loan.model.Loan;
import org.springframework.data.domain.Page;

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
    Page<Loan> getAllActiveLoans(int page, int size);
    // get all loan records
    Page<Loan> getAllLoans(int page, int size);
    // get all historical loan records
    Page<Loan> getAllHistoryLoans(int page, int size);
}
