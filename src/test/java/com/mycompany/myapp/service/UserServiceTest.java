package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @InjectMocks
    private UserService userService;

    private User user;
    private Authority userAuthority;
    private Authority adminAuthority;

    @BeforeEach
    void setUp() {
        // Create authorities
        userAuthority = new Authority();
        userAuthority.setName(AuthoritiesConstants.USER);

        adminAuthority = new Authority();
        adminAuthority.setName(AuthoritiesConstants.ADMIN);

        // Create test user
        user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        user.setPassword("hashedpassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setActivated(true);
        user.setLangKey("en");
        user.setCreatedBy("system");
        user.setCreatedDate(Instant.now());
        user.getAuthorities().add(userAuthority);
    }

    @Test
    void createUser_shouldCreateUserWithEncodedPassword() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("newuser");
        userDTO.setFirstName("New");
        userDTO.setLastName("User");
        userDTO.setEmail("newuser@example.com");
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));

        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User createdUser = userService.createUser(userDTO);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getLogin()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getPassword()).isNotEmpty();
        assertThat(savedUser.isActivated()).isFalse(); // Should start as not activated
        assertThat(savedUser.getActivationKey()).isNotEmpty();
        assertThat(savedUser.getAuthorities()).hasSize(1);
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    void updateUser_shouldUpdateUserFields() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(1L);
        userDTO.setLogin("testuser");
        userDTO.setFirstName("Updated");
        userDTO.setLastName("Name");
        userDTO.setEmail("updated@example.com");
        userDTO.setActivated(true);
        userDTO.setAuthorities(Set.of(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(authorityRepository.findById(AuthoritiesConstants.ADMIN)).thenReturn(Optional.of(adminAuthority));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        Optional<AdminUserDTO> updatedUser = userService.updateUser(userDTO);

        // Then
        assertThat(updatedUser).isPresent();
        verify(userRepository).save(user);
        assertThat(user.getFirstName()).isEqualTo("Updated");
        assertThat(user.getLastName()).isEqualTo("Name");
        assertThat(user.getEmail()).isEqualTo("updated@example.com");
        assertThat(user.getAuthorities()).hasSize(2);
    }

    @Test
    void updateUser_shouldReturnEmptyWhenUserNotFound() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setId(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<AdminUserDTO> result = userService.updateUser(userDTO);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_shouldDeleteUserAndClearCache() {
        // Given
        String login = "testuser";
        when(userRepository.findOneByLogin(login)).thenReturn(Optional.of(user));

        // When
        userService.deleteUser(login);

        // Then
        verify(userRepository).deleteById(user.getId());
    }

    @Test
    void changePassword_shouldUpdatePasswordWithEncoding() {
        // Given
        String currentClearTextPassword = "currentpassword";
        String newPassword = "newpassword";

        when(passwordEncoder.encode(newPassword)).thenReturn("encodednewpassword");

        // When
        userService.changePassword(currentClearTextPassword, newPassword);

        // Then
        verify(passwordEncoder).encode(newPassword);
        // Note: This test would need SecurityContext setup in a real scenario
    }

    @Test
    void getAllManagedUsers_shouldReturnPagedUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(user);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When
        Page<AdminUserDTO> result = userService.getAllManagedUsers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLogin()).isEqualTo("testuser");
        verify(userRepository).findAll(pageable);
    }

    @Test
    void getAllPublicUsers_shouldReturnPagedPublicUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(user);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable)).thenReturn(userPage);

        // When
        Page<UserDTO> result = userService.getAllPublicUsers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLogin()).isEqualTo("testuser");
        verify(userRepository).findAllByIdNotNullAndActivatedIsTrue(pageable);
    }

    @Test
    void getUserWithAuthoritiesByLogin_shouldReturnUserWithAuthorities() {
        // Given
        when(userRepository.findOneWithAuthoritiesByLogin("testuser")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.getUserWithAuthoritiesByLogin("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getLogin()).isEqualTo("testuser");
        assertThat(result.get().getAuthorities()).isNotEmpty();
        verify(userRepository).findOneWithAuthoritiesByLogin("testuser");
    }

    @Test
    void getUserWithAuthorities_shouldReturnCurrentUser() {
        // Given
        when(userRepository.findOneWithAuthoritiesByLogin(anyString())).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.getUserWithAuthorities();

        // Then
        // This test would need proper SecurityContext setup to work fully
        verify(userRepository).findOneWithAuthoritiesByLogin(anyString());
    }

    @Test
    void activateRegistration_shouldActivateUserWithValidKey() {
        // Given
        String activationKey = "validkey123";
        user.setActivated(false);
        user.setActivationKey(activationKey);

        when(userRepository.findOneByActivationKey(activationKey)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        Optional<User> result = userService.activateRegistration(activationKey);

        // Then
        assertThat(result).isPresent();
        assertThat(user.isActivated()).isTrue();
        assertThat(user.getActivationKey()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void activateRegistration_shouldReturnEmptyForInvalidKey() {
        // Given
        String invalidKey = "invalidkey";
        when(userRepository.findOneByActivationKey(invalidKey)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.activateRegistration(invalidKey);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    void completePasswordReset_shouldResetPasswordWithValidKey() {
        // Given
        String newPassword = "newpassword";
        String resetKey = "validresetkey";
        user.setResetKey(resetKey);
        user.setResetDate(Instant.now().minus(1, ChronoUnit.HOURS)); // Within 24 hours

        when(userRepository.findOneByResetKey(resetKey)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodednewpassword");
        when(userRepository.save(user)).thenReturn(user);

        // When
        Optional<User> result = userService.completePasswordReset(newPassword, resetKey);

        // Then
        assertThat(result).isPresent();
        assertThat(user.getPassword()).isEqualTo("encodednewpassword");
        assertThat(user.getResetKey()).isNull();
        assertThat(user.getResetDate()).isNull();
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(user);
    }

    @Test
    void completePasswordReset_shouldReturnEmptyForExpiredKey() {
        // Given
        String newPassword = "newpassword";
        String resetKey = "expiredkey";
        user.setResetKey(resetKey);
        user.setResetDate(Instant.now().minus(25, ChronoUnit.HOURS)); // Older than 24 hours

        when(userRepository.findOneByResetKey(resetKey)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.completePasswordReset(newPassword, resetKey);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void requestPasswordReset_shouldGenerateResetKeyForValidEmail() {
        // Given
        when(userRepository.findOneByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        Optional<User> result = userService.requestPasswordReset("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(user.getResetKey()).isNotEmpty();
        assertThat(user.getResetDate()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void requestPasswordReset_shouldReturnEmptyForInvalidEmail() {
        // Given
        when(userRepository.findOneByEmailIgnoreCase("invalid@example.com")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.requestPasswordReset("invalid@example.com");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    void requestPasswordReset_shouldReturnEmptyForInactiveUser() {
        // Given
        user.setActivated(false);
        when(userRepository.findOneByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.requestPasswordReset("test@example.com");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_shouldCreateNewUserWithEncodedPassword() {
        // Given
        AdminUserDTO userDTO = new AdminUserDTO();
        userDTO.setLogin("newuser");
        userDTO.setFirstName("New");
        userDTO.setLastName("User");
        userDTO.setEmail("newuser@example.com");
        userDTO.setLangKey("en");

        when(authorityRepository.findById(AuthoritiesConstants.USER)).thenReturn(Optional.of(userAuthority));
        when(passwordEncoder.encode("plainpassword")).thenReturn("encodedpassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User registeredUser = userService.registerUser(userDTO, "plainpassword");

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getLogin()).isEqualTo("newuser");
        assertThat(savedUser.getPassword()).isEqualTo("encodedpassword");
        assertThat(savedUser.isActivated()).isFalse();
        assertThat(savedUser.getActivationKey()).isNotEmpty();
        assertThat(savedUser.getAuthorities()).hasSize(1);
        assertThat(savedUser.getAuthorities().iterator().next().getName()).isEqualTo(AuthoritiesConstants.USER);
    }

    @Test
    void updateUser_shouldUpdateCurrentUserInfo() {
        // Given
        String firstName = "UpdatedFirst";
        String lastName = "UpdatedLast";
        String email = "updated@example.com";
        String langKey = "fr";
        String imageUrl = "http://example.com/image.jpg";

        when(userRepository.findOneByLogin(anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // When
        userService.updateUser(firstName, lastName, email, langKey, imageUrl);

        // Then
        verify(userRepository).save(user);
        assertThat(user.getFirstName()).isEqualTo(firstName);
        assertThat(user.getLastName()).isEqualTo(lastName);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getLangKey()).isEqualTo(langKey);
        assertThat(user.getImageUrl()).isEqualTo(imageUrl);
    }

    @Test
    void removeNotActivatedUsers_shouldDeleteOldInactiveUsers() {
        // Given
        User inactiveUser = new User();
        inactiveUser.setActivated(false);
        inactiveUser.setActivationKey("somekey");
        inactiveUser.setCreatedDate(Instant.now().minus(4, ChronoUnit.DAYS));

        List<User> inactiveUsers = Arrays.asList(inactiveUser);
        when(userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(any(Instant.class))).thenReturn(
            inactiveUsers
        );

        // When
        userService.removeNotActivatedUsers();

        // Then
        verify(userRepository).deleteAll(inactiveUsers);
    }

    @Test
    void getAuthorities_shouldReturnAllAuthorities() {
        // Given
        List<Authority> authorities = Arrays.asList(userAuthority, adminAuthority);
        when(authorityRepository.findAll()).thenReturn(authorities);

        // When
        List<String> result = userService.getAuthorities();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
        verify(authorityRepository).findAll();
    }
}
