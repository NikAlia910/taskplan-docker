package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private org.springframework.cache.Cache usersByLoginCache;

    @Mock
    private org.springframework.cache.Cache usersByEmailCache;

    @InjectMocks
    private UserService userService;

    private User user;
    private Authority authority;

    @BeforeEach
    void setUp() {
        // Use lenient mocking for cache behavior to avoid unnecessary stubbing warnings
        lenient().when(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).thenReturn(usersByLoginCache);
        lenient().when(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).thenReturn(usersByEmailCache);

        user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActivated(false);
        user.setActivationKey("activation-key");

        authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
    }

    @Test
    void activateRegistration_ShouldActivateUser_WhenValidKey() {
        // Given
        when(userRepository.findOneByActivationKey("activation-key")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.activateRegistration("activation-key");

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().isActivated()).isTrue();
        assertThat(result.orElseThrow().getActivationKey()).isNull();
    }

    @Test
    void activateRegistration_ShouldReturnEmpty_WhenInvalidKey() {
        // Given
        when(userRepository.findOneByActivationKey("invalid-key")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.activateRegistration("invalid-key");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void completePasswordReset_ShouldResetPassword_WhenValidKey() {
        // Given
        user.setResetKey("reset-key");
        user.setResetDate(Instant.now());

        when(userRepository.findOneByResetKey("reset-key")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");

        // When
        Optional<User> result = userService.completePasswordReset("newPassword", "reset-key");

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getPassword()).isEqualTo("encodedPassword");
        assertThat(result.orElseThrow().getResetKey()).isNull();
        assertThat(result.orElseThrow().getResetDate()).isNull();
    }

    @Test
    void completePasswordReset_ShouldReturnEmpty_WhenExpiredKey() {
        // Given
        user.setResetKey("reset-key");
        user.setResetDate(Instant.now().minus(2, ChronoUnit.DAYS)); // Expired

        when(userRepository.findOneByResetKey("reset-key")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.completePasswordReset("newPassword", "reset-key");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void requestPasswordReset_ShouldSetResetKey_WhenValidEmail() {
        // Given
        user.setActivated(true);
        when(userRepository.findOneByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.requestPasswordReset("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getResetKey()).isNotNull();
        assertThat(result.orElseThrow().getResetDate()).isNotNull();
    }

    @Test
    void requestPasswordReset_ShouldReturnEmpty_WhenUserNotActivated() {
        // Given
        user.setActivated(false);
        when(userRepository.findOneByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.requestPasswordReset("test@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void registerUser_ShouldCreateUser_WhenValidData() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("newuser");
        userDTO.setEmail("new@example.com");
        userDTO.setFirstName("New");
        userDTO.setLastName("User");

        when(userRepository.findOneByLogin("newuser")).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(authority));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.registerUser(userDTO, "password");

        // Then
        assertThat(result.getLogin()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.isActivated()).isFalse();
        assertThat(result.getActivationKey()).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ShouldCreateActiveUser() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("adminuser");
        userDTO.setEmail("admin@example.com");
        userDTO.setFirstName("Admin");
        userDTO.setLastName("User");
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(authority));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.createUser(userDTO);

        // Then
        assertThat(result.getLogin()).isEqualTo("adminuser");
        assertThat(result.isActivated()).isTrue();
        assertThat(result.getResetKey()).isNotNull();
        assertThat(result.getAuthorities()).hasSize(1);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_ShouldUpdateExistingUser() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(1L);
        userDTO.setLogin("updateduser");
        userDTO.setEmail("updated@example.com");
        userDTO.setFirstName("Updated");
        userDTO.setLastName("User");
        userDTO.setActivated(true);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(authority));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<AdminUserDTO> result = userService.updateUser(userDTO);

        // Then
        assertThat(result).isPresent();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_ShouldDeleteUser() {
        // Given
        when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));

        // When
        userService.deleteUser("testuser");

        // Then
        verify(userRepository).delete(user);
    }
}
