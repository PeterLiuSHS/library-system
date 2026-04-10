package com.kexun.loan.service.impl;

import com.kexun.loan.client.BookClient;
import com.kexun.loan.client.UserClient;
import com.kexun.loan.exception.ConflictException;
import com.kexun.loan.exception.ResourceNotFoundException;
import com.kexun.loan.model.Loan;
import com.kexun.loan.repository.LoanRepository;
import com.kexun.loan.service.LoanService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final UserClient userClient;
    private final BookClient bookClient;

    public LoanServiceImpl(LoanRepository loanRepository, UserClient userClient, BookClient bookClient) {
        this.loanRepository = loanRepository;
        this.userClient = userClient;
        this.bookClient = bookClient;
    }

    @Override
    public Loan borrow(Long userId, Long bookId, int days) {
        userClient.assertUserExists(userId);
        bookClient.assertBookExists(bookId);
        // check the availability of the book first
        Optional<Loan> activeLoan =
                        loanRepository.findByBookIdAndReturnDateIsNull(bookId);

        if (activeLoan.isPresent()) {  // if the value exists, .isPresent() will be true
            throw new ConflictException("Book is not available");
        }
        // when the book is available
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBookId(bookId);
        // freeze current time
        LocalDate today = LocalDate.now();
        loan.setLoanDate(today);
        loan.setDueDate(today.plusDays(days));
        loan.setReturnDate(null);
        return loanRepository.save(loan);
    }

    @Override
    public Loan returnBook(Long userId, Long bookId) {
        Loan loan = loanRepository
                .findByUserIdAndBookIdAndReturnDateIsNull(userId, bookId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Active loan not found for user " + userId + " and book " + bookId));

        loan.setReturnDate(LocalDate.now());
        return loanRepository.save(loan);
    }

    @Override
    public List<Loan> getActiveLoansByUser(Long userId) {
        return loanRepository
                .findByUserIdAndReturnDateIsNullOrderByDueDateAsc(userId);
    }

    @Override
    public List<Loan> getLoanHistoryByUser(Long userId) {
        return loanRepository
                .findByUserIdAndReturnDateIsNotNullOrderByReturnDateDesc(userId);
    }

    @Override
    public boolean isBookAvailable(Long bookId) {
        return loanRepository
                .findByBookIdAndReturnDateIsNull(bookId)
                .isEmpty();  // if the value doesn't exist, return true
    }

    @Override
    public long getRemainingDays(Long bookId) {
        // Optional<Loan> loan =
        //        loanRepository.findByBookIdAndReturnDateIsNull(bookId);

        Loan loan = loanRepository.findByBookIdAndReturnDateIsNull(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Active loan not found for book " + bookId));
        // .orElseThrow() will unwrap the Optional<Loan> object
        // so for loan.getDueDate(), should not add .get() between loan and getDueDate()
        LocalDate dueDate = loan.getDueDate();
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);  // ChronoUnit.DAYS.between(start, end);
    }

    @Override
    public List<Loan> getAllLoansForUser(Long userId) {
        List<Loan> active = loanRepository
                .findByUserIdAndReturnDateIsNullOrderByDueDateAsc(userId);
        List<Loan> history = loanRepository
                .findByUserIdAndReturnDateIsNotNullOrderByReturnDateDesc(userId);
        List<Loan> result = new ArrayList<>(active);
        result.addAll(history);
        return result;
    }
}
