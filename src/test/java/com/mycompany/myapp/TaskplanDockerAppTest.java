package com.mycompany.myapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mycompany.myapp.config.AsyncSyncConfiguration;
import com.mycompany.myapp.config.EmbeddedSQL;
import com.mycompany.myapp.config.JacksonConfiguration;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import tech.jhipster.config.JHipsterConstants;

/**
 * Unit tests for the {@link TaskplanDockerApp} main class.
 */
@SpringBootTest(classes = { TaskplanDockerApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class })
@EmbeddedSQL
@AutoConfigureMockMvc
class TaskplanDockerAppTest {

    @Autowired
    private Environment env;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testApplicationStarts() {
        // Test that the Spring context loads successfully
        assertThat(env).isNotNull();
    }

    @Test
    void testDefaultProfile() {
        String[] profiles = env.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = env.getDefaultProfiles();
        }
        assertThat(profiles).contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT);
    }

    @Test
    void testApplicationContext() throws Exception {
        // Test that the application responds to health check
        mockMvc.perform(get("/management/health")).andExpect(status().isOk());
    }

    @Test
    void testTaskplanDockerAppAnnotations() {
        Class<TaskplanDockerApp> clazz = TaskplanDockerApp.class;

        // Check that the main class has the necessary Spring Boot annotations
        Annotation[] annotations = clazz.getAnnotations();
        assertThat(annotations).isNotEmpty();

        // Check for @SpringBootApplication annotation
        boolean hasSpringBootApplication = Arrays.stream(annotations).anyMatch(annotation ->
            annotation.annotationType().getSimpleName().equals("SpringBootApplication")
        );
        assertThat(hasSpringBootApplication).isTrue();
    }

    @Test
    void testMainMethodExists() throws NoSuchMethodException {
        // Verify that the main method exists
        TaskplanDockerApp.class.getDeclaredMethod("main", String[].class);
    }

    @Test
    void testApplicationCanBeConstructed() {
        // Test that the application can be constructed with an Environment
        TaskplanDockerApp app = new TaskplanDockerApp(env);
        assertThat(app).isNotNull();
    }

    @Test
    void testInitApplicationMethod() throws NoSuchMethodException {
        // Test that the initApplication method exists
        TaskplanDockerApp.class.getDeclaredMethod("initApplication");
    }

    @Test
    void testApplicationProperties() {
        // Test that basic application properties are loaded
        String applicationName = env.getProperty("spring.application.name");
        assertThat(StringUtils.isNotBlank(applicationName)).isTrue();
    }

    @Test
    void testDatabaseConfiguration() {
        // Test that database is configured
        String datasourceUrl = env.getProperty("spring.datasource.url");
        assertThat(datasourceUrl).isNotNull();
    }

    @Test
    void testSecurityConfiguration() {
        // Test that security is configured
        String jwtSecret = env.getProperty("jhipster.security.authentication.jwt.secret");
        // In test environment, this should be configured
        assertThat(jwtSecret).isNotNull();
    }

    @Test
    void testLoggingConfiguration() {
        // Test that logging is configured
        String loggingLevel = env.getProperty("logging.level.com.mycompany.myapp");
        // Should have some logging configuration
        assertThat(env.getProperty("logging.level.root")).isNotNull();
    }
}
