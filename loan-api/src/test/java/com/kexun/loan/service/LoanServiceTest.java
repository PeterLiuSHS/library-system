package com.kexun.loan.service;

import com.kexun.loan.client.BookClient;
import com.kexun.loan.client.UserClient;
import com.kexun.loan.repository.LoanRepository;
import com.kexun.loan.service.impl.LoanServiceImpl;
import com.kexun.loan.model.Loan;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LoanServiceTest {
    @Test
    void borrow_shouldCreateLoan_whenBookAvailable() {
        // create mock dependencies
        LoanRepository loanRepository = mock(LoanRepository.class);  // create a fake LoanRepository object, a mocking object
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);

        // create a test object
        LoanServiceImpl loanService =
                new LoanServiceImpl(loanRepository, userClient, bookClient);

        // define the action of mock object: book not borrowed
        when(loanRepository.findByBookIdAndReturnDateIsNull(5L))   // coz it's mocked, so when(...).thenReturn(...) is a must
                .thenReturn(Optional.empty());  // the return type of mockito should be same as the original method signature, the original return type is Optional<Loan>

        // mock the repository save successfully
        Loan loan = new Loan();
        loan.setId(1L);

        when(loanRepository.save(any())).thenReturn(loan);

        // run method
        Loan result = loanService.borrow(1L, 5L, 7);

        // verify
        assertNotNull(result);

        verify(userClient).assertUserExists(1L);   // verify the process, log check
        verify(bookClient).assertBookExists(5L);
        verify(loanRepository).save(any());               // verify the result, log check
    }

    @Test
    void borrow_shouldFail_whenBookAlreadyBorrowed() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);

        LoanServiceImpl loanService =
                new LoanServiceImpl(loanRepository, userClient, bookClient);

        Loan activeLoan = new Loan();
        activeLoan.setBookId(5L);

        when(loanRepository.findByBookIdAndReturnDateIsNull(5L))
                .thenReturn(Optional.of(activeLoan));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> loanService.borrow(1L, 5L, 7)
        );

        assertEquals("Book is not available", ex.getMessage());  // compare the String with the message in the exception

        verify(userClient).assertUserExists(1L);
        verify(bookClient).assertBookExists(5L);
        verify(loanRepository, times(0)).save(any());
    }

    @Test
    void borrow_shouldFail_whenUserNotFound() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);

        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        doThrow(new RuntimeException("User 1 not found"))
                .when(userClient).assertUserExists(1L);
        // coz assertUserExists this method responds nothing, so we can't use when(...)do(...),
        // should use doThrow(...).when(...).voidMethodName(...) instead

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> loanService.borrow(1L, 5L, 7)  // use lambda to wrap the code to an object sending to asserThrows()
        );

        assertEquals("User 1 not found", ex.getMessage());

        verify(userClient).assertUserExists(1L);
        verify(bookClient, times(0)).assertBookExists(anyLong());
        verify(loanRepository, never()).findByBookIdAndReturnDateIsNull(anyLong());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrow_shouldFail_whenBookNotFound() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);

        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        doThrow(new RuntimeException("Book 5 not found"))
                .when(bookClient).assertBookExists(5L);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> loanService.borrow(1L, 5L, 7)
        );

        assertEquals("Book 5 not found", ex.getMessage());
        verify(userClient).assertUserExists(1L);
        verify(bookClient).assertBookExists(5L);
        verify(loanRepository, never()).findByBookIdAndReturnDateIsNull(anyLong());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void returnBook_shouldUpdateReturnDate_whenActiveLoanExists() {

        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);

        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        // generate an active but not returned/historical loan record, return date will be null
        Loan activeLoan = new Loan();
        activeLoan.setId(1L);
        activeLoan.setUserId(1L);
        activeLoan.setBookId(5L);
        // check
        when(loanRepository.findByUserIdAndBookIdAndReturnDateIsNull(1L, 5L))
                .thenReturn(Optional.of(activeLoan));
        // update record
        when(loanRepository.save(any())).thenReturn(activeLoan);
        // call return method
        Loan result = loanService.returnBook(1L, 5L);

        assertNotNull(result);
        assertNotNull(result.getReturnDate());

        verify(loanRepository).findByUserIdAndBookIdAndReturnDateIsNull(1L, 5L);
        verify(loanRepository).save(activeLoan);
    }

    @Test
    void returnBook_shouldFail_whenActiveLoanNotFound() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);
        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        when(loanRepository.findByUserIdAndBookIdAndReturnDateIsNull(1L, 5L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> loanService.returnBook(1L, 5L)
        );

        assertEquals("Active loan not found for user 1 and book 5", ex.getMessage());

        verify(loanRepository).findByUserIdAndBookIdAndReturnDateIsNull(1L, 5L);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void getActiveLoanByUser_ShouldReturnActiveLoans() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);
        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        Loan activeLoan1 = new Loan();
        activeLoan1.setId(1L);
        activeLoan1.setUserId(1L);
        activeLoan1.setBookId(5L);
        activeLoan1.setDueDate(LocalDate.of(2026, 1, 1));
        activeLoan1.setReturnDate(null);

        Loan activeLoan2 = new Loan();
        activeLoan2.setId(2L);
        activeLoan2.setUserId(1L);
        activeLoan2.setBookId(6L);
        activeLoan1.setDueDate(LocalDate.of(2026, 1, 2));
        activeLoan2.setReturnDate(null);

        List<Loan> activeLoans = Arrays.asList(activeLoan1, activeLoan2);

        when(loanRepository.findByUserIdAndReturnDateIsNullOrderByDueDateAsc(1L))
                .thenReturn(activeLoans);

        List<Loan> result = loanService.getActiveLoansByUser(1L);

        assertNotNull(result);
        assertEquals(activeLoans.size(), result.size());
        assertEquals(activeLoans, result);

        verify(loanRepository).findByUserIdAndReturnDateIsNullOrderByDueDateAsc(1L);
    }

    @Test
    void getLoanHistory_ShouldReturnLoanHistory() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);
        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        Loan hisLoan1 = new Loan();
        hisLoan1.setId(1L);
        hisLoan1.setUserId(1L);
        hisLoan1.setBookId(5L);
        hisLoan1.setReturnDate(LocalDate.of(2026, 1, 2));

        Loan hisLoan2 = new Loan();
        hisLoan2.setId(2L);
        hisLoan2.setUserId(1L);
        hisLoan2.setBookId(6L);
        hisLoan2.setReturnDate(LocalDate.of(2026, 1, 1));

        List<Loan> hisLoans = Arrays.asList(hisLoan1, hisLoan2);

        when(loanRepository.findByUserIdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(hisLoans);

        List<Loan> result = loanService.getLoanHistoryByUser(1L);

        assertNotNull(result);
        assertEquals(hisLoans.size(), result.size());
        assertEquals(hisLoans, result);

        verify(loanRepository).findByUserIdAndReturnDateIsNotNullOrderByReturnDateDesc(1L);
    }

    @Test
    void isBookAvailable_shouldReturnTrue_whenNoActiveLoan() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);
        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        when(loanRepository.findByBookIdAndReturnDateIsNull(1L))
                .thenReturn(Optional.empty());

        boolean result = loanService.isBookAvailable(1L);

        assertTrue(result);

        verify(loanRepository).findByBookIdAndReturnDateIsNull(1L);
    }

    @Test
    void isBookAvailable_shouldReturnFalse_whenActiveLoanExists() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);
        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        Loan activeLoan = new Loan();
        activeLoan.setId(1L);
        activeLoan.setUserId(1L);
        activeLoan.setBookId(5L);
        activeLoan.setReturnDate(LocalDate.of(2026, 1, 1));

        when(loanRepository.findByBookIdAndReturnDateIsNull(5L))
                .thenReturn(Optional.of(activeLoan));

        boolean result = loanService.isBookAvailable(5L);

        assertFalse(result);
        verify(loanRepository).findByBookIdAndReturnDateIsNull(5L);
    }

    @Test
    void getRemainingDays_shouldFail_whenBookAvailable() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);
        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        when(loanRepository.findByBookIdAndReturnDateIsNull(5L))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> loanService.getRemainingDays(5L)
        );

        assertEquals("Active loan not found for book 5", ex.getMessage());
        verify(loanRepository).findByBookIdAndReturnDateIsNull(5L);
    }

    @Test
    void getRemainingDays_shouldReturnRemainingDays_whenLoanExists() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);
        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        Loan loan = new Loan();
        loan.setId(1L);
        loan.setUserId(1L);
        loan.setBookId(5L);
        LocalDate testDay = LocalDate.now();
        loan.setDueDate(testDay.plusDays(5));

        when(loanRepository.findByBookIdAndReturnDateIsNull(5L))
                .thenReturn(Optional.of(loan));

        long result = loanService.getRemainingDays(5L);

        assertEquals(5L, result);

        verify(loanRepository).findByBookIdAndReturnDateIsNull(5L);
    }

    @Test
    void getAllLoansForUser_shouldMergeActiveAndHistoryLoans() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);
        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        Loan activeLoan = new Loan();
        activeLoan.setId(1L);
        activeLoan.setUserId(1L);
        activeLoan.setBookId(5L);
        activeLoan.setReturnDate(null);

        List<Loan> activeLoans = Arrays.asList(activeLoan);

        Loan deadLoan = new Loan();
        deadLoan.setId(2L);
        deadLoan.setUserId(1L);
        deadLoan.setBookId(6L);
        deadLoan.setReturnDate(LocalDate.of(2026, 1, 1));

        List<Loan> deadLoans = Arrays.asList(deadLoan);
        List<Loan> allLoans = Arrays.asList(activeLoan, deadLoan);

        when(loanRepository.findByUserIdAndReturnDateIsNullOrderByDueDateAsc(1L))
                .thenReturn(activeLoans);
        when(loanRepository.findByUserIdAndReturnDateIsNotNullOrderByReturnDateDesc(1L))
                .thenReturn(deadLoans);

        List<Loan> result = loanService.getAllLoansForUser(1L);
        assertNotNull(result);
        assertEquals(allLoans.size(), result.size());
        assertEquals(activeLoans.get(0), result.get(0));
        assertEquals(deadLoans.get(0), result.get(1));

        verify(loanRepository).findByUserIdAndReturnDateIsNullOrderByDueDateAsc(1L);
        verify(loanRepository).findByUserIdAndReturnDateIsNotNullOrderByReturnDateDesc(1L);
    }

    @Test
    void hasActiveLoans_shouldReturnTrue_whenUserHasActiveLoans() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);

        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        when(loanRepository.existsByUserIdAndReturnDateIsNull(1L))
                .thenReturn(true);

        boolean result = loanService.hasActiveLoans(1L);

        assertTrue(result);
        verify(loanRepository).existsByUserIdAndReturnDateIsNull(1L);
        verifyNoInteractions(userClient);
        verifyNoInteractions(bookClient);
    }

    @Test
    void hasActiveLoans_shouldReturnFalse_whenUserHasNoActiveLoans() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);

        LoanServiceImpl loanService =
                new LoanServiceImpl(loanRepository, userClient, bookClient);

        when(loanRepository.existsByUserIdAndReturnDateIsNull(1L))
                .thenReturn(false);

        boolean result = loanService.hasActiveLoans(1L);

        assertFalse(result);
        verify(loanRepository).existsByUserIdAndReturnDateIsNull(1L);
        verifyNoInteractions(userClient);
        verifyNoInteractions(bookClient);
    }

    @Test
    void getAllLoans_shouldReturnAllLoans() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);

        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);

        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setUserId(1L);
        loan1.setBookId(5L);
        loan1.setReturnDate(LocalDate.of(2026, 7, 10));

        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setUserId(2L);
        loan2.setBookId(6L);
        loan2.setReturnDate(LocalDate.of(2026, 7, 6));

        List<Loan> historyLoans = Arrays.asList(loan1, loan2);

        when(loanRepository.findAllByOrderByLoanDateDesc())
                .thenReturn(historyLoans);

        List<Loan> result = loanService.getAllLoans();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(historyLoans, result);

        verify(loanRepository).findAllByOrderByLoanDateDesc();

        verifyNoInteractions(userClient);
        verifyNoInteractions(bookClient);
    }

    @Test
    void getAllActiveLoans_shouldReturnActiveLoans() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);
        LoanServiceImpl loanService = new LoanServiceImpl(loanRepository, userClient, bookClient);
        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setUserId(1L);
        loan1.setBookId(5L);
        loan1.setDueDate(LocalDate.of(2026, 7, 15));
        loan1.setReturnDate(null);

        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setUserId(2L);
        loan2.setBookId(6L);
        loan2.setDueDate(LocalDate.of(2026, 7, 21));
        loan2.setReturnDate(null);

        List<Loan> activeLoans = Arrays.asList(loan1, loan2);

        when(loanRepository.findByReturnDateIsNullOrderByDueDateAsc())
                .thenReturn(activeLoans);

        List<Loan> result = loanService.getAllActiveLoans();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(activeLoans, result);

        verify(loanRepository).findByReturnDateIsNullOrderByDueDateAsc();
        verifyNoInteractions(userClient);
        verifyNoInteractions(bookClient);
    }

    @Test
    void getAllHistoryLoans_shouldReturnHistoryLoans() {
        LoanRepository loanRepository = mock(LoanRepository.class);
        UserClient userClient = mock(UserClient.class);
        BookClient bookClient = mock(BookClient.class);

        LoanServiceImpl loanService =
                new LoanServiceImpl(loanRepository, userClient, bookClient);

        Loan loan1 = new Loan();
        loan1.setId(1L);
        loan1.setUserId(1L);
        loan1.setBookId(5L);
        loan1.setReturnDate(LocalDate.of(2026, 7, 10));

        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setUserId(2L);
        loan2.setBookId(6L);
        loan2.setReturnDate(LocalDate.of(2026, 7, 8));

        List<Loan> historyLoans = Arrays.asList(loan1, loan2);

        when(loanRepository.findByReturnDateIsNotNullOrderByReturnDateDesc())
                .thenReturn(historyLoans);

        List<Loan> result = loanService.getAllHistoryLoans();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(historyLoans, result);

        verify(loanRepository)
                .findByReturnDateIsNotNullOrderByReturnDateDesc();

        verifyNoInteractions(userClient);
        verifyNoInteractions(bookClient);
    }
}
