package com.kexun.loan.repository;

import com.kexun.loan.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    // find historical loan records by userId
    List<Loan> findByUserIdAndReturnDateIsNotNullOrderByReturnDateDesc(Long userId);

    // find alive loan records by userId
    List<Loan> findByUserIdAndReturnDateIsNullOrderByDueDateAsc(Long userId);

    // generated for return-book action
    Optional<Loan> findByUserIdAndBookIdAndReturnDateIsNull(Long userId, Long bookId);

    // check the availability of a book, only return boolean value
    // if the book is not available, return more info (how many days left for last borrower)
    Optional<Loan> findByBookIdAndReturnDateIsNull(Long bookId);

    // check the existence of active loan records
    boolean existsByUserIdAndReturnDateIsNull(Long UserId);
}
