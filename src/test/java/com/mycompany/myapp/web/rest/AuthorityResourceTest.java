package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.repository.AuthorityRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AuthorityResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebMvc
@WithMockUser(authorities = "ROLE_ADMIN")
class AuthorityResourceTest {

    private static final String DEFAULT_NAME = "ROLE_TEST";
    private static final String UPDATED_NAME = "ROLE_UPDATED";
    private static final String ENTITY_API_URL = "/api/authorities";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAuthorityMockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Authority authority;

    /**
     * Create an entity for this test.
     */
    public static Authority createEntity(EntityManager em) {
        Authority authority = new Authority();
        authority.setName(DEFAULT_NAME);
        return authority;
    }

    @BeforeEach
    public void initTest() {
        authority = createEntity(em);
    }

    @Test
    @Transactional
    void createAuthority() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();

        // Create the Authority
        restAuthorityMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authority))
            )
            .andExpect(status().isCreated());

        // Validate the Authority in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        Authority testAuthority = authorityRepository.findById(authority.getName()).orElseThrow();
        assertThat(testAuthority.getName()).isEqualTo(DEFAULT_NAME);
    }

    @Test
    @Transactional
    void createAuthorityWithExistingId() throws Exception {
        // Create the Authority with an existing ID
        authority.setName("ROLE_EXISTING");
        authorityRepository.saveAndFlush(authority);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAuthorityMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authority))
            )
            .andExpect(status().isBadRequest());

        // Validate the Authority in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        authority.setName(null);

        // Create the Authority, which fails.

        restAuthorityMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authority))
            )
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAuthorities() throws Exception {
        // Initialize the database
        authorityRepository.saveAndFlush(authority);

        // Get all the authorityList
        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL + "?sort=name,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)));
    }

    @Test
    @Transactional
    void getAuthority() throws Exception {
        // Initialize the database
        authorityRepository.saveAndFlush(authority);

        // Get the authority
        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL_ID, authority.getName()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME));
    }

    @Test
    @Transactional
    void getNonExistingAuthority() throws Exception {
        // Get the authority
        restAuthorityMockMvc.perform(get(ENTITY_API_URL_ID, "ROLE_NONEXISTENT")).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void deleteAuthority() throws Exception {
        // Initialize the database
        authorityRepository.saveAndFlush(authority);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the authority
        restAuthorityMockMvc
            .perform(delete(ENTITY_API_URL_ID, authority.getName()).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_USER")
    void createAuthorityAsUser() throws Exception {
        // Try to create authority as regular user (should fail)
        restAuthorityMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authority))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_USER")
    void getAllAuthoritiesAsUser() throws Exception {
        // Try to get authorities as regular user (should fail)
        restAuthorityMockMvc.perform(get(ENTITY_API_URL)).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_USER")
    void getAuthorityAsUser() throws Exception {
        // Initialize the database
        authorityRepository.saveAndFlush(authority);

        // Try to get authority as regular user (should fail)
        restAuthorityMockMvc.perform(get(ENTITY_API_URL_ID, authority.getName())).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "ROLE_USER")
    void deleteAuthorityAsUser() throws Exception {
        // Initialize the database
        authorityRepository.saveAndFlush(authority);

        // Try to delete authority as regular user (should fail)
        restAuthorityMockMvc.perform(delete(ENTITY_API_URL_ID, authority.getName()).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void createAuthorityWithEmptyName() throws Exception {
        // Create authority with empty name
        authority.setName("");

        restAuthorityMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authority))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void createAuthorityWithSpecialCharacters() throws Exception {
        // Create authority with special characters in name
        authority.setName("ROLE_TEST_!@#$%");

        long databaseSizeBeforeCreate = getRepositoryCount();

        restAuthorityMockMvc
            .perform(
                post(ENTITY_API_URL)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authority))
            )
            .andExpect(status().isCreated());

        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllAuthoritiesEmpty() throws Exception {
        // Test when no authorities exist
        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    void deleteNonExistingAuthority() throws Exception {
        // Try to delete non-existing authority
        restAuthorityMockMvc.perform(delete(ENTITY_API_URL_ID, "ROLE_NONEXISTENT").with(csrf())).andExpect(status().isNoContent()); // Should still return 204 even if doesn't exist
    }

    @Test
    @Transactional
    void createMultipleAuthorities() throws Exception {
        // Create multiple authorities to test list functionality
        Authority authority1 = new Authority();
        authority1.setName("ROLE_TEST_1");

        Authority authority2 = new Authority();
        authority2.setName("ROLE_TEST_2");

        authorityRepository.saveAndFlush(authority1);
        authorityRepository.saveAndFlush(authority2);

        restAuthorityMockMvc
            .perform(get(ENTITY_API_URL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].name").value(hasItem("ROLE_TEST_1")))
            .andExpect(jsonPath("$.[*].name").value(hasItem("ROLE_TEST_2")));
    }

    protected long getRepositoryCount() {
        return authorityRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }
}
