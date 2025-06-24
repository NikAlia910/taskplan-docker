package com.mycompany.myapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests for {@link SecurityConfiguration}.
 */
@IntegrationTest
@TestPropertySource(properties = { "jhipster.clientApp.name=taskplanDockerApp" })
class SecurityConfigurationTest {

    @Autowired
    private SecurityConfiguration securityConfiguration;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityFilterChain filterChain;

    @Test
    void passwordEncoder_shouldBeConfigured() {
        // Given/When
        PasswordEncoder encoder = securityConfiguration.passwordEncoder();

        // Then
        assertThat(encoder).isNotNull();
        assertThat(encoder.encode("password")).isNotEqualTo("password");
        assertThat(encoder.matches("password", encoder.encode("password"))).isTrue();
    }

    @Test
    void securityFilterChain_shouldBeConfigured() {
        // Given/When/Then
        assertThat(filterChain).isNotNull();
    }

    @Test
    void passwordEncoder_shouldUseStrongEncoding() {
        // Given
        String plainPassword = "testPassword123";

        // When
        String encoded1 = passwordEncoder.encode(plainPassword);
        String encoded2 = passwordEncoder.encode(plainPassword);

        // Then
        assertThat(encoded1).isNotEqualTo(encoded2); // Should be different due to salt
        assertThat(encoded1).hasSize(60); // BCrypt hash should be 60 characters
        assertThat(passwordEncoder.matches(plainPassword, encoded1)).isTrue();
        assertThat(passwordEncoder.matches(plainPassword, encoded2)).isTrue();
        assertThat(passwordEncoder.matches("wrongPassword", encoded1)).isFalse();
    }

    @Test
    void securityConfiguration_shouldBeInjectable() {
        // Given/When/Then
        assertThat(securityConfiguration).isNotNull();
    }

    @Test
    void passwordEncoder_shouldHandleNullAndEmptyPasswords() {
        // Given
        String emptyPassword = "";
        String nonEmptyPassword = "test";

        // When
        String encodedEmpty = passwordEncoder.encode(emptyPassword);
        String encodedNonEmpty = passwordEncoder.encode(nonEmptyPassword);

        // Then
        assertThat(encodedEmpty).isNotEmpty();
        assertThat(encodedNonEmpty).isNotEmpty();
        assertThat(passwordEncoder.matches(emptyPassword, encodedEmpty)).isTrue();
        assertThat(passwordEncoder.matches(nonEmptyPassword, encodedNonEmpty)).isTrue();
        assertThat(passwordEncoder.matches(emptyPassword, encodedNonEmpty)).isFalse();
    }

    @Test
    void passwordEncoder_shouldHandleSpecialCharacters() {
        // Given
        String specialCharPassword = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        // When
        String encoded = passwordEncoder.encode(specialCharPassword);

        // Then
        assertThat(encoded).hasSize(60);
        assertThat(passwordEncoder.matches(specialCharPassword, encoded)).isTrue();
        assertThat(passwordEncoder.matches("differentPassword", encoded)).isFalse();
    }
}
