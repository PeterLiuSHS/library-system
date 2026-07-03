package com.kexun.user.service;

import com.kexun.user.client.LoanClient;
import com.kexun.user.dto.UserUpdateRequest;
import com.kexun.user.exception.ConflictException;
import com.kexun.user.exception.ResourceNotFoundException;
import com.kexun.user.model.User;
import com.kexun.user.repository.UserRepository;
import com.kexun.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserRepository userRepository;
    private LoanClient loanClient;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        loanClient = mock(LoanClient.class);
        userService = new UserServiceImpl(userRepository, loanClient);
    }

    @Test
    void create_shouldSaveUser_whenUserProvided() {

        User user = new User();
        when(userRepository.save(user))
                .thenReturn(user);

        User result = userService.create(user);

        assertEquals(user, result);
        verify(userRepository).save(user);
    }

    @Test
    void getById_shouldReturnUser_whenUserExistsAndIsNotDeleted() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(user));

        User result = userService.getById(1L);

        assertEquals(user, result);
        verify(userRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    void getById_shouldThrowException_whenUserNotFoundOrDeleted() {
        when(userRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getById(1L)
        );

        assertEquals("User 1 not found", ex.getMessage());
        verify(userRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    void list_shouldReturnNonDeletedUsers_whenSearchIsNull() {
        User user1 = new User();
        user1.setId(1L);

        User user2 = new User();
        user2.setId(2L);

        List<User> users = Arrays.asList(user1, user2);
        Page<User> userPage = new PageImpl<>(users);

        Pageable pageable = PageRequest.of(0, 5);

        when(userRepository.findByDeletedFalse(pageable))
                .thenReturn(userPage);

        Page<User> result = userService.list(null, 0, 5);

        assertEquals(users, result.getContent());
        verify(userRepository).findByDeletedFalse(pageable);
    }

    @Test
    void list_shouldReturnNonDeletedUsers_whenSearchIsBlank() {
        User user1 = new User();
        user1.setId(1L);

        User user2 = new User();
        user2.setId(2L);

        List<User> users = Arrays.asList(user1, user2);
        Page<User> userPage = new PageImpl<>(users);

        Pageable pageable = PageRequest.of(0, 5);
        when(userRepository.findByDeletedFalse(pageable))
                .thenReturn(userPage);

        Page<User> result = userService.list(" ", 0, 5);

        assertEquals(users, result.getContent());  // what we test is not the address, but the data
        // in case of the service code written as "return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());"
        verify(userRepository).findByDeletedFalse(pageable);
    }

    @Test
    void list_shouldSearchNonDeletedUsers_whenSearchKeywordProvided() {
        String keyword = "Peter";

        User user1 = new User();
        user1.setId(1L);
        user1.setName("Peter");

        List<User> users = List.of(user1);
        Page<User> userPage = new PageImpl<>(users);

        when(userRepository.
                findByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndEmailContainingIgnoreCase(
                        eq(keyword),
                        eq(keyword),
                        any(Pageable.class)
                ))
                .thenReturn(userPage);

        Page<User> result = userService.list(keyword, 0, 5);

        assertEquals(users, result.getContent());

        verify(userRepository).findByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndEmailContainingIgnoreCase(
                eq(keyword),
                eq(keyword),
                any(Pageable.class)
        );
    }

    @Test
    void list_shouldUseCorrectPaginationParameters() {
        Page<User> emptyPage = new PageImpl<>(List.of());

        when(userRepository.findByDeletedFalse(any(Pageable.class)))
                .thenReturn(emptyPage);

        userService.list(null, 0, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findByDeletedFalse(pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();

        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(5, capturedPageable.getPageSize());
    }

    @Test
    void update_shouldUpdateUserName_whenUserExistsAndIsNotDeleted() {
        User user = new User();
        user.setId(1L);
        user.setName("Peter");

        when(userRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Jack");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.update(1L, request);

        assertEquals("Jack", result.getName());

        verify(userRepository).findByIdAndDeletedFalse(1L);
        verify(userRepository).save(user);
    }

    @Test
    void update_shouldThrowException_whenUserNotFoundOrDeleted() {
        when(userRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Clark");

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.update(1L, request)
        );

        assertEquals("User 1 not found", ex.getMessage());
        verify(userRepository).findByIdAndDeletedFalse(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_shouldCallRepositorySave_whenUserExistsAndIsNotDeleted() {
        User user = new User();
        user.setId(1L);
        user.setName("Peter");

        when(userRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Jack");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        userService.update(1L, request);

        verify(userRepository).save(user);
    }

    @Test
    void delete_shouldThrowException_whenUserNotFoundOrDeleted() {
        when(userRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.delete(1L)
        );

        assertEquals("User 1 not found", ex.getMessage());

        verify(userRepository).findByIdAndDeletedFalse(1L);
        verifyNoInteractions(loanClient);
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_shouldThrowConflictException_whenUserHasActiveLoans() {
        User user = new User();
        user.setId(1L);
        user.setDeleted(false);

        when(userRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(user));

        when(loanClient.hasActiveLoans(1L))
                .thenReturn(true);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> userService.delete(1L)
        );

        assertEquals("User 1 has active loans and cannot be deleted", ex.getMessage());
        assertFalse(user.isDeleted());

        verify(userRepository).findByIdAndDeletedFalse(1L);
        verify(loanClient).hasActiveLoans(1L);
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleted_shouldSoftDeleteUser_whenUserExistsAndHasNotActiveLoans() {
        User user = new User();
        user.setId(1L);
        user.setDeleted(true);

        when(userRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(user));

        when(loanClient.hasActiveLoans(1L))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.delete(1L);

        assertTrue(user.isDeleted());

        verify(userRepository).findByIdAndDeletedFalse(1L);
        verify(loanClient).hasActiveLoans(1L);
        verify(userRepository).save(user);
        verify(userRepository, never()).deleteById(anyLong());
    }
}
