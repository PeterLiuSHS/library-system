package com.kexun.user.controller;

import com.kexun.user.dto.UserUpdateRequest;
import com.kexun.user.exception.ConflictException;
import com.kexun.user.exception.GlobalExceptionHandler;
import com.kexun.user.exception.ResourceNotFoundException;
import com.kexun.user.model.User;
import com.kexun.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void create_shouldReturnUser_whenRequestIsValid() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Alice");
        user.setEmail("alice@example.com");

        when(userService.create(any(User.class))).thenReturn(user);

        String requestBody = """
                {
                  "name": "Alice",
                  "email": "alice@example.com"
                }
                """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        verify(userService).create(any(User.class));
    }

    @Test
    void create_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        String requestBody = """
                {
                "name": "",
                "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void create_shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
        doThrow(new DataIntegrityViolationException("Duplicate email")).when(userService).create(any(User.class));

        String requestBody = """
                {
                "name": "Alice",
                "email": "alice@example.com"
                }
                """;

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        verify(userService).create(any(User.class));
    }

    @Test
    void getById_shouldReturnUser_whenUserExists() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Alice");
        user.setEmail("alice@example.com");

        when(userService.getById(1L)).thenReturn(user);

        mockMvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        verify(userService).getById(1L);
    }

    @Test
    void getById_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("User 1 not found")).when(userService).getById(1L);

        mockMvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User 1 not found"));

        verify(userService).getById(1L);
    }

    @Test
    void list_shouldReturnUsers_whenUsersExist() throws Exception {
        User user1 = new User();
        user1.setId(1L);
        user1.setName("Alice Brown");
        user1.setEmail("alicebrown@example.com");
        User user2 = new User();
        user2.setId(2L);
        user2.setName("Alice White");
        user2.setEmail("alicewhite@example.com");
        User user3 = new User();
        user3.setId(3L);
        user3.setName("Danny Brown");
        user3.setEmail("dannybrown@gmail.com");
        List<User> users = Arrays.asList(user1, user2, user3);
        Page<User> userPage = new PageImpl<>(users);

        when(userService.list("Alice", 0, 5)).thenReturn(userPage);

        mockMvc.perform(get("/users")
                .param("search", "Alice")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alice Brown"))
                .andExpect(jsonPath("$.content[0].email").value("alicebrown@example.com"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].name").value("Alice White"))
                .andExpect(jsonPath("$.content[1].email").value("alicewhite@example.com"));

        verify(userService).list("Alice", 0, 5);
    }

    @Test
    void list_shouldReturnEmptyPage_whenNoUsersExist() throws Exception {
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList());

        when(userService.list(null, 0, 5)).thenReturn(emptyPage);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(userService).list(null, 0, 5);
    }

    @Test
    void update_shouldReturnUpdatedUser_whenRequestIsValid() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("David");

        when(userService.update(eq(1L), any(UserUpdateRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/users/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "name": "David"
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("David"));

        verify(userService).update(eq(1L), any(UserUpdateRequest.class));
    }

    @Test
    void update_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        mockMvc.perform(put("/users/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                         "name": ""
                        }
                        """))
                .andExpect(status().isBadRequest());

        verify(userService, never()).update(anyLong(), any(UserUpdateRequest.class));
    }

    @Test
    void update_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("User 1 not found")).when(userService).update(eq(1L), any(UserUpdateRequest.class));

        mockMvc.perform(put("/users/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        "name": "Jack"                                              
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User 1 not found"));

        verify(userService).update(eq(1L), any(UserUpdateRequest.class));
    }

    @Test
    void delete_shouldReturnNoContent_whenUserExists() throws Exception {

        mockMvc.perform(delete("/users/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    void delete_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("User 1 not found"))
                .when(userService).delete(1L);

        mockMvc.perform(delete("/users/{id}", 1L))
                .andExpect(status().isNotFound());

        verify(userService).delete(1L);
    }

    @Test
    void delete_shouldReturnConflict_whenUserHasActiveLoans() throws Exception {
        doThrow(new ConflictException("User 1 has active loans and cannot be deleted"))
                .when(userService).delete(1L);

        mockMvc.perform(delete("/users/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("User 1 has active loans and cannot be deleted"));

        verify(userService).delete(1L);
    }
}