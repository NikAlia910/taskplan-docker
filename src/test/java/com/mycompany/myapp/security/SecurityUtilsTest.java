package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for {@link SecurityUtils}.
 */
class SecurityUtilsTest {

    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserLogin_WhenUserDetailsAuthentication_ShouldReturnUsername() {
        // Given
        User userDetails = new User("testuser", "password", Collections.emptyList());
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "password");
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        var result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).isEqualTo("testuser");
    }

    @Test
    void getCurrentUserLogin_WhenJwtAuthentication_ShouldReturnSubject() {
        // Given
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("jwtuser");
        Authentication authentication = new UsernamePasswordAuthenticationToken(jwt, null);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        var result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).isEqualTo("jwtuser");
    }

    @Test
    void getCurrentUserLogin_WhenStringPrincipal_ShouldReturnString() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken("stringuser", null);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        var result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).isEqualTo("stringuser");
    }

    @Test
    void getCurrentUserLogin_WhenNoAuthentication_ShouldReturnEmpty() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        var result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentUserLogin_WhenUnknownPrincipalType_ShouldReturnEmpty() {
        // Given
        Object unknownPrincipal = new Object();
        Authentication authentication = new UsernamePasswordAuthenticationToken(unknownPrincipal, null);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        var result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentUserJWT_WhenStringCredentials_ShouldReturnJWT() {
        // Given
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", jwtToken);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        var result = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).isEqualTo(jwtToken);
    }

    @Test
    void getCurrentUserJWT_WhenNoAuthentication_ShouldReturnEmpty() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        var result = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentUserJWT_WhenNonStringCredentials_ShouldReturnEmpty() {
        // Given
        Object nonStringCredentials = new Object();
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", nonStringCredentials);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        var result = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void isAuthenticated_WhenAuthenticatedWithoutAnonymous_ShouldReturnTrue() {
        // Given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean result = SecurityUtils.isAuthenticated();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isAuthenticated_WhenAnonymousUser_ShouldReturnFalse() {
        // Given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(AuthoritiesConstants.ANONYMOUS));
        Authentication authentication = new UsernamePasswordAuthenticationToken("anonymous", null, authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean result = SecurityUtils.isAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isAuthenticated_WhenNoAuthentication_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = SecurityUtils.isAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_WhenUserHasOneOfAuthorities_ShouldReturnTrue() {
        // Given
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(AuthoritiesConstants.USER),
            new SimpleGrantedAuthority("ROLE_CUSTOM")
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean result = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_WhenUserHasNoneOfAuthorities_ShouldReturnFalse() {
        // Given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOM"));
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean result = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_WhenNoAuthentication_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.USER);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasCurrentUserNoneOfAuthorities_WhenUserHasNoneOfAuthorities_ShouldReturnTrue() {
        // Given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOM"));
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean result = SecurityUtils.hasCurrentUserNoneOfAuthorities(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasCurrentUserNoneOfAuthorities_WhenUserHasOneOfAuthorities_ShouldReturnFalse() {
        // Given
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(AuthoritiesConstants.USER),
            new SimpleGrantedAuthority("ROLE_CUSTOM")
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean result = SecurityUtils.hasCurrentUserNoneOfAuthorities(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasCurrentUserThisAuthority_WhenUserHasAuthority_ShouldReturnTrue() {
        // Given
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN),
            new SimpleGrantedAuthority("ROLE_CUSTOM")
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean result = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasCurrentUserThisAuthority_WhenUserDoesNotHaveAuthority_ShouldReturnFalse() {
        // Given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOM"));
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        // When
        boolean result = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasCurrentUserThisAuthority_WhenNoAuthentication_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.USER);

        // Then
        assertThat(result).isFalse();
    }
}
