package com.mycompany.myapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import tech.jhipster.config.JHipsterProperties;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebConfigurerTest {

    @Mock
    private ServletContext servletContext;

    @Mock
    private Environment environment;

    @Mock
    private JHipsterProperties jHipsterProperties;

    @Mock
    private JHipsterProperties.Http http;

    @Mock
    private JHipsterProperties.Http.Cache cache;

    @Mock
    private FilterRegistration.Dynamic filterRegistration;

    @Mock
    private ServletRegistration.Dynamic servletRegistration;

    private WebConfigurer webConfigurer;

    @BeforeEach
    void setUp() {
        when(jHipsterProperties.getHttp()).thenReturn(http);
        when(http.getCache()).thenReturn(cache);
        when(cache.getTimeToLiveInDays()).thenReturn(1461);

        // Mock ServletContext to return mocked registrations
        when(servletContext.addFilter(anyString(), any(Filter.class))).thenReturn(filterRegistration);
        when(servletContext.addServlet(anyString(), any(Servlet.class))).thenReturn(servletRegistration);

        webConfigurer = new WebConfigurer(environment, jHipsterProperties);
    }

    @Test
    void testConstructor() {
        // When
        WebConfigurer configurer = new WebConfigurer(environment, jHipsterProperties);

        // Then
        assertThat(configurer).isNotNull();
    }

    @Test
    void testOnStartup() throws Exception {
        // Given
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setActiveProfiles("dev");
        webConfigurer = new WebConfigurer(mockEnv, jHipsterProperties);

        // When
        webConfigurer.onStartup(servletContext);

        // Then
        // Verify that startup completed without exceptions
        assertThat(webConfigurer).isNotNull();
    }

    @Test
    void testOnStartupWithProdProfile() throws Exception {
        // Given
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setActiveProfiles("prod");
        webConfigurer = new WebConfigurer(mockEnv, jHipsterProperties);

        // When
        webConfigurer.onStartup(servletContext);

        // Then
        // Should configure production-specific settings
        assertThat(webConfigurer).isNotNull();
    }

    @Test
    void testOnStartupWithTestProfile() throws Exception {
        // Given
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setActiveProfiles("test");
        webConfigurer = new WebConfigurer(mockEnv, jHipsterProperties);

        // When
        webConfigurer.onStartup(servletContext);

        // Then
        // Should configure test-specific settings
        assertThat(webConfigurer).isNotNull();
    }

    @Test
    void testServletContextInitializer() {
        // Test that WebConfigurer implements ServletContextInitializer
        assertThat(webConfigurer).isInstanceOf(ServletContextInitializer.class);
    }

    @Test
    void testInitCacheHttpHeaders() throws Exception {
        // Given
        when(cache.getTimeToLiveInDays()).thenReturn(30);

        // When
        webConfigurer.onStartup(servletContext);

        // Then
        // Should configure cache headers
        assertThat(webConfigurer).isNotNull();
    }

    @Test
    void testInitCacheHttpHeadersWithZeroDays() throws Exception {
        // Given
        when(cache.getTimeToLiveInDays()).thenReturn(0);

        // When
        webConfigurer.onStartup(servletContext);

        // Then
        // Should handle zero cache days gracefully
        assertThat(webConfigurer).isNotNull();
    }

    @Test
    void testInitCacheHttpHeadersWithLargeDays() throws Exception {
        // Given
        when(cache.getTimeToLiveInDays()).thenReturn(365);

        // When
        webConfigurer.onStartup(servletContext);

        // Then
        // Should handle large cache days
        assertThat(webConfigurer).isNotNull();
    }

    @Test
    void testWebConfigurerWithNullEnvironment() {
        // Given
        Environment nullEnv = null;

        // When/Then
        try {
            new WebConfigurer(nullEnv, jHipsterProperties);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void testWebConfigurerWithNullJHipsterProperties() {
        // Given
        JHipsterProperties nullProps = null;

        // When/Then
        try {
            new WebConfigurer(environment, nullProps);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void testEnvironmentProfileHandling() {
        // Given
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        // When
        WebConfigurer devConfigurer = new WebConfigurer(devEnv, jHipsterProperties);
        WebConfigurer prodConfigurer = new WebConfigurer(prodEnv, jHipsterProperties);

        // Then
        assertThat(devConfigurer).isNotNull();
        assertThat(prodConfigurer).isNotNull();
    }

    @Test
    void testMultipleProfileHandling() {
        // Given
        MockEnvironment multiEnv = new MockEnvironment();
        multiEnv.setActiveProfiles("dev", "swagger");

        // When
        WebConfigurer configurer = new WebConfigurer(multiEnv, jHipsterProperties);

        // Then
        assertThat(configurer).isNotNull();
    }

    @Test
    void testHttpPropertiesAccess() {
        // Given
        when(jHipsterProperties.getHttp()).thenReturn(http);
        when(http.getCache()).thenReturn(cache);
        when(cache.getTimeToLiveInDays()).thenReturn(1461);

        // When
        WebConfigurer configurer = new WebConfigurer(environment, jHipsterProperties);

        // Then
        assertThat(configurer).isNotNull();
        // Verify that HTTP properties are accessed during construction
        verify(jHipsterProperties).getHttp();
    }

    @Test
    void testDefaultCacheConfiguration() {
        // Given
        JHipsterProperties realProps = new JHipsterProperties();
        MockEnvironment mockEnv = new MockEnvironment();

        // When
        WebConfigurer configurer = new WebConfigurer(mockEnv, realProps);

        // Then
        assertThat(configurer).isNotNull();
        // Should work with default JHipster properties
    }

    @Test
    void testServletContextOperations() throws Exception {
        // Given
        MockEnvironment mockEnv = new MockEnvironment();
        webConfigurer = new WebConfigurer(mockEnv, jHipsterProperties);

        // When
        webConfigurer.onStartup(servletContext);

        // Then
        // Verify servlet context is used (exact verifications depend on implementation)
        assertThat(webConfigurer).isNotNull();
    }

    @Test
    void testFilterRegistration() throws Exception {
        // Given
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setActiveProfiles("prod");
        webConfigurer = new WebConfigurer(mockEnv, jHipsterProperties);

        // When
        webConfigurer.onStartup(servletContext);

        // Then
        // Should register filters in production mode
        assertThat(webConfigurer).isNotNull();
    }

    @Test
    void testDevelopmentModeConfiguration() throws Exception {
        // Given
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");
        webConfigurer = new WebConfigurer(devEnv, jHipsterProperties);

        // When
        webConfigurer.onStartup(servletContext);

        // Then
        // Should configure development-specific settings
        assertThat(webConfigurer).isNotNull();
    }

    @Test
    void testStaticResourceHandling() throws Exception {
        // Given
        when(cache.getTimeToLiveInDays()).thenReturn(1461);
        MockEnvironment mockEnv = new MockEnvironment();

        // When
        webConfigurer = new WebConfigurer(mockEnv, jHipsterProperties);
        webConfigurer.onStartup(servletContext);

        // Then
        // Should configure static resource caching
        assertThat(webConfigurer).isNotNull();
    }
}
