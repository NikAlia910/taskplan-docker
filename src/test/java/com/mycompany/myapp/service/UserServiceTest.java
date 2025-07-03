package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.config.Constants;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    private static final String DEFAULT_LOGIN = "testuser";
    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_FIRSTNAME = "Test";
    private static final String DEFAULT_LASTNAME = "User";
    private static final String DEFAULT_LANGKEY = "en";
    private static final String DEFAULT_PASSWORD = "password123";
    private static final String ENCRYPTED_PASSWORD = "$2a$10$encrypted";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private UserService userService;

    @BeforeEach
    void setUp() {
        // Mock the cache manager to return a cache instance
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        doNothing().when(cache).evict(any());

        userService = new UserService(userRepository, passwordEncoder, authorityRepository, cacheManager);
    }

    @Test
    void testRegisterUser() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setFirstName(DEFAULT_FIRSTNAME);
        userDTO.setLastName(DEFAULT_LASTNAME);
        userDTO.setLangKey(DEFAULT_LANGKEY);

        User user = createUser();
        user.setActivated(false);
        user.setActivationKey("activationKey");

        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);

        when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn(ENCRYPTED_PASSWORD);
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(authority));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User result = userService.registerUser(userDTO, DEFAULT_PASSWORD);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogin()).isEqualTo(DEFAULT_LOGIN.toLowerCase());
        assertThat(result.getEmail()).isEqualTo(DEFAULT_EMAIL.toLowerCase());
        assertThat(result.isActivated()).isFalse();
        assertThat(result.getActivationKey()).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUserWithExistingLogin() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setEmail(DEFAULT_EMAIL);

        User existingUser = createUser();
        existingUser.setActivated(true);

        when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(existingUser));

        // When/Then
        assertThatExceptionOfType(UsernameAlreadyUsedException.class).isThrownBy(() -> userService.registerUser(userDTO, DEFAULT_PASSWORD));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUserWithExistingEmail() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setEmail(DEFAULT_EMAIL);

        User existingUser = createUser();
        existingUser.setActivated(true);

        when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.of(existingUser));

        // When/Then
        assertThatExceptionOfType(EmailAlreadyUsedException.class).isThrownBy(() -> userService.registerUser(userDTO, DEFAULT_PASSWORD));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testActivateRegistration() {
        // Given
        User user = createUser();
        user.setActivated(false);
        user.setActivationKey("activationKey");

        when(userRepository.findOneByActivationKey("activationKey")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        Optional<User> result = userService.activateRegistration("activationKey");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().isActivated()).isTrue();
        assertThat(result.get().getActivationKey()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void testActivateRegistrationWithInvalidKey() {
        // Given
        when(userRepository.findOneByActivationKey("invalidKey")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.activateRegistration("invalidKey");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCompletePasswordReset() {
        // Given
        User user = createUser();
        user.setResetKey("resetKey");
        user.setResetDate(Instant.now().minus(2, ChronoUnit.HOURS));

        when(userRepository.findOneByResetKey("resetKey")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn(ENCRYPTED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        Optional<User> result = userService.completePasswordReset("newPassword", "resetKey");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPassword()).isEqualTo(ENCRYPTED_PASSWORD);
        assertThat(result.get().getResetKey()).isNull();
        assertThat(result.get().getResetDate()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void testRequestPasswordReset() {
        // Given
        User user = createUser();
        user.setActivated(true);

        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        Optional<User> result = userService.requestPasswordReset(DEFAULT_EMAIL);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getResetKey()).isNotNull();
        assertThat(result.get().getResetDate()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void testCreateUser() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setFirstName(DEFAULT_FIRSTNAME);
        userDTO.setLastName(DEFAULT_LASTNAME);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setLangKey(DEFAULT_LANGKEY);
        userDTO.setActivated(true);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);

        when(passwordEncoder.encode(anyString())).thenReturn(ENCRYPTED_PASSWORD);
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(authority));
        when(userRepository.save(any(User.class))).thenReturn(createUser());

        // When
        User result = userService.createUser(userDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogin()).isEqualTo(DEFAULT_LOGIN.toLowerCase());
        assertThat(result.getEmail()).isEqualTo(DEFAULT_EMAIL.toLowerCase());
        assertThat(result.isActivated()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUser() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(1L);
        userDTO.setLogin(DEFAULT_LOGIN);
        userDTO.setFirstName(DEFAULT_FIRSTNAME);
        userDTO.setLastName(DEFAULT_LASTNAME);
        userDTO.setEmail(DEFAULT_EMAIL);
        userDTO.setActivated(true);
        userDTO.setLangKey(DEFAULT_LANGKEY);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        User existingUser = createUser();
        existingUser.setId(1L);

        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(authority));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        Optional<AdminUserDTO> result = userService.updateUser(userDTO);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getLogin()).isEqualTo(DEFAULT_LOGIN.toLowerCase());
        assertThat(result.get().isActivated()).isTrue();
        verify(userRepository).save(existingUser);
    }

    @Test
    void testDeleteUser() {
        // Given
        User user = createUser();
        when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(user));

        // When
        userService.deleteUser(DEFAULT_LOGIN);

        // Then
        verify(userRepository).delete(user);
    }

    @Test
    void testUpdateUserInfo() {
        // Given
        User user = createUser();

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            userService.updateUser("NewFirst", "NewLast", "new@example.com", "es", "http://image.url");

            // Then
            verify(userRepository).save(user);
            assertThat(user.getFirstName()).isEqualTo("NewFirst");
            assertThat(user.getLastName()).isEqualTo("NewLast");
            assertThat(user.getEmail()).isEqualTo("new@example.com");
            assertThat(user.getLangKey()).isEqualTo("es");
            assertThat(user.getImageUrl()).isEqualTo("http://image.url");
        }
    }

    @Test
    void testChangePassword() {
        // Given
        User user = createUser();
        user.setPassword(ENCRYPTED_PASSWORD);
        String newPassword = "newPassword123";

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(DEFAULT_PASSWORD, ENCRYPTED_PASSWORD)).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$newEncrypted");

            // When
            userService.changePassword(DEFAULT_PASSWORD, newPassword);

            // Then
            assertThat(user.getPassword()).isEqualTo("$2a$10$newEncrypted");
        }
    }

    @Test
    void testChangePasswordWithWrongCurrentPassword() {
        // Given
        User user = createUser();
        user.setPassword(ENCRYPTED_PASSWORD);

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPassword", ENCRYPTED_PASSWORD)).thenReturn(false);

            // When/Then
            assertThatExceptionOfType(InvalidPasswordException.class).isThrownBy(() ->
                userService.changePassword("wrongPassword", "newPassword")
            );
        }
    }

    @Test
    void testGetAllManagedUsers() {
        // Given
        User user1 = createUser();
        user1.setId(1L);
        User user2 = createUser();
        user2.setId(2L);
        user2.setLogin("user2");

        Page<User> userPage = new PageImpl<>(List.of(user1, user2));
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When
        Page<AdminUserDTO> result = userService.getAllManagedUsers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(AdminUserDTO::getLogin).containsExactly(DEFAULT_LOGIN, "user2");
        verify(userRepository).findAll(pageable);
    }

    @Test
    void testGetUserWithAuthorities() {
        // Given
        User user = createUser();

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userRepository.findOneWithAuthoritiesByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(user));

            // When
            Optional<User> result = userService.getUserWithAuthorities();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(user);
            verify(userRepository).findOneWithAuthoritiesByLogin(DEFAULT_LOGIN);
        }
    }

    @Test
    void testGetUserWithAuthoritiesByLogin() {
        // Given
        User user = createUser();
        when(userRepository.findOneWithAuthoritiesByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.getUserWithAuthoritiesByLogin(DEFAULT_LOGIN);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        verify(userRepository).findOneWithAuthoritiesByLogin(DEFAULT_LOGIN);
    }

    @Test
    void testGetAuthorities() {
        // Given
        Authority userAuth = new Authority();
        userAuth.setName(AuthoritiesConstants.USER);
        Authority adminAuth = new Authority();
        adminAuth.setName(AuthoritiesConstants.ADMIN);

        when(authorityRepository.findAll()).thenReturn(List.of(userAuth, adminAuth));

        // When
        List<String> result = userService.getAuthorities();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
        verify(authorityRepository).findAll();
    }

    private User createUser() {
        User user = new User();
        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(ENCRYPTED_PASSWORD);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setEmail(DEFAULT_EMAIL);
        user.setActivated(true);
        user.setLangKey(DEFAULT_LANGKEY);
        return user;
    }
}
