package com.mycompany.myapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Additional unit tests for {@link SecurityUtils}.
 */
class SecurityUtilsTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserLogin_shouldReturnEmpty_whenNoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When
        Optional<String> result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentUserLogin_shouldReturnEmpty_whenAuthenticationIsNull() {
        // Given
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentUserLogin_shouldReturnLogin_fromUsernamePasswordAuthentication() {
        // Given
        String expectedLogin = "testuser";
        Authentication authentication = new UsernamePasswordAuthenticationToken(expectedLogin, "password");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedLogin);
    }

    @Test
    void getCurrentUserLogin_shouldReturnLogin_fromJwtAuthentication() {
        // Given
        String expectedLogin = "jwtuser";
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", MacAlgorithm.HS256.getName())
            .claim("sub", expectedLogin)
            .claim("preferred_username", expectedLogin)
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedLogin);
    }

    @Test
    void getCurrentUserLogin_shouldReturnEmpty_whenPrincipalIsNull() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> result = SecurityUtils.getCurrentUserLogin();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentUserJWT_shouldReturnEmpty_whenNoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When
        Optional<String> result = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentUserJWT_shouldReturnToken_fromJwtAuthentication() {
        // Given
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        Jwt jwt = Jwt.withTokenValue(expectedToken).header("alg", MacAlgorithm.HS256.getName()).claim("sub", "user").build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> result = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedToken);
    }

    @Test
    void getCurrentUserJWT_shouldReturnEmpty_fromNonJwtAuthentication() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        Optional<String> result = SecurityUtils.getCurrentUserJWT();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void isAuthenticated_shouldReturnTrue_whenUserIsAuthenticated() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "user",
            "password",
            java.util.Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean result = SecurityUtils.isAuthenticated();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isAuthenticated_shouldReturnFalse_whenNoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When
        boolean result = SecurityUtils.isAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isAuthenticated_shouldReturnFalse_whenUserIsAnonymous() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "user",
            "password",
            java.util.Arrays.asList(new SimpleGrantedAuthority(AuthoritiesConstants.ANONYMOUS))
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean result = SecurityUtils.isAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_shouldReturnTrue_whenUserHasAuthority() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "user",
            "password",
            java.util.Arrays.asList(
                new SimpleGrantedAuthority(AuthoritiesConstants.USER),
                new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
            )
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean result = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN, "ROLE_MANAGER");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_shouldReturnFalse_whenUserDoesNotHaveAuthority() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "user",
            "password",
            java.util.Arrays.asList(new SimpleGrantedAuthority(AuthoritiesConstants.USER))
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean result = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN, "ROLE_MANAGER");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasCurrentUserAnyOfAuthorities_shouldReturnFalse_whenNoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When
        boolean result = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.USER);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasCurrentUserThisAuthority_shouldReturnTrue_whenUserHasExactAuthority() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "user",
            "password",
            java.util.Arrays.asList(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean result = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasCurrentUserThisAuthority_shouldReturnFalse_whenUserDoesNotHaveAuthority() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "user",
            "password",
            java.util.Arrays.asList(new SimpleGrantedAuthority(AuthoritiesConstants.USER))
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean result = SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasCurrentUserNoneOfAuthorities_shouldReturnTrue_whenUserHasNoneOfAuthorities() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "user",
            "password",
            java.util.Arrays.asList(new SimpleGrantedAuthority(AuthoritiesConstants.USER))
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean result = SecurityUtils.hasCurrentUserNoneOfAuthorities(AuthoritiesConstants.ADMIN, "ROLE_MANAGER");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasCurrentUserNoneOfAuthorities_shouldReturnFalse_whenUserHasOneOfAuthorities() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "user",
            "password",
            java.util.Arrays.asList(
                new SimpleGrantedAuthority(AuthoritiesConstants.USER),
                new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
            )
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        boolean result = SecurityUtils.hasCurrentUserNoneOfAuthorities(AuthoritiesConstants.ADMIN, "ROLE_MANAGER");

        // Then
        assertThat(result).isFalse();
    }
}
