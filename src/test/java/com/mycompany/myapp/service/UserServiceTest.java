package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.config.Constants;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.jhipster.security.RandomUtil;

/**
 * Unit tests for {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private org.springframework.cache.Cache loginCache;

    @Mock
    private org.springframework.cache.Cache emailCache;

    @InjectMocks
    private UserService userService;

    private User user;
    private AdminUserDTO adminUserDTO;
    private Authority userAuthority;

    @BeforeEach
    void setUp() {
        userAuthority = new Authority();
        userAuthority.setName(AuthoritiesConstants.USER);

        user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        user.setPassword("hashedpassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setActivated(true);
        user.setLangKey("en");
        user.setActivationKey("activation123");
        user.setResetKey("reset123");
        user.setResetDate(Instant.now());
        user.setAuthorities(new HashSet<>(Set.of(userAuthority))); // Use mutable HashSet

        adminUserDTO = new AdminUserDTO();
        adminUserDTO.setId(1L);
        adminUserDTO.setLogin("testuser");
        adminUserDTO.setFirstName("Test");
        adminUserDTO.setLastName("User");
        adminUserDTO.setEmail("test@example.com");
        adminUserDTO.setActivated(true);
        adminUserDTO.setLangKey("en");
        adminUserDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));
    }

    @Test
    void activateRegistration_WhenValidKey_ShouldActivateUser() {
        // Given
        String activationKey = "activation123";
        User inactiveUser = new User();
        inactiveUser.setActivated(false);
        inactiveUser.setActivationKey(activationKey);
        inactiveUser.setLogin("testuser"); // Add login for cache clearing
        inactiveUser.setEmail("test@example.com"); // Add email for cache clearing

        when(userRepository.findOneByActivationKey(activationKey)).thenReturn(Optional.of(inactiveUser));
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

        // When
        Optional<User> result = userService.activateRegistration(activationKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().isActivated()).isTrue();
        assertThat(result.orElseThrow().getActivationKey()).isNull();
        verify(userRepository).findOneByActivationKey(activationKey);
    }

    @Test
    void activateRegistration_WhenInvalidKey_ShouldReturnEmpty() {
        // Given
        String invalidKey = "invalid123";
        when(userRepository.findOneByActivationKey(invalidKey)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.activateRegistration(invalidKey);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findOneByActivationKey(invalidKey);
    }

    @Test
    void completePasswordReset_WhenValidKeyAndNotExpired_ShouldResetPassword() {
        // Given
        String resetKey = "reset123";
        String newPassword = "newpassword";
        String encodedPassword = "encodedpassword";

        User userWithResetKey = new User();
        userWithResetKey.setResetKey(resetKey);
        userWithResetKey.setResetDate(Instant.now().minus(12, ChronoUnit.HOURS)); // Not expired
        userWithResetKey.setLogin("testuser"); // Add login for cache clearing
        userWithResetKey.setEmail("test@example.com"); // Add email for cache clearing

        when(userRepository.findOneByResetKey(resetKey)).thenReturn(Optional.of(userWithResetKey));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

        // When
        Optional<User> result = userService.completePasswordReset(newPassword, resetKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getPassword()).isEqualTo(encodedPassword);
        assertThat(result.orElseThrow().getResetKey()).isNull();
        assertThat(result.orElseThrow().getResetDate()).isNull();
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    void completePasswordReset_WhenExpiredKey_ShouldReturnEmpty() {
        // Given
        String resetKey = "reset123";
        String newPassword = "newpassword";

        User userWithExpiredKey = new User();
        userWithExpiredKey.setResetKey(resetKey);
        userWithExpiredKey.setResetDate(Instant.now().minus(25, ChronoUnit.HOURS)); // Expired

        when(userRepository.findOneByResetKey(resetKey)).thenReturn(Optional.of(userWithExpiredKey));

        // When
        Optional<User> result = userService.completePasswordReset(newPassword, resetKey);

        // Then
        assertThat(result).isEmpty();
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void requestPasswordReset_WhenValidEmailAndActivatedUser_ShouldSetResetKey() {
        // Given
        String email = "test@example.com";
        User activatedUser = new User();
        activatedUser.setEmail(email);
        activatedUser.setActivated(true);

        when(userRepository.findOneByEmailIgnoreCase(email)).thenReturn(Optional.of(activatedUser));
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

        try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
            randomUtilMock.when(RandomUtil::generateResetKey).thenReturn("generatedResetKey");

            // When
            Optional<User> result = userService.requestPasswordReset(email);

            // Then
            assertThat(result).isPresent();
            assertThat(result.orElseThrow().getResetKey()).isEqualTo("generatedResetKey");
            assertThat(result.orElseThrow().getResetDate()).isNotNull();
        }
    }

    @Test
    void requestPasswordReset_WhenInactiveUser_ShouldReturnEmpty() {
        // Given
        String email = "test@example.com";
        User inactiveUser = new User();
        inactiveUser.setEmail(email);
        inactiveUser.setActivated(false);

        when(userRepository.findOneByEmailIgnoreCase(email)).thenReturn(Optional.of(inactiveUser));

        // When
        Optional<User> result = userService.requestPasswordReset(email);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void registerUser_WhenNewUser_ShouldCreateUser() {
        // Given
        String password = "password123";
        String encodedPassword = "encodedpassword";

        when(userRepository.findOneByLogin(adminUserDTO.getLogin().toLowerCase())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase(adminUserDTO.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

        try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
            randomUtilMock.when(RandomUtil::generateActivationKey).thenReturn("activationKey123");

            // When
            User result = userService.registerUser(adminUserDTO, password);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLogin()).isEqualTo(adminUserDTO.getLogin().toLowerCase());
            assertThat(result.getPassword()).isEqualTo(encodedPassword);
            assertThat(result.isActivated()).isFalse();
            assertThat(result.getActivationKey()).isEqualTo("activationKey123");
            assertThat(result.getAuthorities()).contains(userAuthority);
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void registerUser_WhenLoginExists_ShouldThrowUsernameAlreadyUsedException() {
        // Given
        User existingUser = new User();
        existingUser.setLogin(adminUserDTO.getLogin());
        existingUser.setActivated(true); // Active user cannot be removed

        when(userRepository.findOneByLogin(adminUserDTO.getLogin().toLowerCase())).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(adminUserDTO, "password")).isInstanceOf(UsernameAlreadyUsedException.class);
    }

    @Test
    void registerUser_WhenEmailExists_ShouldThrowEmailAlreadyUsedException() {
        // Given
        User existingUser = new User();
        existingUser.setEmail(adminUserDTO.getEmail());
        existingUser.setActivated(true); // Active user cannot be removed

        when(userRepository.findOneByLogin(adminUserDTO.getLogin().toLowerCase())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase(adminUserDTO.getEmail())).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(adminUserDTO, "password")).isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void registerUser_WhenInactiveUserExists_ShouldRemoveAndCreateNew() {
        // Given
        String password = "password123";
        String encodedPassword = "encodedpassword";

        User inactiveUser = new User();
        inactiveUser.setLogin(adminUserDTO.getLogin());
        inactiveUser.setActivated(false); // Inactive user can be removed

        when(userRepository.findOneByLogin(adminUserDTO.getLogin().toLowerCase())).thenReturn(Optional.of(inactiveUser));
        when(userRepository.findOneByEmailIgnoreCase(adminUserDTO.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

        try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
            randomUtilMock.when(RandomUtil::generateActivationKey).thenReturn("activationKey123");

            // When
            User result = userService.registerUser(adminUserDTO, password);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).delete(inactiveUser);
            verify(userRepository).flush();
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void createUser_ShouldCreateUserWithAllFields() {
        // Given
        String encodedPassword = "encodedpassword";

        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

        try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
            randomUtilMock.when(RandomUtil::generatePassword).thenReturn("generatedPassword");
            randomUtilMock.when(RandomUtil::generateResetKey).thenReturn("resetKey123");

            // When
            User result = userService.createUser(adminUserDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLogin()).isEqualTo(adminUserDTO.getLogin().toLowerCase());
            assertThat(result.getPassword()).isEqualTo(encodedPassword);
            assertThat(result.isActivated()).isTrue();
            assertThat(result.getResetKey()).isEqualTo("resetKey123");
            assertThat(result.getResetDate()).isNotNull();
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void createUser_WhenNoLangKey_ShouldUseDefaultLanguage() {
        // Given
        AdminUserDTO userDTOWithoutLang = new AdminUserDTO();
        userDTOWithoutLang.setLogin("testuser");
        userDTOWithoutLang.setEmail("test@example.com"); // Add email for cache clearing
        userDTOWithoutLang.setLangKey(null); // No language key

        when(passwordEncoder.encode(anyString())).thenReturn("encodedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

        try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
            randomUtilMock.when(RandomUtil::generatePassword).thenReturn("generatedPassword");
            randomUtilMock.when(RandomUtil::generateResetKey).thenReturn("resetKey123");

            // When
            User result = userService.createUser(userDTOWithoutLang);

            // Then
            assertThat(result.getLangKey()).isEqualTo(Constants.DEFAULT_LANGUAGE);
        }
    }

    @Test
    void updateUser_WhenUserExists_ShouldUpdateUser() {
        // Given
        when(userRepository.findById(adminUserDTO.getId())).thenReturn(Optional.of(user));
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

        // When
        Optional<AdminUserDTO> result = userService.updateUser(adminUserDTO);

        // Then
        assertThat(result).isPresent();
        assertThat(user.getLogin()).isEqualTo(adminUserDTO.getLogin().toLowerCase());
        assertThat(user.getFirstName()).isEqualTo(adminUserDTO.getFirstName());
        assertThat(user.getLastName()).isEqualTo(adminUserDTO.getLastName());
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_WhenUserNotFound_ShouldReturnEmpty() {
        // Given
        when(userRepository.findById(adminUserDTO.getId())).thenReturn(Optional.empty());

        // When
        Optional<AdminUserDTO> result = userService.updateUser(adminUserDTO);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_WhenUserExists_ShouldDeleteUser() {
        // Given
        String login = "testuser";
        when(userRepository.findOneByLogin(login)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
        when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

        // When
        userService.deleteUser(login);

        // Then
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_WhenUserNotFound_ShouldNotDelete() {
        // Given
        String login = "nonexistent";
        when(userRepository.findOneByLogin(login)).thenReturn(Optional.empty());

        // When
        userService.deleteUser(login);

        // Then
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void updateCurrentUser_WhenUserExists_ShouldUpdateBasicInfo() {
        // Given
        String firstName = "UpdatedFirst";
        String lastName = "UpdatedLast";
        String email = "updated@example.com";
        String langKey = "fr";
        String imageUrl = "http://example.com/image.jpg";

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
            when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

            // When
            userService.updateUser(firstName, lastName, email, langKey, imageUrl);

            // Then
            assertThat(user.getFirstName()).isEqualTo(firstName);
            assertThat(user.getLastName()).isEqualTo(lastName);
            assertThat(user.getEmail()).isEqualTo(email.toLowerCase());
            assertThat(user.getLangKey()).isEqualTo(langKey);
            assertThat(user.getImageUrl()).isEqualTo(imageUrl);
            verify(userRepository).save(user);
        }
    }

    @Test
    void changePassword_WhenCorrectCurrentPassword_ShouldChangePassword() {
        // Given
        String currentPassword = "currentpassword";
        String newPassword = "newpassword";
        String encodedNewPassword = "encodednewpassword";

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(currentPassword, user.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
            when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(loginCache);
            when(cacheManager.getCache(com.mycompany.myapp.repository.UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(emailCache);

            // When
            userService.changePassword(currentPassword, newPassword);

            // Then
            assertThat(user.getPassword()).isEqualTo(encodedNewPassword);
            verify(passwordEncoder).matches(currentPassword, "hashedpassword");
            verify(passwordEncoder).encode(newPassword);
        }
    }

    @Test
    void changePassword_WhenIncorrectCurrentPassword_ShouldThrowException() {
        // Given
        String currentPassword = "wrongpassword";
        String newPassword = "newpassword";

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(currentPassword, user.getPassword())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(currentPassword, newPassword)).isInstanceOf(InvalidPasswordException.class);

            verify(passwordEncoder, never()).encode(newPassword);
        }
    }

    @Test
    void getAllManagedUsers_ShouldReturnPageOfAdminUserDTO() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(user, new User());
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When
        Page<AdminUserDTO> result = userService.getAllManagedUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0)).isInstanceOf(AdminUserDTO.class);
        verify(userRepository).findAll(pageable);
    }

    @Test
    void getAllPublicUsers_ShouldReturnPageOfUserDTO() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(user, new User());
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable)).thenReturn(userPage);

        // When
        Page<UserDTO> result = userService.getAllPublicUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0)).isInstanceOf(UserDTO.class);
        verify(userRepository).findAllByIdNotNullAndActivatedIsTrue(pageable);
    }

    @Test
    void getUserWithAuthoritiesByLogin_WhenUserExists_ShouldReturnUser() {
        // Given
        String login = "testuser";
        when(userRepository.findOneWithAuthoritiesByLogin(login)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.getUserWithAuthoritiesByLogin(login);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).isEqualTo(user);
        verify(userRepository).findOneWithAuthoritiesByLogin(login);
    }

    @Test
    void getUserWithAuthorities_WhenCurrentUserExists_ShouldReturnUser() {
        // Given
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneWithAuthoritiesByLogin("testuser")).thenReturn(Optional.of(user));

            // When
            Optional<User> result = userService.getUserWithAuthorities();

            // Then
            assertThat(result).isPresent();
            assertThat(result.orElseThrow()).isEqualTo(user);
            verify(userRepository).findOneWithAuthoritiesByLogin("testuser");
        }
    }

    @Test
    void getAuthorities_ShouldReturnListOfAuthorityNames() {
        // Given
        List<String> authorities = Arrays.asList(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
        when(authorityRepository.findAll()).thenReturn(
            authorities
                .stream()
                .map(name -> {
                    Authority auth = new Authority();
                    auth.setName(name);
                    return auth;
                })
                .toList()
        );

        // When
        List<String> result = userService.getAuthorities();

        // Then
        assertThat(result).containsExactlyInAnyOrder(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
        verify(authorityRepository).findAll();
    }
}
