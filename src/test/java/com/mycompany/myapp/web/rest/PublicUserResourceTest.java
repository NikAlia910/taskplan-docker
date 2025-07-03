package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link PublicUserResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebMvc
class PublicUserResourceTest {

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String DEFAULT_EMAIL = "johndoe@localhost";
    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String DEFAULT_LASTNAME = "doe";
    private static final String DEFAULT_LANGKEY = "en";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc restUserMockMvc;

    private User user;

    @BeforeEach
    public void setup() {
        cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).clear();
        cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE).clear();
    }

    @BeforeEach
    public void initTest() {
        user = createEntity(em);
        user.setLogin(DEFAULT_LOGIN);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setLangKey(DEFAULT_LANGKEY);
    }

    /**
     * Create an entity for this test.
     */
    public static User createEntity(EntityManager em) {
        User user = new User();
        user.setLogin("johndoe");
        user.setEmail("johndoe@localhost");
        user.setActivated(true);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setLangKey("en");
        return user;
    }

    @Test
    @Transactional
    void getAllPublicUsers() throws Exception {
        // Initialize the database
        userRepository.saveAndFlush(user);

        // Get all the users
        restUserMockMvc
            .perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
            .andExpect(jsonPath("$.[*].email").doesNotExist())
            .andExpect(jsonPath("$.[*].firstName").doesNotExist())
            .andExpect(jsonPath("$.[*].lastName").doesNotExist())
            .andExpect(jsonPath("$.[*].langKey").doesNotExist())
            .andExpect(jsonPath("$.[*].imageUrl").doesNotExist());
    }

    @Test
    @Transactional
    void getAllPublicUsersWithPagination() throws Exception {
        // Initialize the database with multiple users
        userRepository.saveAndFlush(user);

        User secondUser = new User();
        secondUser.setLogin("janedoe");
        secondUser.setEmail("janedoe@localhost");
        secondUser.setActivated(true);
        userRepository.saveAndFlush(secondUser);

        // Get users with pagination
        restUserMockMvc
            .perform(get("/api/users?page=0&size=1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().string("X-Total-Count", "2"))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @Transactional
    void getAllPublicUsersSortedByLogin() throws Exception {
        // Initialize the database with multiple users
        userRepository.saveAndFlush(user);

        User secondUser = new User();
        secondUser.setLogin("alice");
        secondUser.setEmail("alice@localhost");
        secondUser.setActivated(true);
        userRepository.saveAndFlush(secondUser);

        // Get users sorted by login
        restUserMockMvc
            .perform(get("/api/users?sort=login,desc").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[0].login").value(DEFAULT_LOGIN))
            .andExpect(jsonPath("$.[1].login").value("alice"));
    }

    @Test
    @Transactional
    void getAllPublicUsersDoesNotShowInactiveUsers() throws Exception {
        // Initialize the database with active and inactive users
        userRepository.saveAndFlush(user);

        User inactiveUser = new User();
        inactiveUser.setLogin("inactive");
        inactiveUser.setEmail("inactive@localhost");
        inactiveUser.setActivated(false);
        userRepository.saveAndFlush(inactiveUser);

        // Get all users should only return active users
        restUserMockMvc
            .perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
            .andExpect(jsonPath("$.[*].login").value(not(hasItem("inactive"))));
    }

    @Test
    @Transactional
    void getAllPublicUsersWithAuthorities() throws Exception {
        // Create authority
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorityRepository.saveAndFlush(authority);

        // Set authority to user
        user.setAuthorities(Set.of(authority));
        userRepository.saveAndFlush(user);

        // Get all users - authorities should not be exposed
        restUserMockMvc
            .perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
            .andExpect(jsonPath("$.[*].authorities").doesNotExist());
    }

    @Test
    @Transactional
    void getAllPublicUsersEmpty() throws Exception {
        // Test empty user list
        restUserMockMvc
            .perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    void getAllPublicUsersWithDifferentPageSizes() throws Exception {
        // Create multiple users
        for (int i = 0; i < 5; i++) {
            User testUser = new User();
            testUser.setLogin("user" + i);
            testUser.setEmail("user" + i + "@localhost");
            testUser.setActivated(true);
            userRepository.saveAndFlush(testUser);
        }

        // Test different page sizes
        restUserMockMvc
            .perform(get("/api/users?size=2").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @Transactional
    void getAllPublicUsersWithSpecialCharacters() throws Exception {
        // Create user with special characters
        User specialUser = new User();
        specialUser.setLogin("user.with-special_chars");
        specialUser.setEmail("special@localhost");
        specialUser.setActivated(true);
        userRepository.saveAndFlush(specialUser);

        restUserMockMvc
            .perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[*].login").value(hasItem("user.with-special_chars")));
    }

    @Test
    @Transactional
    void getAllPublicUsersWithMaxResults() throws Exception {
        // Create many users to test pagination limits
        for (int i = 0; i < 25; i++) {
            User testUser = new User();
            testUser.setLogin("testuser" + i);
            testUser.setEmail("testuser" + i + "@localhost");
            testUser.setActivated(true);
            userRepository.saveAndFlush(testUser);
        }

        // Test default page size limit
        restUserMockMvc
            .perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(20))); // Default page size should be 20
    }

    @Test
    @Transactional
    void getAllPublicUsersWithInvalidPageParameter() throws Exception {
        userRepository.saveAndFlush(user);

        // Test invalid page parameter
        restUserMockMvc
            .perform(get("/api/users?page=-1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()) // Should handle gracefully
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Transactional
    void getAllPublicUsersWithInvalidSizeParameter() throws Exception {
        userRepository.saveAndFlush(user);

        // Test invalid size parameter
        restUserMockMvc
            .perform(get("/api/users?size=0").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()) // Should handle gracefully
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Transactional
    void getAllPublicUsersWithInvalidSortParameter() throws Exception {
        userRepository.saveAndFlush(user);

        // Test invalid sort parameter
        restUserMockMvc
            .perform(get("/api/users?sort=invalidfield").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()) // Should handle gracefully
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Transactional
    @WithMockUser
    void getAllPublicUsersAsAuthenticatedUser() throws Exception {
        userRepository.saveAndFlush(user);

        // Authenticated users should still only see public data
        restUserMockMvc
            .perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[*].login").value(hasItem(DEFAULT_LOGIN)))
            .andExpect(jsonPath("$.[*].email").doesNotExist()); // Email should not be exposed
    }

    @Test
    @Transactional
    void getAllPublicUsersCheckResponseHeaders() throws Exception {
        userRepository.saveAndFlush(user);

        restUserMockMvc
            .perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header().string("X-Total-Count", "1"));
    }

    @Test
    @Transactional
    void getAllPublicUsersWithMultiplePages() throws Exception {
        // Create users for multiple pages
        for (int i = 0; i < 25; i++) {
            User testUser = new User();
            testUser.setLogin("user" + String.format("%02d", i));
            testUser.setEmail("user" + i + "@localhost");
            testUser.setActivated(true);
            userRepository.saveAndFlush(testUser);
        }

        // Test first page
        restUserMockMvc
            .perform(get("/api/users?page=0&size=10").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "25"))
            .andExpect(jsonPath("$", hasSize(10)));

        // Test second page
        restUserMockMvc
            .perform(get("/api/users?page=1&size=10").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "25"))
            .andExpect(jsonPath("$", hasSize(10)));

        // Test last page
        restUserMockMvc
            .perform(get("/api/users?page=2&size=10").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "25"))
            .andExpect(jsonPath("$", hasSize(5)));
    }
}
