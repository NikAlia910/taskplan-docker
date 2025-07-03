package com.mycompany.myapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import tech.jhipster.config.JHipsterConstants;

@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
class TaskplanDockerAppTest {

    private Environment environment;
    private TaskplanDockerApp app;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        app = new TaskplanDockerApp(environment);
    }

    @Test
    void testConstructor() {
        // When
        TaskplanDockerApp application = new TaskplanDockerApp(environment);

        // Then
        assertThat(application).isNotNull();
    }

    @Test
    void testInitApplicationWithValidProfiles(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationWithDevAndProdProfiles(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(
            new String[] { JHipsterConstants.SPRING_PROFILE_DEVELOPMENT, JHipsterConstants.SPRING_PROFILE_PRODUCTION }
        );

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).contains("You have misconfigured your application!").contains("'dev' and 'prod' profiles");
    }

    @Test
    void testInitApplicationWithDevAndCloudProfiles(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(
            new String[] { JHipsterConstants.SPRING_PROFILE_DEVELOPMENT, JHipsterConstants.SPRING_PROFILE_CLOUD }
        );

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).contains("You have misconfigured your application!").contains("'dev' and 'cloud' profiles");
    }

    @Test
    void testInitApplicationWithProductionProfile(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { JHipsterConstants.SPRING_PROFILE_PRODUCTION });

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationWithCloudProfile(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { JHipsterConstants.SPRING_PROFILE_CLOUD });

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationWithMultipleValidProfiles(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { JHipsterConstants.SPRING_PROFILE_DEVELOPMENT, "api-docs", "test" });

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationWithEmptyProfiles(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] {});

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationWithNullProfiles(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(null);

        // When
        app.initApplication();

        // Then
        // Should handle null gracefully
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationWithOnlyDevProfile(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { JHipsterConstants.SPRING_PROFILE_DEVELOPMENT });

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationWithOnlyProdProfile(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { JHipsterConstants.SPRING_PROFILE_PRODUCTION });

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationWithInvalidCombination1(CapturedOutput output) {
        // Given - All three conflicting profiles
        when(environment.getActiveProfiles()).thenReturn(
            new String[] {
                JHipsterConstants.SPRING_PROFILE_DEVELOPMENT,
                JHipsterConstants.SPRING_PROFILE_PRODUCTION,
                JHipsterConstants.SPRING_PROFILE_CLOUD,
            }
        );

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).contains("You have misconfigured your application!");
    }

    @Test
    void testInitApplicationWithTestProfile(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "test" });

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationWithSwaggerProfile(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "api-docs" });

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // No error should be logged
    }

    @Test
    void testInitApplicationMultipleTimes(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });

        // When
        app.initApplication();
        app.initApplication(); // Call multiple times

        // Then
        assertThat(output.getOut()).isEmpty(); // Should be safe to call multiple times
    }

    @Test
    void testInitApplicationWithCustomProfile(CapturedOutput output) {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "custom", "local" });

        // When
        app.initApplication();

        // Then
        assertThat(output.getOut()).isEmpty(); // Custom profiles should be allowed
    }

    @Test
    void testInitApplicationProfileCaseSensitivity(CapturedOutput output) {
        // Given - Test case sensitivity
        when(environment.getActiveProfiles()).thenReturn(new String[] { "DEV", "PROD" });

        // When
        app.initApplication();

        // Then
        // Should not trigger error as case doesn't match JHipster constants
        assertThat(output.getOut()).isEmpty();
    }

    @Test
    void testInitApplicationPerformance() {
        // Given
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });

        // When
        long startTime = System.currentTimeMillis();
        app.initApplication();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration).isLessThan(100); // Should be fast
    }

    @Test
    void testMainMethodRequirements() {
        // Test that main method exists and is properly formed
        try {
            TaskplanDockerApp.class.getDeclaredMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Main method should exist", e);
        }
    }
}
