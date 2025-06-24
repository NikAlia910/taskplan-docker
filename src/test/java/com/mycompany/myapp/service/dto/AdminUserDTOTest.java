package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.security.AuthoritiesConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AdminUserDTO}.
 */
class AdminUserDTOTest {

    private static final String DEFAULT_LOGIN = "testuser";
    private static final String DEFAULT_FIRST_NAME = "John";
    private static final String DEFAULT_LAST_NAME = "Doe";
    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_IMAGE_URL = "http://example.com/image.jpg";
    private static final String DEFAULT_LANG_KEY = "en";
    private static final boolean DEFAULT_ACTIVATED = true;

    private Validator validator;
    private AdminUserDTO adminUserDTO;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        adminUserDTO = new AdminUserDTO();
        adminUserDTO.setLogin(DEFAULT_LOGIN);
        adminUserDTO.setFirstName(DEFAULT_FIRST_NAME);
        adminUserDTO.setLastName(DEFAULT_LAST_NAME);
        adminUserDTO.setEmail(DEFAULT_EMAIL);
        adminUserDTO.setImageUrl(DEFAULT_IMAGE_URL);
        adminUserDTO.setLangKey(DEFAULT_LANG_KEY);
        adminUserDTO.setActivated(DEFAULT_ACTIVATED);
        adminUserDTO.setAuthorities(Set.of(AuthoritiesConstants.USER));
    }

    @Test
    void constructor_withUser_shouldMapAllFields() {
        // Given
        User user = createTestUser();

        // When
        AdminUserDTO dto = new AdminUserDTO(user);

        // Then
        assertThat(dto.getId()).isEqualTo(user.getId());
        assertThat(dto.getLogin()).isEqualTo(user.getLogin());
        assertThat(dto.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(dto.getLastName()).isEqualTo(user.getLastName());
        assertThat(dto.getEmail()).isEqualTo(user.getEmail());
        assertThat(dto.isActivated()).isEqualTo(user.isActivated());
        assertThat(dto.getImageUrl()).isEqualTo(user.getImageUrl());
        assertThat(dto.getLangKey()).isEqualTo(user.getLangKey());
        assertThat(dto.getCreatedBy()).isEqualTo(user.getCreatedBy());
        assertThat(dto.getCreatedDate()).isEqualTo(user.getCreatedDate());
        assertThat(dto.getLastModifiedBy()).isEqualTo(user.getLastModifiedBy());
        assertThat(dto.getLastModifiedDate()).isEqualTo(user.getLastModifiedDate());
        assertThat(dto.getAuthorities()).containsExactlyInAnyOrder(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
    }

    @Test
    void emptyConstructor_shouldCreateEmptyDTO() {
        // When
        AdminUserDTO dto = new AdminUserDTO();

        // Then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getLogin()).isNull();
        assertThat(dto.getFirstName()).isNull();
        assertThat(dto.getLastName()).isNull();
        assertThat(dto.getEmail()).isNull();
        assertThat(dto.isActivated()).isFalse(); // default value
        assertThat(dto.getImageUrl()).isNull();
        assertThat(dto.getLangKey()).isNull();
        assertThat(dto.getCreatedBy()).isNull();
        assertThat(dto.getCreatedDate()).isNull();
        assertThat(dto.getLastModifiedBy()).isNull();
        assertThat(dto.getLastModifiedDate()).isNull();
        assertThat(dto.getAuthorities()).isNull();
    }

    @Test
    void validation_withValidData_shouldPassValidation() {
        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void validation_withBlankLogin_shouldFailValidation() {
        // Given
        adminUserDTO.setLogin("");

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("login");
        assertThat(violations.iterator().next().getMessage()).contains("must not be blank");
    }

    @Test
    void validation_withInvalidLoginPattern_shouldFailValidation() {
        // Given
        adminUserDTO.setLogin("invalid-login-with-uppercase-AND-special-chars!");

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("login");
    }

    @Test
    void validation_withTooLongLogin_shouldFailValidation() {
        // Given
        adminUserDTO.setLogin(RandomStringUtils.insecure().nextAlphanumeric(51)); // Max is 50

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("login");
    }

    @Test
    void validation_withTooLongFirstName_shouldFailValidation() {
        // Given
        adminUserDTO.setFirstName(RandomStringUtils.insecure().nextAlphanumeric(51)); // Max is 50

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("firstName");
    }

    @Test
    void validation_withTooLongLastName_shouldFailValidation() {
        // Given
        adminUserDTO.setLastName(RandomStringUtils.insecure().nextAlphanumeric(51)); // Max is 50

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("lastName");
    }

    @Test
    void validation_withInvalidEmail_shouldFailValidation() {
        // Given
        adminUserDTO.setEmail("invalid-email");

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    void validation_withTooLongEmail_shouldFailValidation() {
        // Given
        String longEmail = RandomStringUtils.insecure().nextAlphanumeric(250) + "@example.com"; // Max is 254
        adminUserDTO.setEmail(longEmail);

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("email");
    }

    @Test
    void validation_withTooLongImageUrl_shouldFailValidation() {
        // Given
        adminUserDTO.setImageUrl(RandomStringUtils.insecure().nextAlphanumeric(257)); // Max is 256

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("imageUrl");
    }

    @Test
    void validation_withInvalidLangKey_shouldFailValidation() {
        // Given
        adminUserDTO.setLangKey("x"); // Min is 2

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("langKey");
    }

    @Test
    void validation_withTooLongLangKey_shouldFailValidation() {
        // Given
        adminUserDTO.setLangKey("toolongkey"); // Max is 10

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("langKey");
    }

    @Test
    void gettersAndSetters_shouldWorkCorrectly() {
        // Given
        Long id = 1L;
        String login = "newlogin";
        String firstName = "Jane";
        String lastName = "Smith";
        String email = "jane@example.com";
        String imageUrl = "http://example.com/jane.jpg";
        boolean activated = false;
        String langKey = "fr";
        String createdBy = "admin";
        Instant createdDate = Instant.now();
        String lastModifiedBy = "user";
        Instant lastModifiedDate = Instant.now();
        Set<String> authorities = Set.of(AuthoritiesConstants.ADMIN);

        // When
        adminUserDTO.setId(id);
        adminUserDTO.setLogin(login);
        adminUserDTO.setFirstName(firstName);
        adminUserDTO.setLastName(lastName);
        adminUserDTO.setEmail(email);
        adminUserDTO.setImageUrl(imageUrl);
        adminUserDTO.setActivated(activated);
        adminUserDTO.setLangKey(langKey);
        adminUserDTO.setCreatedBy(createdBy);
        adminUserDTO.setCreatedDate(createdDate);
        adminUserDTO.setLastModifiedBy(lastModifiedBy);
        adminUserDTO.setLastModifiedDate(lastModifiedDate);
        adminUserDTO.setAuthorities(authorities);

        // Then
        assertThat(adminUserDTO.getId()).isEqualTo(id);
        assertThat(adminUserDTO.getLogin()).isEqualTo(login);
        assertThat(adminUserDTO.getFirstName()).isEqualTo(firstName);
        assertThat(adminUserDTO.getLastName()).isEqualTo(lastName);
        assertThat(adminUserDTO.getEmail()).isEqualTo(email);
        assertThat(adminUserDTO.getImageUrl()).isEqualTo(imageUrl);
        assertThat(adminUserDTO.isActivated()).isEqualTo(activated);
        assertThat(adminUserDTO.getLangKey()).isEqualTo(langKey);
        assertThat(adminUserDTO.getCreatedBy()).isEqualTo(createdBy);
        assertThat(adminUserDTO.getCreatedDate()).isEqualTo(createdDate);
        assertThat(adminUserDTO.getLastModifiedBy()).isEqualTo(lastModifiedBy);
        assertThat(adminUserDTO.getLastModifiedDate()).isEqualTo(lastModifiedDate);
        assertThat(adminUserDTO.getAuthorities()).isEqualTo(authorities);
    }

    @Test
    void toString_shouldContainAllFields() {
        // When
        String toString = adminUserDTO.toString();

        // Then
        assertThat(toString).contains(DEFAULT_LOGIN);
        assertThat(toString).contains(DEFAULT_FIRST_NAME);
        assertThat(toString).contains(DEFAULT_LAST_NAME);
        assertThat(toString).contains(DEFAULT_EMAIL);
        assertThat(toString).contains(DEFAULT_LANG_KEY);
        assertThat(toString).contains(String.valueOf(DEFAULT_ACTIVATED));
    }

    @Test
    void serialization_shouldBeSupported() {
        // Given - AdminUserDTO implements Serializable
        // When & Then - Should compile without issues
        assertThat(adminUserDTO).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void validation_withNullValues_shouldHandleGracefully() {
        // Given
        AdminUserDTO dto = new AdminUserDTO();
        dto.setLogin(null);
        dto.setEmail(null);

        // When
        Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("login"));
    }

    @Test
    void validation_withValidEmailFormats_shouldPass() {
        // Given & When & Then
        String[] validEmails = { "test@example.com", "user.name@example.org", "user+tag@example.co.uk", "123@example.com" };

        for (String email : validEmails) {
            adminUserDTO.setEmail(email);
            Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);
            assertThat(violations).filteredOn(v -> v.getPropertyPath().toString().equals("email")).isEmpty();
        }
    }

    @Test
    void validation_withInvalidEmailFormats_shouldFail() {
        // Given & When & Then
        String[] invalidEmails = { "invalid-email", "@example.com", "test@", "test..test@example.com", "test@example", "" };

        for (String email : invalidEmails) {
            adminUserDTO.setEmail(email);
            Set<ConstraintViolation<AdminUserDTO>> violations = validator.validate(adminUserDTO);
            assertThat(violations).filteredOn(v -> v.getPropertyPath().toString().equals("email")).isNotEmpty();
        }
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setLogin(DEFAULT_LOGIN);
        user.setFirstName(DEFAULT_FIRST_NAME);
        user.setLastName(DEFAULT_LAST_NAME);
        user.setEmail(DEFAULT_EMAIL);
        user.setActivated(DEFAULT_ACTIVATED);
        user.setImageUrl(DEFAULT_IMAGE_URL);
        user.setLangKey(DEFAULT_LANG_KEY);
        user.setCreatedBy("system");
        user.setCreatedDate(Instant.now());
        user.setLastModifiedBy("system");
        user.setLastModifiedDate(Instant.now());

        Authority userAuth = new Authority();
        userAuth.setName(AuthoritiesConstants.USER);
        Authority adminAuth = new Authority();
        adminAuth.setName(AuthoritiesConstants.ADMIN);

        user.getAuthorities().add(userAuth);
        user.getAuthorities().add(adminAuth);

        return user;
    }
}
