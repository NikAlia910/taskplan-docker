package com.mycompany.myapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.security.AuthoritiesConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.jhipster.config.JHipsterProperties;

/**
 * Integration tests for {@link SecurityConfiguration}.
 */
@IntegrationTest
@AutoConfigureMockMvc
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JHipsterProperties jHipsterProperties;

    @Test
    void testPasswordEncoder() {
        String password = "testPassword";
        String encoded = passwordEncoder.encode(password);

        assertThat(encoded).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, encoded)).isTrue();
        assertThat(passwordEncoder.matches("wrongPassword", encoded)).isFalse();
    }

    @Test
    @WithAnonymousUser
    void testPublicEndpointsAreAccessible() throws Exception {
        // Test static resources
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());

        // Test authentication endpoints
        mockMvc.perform(get("/api/authenticate")).andExpect(status().isOk());

        // Test registration endpoint
        mockMvc.perform(post("/api/register")).andExpect(status().isBadRequest()); // Bad request due to missing body, but accessible

        // Test account activation
        mockMvc.perform(get("/api/activate")).andExpect(status().isInternalServerError()); // Missing key param, but accessible

        // Test health endpoints
        mockMvc.perform(get("/management/health")).andExpect(status().isOk());

        mockMvc.perform(get("/management/info")).andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testProtectedEndpointsRequireAuthentication() throws Exception {
        // Test API endpoints require authentication
        mockMvc.perform(get("/api/account")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());

        // Test WebSocket endpoints require authentication
        mockMvc.perform(get("/websocket/tracker")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void testUserCanAccessUserEndpoints() throws Exception {
        mockMvc.perform(get("/api/account")).andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void testUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());

        mockMvc.perform(get("/management/health/diskSpace")).andExpect(status().isForbidden());

        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
    void testAdminCanAccessAllEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk());

        mockMvc.perform(get("/api/account")).andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks")).andExpect(status().isOk());

        mockMvc.perform(get("/management/health/diskSpace")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
    void testAdminCanAccessApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testStaticResourcesAreAccessible() throws Exception {
        // Test various static resources
        mockMvc.perform(get("/content/css/main.css")).andExpect(status().isNotFound()); // 404 is expected, but accessible

        mockMvc.perform(get("/app/home/home.css")).andExpect(status().isNotFound()); // 404 is expected, but accessible

        mockMvc.perform(get("/i18n/en.json")).andExpect(status().isNotFound()); // 404 is expected, but accessible
    }

    @Test
    void testSecurityHeadersConfiguration() throws Exception {
        mockMvc
            .perform(get("/"))
            .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
            .andExpect(header().exists("Permissions-Policy"));
    }

    @Test
    void testContentSecurityPolicyHeader() throws Exception {
        String expectedCSP = jHipsterProperties.getSecurity().getContentSecurityPolicy();

        mockMvc.perform(get("/")).andExpect(header().string("Content-Security-Policy", expectedCSP));
    }

    @Test
    @WithMockUser
    void testCorsConfiguration() throws Exception {
        // Test CORS headers are properly configured
        mockMvc
            .perform(options("/api/tasks").header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
