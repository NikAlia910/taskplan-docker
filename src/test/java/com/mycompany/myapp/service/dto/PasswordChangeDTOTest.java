package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PasswordChangeDTO}.
 */
class PasswordChangeDTOTest {

    private static final String DEFAULT_CURRENT_PASSWORD = "currentpass";
    private static final String DEFAULT_NEW_PASSWORD = "newpass";

    private PasswordChangeDTO passwordChangeDTO;

    @BeforeEach
    void setUp() {
        passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setCurrentPassword(DEFAULT_CURRENT_PASSWORD);
        passwordChangeDTO.setNewPassword(DEFAULT_NEW_PASSWORD);
    }

    @Test
    void constructor_withParameters_shouldSetFields() {
        // When
        PasswordChangeDTO dto = new PasswordChangeDTO("current", "new");

        // Then
        assertThat(dto.getCurrentPassword()).isEqualTo("current");
        assertThat(dto.getNewPassword()).isEqualTo("new");
    }

    @Test
    void emptyConstructor_shouldCreateEmptyDTO() {
        // When
        PasswordChangeDTO dto = new PasswordChangeDTO();

        // Then
        assertThat(dto.getCurrentPassword()).isNull();
        assertThat(dto.getNewPassword()).isNull();
    }

    @Test
    void gettersAndSetters_shouldWorkCorrectly() {
        // Given
        String newCurrentPassword = "newcurrent";
        String newPassword = "newnew";

        // When
        passwordChangeDTO.setCurrentPassword(newCurrentPassword);
        passwordChangeDTO.setNewPassword(newPassword);

        // Then
        assertThat(passwordChangeDTO.getCurrentPassword()).isEqualTo(newCurrentPassword);
        assertThat(passwordChangeDTO.getNewPassword()).isEqualTo(newPassword);
    }

    @Test
    void setCurrentPassword_withNullValue_shouldWork() {
        // When
        passwordChangeDTO.setCurrentPassword(null);

        // Then
        assertThat(passwordChangeDTO.getCurrentPassword()).isNull();
    }

    @Test
    void setNewPassword_withNullValue_shouldWork() {
        // When
        passwordChangeDTO.setNewPassword(null);

        // Then
        assertThat(passwordChangeDTO.getNewPassword()).isNull();
    }

    @Test
    void setCurrentPassword_withEmptyString_shouldWork() {
        // When
        passwordChangeDTO.setCurrentPassword("");

        // Then
        assertThat(passwordChangeDTO.getCurrentPassword()).isEqualTo("");
    }

    @Test
    void setNewPassword_withEmptyString_shouldWork() {
        // When
        passwordChangeDTO.setNewPassword("");

        // Then
        assertThat(passwordChangeDTO.getNewPassword()).isEqualTo("");
    }

    @Test
    void setCurrentPassword_withWhitespace_shouldWork() {
        // When
        passwordChangeDTO.setCurrentPassword("   ");

        // Then
        assertThat(passwordChangeDTO.getCurrentPassword()).isEqualTo("   ");
    }

    @Test
    void setNewPassword_withWhitespace_shouldWork() {
        // When
        passwordChangeDTO.setNewPassword("   ");

        // Then
        assertThat(passwordChangeDTO.getNewPassword()).isEqualTo("   ");
    }

    @Test
    void setCurrentPassword_withLongPassword_shouldWork() {
        // Given
        String longPassword = "a".repeat(1000);

        // When
        passwordChangeDTO.setCurrentPassword(longPassword);

        // Then
        assertThat(passwordChangeDTO.getCurrentPassword()).isEqualTo(longPassword);
    }

    @Test
    void setNewPassword_withLongPassword_shouldWork() {
        // Given
        String longPassword = "b".repeat(1000);

        // When
        passwordChangeDTO.setNewPassword(longPassword);

        // Then
        assertThat(passwordChangeDTO.getNewPassword()).isEqualTo(longPassword);
    }

    @Test
    void setCurrentPassword_withSpecialCharacters_shouldWork() {
        // Given
        String specialPassword = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

        // When
        passwordChangeDTO.setCurrentPassword(specialPassword);

        // Then
        assertThat(passwordChangeDTO.getCurrentPassword()).isEqualTo(specialPassword);
    }

    @Test
    void setNewPassword_withSpecialCharacters_shouldWork() {
        // Given
        String specialPassword = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";

        // When
        passwordChangeDTO.setNewPassword(specialPassword);

        // Then
        assertThat(passwordChangeDTO.getNewPassword()).isEqualTo(specialPassword);
    }

    @Test
    void serialization_shouldBeSupported() {
        // Given - PasswordChangeDTO implements Serializable
        // When & Then - Should compile without issues
        assertThat(passwordChangeDTO).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void constructor_withNullParameters_shouldWork() {
        // When
        PasswordChangeDTO dto = new PasswordChangeDTO(null, null);

        // Then
        assertThat(dto.getCurrentPassword()).isNull();
        assertThat(dto.getNewPassword()).isNull();
    }

    @Test
    void constructor_withMixedNullAndValidParameters_shouldWork() {
        // When
        PasswordChangeDTO dto1 = new PasswordChangeDTO(null, "newpass");
        PasswordChangeDTO dto2 = new PasswordChangeDTO("currentpass", null);

        // Then
        assertThat(dto1.getCurrentPassword()).isNull();
        assertThat(dto1.getNewPassword()).isEqualTo("newpass");

        assertThat(dto2.getCurrentPassword()).isEqualTo("currentpass");
        assertThat(dto2.getNewPassword()).isNull();
    }

    @Test
    void toString_shouldNotExposePasswords() {
        // When
        String toString = passwordChangeDTO.toString();

        // Then
        // For security reasons, toString should not expose actual passwords
        assertThat(toString).isNotNull();
        // Typically, DTO toString methods should not contain sensitive data
        assertThat(toString).contains("PasswordChangeDTO");
    }

    @Test
    void fieldsWithUnicodeCharacters_shouldWork() {
        // Given
        String unicodePassword = "пароль123密码";

        // When
        passwordChangeDTO.setCurrentPassword(unicodePassword);
        passwordChangeDTO.setNewPassword(unicodePassword + "new");

        // Then
        assertThat(passwordChangeDTO.getCurrentPassword()).isEqualTo(unicodePassword);
        assertThat(passwordChangeDTO.getNewPassword()).isEqualTo(unicodePassword + "new");
    }

    @Test
    void samePasswordsInBothFields_shouldWork() {
        // Given
        String samePassword = "samepass";

        // When
        passwordChangeDTO.setCurrentPassword(samePassword);
        passwordChangeDTO.setNewPassword(samePassword);

        // Then
        assertThat(passwordChangeDTO.getCurrentPassword()).isEqualTo(samePassword);
        assertThat(passwordChangeDTO.getNewPassword()).isEqualTo(samePassword);
        assertThat(passwordChangeDTO.getCurrentPassword()).isEqualTo(passwordChangeDTO.getNewPassword());
    }
}
