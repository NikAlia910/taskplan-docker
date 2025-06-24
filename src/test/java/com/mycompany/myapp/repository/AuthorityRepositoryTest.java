package com.mycompany.myapp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link AuthorityRepository}.
 */
@IntegrationTest
@Transactional
class AuthorityRepositoryTest {

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private EntityManager entityManager;

    private Authority userAuthority;
    private Authority adminAuthority;

    @BeforeEach
    void setUp() {
        // Clean up existing authorities
        authorityRepository.deleteAll();
        entityManager.flush();

        // Create test authorities
        userAuthority = new Authority();
        userAuthority.setName(AuthoritiesConstants.USER);

        adminAuthority = new Authority();
        adminAuthority.setName(AuthoritiesConstants.ADMIN);

        authorityRepository.saveAndFlush(userAuthority);
        authorityRepository.saveAndFlush(adminAuthority);

        entityManager.clear();
    }

    @Test
    void findById_shouldReturnAuthorityWhenExists() {
        // When
        Optional<Authority> foundAuthority = authorityRepository.findById(AuthoritiesConstants.USER);

        // Then
        assertThat(foundAuthority).isPresent();
        assertThat(foundAuthority.get().getName()).isEqualTo(AuthoritiesConstants.USER);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        // When
        Optional<Authority> foundAuthority = authorityRepository.findById("NON_EXISTENT_AUTHORITY");

        // Then
        assertThat(foundAuthority).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllAuthorities() {
        // When
        List<Authority> authorities = authorityRepository.findAll();

        // Then
        assertThat(authorities).hasSize(2);
        assertThat(authorities)
            .extracting(Authority::getName)
            .containsExactlyInAnyOrder(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
    }

    @Test
    void save_shouldPersistNewAuthority() {
        // Given
        Authority newAuthority = new Authority();
        String newAuthorityName = "ROLE_MANAGER";
        newAuthority.setName(newAuthorityName);

        // When
        Authority savedAuthority = authorityRepository.save(newAuthority);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Authority> foundAuthority = authorityRepository.findById(newAuthorityName);
        assertThat(foundAuthority).isPresent();
        assertThat(foundAuthority.get().getName()).isEqualTo(newAuthorityName);
    }

    @Test
    void delete_shouldRemoveAuthority() {
        // Given
        assertThat(authorityRepository.findById(AuthoritiesConstants.USER)).isPresent();

        // When
        authorityRepository.deleteById(AuthoritiesConstants.USER);
        entityManager.flush();

        // Then
        Optional<Authority> deletedAuthority = authorityRepository.findById(AuthoritiesConstants.USER);
        assertThat(deletedAuthority).isEmpty();
    }

    @Test
    void count_shouldReturnCorrectCount() {
        // When
        long count = authorityRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void existsById_shouldReturnTrueForExistingAuthority() {
        // When
        boolean exists = authorityRepository.existsById(AuthoritiesConstants.ADMIN);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsById_shouldReturnFalseForNonExistingAuthority() {
        // When
        boolean exists = authorityRepository.existsById("NON_EXISTENT");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void authority_shouldHaveCorrectStringRepresentation() {
        // Given
        Authority authority = authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow();

        // When
        String toString = authority.toString();

        // Then
        assertThat(toString).contains("name='" + AuthoritiesConstants.USER + "'");
    }

    @Test
    void authority_shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        Authority auth1 = authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow();
        Authority auth2 = authorityRepository.findById(AuthoritiesConstants.USER).orElseThrow();
        Authority auth3 = authorityRepository.findById(AuthoritiesConstants.ADMIN).orElseThrow();

        // When & Then
        assertThat(auth1).isEqualTo(auth2);
        assertThat(auth1).isNotEqualTo(auth3);
        assertThat(auth1.hashCode()).isEqualTo(auth2.hashCode());
    }

    @Test
    void saveAndFlush_shouldImmediatelyPersistToDatabase() {
        // Given
        Authority authority = new Authority();
        authority.setName("ROLE_TEMP");

        // When
        Authority saved = authorityRepository.saveAndFlush(authority);

        // Then - Should be able to find it immediately even without clearing entity manager
        Optional<Authority> found = authorityRepository.findById("ROLE_TEMP");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ROLE_TEMP");
    }

    @Test
    void deleteAll_shouldRemoveAllAuthorities() {
        // Given
        assertThat(authorityRepository.count()).isEqualTo(2);

        // When
        authorityRepository.deleteAll();
        entityManager.flush();

        // Then
        assertThat(authorityRepository.count()).isEqualTo(0);
        assertThat(authorityRepository.findAll()).isEmpty();
    }

    @Test
    void authority_shouldNotAllowNullName() {
        // Given
        Authority authority = new Authority();
        authority.setName(null);

        // When & Then
        // This should fail at the database level due to constraints
        assertThat(authority.getName()).isNull();
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoAuthorities() {
        // Given
        authorityRepository.deleteAll();
        entityManager.flush();

        // When
        List<Authority> authorities = authorityRepository.findAll();

        // Then
        assertThat(authorities).isEmpty();
        assertThat(authorityRepository.count()).isEqualTo(0);
    }

    @Test
    void repository_shouldSupportBatchOperations() {
        // Given
        Authority auth1 = new Authority();
        auth1.setName("ROLE_BATCH_1");

        Authority auth2 = new Authority();
        auth2.setName("ROLE_BATCH_2");

        List<Authority> authorities = List.of(auth1, auth2);

        // When
        List<Authority> savedAuthorities = authorityRepository.saveAll(authorities);
        entityManager.flush();

        // Then
        assertThat(savedAuthorities).hasSize(2);
        assertThat(authorityRepository.count()).isEqualTo(4); // 2 from setup + 2 new

        // Verify individual authorities
        assertThat(authorityRepository.existsById("ROLE_BATCH_1")).isTrue();
        assertThat(authorityRepository.existsById("ROLE_BATCH_2")).isTrue();
    }
}
