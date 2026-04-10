package com.kexun.user.service;

import com.kexun.user.dto.UserUpdateRequest;
import com.kexun.user.exception.ResourceNotFoundException;
import com.kexun.user.model.User;
import com.kexun.user.repository.UserRepository;
import com.kexun.user.service.impl.UserServiceImpl;
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
import static org.mockito.Mockito.*;

public class UserServiceTest {
    @Test
    void create_shouldSaveUser_whenUserProvided() {
        UserRepository userRepository = mock(UserRepository.class);

        UserService userService = new UserServiceImpl(userRepository);

        User user = new User();
        when(userRepository.save(user))
                .thenReturn(user);

        User result = userService.create(user);

        assertEquals(user, result);
        verify(userRepository).save(user);
    }

    @Test
    void getById_shouldReturnUser_whenUserExists() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.getById(1L);
        assertEquals(user, result);

        verify(userRepository).findById(1L);
    }

    @Test
    void getById_shouldThrowException_whenUserNotFound() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getById(1L)
        );

        assertEquals("User 1 not found", ex.getMessage());
        verify(userRepository).findById(1L);
    }

    @Test
    void list_shouldReturnAllUsers_whenSearchIsNull() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        User user1 = new User();
        user1.setId(1L);

        User user2 = new User();
        user2.setId(2L);

        List<User> users = Arrays.asList(user1, user2);
        Page<User> userPage = new PageImpl<>(users);

        Pageable pageable = PageRequest.of(0, 5);
        when(userRepository.findAll(pageable))
                .thenReturn(userPage);

        Page<User> result = userService.list(null, 0, 5);

        assertEquals(users, result.getContent());  // what we test is not the address, but the data
        // in case of the service code written as "return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());"
        verify(userRepository).findAll(pageable);
    }

    @Test
    void list_shouldReturnAllUsers_whenSearchIsBlank() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        User user1 = new User();
        user1.setId(1L);

        User user2 = new User();
        user2.setId(2L);

        List<User> users = Arrays.asList(user1, user2);
        Page<User> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 5);

        when(userRepository.findAll(pageable))
                .thenReturn(userPage);

        Page<User> result = userService.list(" ", 0, 5);

        assertEquals(userPage.getContent(), result.getContent());  // what we test is not the address, but the data
        // in case of the service code written as "return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());"
        verify(userRepository).findAll(pageable);
    }

    @Test
    void list_shouldSearchUsers_whenSearchKeywordProvided() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        String keyword = "Peter";

        User user1 = new User();
        user1.setId(1L);
        user1.setName("Peter");

        // User user2 = new User();
        // user2.setId(2L);
        List<User> users = Arrays.asList(user1);
        Page<User> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 5);

        when(userRepository.
                findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        eq(keyword),
                        eq(keyword),
                        any(Pageable.class)
                ))
                .thenReturn(userPage);

        Page<User> result = userService.list(keyword, 0, 5);

        assertEquals(users, result.getContent());

        verify(userRepository).findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                eq(keyword),
                eq(keyword),
                any(Pageable.class)
        );
    }

    @Test
    void list_shouldUseCorrectPaginationParameters() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        Page<User> emptyPage = new PageImpl<>(List.of());

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(emptyPage);

        userService.list(null, 0, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();

        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(5, capturedPageable.getPageSize());
    }

    @Test
    void update_shouldUpdateUserName_whenUserExists() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);
        User user = new User();
        user.setId(1L);
        user.setName("Peter");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Jack");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when(userRepository.save(user))
        //        .thenReturn(user);

        User result = userService.update(1L, request);

        assertEquals("Jack", result.getName());

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void update_shouldThrowException_whenUserNotFound() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.update(1L, new UserUpdateRequest())
        );

        assertEquals("User 1 not found", ex.getMessage());
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_shouldCallRepositorySave_whenUserExists() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        User user = new User();
        user.setId(1L);
        user.setName("Peter");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Jack");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        userService.update(1L, request);

        verify(userRepository).save(user);
    }

    @Test
    void delete_shouldThrowException_whenUserNotFound() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);
        when(userRepository.existsById(1L))
                .thenReturn(false);

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.delete(1L)
        );

        assertEquals("User 1 not found", ex.getMessage());
        verify(userRepository).existsById(1L);
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_shouldCallRepositoryDeleteById_whenUserExists() {
        UserRepository userRepository = mock(UserRepository.class);
        UserService userService = new UserServiceImpl(userRepository);

        when(userRepository.existsById(1L))
                .thenReturn(true);
        userService.delete(1L);
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }
}
