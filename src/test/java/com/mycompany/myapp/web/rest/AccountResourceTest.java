package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.MailService;
import com.mycompany.myapp.service.UserService;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.PasswordChangeDTO;
import com.mycompany.myapp.web.rest.vm.KeyAndPasswordVM;
import com.mycompany.myapp.web.rest.vm.ManagedUserVM;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AccountResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
class AccountResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private MailService mailService;

    @Autowired
    private UserService userService;

    private User user;
    private Authority userAuthority;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        authorityRepository.deleteAll();

        userAuthority = new Authority();
        userAuthority.setName(AuthoritiesConstants.USER);
        authorityRepository.saveAndFlush(userAuthority);

        user = new User();
        user.setLogin("testuser");
        user.setPassword(passwordEncoder.encode("oldpassword"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setActivated(true);
        user.setLangKey("en");
        user.setCreatedBy("system");
        user.setCreatedDate(Instant.now());
        Set<Authority> authorities = new HashSet<>();
        authorities.add(userAuthority);
        user.setAuthorities(authorities);
        userRepository.saveAndFlush(user);
    }

    @Test
    @WithMockUser("testuser")
    void getAccount_shouldReturnCurrentUser() throws Exception {
        mockMvc
            .perform(get("/api/account").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.login").value("testuser"))
            .andExpect(jsonPath("$.firstName").value("Test"))
            .andExpect(jsonPath("$.lastName").value("User"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.langKey").value("en"))
            .andExpect(jsonPath("$.authorities").isArray());
    }

    @Test
    void getAccount_whenNotAuthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/account").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser("testuser")
    void saveAccount_shouldUpdateUserDetails() throws Exception {
        AdminUserDTO updatedUser = new AdminUserDTO();
        updatedUser.setLogin("testuser");
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("Name");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setLangKey("fr");

        mockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(updatedUser)))
            .andExpect(status().isOk());

        User updatedUserEntity = userRepository.findOneByLogin("testuser").orElseThrow();
        assertThat(updatedUserEntity.getFirstName()).isEqualTo("Updated");
        assertThat(updatedUserEntity.getLastName()).isEqualTo("Name");
        assertThat(updatedUserEntity.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUserEntity.getLangKey()).isEqualTo("fr");
    }

    @Test
    @WithMockUser("testuser")
    void saveAccount_withInvalidEmail_shouldReturnBadRequest() throws Exception {
        AdminUserDTO invalidUser = new AdminUserDTO();
        invalidUser.setLogin("testuser");
        invalidUser.setFirstName("Test");
        invalidUser.setLastName("User");
        invalidUser.setEmail("invalid-email");
        invalidUser.setLangKey("en");

        mockMvc
            .perform(post("/api/account").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(invalidUser)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser("testuser")
    void changePassword_shouldUpdatePassword() throws Exception {
        PasswordChangeDTO passwordChangeDto = new PasswordChangeDTO();
        passwordChangeDto.setCurrentPassword("oldpassword");
        passwordChangeDto.setNewPassword("newpassword");

        mockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(passwordChangeDto))
            )
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin("testuser").orElseThrow();
        assertThat(passwordEncoder.matches("newpassword", updatedUser.getPassword())).isTrue();
    }

    @Test
    @WithMockUser("testuser")
    void changePassword_withInvalidCurrentPassword_shouldReturnBadRequest() throws Exception {
        PasswordChangeDTO passwordChangeDto = new PasswordChangeDTO();
        passwordChangeDto.setCurrentPassword("wrongpassword");
        passwordChangeDto.setNewPassword("newpassword");

        mockMvc
            .perform(
                post("/api/account/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(passwordChangeDto))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void registerAccount_shouldCreateNewUser() throws Exception {
        ManagedUserVM validUser = new ManagedUserVM();
        validUser.setLogin("newuser");
        validUser.setPassword("password");
        validUser.setFirstName("New");
        validUser.setLastName("User");
        validUser.setEmail("newuser@example.com");
        validUser.setActivated(false);
        validUser.setLangKey("en");

        mockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<User> newUser = userRepository.findOneByLogin("newuser");
        assertThat(newUser).isPresent();
        assertThat(newUser.get().getEmail()).isEqualTo("newuser@example.com");
        assertThat(newUser.get().isActivated()).isFalse();
        verify(mailService).sendActivationEmail(any(User.class));
    }

    @Test
    void registerAccount_withExistingLogin_shouldReturnBadRequest() throws Exception {
        ManagedUserVM duplicateUser = new ManagedUserVM();
        duplicateUser.setLogin("testuser"); // Already exists
        duplicateUser.setPassword("password");
        duplicateUser.setFirstName("Duplicate");
        duplicateUser.setLastName("User");
        duplicateUser.setEmail("duplicate@example.com");
        duplicateUser.setLangKey("en");

        mockMvc
            .perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(duplicateUser)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerAccount_withExistingEmail_shouldReturnBadRequest() throws Exception {
        ManagedUserVM duplicateEmailUser = new ManagedUserVM();
        duplicateEmailUser.setLogin("newuser");
        duplicateEmailUser.setPassword("password");
        duplicateEmailUser.setFirstName("New");
        duplicateEmailUser.setLastName("User");
        duplicateEmailUser.setEmail("test@example.com"); // Already exists
        duplicateEmailUser.setLangKey("en");

        mockMvc
            .perform(
                post("/api/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(duplicateEmailUser))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void activateAccount_shouldActivateUser() throws Exception {
        User inactiveUser = new User();
        inactiveUser.setLogin("inactive");
        inactiveUser.setPassword(passwordEncoder.encode("password"));
        inactiveUser.setFirstName("Inactive");
        inactiveUser.setLastName("User");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setActivated(false);
        inactiveUser.setActivationKey("activationkey");
        inactiveUser.setLangKey("en");
        inactiveUser.setCreatedBy("system");
        inactiveUser.setCreatedDate(Instant.now());
        Set<Authority> authorities = new HashSet<>();
        authorities.add(userAuthority);
        inactiveUser.setAuthorities(authorities);
        userRepository.saveAndFlush(inactiveUser);

        mockMvc.perform(get("/api/activate").param("key", "activationkey")).andExpect(status().isOk());

        User activatedUser = userRepository.findOneByLogin("inactive").orElseThrow();
        assertThat(activatedUser.isActivated()).isTrue();
        assertThat(activatedUser.getActivationKey()).isNull();
    }

    @Test
    void activateAccount_withInvalidKey_shouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/api/activate").param("key", "invalidkey")).andExpect(status().isInternalServerError());
    }

    @Test
    void requestPasswordReset_shouldSendResetEmail() throws Exception {
        mockMvc
            .perform(post("/api/account/reset-password/init").contentType(MediaType.TEXT_PLAIN).content("test@example.com"))
            .andExpect(status().isOk());

        verify(mailService).sendPasswordResetMail(any(User.class));
    }

    @Test
    void requestPasswordReset_withInvalidEmail_shouldStillReturnOk() throws Exception {
        mockMvc
            .perform(post("/api/account/reset-password/init").contentType(MediaType.TEXT_PLAIN).content("nonexistent@example.com"))
            .andExpect(status().isOk());
    }

    @Test
    void finishPasswordReset_shouldResetPassword() throws Exception {
        user.setResetKey("resetkey");
        user.setResetDate(Instant.now());
        userRepository.saveAndFlush(user);

        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("resetkey");
        keyAndPassword.setNewPassword("newpassword");

        mockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(keyAndPassword))
            )
            .andExpect(status().isOk());

        User updatedUser = userRepository.findOneByLogin("testuser").orElseThrow();
        assertThat(passwordEncoder.matches("newpassword", updatedUser.getPassword())).isTrue();
        assertThat(updatedUser.getResetKey()).isNull();
        assertThat(updatedUser.getResetDate()).isNull();
    }

    @Test
    void finishPasswordReset_withInvalidKey_shouldReturnInternalServerError() throws Exception {
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("invalidkey");
        keyAndPassword.setNewPassword("newpassword");

        mockMvc
            .perform(
                post("/api/account/reset-password/finish")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(keyAndPassword))
            )
            .andExpect(status().isInternalServerError());
    }
}
