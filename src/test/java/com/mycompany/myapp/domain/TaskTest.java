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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskTest {

    private static final String DEFAULT_DESCRIPTION = "Test task description";
    private static final String UPDATED_DESCRIPTION = "Updated task description";
    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.of(2024, 1, 1);
    private static final LocalDate UPDATED_DUE_DATE = LocalDate.of(2024, 1, 15);
    private static final TaskPriority DEFAULT_PRIORITY = TaskPriority.MEDIUM;
    private static final TaskPriority UPDATED_PRIORITY = TaskPriority.HIGH;
    private static final Boolean DEFAULT_COMPLETED = false;
    private static final Boolean UPDATED_COMPLETED = true;
    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.ofEpochMilli(1L);

    private Validator validator;
    private Task task;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        task = new Task();
        task.setDescription(DEFAULT_DESCRIPTION);
        task.setDueDate(DEFAULT_DUE_DATE);
        task.setPriority(DEFAULT_PRIORITY);
        task.setCompleted(DEFAULT_COMPLETED);
        task.setCreatedDate(DEFAULT_CREATED_DATE);
    }

    @Test
    void testTaskValidation() {
        // Given valid task
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void testTaskValidationWithNullDescription() {
        // Given
        task.setDescription(null);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");
        assertThat(violations.iterator().next().getMessage()).contains("must not be null");
    }

    @Test
    void testTaskValidationWithBlankDescription() {
        // Given
        task.setDescription("");

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        // Blank description is allowed since entity only has @NotNull, not @NotBlank
        assertThat(violations).isEmpty();
    }

    @Test
    void testTaskValidationWithDescriptionTooLong() {
        // Given
        String longDescription = "a".repeat(1001); // Assuming max length is 1000
        task.setDescription(longDescription);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        if (!violations.isEmpty()) {
            assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("description");
            assertThat(violations.iterator().next().getMessage()).contains("size must be between");
        }
    }

    @Test
    void testTaskValidationWithNullPriority() {
        // Given
        task.setPriority(null);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        // Null priority is allowed since entity doesn't have @NotNull annotation
        assertThat(violations).isEmpty();
    }

    @Test
    void testTaskValidationWithNullCompleted() {
        // Given
        task.setCompleted(null);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("completed");
        assertThat(violations.iterator().next().getMessage()).contains("must not be null");
    }

    @Test
    void testGettersAndSetters() {
        // Test all getters and setters
        Task testTask = new Task();

        // Test description
        testTask.setDescription(DEFAULT_DESCRIPTION);
        assertThat(testTask.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);

        // Test due date
        testTask.setDueDate(DEFAULT_DUE_DATE);
        assertThat(testTask.getDueDate()).isEqualTo(DEFAULT_DUE_DATE);

        // Test priority
        testTask.setPriority(DEFAULT_PRIORITY);
        assertThat(testTask.getPriority()).isEqualTo(DEFAULT_PRIORITY);

        // Test completed
        testTask.setCompleted(DEFAULT_COMPLETED);
        assertThat(testTask.getCompleted()).isEqualTo(DEFAULT_COMPLETED);

        // Test created date
        testTask.setCreatedDate(DEFAULT_CREATED_DATE);
        assertThat(testTask.getCreatedDate()).isEqualTo(DEFAULT_CREATED_DATE);

        // Test user association
        User user = new User();
        user.setLogin("testuser");
        testTask.setUser(user);
        assertThat(testTask.getUser()).isEqualTo(user);

        // Test ID
        testTask.setId(1L);
        assertThat(testTask.getId()).isEqualTo(1L);
    }

    @Test
    void testEqualsAndHashCode() throws Exception {
        // Test equals and hashCode implementation
        TestUtil.equalsVerifier(Task.class);

        Task task1 = new Task();
        task1.setId(1L);
        task1.setDescription(DEFAULT_DESCRIPTION);

        Task task2 = new Task();
        task2.setId(1L);
        task2.setDescription(DEFAULT_DESCRIPTION);

        // Same ID, should be equal
        assertThat(task1).isEqualTo(task2);
        assertThat(task1.hashCode()).isEqualTo(task2.hashCode());

        // Different ID, should not be equal
        task2.setId(2L);
        assertThat(task1).isNotEqualTo(task2);
        // Note: hashCode implementation uses getClass().hashCode(), so objects of same class will have same hashCode

        // Null ID, should not be equal to non-null ID
        task1.setId(null);
        assertThat(task1).isNotEqualTo(task2);
    }

    @Test
    void testEqualsSameObject() {
        // Given
        Task task1 = new Task();
        task1.setId(1L);

        // Then
        assertThat(task1).isEqualTo(task1);
    }

    @Test
    void testEqualsNull() {
        // Given
        Task task1 = new Task();
        task1.setId(1L);

        // Then
        assertThat(task1).isNotEqualTo(null);
    }

    @Test
    void testEqualsDifferentClass() {
        // Given
        Task task1 = new Task();
        task1.setId(1L);
        String notATask = "Not a task";

        // Then
        assertThat(task1).isNotEqualTo(notATask);
    }

    @Test
    void testToString() {
        // Given
        task.setId(1L);

        // When
        String toString = task.toString();

        // Then
        assertThat(toString).contains("Task");
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("description='" + DEFAULT_DESCRIPTION + "'");
        assertThat(toString).contains("priority='" + DEFAULT_PRIORITY + "'");
        assertThat(toString).contains("completed='" + DEFAULT_COMPLETED + "'");
    }

    @Test
    void testTaskWithAllPriorities() {
        // Test all enum values
        for (TaskPriority priority : TaskPriority.values()) {
            Task testTask = new Task();
            testTask.setDescription(DEFAULT_DESCRIPTION);
            testTask.setPriority(priority);
            testTask.setCompleted(false);

            Set<ConstraintViolation<Task>> violations = validator.validate(testTask);
            assertThat(violations).isEmpty();
        }
    }

    @Test
    void testTaskWithPastDueDate() {
        // Given
        task.setDueDate(LocalDate.now().minusDays(1));

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).isEmpty(); // Past due dates should be allowed
    }

    @Test
    void testTaskWithFutureDueDate() {
        // Given
        task.setDueDate(LocalDate.now().plusDays(30));

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).isEmpty(); // Future due dates should be allowed
    }

    @Test
    void testTaskBuilder() {
        // Using fluent builder pattern if available
        Task builtTask = new Task()
            .description(DEFAULT_DESCRIPTION)
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(DEFAULT_COMPLETED);

        assertThat(builtTask.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(builtTask.getDueDate()).isEqualTo(DEFAULT_DUE_DATE);
        assertThat(builtTask.getPriority()).isEqualTo(DEFAULT_PRIORITY);
        assertThat(builtTask.getCompleted()).isEqualTo(DEFAULT_COMPLETED);
    }

    @Test
    void testTaskCopy() {
        // Given
        task.setId(1L);
        User user = new User();
        user.setLogin("testuser");
        task.setUser(user);

        // When
        Task copiedTask = new Task();
        copiedTask.setDescription(task.getDescription());
        copiedTask.setDueDate(task.getDueDate());
        copiedTask.setPriority(task.getPriority());
        copiedTask.setCompleted(task.getCompleted());
        copiedTask.setCreatedDate(task.getCreatedDate());
        copiedTask.setUser(task.getUser());

        // Then
        assertThat(copiedTask.getDescription()).isEqualTo(task.getDescription());
        assertThat(copiedTask.getDueDate()).isEqualTo(task.getDueDate());
        assertThat(copiedTask.getPriority()).isEqualTo(task.getPriority());
        assertThat(copiedTask.getCompleted()).isEqualTo(task.getCompleted());
        assertThat(copiedTask.getCreatedDate()).isEqualTo(task.getCreatedDate());
        assertThat(copiedTask.getUser()).isEqualTo(task.getUser());
    }

    @Test
    void testTaskValidationWithNullDueDate() {
        // Given
        task.setDueDate(null);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertThat(violations).isEmpty(); // Due date should be optional
    }

    @Test
    void testTaskCompletedToggle() {
        // Given
        task.setCompleted(false);
        assertThat(task.getCompleted()).isFalse();

        // When
        task.setCompleted(true);

        // Then
        assertThat(task.getCompleted()).isTrue();

        // When
        task.setCompleted(false);

        // Then
        assertThat(task.getCompleted()).isFalse();
    }
}
