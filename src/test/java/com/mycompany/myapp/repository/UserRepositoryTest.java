package com.mycompany.myapp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link UserRepository}.
 */
@IntegrationTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private EntityManager entityManager;

    private User activeUser;
    private User inactiveUser;
    private User userWithResetKey;
    private User userWithActivationKey;
    private Authority userAuthority;
    private Authority adminAuthority;

    @BeforeEach
    void setUp() {
        // Clean up
        userRepository.deleteAll();
        authorityRepository.deleteAll();
        entityManager.flush();

        // Create authorities
        userAuthority = new Authority();
        userAuthority.setName(AuthoritiesConstants.USER);
        userAuthority = authorityRepository.saveAndFlush(userAuthority);

        adminAuthority = new Authority();
        adminAuthority.setName(AuthoritiesConstants.ADMIN);
        adminAuthority = authorityRepository.saveAndFlush(adminAuthority);

        // Create test users
        activeUser = createUser("activeuser", "active@example.com", true, null, null);
        activeUser.getAuthorities().add(userAuthority);
        activeUser = userRepository.saveAndFlush(activeUser);

        inactiveUser = createUser("inactiveuser", "inactive@example.com", false, "activationkey123", null);
        inactiveUser.getAuthorities().add(userAuthority);
        inactiveUser = userRepository.saveAndFlush(inactiveUser);

        userWithResetKey = createUser("resetuser", "reset@example.com", true, null, "resetkey456");
        userWithResetKey.setResetDate(Instant.now().minus(1, ChronoUnit.HOURS));
        userWithResetKey.getAuthorities().add(userAuthority);
        userWithResetKey = userRepository.saveAndFlush(userWithResetKey);

        userWithActivationKey = createUser("pendinguser", "pending@example.com", false, "pendingkey789", null);
        userWithActivationKey.setCreatedDate(Instant.now().minus(4, ChronoUnit.DAYS));
        userWithActivationKey.getAuthorities().add(userAuthority);
        userWithActivationKey = userRepository.saveAndFlush(userWithActivationKey);

        entityManager.clear();
    }

    private User createUser(String login, String email, boolean activated, String activationKey, String resetKey) {
        User user = new User();
        user.setLogin(login);
        user.setPassword("$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vFedUPOdxOjPWa"); // 60 character BCrypt hash
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setActivated(activated);
        user.setActivationKey(activationKey);
        user.setResetKey(resetKey);
        user.setLangKey("en");
        user.setCreatedBy("system");
        user.setCreatedDate(Instant.now());
        return user;
    }

    @Test
    void findOneByActivationKey_shouldReturnUserWithMatchingKey() {
        // When
        Optional<User> foundUser = userRepository.findOneByActivationKey("activationkey123");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getLogin()).isEqualTo("inactiveuser");
        assertThat(foundUser.get().isActivated()).isFalse();
    }

    @Test
    void findOneByActivationKey_shouldReturnEmptyForNonExistentKey() {
        // When
        Optional<User> foundUser = userRepository.findOneByActivationKey("nonexistentkey");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore_shouldReturnOldInactiveUsers() {
        // Given
        Instant threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS);

        // When
        List<User> oldInactiveUsers = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(threeDaysAgo);

        // Then
        assertThat(oldInactiveUsers).hasSize(1);
        assertThat(oldInactiveUsers.get(0).getLogin()).isEqualTo("pendinguser");
        assertThat(oldInactiveUsers.get(0).isActivated()).isFalse();
        assertThat(oldInactiveUsers.get(0).getActivationKey()).isNotNull();
    }

    @Test
    void findOneByResetKey_shouldReturnUserWithMatchingResetKey() {
        // When
        Optional<User> foundUser = userRepository.findOneByResetKey("resetkey456");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getLogin()).isEqualTo("resetuser");
        assertThat(foundUser.get().getResetDate()).isNotNull();
    }

    @Test
    void findOneByResetKey_shouldReturnEmptyForNonExistentKey() {
        // When
        Optional<User> foundUser = userRepository.findOneByResetKey("nonexistentresetkey");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void findOneByEmailIgnoreCase_shouldReturnUserWithMatchingEmail() {
        // When
        Optional<User> foundUser = userRepository.findOneByEmailIgnoreCase("ACTIVE@EXAMPLE.COM");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getLogin()).isEqualTo("activeuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("active@example.com");
    }

    @Test
    void findOneByEmailIgnoreCase_shouldReturnEmptyForNonExistentEmail() {
        // When
        Optional<User> foundUser = userRepository.findOneByEmailIgnoreCase("nonexistent@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void findOneByLogin_shouldReturnUserWithMatchingLogin() {
        // When
        Optional<User> foundUser = userRepository.findOneByLogin("activeuser");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getLogin()).isEqualTo("activeuser");
        assertThat(foundUser.get().isActivated()).isTrue();
    }

    @Test
    void findOneByLogin_shouldReturnEmptyForNonExistentLogin() {
        // When
        Optional<User> foundUser = userRepository.findOneByLogin("nonexistentuser");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void findOneWithAuthoritiesByLogin_shouldReturnUserWithAuthorities() {
        // When
        Optional<User> foundUser = userRepository.findOneWithAuthoritiesByLogin("activeuser");

        // Then
        assertThat(foundUser).isPresent();
        User user = foundUser.get();
        assertThat(user.getLogin()).isEqualTo("activeuser");

        // Clear entity manager to test eager loading
        entityManager.clear();

        // Authorities should be loaded eagerly
        assertThat(user.getAuthorities()).isNotEmpty();
        assertThat(user.getAuthorities()).hasSize(1);
        assertThat(user.getAuthorities().iterator().next().getName()).isEqualTo(AuthoritiesConstants.USER);
    }

    @Test
    void findOneWithAuthoritiesByEmailIgnoreCase_shouldReturnUserWithAuthorities() {
        // When
        Optional<User> foundUser = userRepository.findOneWithAuthoritiesByEmailIgnoreCase("ACTIVE@EXAMPLE.COM");

        // Then
        assertThat(foundUser).isPresent();
        User user = foundUser.get();
        assertThat(user.getEmail()).isEqualTo("active@example.com");

        // Clear entity manager to test eager loading
        entityManager.clear();

        // Authorities should be loaded eagerly
        assertThat(user.getAuthorities()).isNotEmpty();
        assertThat(user.getAuthorities()).hasSize(1);
    }

    @Test
    void findAllByIdNotNullAndActivatedIsTrue_shouldReturnOnlyActivatedUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> activatedUsers = userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable);

        // Then
        assertThat(activatedUsers.getContent()).hasSize(2); // activeUser and userWithResetKey
        assertThat(activatedUsers.getContent())
            .allMatch(User::isActivated)
            .extracting(User::getLogin)
            .containsExactlyInAnyOrder("activeuser", "resetuser");
    }

    @Test
    void findAll_withPageable_shouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<User> users = userRepository.findAll(pageable);

        // Then
        assertThat(users.getContent()).hasSize(2);
        assertThat(users.getTotalElements()).isEqualTo(4);
        assertThat(users.getTotalPages()).isEqualTo(2);
        assertThat(users.hasNext()).isTrue();
    }

    @Test
    void save_shouldPersistUserWithAuthorities() {
        // Given
        User newUser = createUser("newuser", "new@example.com", true, null, null);
        newUser.getAuthorities().add(userAuthority);
        newUser.getAuthorities().add(adminAuthority);

        // When
        User savedUser = userRepository.save(newUser);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> foundUser = userRepository.findOneWithAuthoritiesByLogin("newuser");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getAuthorities()).hasSize(2);
        assertThat(foundUser.get().getAuthorities())
            .extracting(Authority::getName)
            .containsExactlyInAnyOrder(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
    }

    @Test
    void delete_shouldRemoveUserAndRelationships() {
        // Given
        Long userId = activeUser.getId();
        assertThat(userRepository.findById(userId)).isPresent();

        // When
        userRepository.deleteById(userId);
        entityManager.flush();

        // Then
        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(userRepository.findOneByLogin("activeuser")).isEmpty();
    }

    @Test
    void count_shouldReturnCorrectUserCount() {
        // When
        long count = userRepository.count();

        // Then
        assertThat(count).isEqualTo(4);
    }

    @Test
    void existsByLogin_shouldReturnTrueForExistingUser() {
        // When
        boolean exists = userRepository.findOneByLogin("activeuser").isPresent();

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByLogin_shouldReturnFalseForNonExistentUser() {
        // When
        boolean exists = userRepository.findOneByLogin("nonexistentuser").isPresent();

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailIgnoreCase_shouldReturnTrueForExistingEmail() {
        // When
        boolean exists = userRepository.findOneByEmailIgnoreCase("ACTIVE@EXAMPLE.COM").isPresent();

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailIgnoreCase_shouldReturnFalseForNonExistentEmail() {
        // When
        boolean exists = userRepository.findOneByEmailIgnoreCase("nonexistent@example.com").isPresent();

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void updateUser_shouldModifyExistingUser() {
        // Given
        User user = userRepository.findOneByLogin("activeuser").orElseThrow();
        user.setFirstName("Updated");
        user.setLastName("Name");
        user.setEmail("updated@example.com");

        // When
        User updatedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> foundUser = userRepository.findOneByLogin("activeuser");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getFirstName()).isEqualTo("Updated");
        assertThat(foundUser.get().getLastName()).isEqualTo("Name");
        assertThat(foundUser.get().getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void repository_shouldHandleConcurrentModifications() {
        // Given
        User user1 = userRepository.findOneByLogin("activeuser").orElseThrow();
        User user2 = userRepository.findOneByLogin("activeuser").orElseThrow();

        // When
        user1.setFirstName("FirstUpdate");
        user2.setLastName("SecondUpdate");

        userRepository.save(user1);
        userRepository.save(user2);
        entityManager.flush();

        // Then
        User finalUser = userRepository.findOneByLogin("activeuser").orElseThrow();
        // The last save should win
        assertThat(finalUser.getLastName()).isEqualTo("SecondUpdate");
    }
}
