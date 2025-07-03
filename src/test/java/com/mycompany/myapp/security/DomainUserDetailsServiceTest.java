package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class DomainUserDetailsServiceTest {

    private static final String USER_LOGIN = "testuser";
    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_PASSWORD = "password";
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";
    private static final String USER_AUTHORITY = "ROLE_USER";

    @Mock
    private UserRepository userRepository;

    private DomainUserDetailsService domainUserDetailsService;

    @BeforeEach
    void setUp() {
        domainUserDetailsService = new DomainUserDetailsService(userRepository);
    }

    @Test
    void testLoadUserByUsernameWithValidLogin() {
        // Given
        User user = createTestUser(USER_LOGIN, USER_EMAIL, true);
        when(userRepository.findOneWithAuthoritiesByLogin(USER_LOGIN.toLowerCase())).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_LOGIN);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_LOGIN);
        assertThat(userDetails.getPassword()).isEqualTo(USER_PASSWORD);
        assertThat(userDetails.getAuthorities()).hasSize(2);
        assertThat(userDetails.getAuthorities())
            .extracting(authority -> authority.getAuthority())
            .containsExactlyInAnyOrder(ADMIN_AUTHORITY, USER_AUTHORITY);
    }

    @Test
    void testLoadUserByUsernameWithValidEmail() {
        // Given
        User user = createTestUser(USER_LOGIN, USER_EMAIL, true);
        when(userRepository.findOneWithAuthoritiesByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_EMAIL);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_LOGIN);
        assertThat(userDetails.getPassword()).isEqualTo(USER_PASSWORD);
        assertThat(userDetails.getAuthorities()).hasSize(2);
    }

    @Test
    void testLoadUserByUsernameWithMixedCaseLogin() {
        // Given
        String mixedCaseLogin = "TestUser";
        User user = createTestUser(USER_LOGIN, USER_EMAIL, true);
        when(userRepository.findOneWithAuthoritiesByLogin(mixedCaseLogin.toLowerCase())).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(mixedCaseLogin);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_LOGIN);
    }

    @Test
    void testLoadUserByUsernameWithInactiveUser() {
        // Given
        User inactiveUser = createTestUser(USER_LOGIN, USER_EMAIL, false);
        when(userRepository.findOneWithAuthoritiesByLogin(USER_LOGIN.toLowerCase())).thenReturn(Optional.of(inactiveUser));

        // When & Then
        assertThatExceptionOfType(UserNotActivatedException.class)
            .isThrownBy(() -> domainUserDetailsService.loadUserByUsername(USER_LOGIN))
            .withMessage("User " + USER_LOGIN.toLowerCase() + " was not activated");
    }

    @Test
    void testLoadUserByUsernameWithInactiveUserByEmail() {
        // Given
        User inactiveUser = createTestUser(USER_LOGIN, USER_EMAIL, false);
        when(userRepository.findOneWithAuthoritiesByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(inactiveUser));

        // When & Then
        assertThatExceptionOfType(UserNotActivatedException.class)
            .isThrownBy(() -> domainUserDetailsService.loadUserByUsername(USER_EMAIL))
            .withMessage("User " + USER_EMAIL + " was not activated");
    }

    @Test
    void testLoadUserByUsernameWithNonExistentLogin() {
        // Given
        String nonExistentLogin = "nonexistent";
        when(userRepository.findOneWithAuthoritiesByLogin(nonExistentLogin.toLowerCase())).thenReturn(Optional.empty());

        // When & Then
        assertThatExceptionOfType(UsernameNotFoundException.class)
            .isThrownBy(() -> domainUserDetailsService.loadUserByUsername(nonExistentLogin))
            .withMessage("User " + nonExistentLogin.toLowerCase() + " was not found in the database");
    }

    @Test
    void testLoadUserByUsernameWithNonExistentEmail() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findOneWithAuthoritiesByEmailIgnoreCase(nonExistentEmail)).thenReturn(Optional.empty());

        // When & Then
        assertThatExceptionOfType(UsernameNotFoundException.class)
            .isThrownBy(() -> domainUserDetailsService.loadUserByUsername(nonExistentEmail))
            .withMessage("User with email " + nonExistentEmail + " was not found in the database");
    }

    @Test
    void testLoadUserByUsernameWithUserWithoutAuthorities() {
        // Given
        User userWithoutAuthorities = createTestUserWithoutAuthorities(USER_LOGIN, USER_EMAIL, true);
        when(userRepository.findOneWithAuthoritiesByLogin(USER_LOGIN.toLowerCase())).thenReturn(Optional.of(userWithoutAuthorities));

        // When
        UserDetails userDetails = domainUserDetailsService.loadUserByUsername(USER_LOGIN);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    @Test
    void testConstructor() {
        // When
        DomainUserDetailsService service = new DomainUserDetailsService(userRepository);

        // Then
        assertThat(service).isNotNull();
    }

    private User createTestUser(String login, String email, boolean activated) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(email);
        user.setPassword(USER_PASSWORD);
        user.setActivated(activated);

        Authority adminAuthority = new Authority();
        adminAuthority.setName(ADMIN_AUTHORITY);

        Authority userAuthority = new Authority();
        userAuthority.setName(USER_AUTHORITY);

        user.setAuthorities(Set.of(adminAuthority, userAuthority));
        return user;
    }

    private User createTestUserWithoutAuthorities(String login, String email, boolean activated) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(email);
        user.setPassword(USER_PASSWORD);
        user.setActivated(activated);
        user.setAuthorities(Set.of());
        return user;
    }
}
