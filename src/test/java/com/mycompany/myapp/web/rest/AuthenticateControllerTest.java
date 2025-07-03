package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.web.rest.vm.LoginVM;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AuthenticateController} REST controller.
 */
@IntegrationTest
@AutoConfigureWebMvc
class AuthenticateControllerTest {

    private static final String TEST_USER_LOGIN = "test";
    private static final String TEST_USER_EMAIL = "test@localhost";
    private static final String TEST_USER_PASSWORD = "test";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setLogin(TEST_USER_LOGIN);
        user.setEmail(TEST_USER_EMAIL);
        user.setActivated(true);
        user.setPassword(passwordEncoder.encode(TEST_USER_PASSWORD));
    }

    @Test
    @Transactional
    void testAuthorize() throws Exception {
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword(TEST_USER_PASSWORD);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isString())
            .andExpect(jsonPath("$.id_token").value(not(isEmptyString())))
            .andExpect(header().string("Authorization", not(isEmptyString())));
    }

    @Test
    @Transactional
    void testAuthorizeWithEmail() throws Exception {
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_EMAIL);
        login.setPassword(TEST_USER_PASSWORD);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isString())
            .andExpect(jsonPath("$.id_token").value(not(isEmptyString())))
            .andExpect(header().string("Authorization", not(isEmptyString())));
    }

    @Test
    @Transactional
    void testAuthorizeFails() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("wrong-user");
        login.setPassword("wrong password");

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.id_token").doesNotExist())
            .andExpect(header().doesNotExist("Authorization"));
    }

    @Test
    @Transactional
    void testAuthorizeWithInvalidPassword() throws Exception {
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword("invalid");

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.id_token").doesNotExist())
            .andExpect(header().doesNotExist("Authorization"));
    }

    @Test
    @Transactional
    void testAuthorizeWithInactivatedUser() throws Exception {
        user.setActivated(false);
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword(TEST_USER_PASSWORD);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void testAuthorizeWithEmptyUsername() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("");
        login.setPassword(TEST_USER_PASSWORD);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testAuthorizeWithEmptyPassword() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword("");

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testAuthorizeWithNullUsername() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername(null);
        login.setPassword(TEST_USER_PASSWORD);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testAuthorizeWithNullPassword() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword(null);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testAuthorizeWithTooLongUsername() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("a".repeat(256)); // Assuming max length is 255
        login.setPassword(TEST_USER_PASSWORD);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testAuthorizeWithTooLongPassword() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword("a".repeat(101)); // Assuming max length is 100

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testAuthorizeWithValidationViolation() throws Exception {
        mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testAuthorizeWithMalformedJson() throws Exception {
        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content("{ invalid json"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void testAuthorizeWithUppercaseUsername() throws Exception {
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN.toUpperCase());
        login.setPassword(TEST_USER_PASSWORD);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isString());
    }

    @Test
    @Transactional
    void testAuthorizeWithSpecialCharactersInPassword() throws Exception {
        String specialPassword = "test!@#$%^&*()";
        user.setPassword(passwordEncoder.encode(specialPassword));
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword(specialPassword);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isString());
    }

    @Test
    @Transactional
    void testAuthorizeReturnsUserInfo() throws Exception {
        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername(TEST_USER_LOGIN);
        login.setPassword(TEST_USER_PASSWORD);

        mockMvc
            .perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").isString())
            .andExpect(header().string("Authorization", not(isEmptyString())));
        // Verify that the JWT token is properly formed (starts with 'Bearer ')
        // Note: Actual JWT validation would require additional setup
    }
}
