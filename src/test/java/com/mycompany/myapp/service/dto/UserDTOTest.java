package com.mycompany.myapp.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UserDTO}.
 */
class UserDTOTest {

    private static final Long DEFAULT_ID = 1L;
    private static final String DEFAULT_LOGIN = "testuser";

    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userDTO = new UserDTO();
        userDTO.setId(DEFAULT_ID);
        userDTO.setLogin(DEFAULT_LOGIN);
    }

    @Test
    void constructor_withUser_shouldMapFields() {
        // Given
        User user = new User();
        user.setId(123L);
        user.setLogin("userlogin");

        // When
        UserDTO dto = new UserDTO(user);

        // Then
        assertThat(dto.getId()).isEqualTo(123L);
        assertThat(dto.getLogin()).isEqualTo("userlogin");
    }

    @Test
    void emptyConstructor_shouldCreateEmptyDTO() {
        // When
        UserDTO dto = new UserDTO();

        // Then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getLogin()).isNull();
    }

    @Test
    void gettersAndSetters_shouldWorkCorrectly() {
        // Given
        Long newId = 999L;
        String newLogin = "newuser";

        // When
        userDTO.setId(newId);
        userDTO.setLogin(newLogin);

        // Then
        assertThat(userDTO.getId()).isEqualTo(newId);
        assertThat(userDTO.getLogin()).isEqualTo(newLogin);
    }

    @Test
    void equals_withSameIdAndLogin_shouldReturnTrue() {
        // Given
        UserDTO dto1 = new UserDTO();
        dto1.setId(1L);
        dto1.setLogin("user");

        UserDTO dto2 = new UserDTO();
        dto2.setId(1L);
        dto2.setLogin("user");

        // When & Then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto2).isEqualTo(dto1);
    }

    @Test
    void equals_withDifferentId_shouldReturnFalse() {
        // Given
        UserDTO dto1 = new UserDTO();
        dto1.setId(1L);
        dto1.setLogin("user");

        UserDTO dto2 = new UserDTO();
        dto2.setId(2L);
        dto2.setLogin("user");

        // When & Then
        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void equals_withDifferentLogin_shouldReturnFalse() {
        // Given
        UserDTO dto1 = new UserDTO();
        dto1.setId(1L);
        dto1.setLogin("user1");

        UserDTO dto2 = new UserDTO();
        dto2.setId(1L);
        dto2.setLogin("user2");

        // When & Then
        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void equals_withNullId_shouldReturnFalse() {
        // Given
        UserDTO dto1 = new UserDTO();
        dto1.setId(null);
        dto1.setLogin("user");

        UserDTO dto2 = new UserDTO();
        dto2.setId(1L);
        dto2.setLogin("user");

        // When & Then
        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void equals_withBothNullIds_shouldReturnFalse() {
        // Given
        UserDTO dto1 = new UserDTO();
        dto1.setId(null);
        dto1.setLogin("user");

        UserDTO dto2 = new UserDTO();
        dto2.setId(null);
        dto2.setLogin("user");

        // When & Then
        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void equals_withSameObject_shouldReturnTrue() {
        // When & Then
        assertThat(userDTO).isEqualTo(userDTO);
    }

    @Test
    void equals_withNull_shouldReturnFalse() {
        // When & Then
        assertThat(userDTO).isNotEqualTo(null);
    }

    @Test
    void equals_withDifferentClass_shouldReturnFalse() {
        // When & Then
        assertThat(userDTO).isNotEqualTo("not a UserDTO");
    }

    @Test
    void hashCode_withSameValues_shouldBeConsistent() {
        // Given
        UserDTO dto1 = new UserDTO();
        dto1.setId(1L);
        dto1.setLogin("user");

        UserDTO dto2 = new UserDTO();
        dto2.setId(1L);
        dto2.setLogin("user");

        // When & Then
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void hashCode_shouldBeConsistentAcrossMultipleCalls() {
        // When
        int hashCode1 = userDTO.hashCode();
        int hashCode2 = userDTO.hashCode();

        // Then
        assertThat(hashCode1).isEqualTo(hashCode2);
    }

    @Test
    void toString_shouldContainIdAndLogin() {
        // When
        String toString = userDTO.toString();

        // Then
        assertThat(toString).contains(String.valueOf(DEFAULT_ID));
        assertThat(toString).contains(DEFAULT_LOGIN);
        assertThat(toString).contains("UserDTO");
    }

    @Test
    void toString_withNullValues_shouldHandleGracefully() {
        // Given
        UserDTO dto = new UserDTO();
        dto.setId(null);
        dto.setLogin(null);

        // When
        String toString = dto.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("UserDTO");
    }

    @Test
    void serialization_shouldBeSupported() {
        // Given - UserDTO implements Serializable
        // When & Then - Should compile without issues
        assertThat(userDTO).isInstanceOf(java.io.Serializable.class);
    }

    @Test
    void constructor_withNullUser_shouldHandleGracefully() {
        // When
        UserDTO dto = new UserDTO(null);

        // Then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getLogin()).isNull();
    }

    @Test
    void constructor_withUserHavingNullFields_shouldHandleGracefully() {
        // Given
        User user = new User();
        user.setId(null);
        user.setLogin(null);

        // When
        UserDTO dto = new UserDTO(user);

        // Then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getLogin()).isNull();
    }

    @Test
    void setId_withVariousValues_shouldWork() {
        // Given & When & Then
        userDTO.setId(0L);
        assertThat(userDTO.getId()).isEqualTo(0L);

        userDTO.setId(Long.MAX_VALUE);
        assertThat(userDTO.getId()).isEqualTo(Long.MAX_VALUE);

        userDTO.setId(Long.MIN_VALUE);
        assertThat(userDTO.getId()).isEqualTo(Long.MIN_VALUE);

        userDTO.setId(null);
        assertThat(userDTO.getId()).isNull();
    }

    @Test
    void setLogin_withVariousValues_shouldWork() {
        // Given & When & Then
        userDTO.setLogin("");
        assertThat(userDTO.getLogin()).isEqualTo("");

        userDTO.setLogin("   ");
        assertThat(userDTO.getLogin()).isEqualTo("   ");

        userDTO.setLogin("verylongusernamethatexceedsnormallimits");
        assertThat(userDTO.getLogin()).isEqualTo("verylongusernamethatexceedsnormallimits");

        userDTO.setLogin(null);
        assertThat(userDTO.getLogin()).isNull();
    }
}
