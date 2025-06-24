package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.TaskTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.domain.enumeration.TaskPriority;
import com.mycompany.myapp.web.rest.TestUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Task.class);
        Task task1 = getTaskSample1();
        Task task2 = new Task();
        assertThat(task1).isNotEqualTo(task2);

        task2.setId(task1.getId());
        assertThat(task1).isEqualTo(task2);

        task2 = getTaskSample2();
        assertThat(task1).isNotEqualTo(task2);
    }

    @Test
    void onCreate_shouldSetDefaultValues() {
        // Given
        Task task = new Task();
        task.setDescription("Test task");
        // Don't set createdDate or completed

        // When
        task.onCreate(); // Manually call @PrePersist method

        // Then
        assertThat(task.getCreatedDate()).isNotNull();
        assertThat(task.getCompleted()).isFalse();
    }

    @Test
    void onCreate_shouldNotOverrideExistingValues() {
        // Given
        Task task = new Task();
        task.setDescription("Test task");
        Instant existingCreatedDate = Instant.now().minusSeconds(3600);
        task.setCreatedDate(existingCreatedDate);
        task.setCompleted(true);

        // When
        task.onCreate(); // Manually call @PrePersist method

        // Then
        assertThat(task.getCreatedDate()).isEqualTo(existingCreatedDate);
        assertThat(task.getCompleted()).isTrue();
    }

    @Test
    void onUpdate_shouldSetLastModifiedDate() {
        // Given
        Task task = new Task();
        task.setDescription("Test task");
        assertThat(task.getLastModifiedDate()).isNull();

        // When
        task.onUpdate(); // Manually call @PreUpdate method

        // Then
        assertThat(task.getLastModifiedDate()).isNotNull();
        assertThat(task.getLastModifiedDate()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void validation_shouldRequireDescription() {
        // Given
        Task task = new Task();
        task.setDescription(null);
        task.setCompleted(true);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(
            violation -> violation.getPropertyPath().toString().equals("description") && violation.getMessage().contains("must not be null")
        );
    }

    @Test
    void validation_shouldRequireCompleted() {
        // Given
        Task task = new Task();
        task.setDescription("Valid description");
        task.setCompleted(null);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(
            violation -> violation.getPropertyPath().toString().equals("completed") && violation.getMessage().contains("must not be null")
        );
    }

    @Test
    void validation_shouldEnforceDescriptionMaxLength() {
        // Given
        Task task = new Task();
        String longDescription = "a".repeat(256); // Exceeds max length of 255
        task.setDescription(longDescription);
        task.setCompleted(true);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(
            violation ->
                violation.getPropertyPath().toString().equals("description") && violation.getMessage().contains("size must be between")
        );
    }

    @Test
    void validation_shouldPassWithValidTask() {
        // Given
        Task task = new Task();
        task.setDescription("Valid description");
        task.setCompleted(false);
        task.setDueDate(LocalDate.now().plusDays(7));
        task.setPriority(TaskPriority.MEDIUM);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void fluentMethods_shouldReturnTaskInstance() {
        // Given
        Task task = new Task();
        User user = new User();
        user.setLogin("testuser");

        // When & Then - test fluent API
        Task result = task
            .description("Test description")
            .dueDate(LocalDate.now().plusDays(5))
            .priority(TaskPriority.HIGH)
            .completed(true)
            .createdDate(Instant.now())
            .lastModifiedDate(Instant.now())
            .user(user);

        assertThat(result).isSameAs(task);
        assertThat(task.getDescription()).isEqualTo("Test description");
        assertThat(task.getDueDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getCompleted()).isTrue();
        assertThat(task.getCreatedDate()).isNotNull();
        assertThat(task.getLastModifiedDate()).isNotNull();
        assertThat(task.getUser()).isEqualTo(user);
    }

    @Test
    void toString_shouldContainAllFields() {
        // Given
        Task task = new Task();
        task.setId(1L);
        task.setDescription("Test task");
        task.setDueDate(LocalDate.of(2024, 12, 31));
        task.setPriority(TaskPriority.HIGH);
        task.setCompleted(false);
        task.setCreatedDate(Instant.parse("2024-01-01T10:00:00Z"));
        task.setLastModifiedDate(Instant.parse("2024-01-02T10:00:00Z"));

        // When
        String toString = task.toString();

        // Then
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("description='Test task'");
        assertThat(toString).contains("dueDate='2024-12-31'");
        assertThat(toString).contains("priority='HIGH'");
        assertThat(toString).contains("completed='false'");
        assertThat(toString).contains("createdDate='2024-01-01T10:00:00Z'");
        assertThat(toString).contains("lastModifiedDate='2024-01-02T10:00:00Z'");
    }

    @Test
    void hashCode_shouldBeConsistent() {
        // Given
        Task task1 = new Task();
        Task task2 = new Task();

        // When & Then - hashCode should be consistent for same class
        assertThat(task1.hashCode()).isEqualTo(task2.hashCode());
        assertThat(task1.hashCode()).isEqualTo(Task.class.hashCode());
    }

    @Test
    void equals_shouldWorkWithNullId() {
        // Given
        Task task1 = new Task();
        Task task2 = new Task();
        task1.setDescription("Same description");
        task2.setDescription("Same description");

        // When & Then - tasks with null IDs should not be equal even if other fields match
        assertThat(task1).isNotEqualTo(task2);
    }

    @Test
    void equals_shouldWorkWithSameId() {
        // Given
        Task task1 = new Task();
        Task task2 = new Task();
        task1.setId(1L);
        task2.setId(1L);
        task1.setDescription("Description 1");
        task2.setDescription("Description 2"); // Different descriptions

        // When & Then - tasks with same ID should be equal regardless of other fields
        assertThat(task1).isEqualTo(task2);
    }

    @Test
    void userRelationship_shouldWork() {
        // Given
        Task task = new Task();
        User user = new User();
        user.setId(1L);
        user.setLogin("testuser");

        // When
        task.setUser(user);

        // Then
        assertThat(task.getUser()).isEqualTo(user);
        assertThat(task.getUser().getLogin()).isEqualTo("testuser");
    }

    @Test
    void allFieldsSetAndGet_shouldWork() {
        // Given
        Task task = new Task();
        User user = new User();
        user.setLogin("testuser");

        LocalDate dueDate = LocalDate.now().plusDays(10);
        Instant createdDate = Instant.now();
        Instant lastModifiedDate = Instant.now().plusSeconds(3600);

        // When
        task.setId(42L);
        task.setDescription("Complete test description");
        task.setDueDate(dueDate);
        task.setPriority(TaskPriority.MEDIUM);
        task.setCompleted(true);
        task.setCreatedDate(createdDate);
        task.setLastModifiedDate(lastModifiedDate);
        task.setUser(user);

        // Then
        assertThat(task.getId()).isEqualTo(42L);
        assertThat(task.getDescription()).isEqualTo("Complete test description");
        assertThat(task.getDueDate()).isEqualTo(dueDate);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.getCompleted()).isTrue();
        assertThat(task.getCreatedDate()).isEqualTo(createdDate);
        assertThat(task.getLastModifiedDate()).isEqualTo(lastModifiedDate);
        assertThat(task.getUser()).isEqualTo(user);
    }
}
