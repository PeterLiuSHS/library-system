package com.kexun.user.integration;

import com.kexun.user.model.User;
import com.kexun.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestBody;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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
                .andExpect(status().isOk());

        assertEquals(1, userRepository.count());

        User savedUser = userRepository.findAll().get(0);
        assertEquals("Alice", savedUser.getName());
        assertEquals("alice@example.com", savedUser.getEmail());
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
    void delete_shouldRemoveUserFromDatabase() throws Exception {
        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@gmail.com");

        User saved = userRepository.save(user);
        Long id = saved.getId();

        mockMvc.perform(delete("/users/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertTrue(userRepository.findById(id).isEmpty());
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
}
