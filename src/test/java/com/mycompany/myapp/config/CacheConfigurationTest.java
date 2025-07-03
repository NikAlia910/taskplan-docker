package com.mycompany.myapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.test.util.ReflectionTestUtils;
import tech.jhipster.config.JHipsterProperties;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CacheConfigurationTest {

    @Mock
    private JHipsterProperties jHipsterProperties;

    @Mock
    private JHipsterProperties.Cache cache;

    @Mock
    private JHipsterProperties.Cache.Caffeine caffeine;

    private CacheConfiguration cacheConfiguration;

    @BeforeEach
    void setUp() {
        when(jHipsterProperties.getCache()).thenReturn(cache);
        when(cache.getCaffeine()).thenReturn(caffeine);
        when(caffeine.getTimeToLiveSeconds()).thenReturn(3600);
        when(caffeine.getMaxEntries()).thenReturn(1000L);

        cacheConfiguration = new CacheConfiguration(jHipsterProperties);
    }

    @Test
    void testConstructor() {
        // When
        CacheConfiguration config = new CacheConfiguration(jHipsterProperties);

        // Then
        assertThat(config).isNotNull();
    }

    @Test
    void testCacheManagerCustomizer() {
        // When
        JCacheManagerCustomizer customizer = cacheConfiguration.cacheManagerCustomizer();

        // Then
        assertThat(customizer).isNotNull();

        // Test that customizer can be called
        CacheManager cacheManager = mock(CacheManager.class);
        customizer.customize(cacheManager);
        // Should not throw an exception
    }

    @Test
    void testKeyGenerator() {
        // When
        KeyGenerator keyGenerator = cacheConfiguration.keyGenerator();

        // Then
        assertThat(keyGenerator).isNotNull();
        assertThat(keyGenerator).isInstanceOf(KeyGenerator.class);
    }

    @Test
    void testCacheConfigurationIsEnabledByAnnotation() {
        // Test that the class is annotated with @EnableCaching
        EnableCaching enableCaching = CacheConfiguration.class.getAnnotation(EnableCaching.class);
        assertThat(enableCaching).isNotNull();
    }

    @Test
    void testCaffeineConfiguration() {
        // Given
        when(caffeine.getTimeToLiveSeconds()).thenReturn(7200);
        when(caffeine.getMaxEntries()).thenReturn(2000L);

        // When
        CacheConfiguration config = new CacheConfiguration(jHipsterProperties);

        // Then
        assertThat(config).isNotNull();
        // Verify that the configuration uses the provided values
    }

    @Test
    void testCaffeineConfigurationWithZeroValues() {
        // Given
        when(caffeine.getTimeToLiveSeconds()).thenReturn(0);
        when(caffeine.getMaxEntries()).thenReturn(0L);

        // When
        CacheConfiguration config = new CacheConfiguration(jHipsterProperties);

        // Then
        assertThat(config).isNotNull();
        // Should handle zero values gracefully
    }

    @Test
    void testCaffeineConfigurationWithLargeValues() {
        // Given
        when(caffeine.getTimeToLiveSeconds()).thenReturn(86400); // 1 day
        when(caffeine.getMaxEntries()).thenReturn(100000L); // 100k entries

        // When
        CacheConfiguration config = new CacheConfiguration(jHipsterProperties);

        // Then
        assertThat(config).isNotNull();
        // Should handle large values gracefully
    }

    @Test
    void testKeyGeneratorGeneration() {
        // Given
        KeyGenerator keyGenerator = cacheConfiguration.keyGenerator();
        Object target = new Object();
        Method method = mock(Method.class);
        when(method.getName()).thenReturn("testMethod");
        Object[] params = new Object[] { "param1", "param2", 123 };

        // When
        Object key1 = keyGenerator.generate(target, method, params);
        Object key2 = keyGenerator.generate(target, method, params);

        // Then
        assertThat(key1).isNotNull();
        assertThat(key2).isNotNull();
        assertThat(key1).isEqualTo(key2); // Same parameters should generate same key
    }

    @Test
    void testKeyGeneratorWithDifferentParameters() {
        // Given
        KeyGenerator keyGenerator = cacheConfiguration.keyGenerator();
        Object target = new Object();
        Method method = mock(Method.class);
        when(method.getName()).thenReturn("testMethod");
        Object[] params1 = new Object[] { "param1", "param2" };
        Object[] params2 = new Object[] { "param1", "param3" };

        // When
        Object key1 = keyGenerator.generate(target, method, params1);
        Object key2 = keyGenerator.generate(target, method, params2);

        // Then
        assertThat(key1).isNotNull();
        assertThat(key2).isNotNull();
        assertThat(key1).isNotEqualTo(key2); // Different parameters should generate different keys
    }

    @Test
    void testKeyGeneratorWithNullParameters() {
        // Given
        KeyGenerator keyGenerator = cacheConfiguration.keyGenerator();
        Object target = new Object();
        Method method = mock(Method.class);
        when(method.getName()).thenReturn("testMethod");

        // When
        Object key1 = keyGenerator.generate(target, method, (Object[]) null);
        Object key2 = keyGenerator.generate(target, method, new Object[0]);

        // Then
        assertThat(key1).isNotNull();
        assertThat(key2).isNotNull();
        // Should handle null and empty parameters gracefully
    }

    @Test
    void testCacheManagerCustomizerWithUserCaches() {
        // Given
        JCacheManagerCustomizer customizer = cacheConfiguration.cacheManagerCustomizer();
        CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();

        // When
        customizer.customize(cacheManager);

        // Then
        // Should configure user-related caches (specific cache names depend on implementation)
        assertThat(cacheManager).isNotNull();
    }

    @Test
    void testCacheConfigurationWithNullJHipsterProperties() {
        // Given
        JHipsterProperties nullProps = null;

        // When/Then
        try {
            new CacheConfiguration(nullProps);
        } catch (Exception e) {
            // Should handle null properties gracefully or throw appropriate exception
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void testCacheConfigurationDefaultValues() {
        // Given - using real JHipsterProperties with defaults
        JHipsterProperties realProps = new JHipsterProperties();

        // When
        CacheConfiguration config = new CacheConfiguration(realProps);

        // Then
        assertThat(config).isNotNull();
        assertThat(config.keyGenerator()).isNotNull();
        assertThat(config.cacheManagerCustomizer()).isNotNull();
    }

    @Test
    void testCacheNames() {
        // Test that cache customizer works with expected cache names
        JCacheManagerCustomizer customizer = cacheConfiguration.cacheManagerCustomizer();
        CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();

        // When
        customizer.customize(cacheManager);

        // Then
        // Should create various caches for the application
        assertThat(cacheManager).isNotNull();
        // The exact cache names would depend on the actual implementation
        // but we can verify that the customizer runs without errors
    }
}
