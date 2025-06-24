package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Advanced test class for the {@link SecurityUtils} utility class.
 * Covers edge cases and complex scenarios.
 */
class SecurityUtilsAdvancedTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserLogin_withNullAuthentication_shouldReturnEmpty() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(null);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> login = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(login).isEmpty();
    }

    @Test
    void getCurrentUserLogin_withAnonymousUser_shouldReturnEmpty() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new TestingAuthenticationToken("anonymousUser", ""));
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> login = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(login).isEmpty();
    }

    @Test
    void getCurrentUserLogin_withNullPrincipal_shouldReturnEmpty() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        TestingAuthenticationToken auth = new TestingAuthenticationToken(null, "credentials");
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> login = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(login).isEmpty();
    }

    @Test
    void getCurrentUserLogin_withEmptyStringPrincipal_shouldReturnEmptyString() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("", "password"));
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> login = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(login).contains("");
    }

    @Test
    void getCurrentUserLogin_withWhitespacePrincipal_shouldReturnWhitespace() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("   ", "password"));
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> login = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(login).contains("   ");
    }

    @Test
    void getCurrentUserLogin_withLongUsername_shouldReturnFullUsername() {
        // Given
        String longUsername = "a".repeat(1000);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(longUsername, "password"));
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> login = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(login).contains(longUsername);
    }

    @Test
    void getCurrentUserJWT_withNullAuthentication_shouldReturnEmpty() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(null);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> jwt = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(jwt).isEmpty();
    }

    @Test
    void getCurrentUserJWT_withNullCredentials_shouldReturnEmpty() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", null);
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> jwt = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(jwt).isEmpty();
    }

    @Test
    void getCurrentUserJWT_withEmptyStringCredentials_shouldReturnEmptyString() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", ""));
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> jwt = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(jwt).contains("");
    }

    @Test
    void getCurrentUserJWT_withValidToken_shouldReturnToken() {
        // Given
        String token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", token));
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> jwt = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(jwt).contains(token);
    }

    @Test
    void hasCurrentUserThisAuthority_withNullAuthentication_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(null);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAuthority = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.USER);

        // Then
        assertThat(hasAuthority).isFalse();
    }

    @Test
    void hasCurrentUserThisAuthority_withEmptyAuthorities_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAuthority = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.USER);

        // Then
        assertThat(hasAuthority).isFalse();
    }

    @Test
    void hasCurrentUserThisAuthority_withMatchingAuthority_shouldReturnTrue() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasUserAuthority = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.USER);
        boolean hasAdminAuthority = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);

        // Then
        assertThat(hasUserAuthority).isTrue();
        assertThat(hasAdminAuthority).isTrue();
    }

    @Test
    void hasCurrentUserThisAuthority_withNonMatchingAuthority_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAdminAuthority = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);

        // Then
        assertThat(hasAdminAuthority).isFalse();
    }

    @Test
    void hasCurrentUserThisAuthority_withNullAuthorityParameter_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAuthority = SecurityUtils.hasCurrentUserThisAuthority(null);

        // Then
        assertThat(hasAuthority).isFalse();
    }

    @Test
    void hasCurrentUserThisAuthority_withEmptyAuthorityParameter_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAuthority = SecurityUtils.hasCurrentUserThisAuthority("");

        // Then
        assertThat(hasAuthority).isFalse();
    }

    @Test
    void hasCurrentUserThisAuthority_withCaseSensitiveAuthority_shouldBeExact() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_user")); // lowercase 'user'
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasExactAuthority = SecurityUtils.hasCurrentUserThisAuthority("ROLE_user");
        boolean hasUppercaseAuthority = SecurityUtils.hasCurrentUserThisAuthority("ROLE_USER");

        // Then
        assertThat(hasExactAuthority).isTrue();
        assertThat(hasUppercaseAuthority).isFalse();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_withNullAuthentication_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(null);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAnyAuthority = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);

        // Then
        assertThat(hasAnyAuthority).isFalse();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_withNullParameters_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAnyAuthority = SecurityUtils.hasCurrentUserAnyOfAuthorities((String[]) null);

        // Then
        assertThat(hasAnyAuthority).isFalse();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_withEmptyParameters_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAnyAuthority = SecurityUtils.hasCurrentUserAnyOfAuthorities();

        // Then
        assertThat(hasAnyAuthority).isFalse();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_withOneMatchingAuthority_shouldReturnTrue() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAnyAuthority = SecurityUtils.hasCurrentUserAnyOfAuthorities(
            AuthoritiesConstants.ADMIN,
            AuthoritiesConstants.USER,
            "ROLE_MANAGER"
        );

        // Then
        assertThat(hasAnyAuthority).isTrue();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_withNoMatchingAuthorities_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasAnyAuthority = SecurityUtils.hasCurrentUserAnyOfAuthorities(
            AuthoritiesConstants.ADMIN,
            "ROLE_MANAGER",
            "ROLE_SUPERVISOR"
        );

        // Then
        assertThat(hasAnyAuthority).isFalse();
    }

    @Test
    void hasCurrentUserNoneOfAuthorities_withNullAuthentication_shouldReturnTrue() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(null);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasNoneOfAuthorities = SecurityUtils.hasCurrentUserNoneOfAuthorities(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);

        // Then
        assertThat(hasNoneOfAuthorities).isTrue();
    }

    @Test
    void hasCurrentUserNoneOfAuthorities_withMatchingAuthority_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasNoneOfAuthorities = SecurityUtils.hasCurrentUserNoneOfAuthorities(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);

        // Then
        assertThat(hasNoneOfAuthorities).isFalse();
    }

    @Test
    void hasCurrentUserNoneOfAuthorities_withNoMatchingAuthorities_shouldReturnTrue() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password", authorities));
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean hasNoneOfAuthorities = SecurityUtils.hasCurrentUserNoneOfAuthorities(AuthoritiesConstants.ADMIN, "ROLE_MANAGER");

        // Then
        assertThat(hasNoneOfAuthorities).isTrue();
    }

    @Test
    void isAuthenticated_withNullAuthentication_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(null);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean isAuthenticated = SecurityUtils.isAuthenticated();

        // Then
        assertThat(isAuthenticated).isFalse();
    }

    @Test
    void isAuthenticated_withUnauthenticatedUser_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "password");
        auth.setAuthenticated(false);
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean isAuthenticated = SecurityUtils.isAuthenticated();

        // Then
        assertThat(isAuthenticated).isFalse();
    }

    @Test
    void isAuthenticated_withAnonymousAuthority_shouldReturnFalse() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.ANONYMOUS));
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "password", authorities);
        auth.setAuthenticated(true);
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean isAuthenticated = SecurityUtils.isAuthenticated();

        // Then
        assertThat(isAuthenticated).isFalse();
    }

    @Test
    void isAuthenticated_withValidAuthentication_shouldReturnTrue() {
        // Given
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(AuthoritiesConstants.USER));
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "password", authorities);
        auth.setAuthenticated(true);
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean isAuthenticated = SecurityUtils.isAuthenticated();

        // Then
        assertThat(isAuthenticated).isTrue();
    }
}
