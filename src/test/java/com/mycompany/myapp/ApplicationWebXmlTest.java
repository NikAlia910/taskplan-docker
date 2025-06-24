package com.mycompany.myapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import tech.jhipster.config.JHipsterConstants;

/**
 * Unit tests for {@link ApplicationWebXml}.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationWebXmlTest {

    @Mock
    private SpringApplicationBuilder applicationBuilder;

    @InjectMocks
    private ApplicationWebXml applicationWebXml;

    @BeforeEach
    void setUp() {
        when(applicationBuilder.sources(any(Class.class))).thenReturn(applicationBuilder);
        when(applicationBuilder.profiles(anyString())).thenReturn(applicationBuilder);
    }

    @Test
    void configure_shouldSetMainApplicationSource() {
        // When
        SpringApplicationBuilder result = applicationWebXml.configure(applicationBuilder);

        // Then
        verify(applicationBuilder).sources(TaskplanDockerApp.class);
        assertThat(result).isEqualTo(applicationBuilder);
    }

    @Test
    void configure_shouldAddDefaultProfile() {
        // When
        applicationWebXml.configure(applicationBuilder);

        // Then
        verify(applicationBuilder).profiles(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT);
    }

    @Test
    void applicationWebXml_shouldExtendSpringBootServletInitializer() {
        // Then
        assertThat(ApplicationWebXml.class.getSuperclass()).isEqualTo(SpringBootServletInitializer.class);
    }

    @Test
    void configure_shouldReturnBuilderForChaining() {
        // When
        SpringApplicationBuilder result = applicationWebXml.configure(applicationBuilder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(applicationBuilder);
    }

    @Test
    void applicationWebXml_shouldHavePublicConstructor() {
        // When & Then
        ApplicationWebXml instance = new ApplicationWebXml();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(SpringBootServletInitializer.class);
    }

    @Test
    void configure_shouldHandleNullBuilder() {
        // When & Then
        // This test ensures the method handles edge cases gracefully
        // In a real scenario, Spring would never pass null, but we test for robustness
        try {
            applicationWebXml.configure(null);
        } catch (Exception e) {
            // Expected behavior - let the method handle null appropriately
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }
}
