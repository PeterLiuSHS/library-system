package com.kexun.user.integration;

import com.kexun.user.client.LoanClient;
import com.kexun.user.exception.DownstreamServiceException;
import com.kexun.user.model.User;
import com.kexun.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestBody;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
public class UserApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private LoanClient loanClient;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void create_shouldPersistUserIntoDatabase() throws Exception {
        String requestBody = """
                {
                  "name": "Alice",
                  "email": "alice@example.com"
                }
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        assertEquals(1, userRepository.count());

        User savedUser = userRepository.findAll().get(0);
        assertEquals("Alice", savedUser.getName());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
        assertFalse(savedUser.isDeleted());
    }

    @Test
    void getById_shouldReturnUser_whenUserExists() throws Exception {
        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@gmail.com");

        User saved = userRepository.save(user);

        mockMvc.perform(get("/users/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@gmail.com"));
    }

    @Test
    void getById_shouldReturn404_whenUserNotFound() throws Exception {

        mockMvc.perform(get("/users/{id}", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User 1 not found"));

    }

    @Test
    void getById_shouldReturn404_afterUserIsSoftDeleted() throws Exception {
        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@gmail.com");
        User savedUser = userRepository.save(user);

        when(loanClient.hasActiveLoans(savedUser.getId())).thenReturn(false);

        mockMvc.perform(delete("/users/{id}", savedUser.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", savedUser.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User "+savedUser.getId() + " not found"));

        assertTrue(userRepository.findById(savedUser.getId()).isPresent());
    }

    @Test
    void delete_shouldSoftDeleteUser_whenUserExistsAndHasNoActiveLoans() throws Exception {
        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@gmail.com");

        User saved = userRepository.save(user);
        Long id = saved.getId();

        when(loanClient.hasActiveLoans(id)).thenReturn(false);

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());

        User deletedUser = userRepository.findById(id).orElseThrow();

        assertTrue(deletedUser.isDeleted());
    }

    @Test
    void delete_shouldReturn404_whenUserNotExists() throws Exception {

        mockMvc.perform(delete("/users/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User 1 not found"));
    }

    @Test
    void create_shouldReturn400_whenNameIsInvalid() throws Exception {
        String requestBody = """
                {
                "name": "",
                "email": "alice@gmail.com"
                }
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("must not be blank"));
    }

    @Test
    void create_shouldReturn400_whenEmailIsInvalid() throws Exception {
        String requestBody = """
                {
                "name": "alice",
                "email": "alice@.com"
                }
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("must be a well-formed email address"));
    }

    @Test
    void create_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        User user1 = new User();
        user1.setName("Alice");
        user1.setEmail("alice@gmail.com");
        User savedUser1 = userRepository.save(user1);

        String requestBody = """
                {
                "name": "Alice Wong",
                "email": "alice@gmail.com"
                }
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        assertEquals(1, userRepository.count());
    }

    @Test
    void list_shouldReturnPagedUsers_whenSearchIsNull() throws Exception {
        User user1 = new User();
        user1.setName("Alice");
        user1.setEmail("alice@gmail.com");
        User savedUser1 = userRepository.save(user1);

        User user2 = new User();
        user2.setName("Bob");
        user2.setEmail("bob@foxmail.com");
        User savedUser2 = userRepository.save(user2);

        User user3 = new User();
        user3.setName("Jack");
        user3.setEmail("jack@example.com");
        User savedUser3 = userRepository.save(user3);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Alice"))
                .andExpect(jsonPath("$.content[1].name").value("Bob"))
                .andExpect(jsonPath("$.content[2].name").value("Jack"))
                .andExpect(jsonPath("$.pageable.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.numberOfElements").value(3));
    }

    @Test
    void list_shouldReturnMatchedUsers_whenSearchKeywordProvided() throws Exception {
        User user1 = new User();
        user1.setName("Alice");
        user1.setEmail("alice@example.com");
        userRepository.save(user1);

        User user2 = new User();
        user2.setName("Bob");
        user2.setEmail("bob@example.com");
        userRepository.save(user2);

        mockMvc.perform(get("/users")
                        .param("search", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Alice"))
                .andExpect(jsonPath("$.content[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Bob"))));
    }

    @Test
    void list_shouldExcludeSoftDeletedUsers() throws Exception {
        User activeUser = new User();
        activeUser.setName("Alice");
        activeUser.setEmail("alice@example.com");
        userRepository.save(activeUser);

        User deletedUser = new User();
        deletedUser.setName("Bob");
        deletedUser.setEmail("bob@example.com");
        deletedUser.setDeleted(true);
        userRepository.save(deletedUser);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alice"))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Bob"))));
    }

    @Test
    void list_shouldReturn400_whenPageIsNegative()
            throws Exception {

        mockMvc.perform(get("/users")
                        .param("page", "-1")
                        .param("size", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"));

        assertEquals(0, userRepository.count());
    }

    @Test
    void list_shouldReturn400_whenSizeIsZero()
            throws Exception {

        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"));

        assertEquals(0, userRepository.count());
    }

    @Test
    void list_shouldReturn400_whenSizeExceedsMaximum()
            throws Exception {

        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"));

        assertEquals(0, userRepository.count());
    }

    @Test
    void update_shouldModifyUserInDatabase_whenUserExists() throws Exception {
        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@example.com");
        User savedUser = userRepository.save(user);

        String requestBody = """
                {
                "name": "Alice Wong"
                }
                """;

        mockMvc.perform(put("/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Wong"));

        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();

        assertEquals("Alice Wong", updatedUser.getName());
        assertEquals("alice@example.com", updatedUser.getEmail());
    }

    @Test
    void update_shouldReturn404_whenUserNotFound() throws Exception {
        String requestBody = """
                {
                "name": "Alice Wong"
                }
                """;
        mockMvc.perform(put("/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());

    }

    @Test
    void update_shouldReturn404_whenUserIsSoftDeleted() throws Exception {
        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setDeleted(true);
        User savedUser = userRepository.save(user);

        mockMvc.perform(put("/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Alice Wong"
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User " + savedUser.getId() + " not found"));
    }

    @Test
    void update_shouldReturn400_whenRequestBodyIsInvalid() throws Exception {
        String requestBody = """
                {
                "name": ""
                }
                """;
        mockMvc.perform(put("/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_shouldReturn409_whenUserHasActiveLoans() throws Exception {
        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@gmail.com");

        User saved = userRepository.save(user);
        Long id = saved.getId();

        when(loanClient.hasActiveLoans(id)).thenReturn(true);

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User "+id+" has active loans and cannot be deleted"));

        User stillUser = userRepository.findById(id).orElseThrow();

        assertFalse(stillUser.isDeleted());
    }

    @Test
    void delete_shouldReturn503_whenLoanServiceIsUnavailable()
            throws Exception {

        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@gmail.com");

        User savedUser = userRepository.save(user);

        when(loanClient.hasActiveLoans(savedUser.getId()))
                .thenThrow(
                        new DownstreamServiceException(
                                "Loan service is unavailable"
                        )
                );

        mockMvc.perform(delete("/users/{id}", savedUser.getId()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message")
                        .value("Loan service is unavailable"));

        User unchangedUser = userRepository
                .findById(savedUser.getId())
                .orElseThrow();

        assertFalse(unchangedUser.isDeleted());
    }
}
