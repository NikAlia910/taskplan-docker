package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mycompany.myapp.domain.Task;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.TaskPriority;
import com.mycompany.myapp.repository.TaskRepository;
import com.mycompany.myapp.repository.UserRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    private static final Long DEFAULT_ID = 1L;
    private static final String DEFAULT_DESCRIPTION = "Test task description";
    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.of(2024, 1, 1);
    private static final TaskPriority DEFAULT_PRIORITY = TaskPriority.MEDIUM;
    private static final Boolean DEFAULT_COMPLETED = false;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository);
    }

    @Test
    void testSave() {
        // Given
        Task task = createTask();
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task result = taskService.save(task);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(result.getPriority()).isEqualTo(DEFAULT_PRIORITY);
        verify(taskRepository).save(task);
    }

    @Test
    void testUpdate() {
        // Given
        Task existingTask = createTask();
        existingTask.setId(DEFAULT_ID);
        Task updatedTask = createTask();
        updatedTask.setId(DEFAULT_ID);
        updatedTask.setDescription("Updated description");
        updatedTask.setPriority(TaskPriority.HIGH);

        when(taskRepository.existsById(DEFAULT_ID)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        // When
        Task result = taskService.update(updatedTask);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
        verify(taskRepository).existsById(DEFAULT_ID);
        verify(taskRepository).save(updatedTask);
    }

    @Test
    void testUpdateNonExistentTask() {
        // Given
        Task task = createTask();
        task.setId(999L);
        when(taskRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> taskService.update(task)).withMessage("Entity not found");

        verify(taskRepository).existsById(999L);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void testPartialUpdate() {
        // Given
        Task existingTask = createTask();
        existingTask.setId(DEFAULT_ID);
        Task partialTask = new Task();
        partialTask.setId(DEFAULT_ID);
        partialTask.setDescription("Partially updated description");

        when(taskRepository.findById(DEFAULT_ID)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // When
        Optional<Task> result = taskService.partialUpdate(partialTask);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Partially updated description");
        assertThat(result.get().getPriority()).isEqualTo(DEFAULT_PRIORITY); // Should keep original priority
        verify(taskRepository).findById(DEFAULT_ID);
        verify(taskRepository).save(existingTask);
    }

    @Test
    void testPartialUpdateNonExistentTask() {
        // Given
        Task task = new Task();
        task.setId(999L);
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Task> result = taskService.partialUpdate(task);

        // Then
        assertThat(result).isEmpty();
        verify(taskRepository).findById(999L);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void testFindAll() {
        // Given
        List<Task> tasks = Arrays.asList(createTask(), createTask());
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());
        when(taskRepository.findAll(pageable)).thenReturn(taskPage);

        // When
        Page<Task> result = taskService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(taskRepository).findAll(pageable);
    }

    @Test
    void testFindAllWithEagerRelationships() {
        // Given
        List<Task> tasks = Arrays.asList(createTask(), createTask());
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());
        when(taskRepository.findAllWithEagerRelationships(pageable)).thenReturn(taskPage);

        // When
        Page<Task> result = taskService.findAllWithEagerRelationships(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(taskRepository).findAllWithEagerRelationships(pageable);
    }

    @Test
    void testFindOne() {
        // Given
        Task task = createTask();
        task.setId(DEFAULT_ID);
        when(taskRepository.findOneWithEagerRelationships(DEFAULT_ID)).thenReturn(Optional.of(task));

        // When
        Optional<Task> result = taskService.findOne(DEFAULT_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(DEFAULT_ID);
        assertThat(result.get().getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        verify(taskRepository).findOneWithEagerRelationships(DEFAULT_ID);
    }

    @Test
    void testFindOneNotFound() {
        // Given
        when(taskRepository.findOneWithEagerRelationships(999L)).thenReturn(Optional.empty());

        // When
        Optional<Task> result = taskService.findOne(999L);

        // Then
        assertThat(result).isEmpty();
        verify(taskRepository).findOneWithEagerRelationships(999L);
    }

    @Test
    void testDelete() {
        // Given
        doNothing().when(taskRepository).deleteById(DEFAULT_ID);

        // When
        taskService.delete(DEFAULT_ID);

        // Then
        verify(taskRepository).deleteById(DEFAULT_ID);
    }

    @Test
    void testFindByUserIsCurrentUser() {
        // Given
        List<Task> userTasks = Arrays.asList(createTask(), createTask());
        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        // When
        List<Task> result = taskRepository.findByUserIsCurrentUser();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(taskRepository).findByUserIsCurrentUser();
    }

    @Test
    void testSaveTaskWithUser() {
        // Given
        Task task = createTask();
        User user = createUser();
        task.setUser(user);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task result = taskService.save(task);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getLogin()).isEqualTo("testuser");
        verify(taskRepository).save(task);
    }

    @Test
    void testSaveTaskWithAllPriorities() {
        // Test saving tasks with different priorities
        for (TaskPriority priority : TaskPriority.values()) {
            // Given
            Task task = createTask();
            task.setPriority(priority);
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            // When
            Task result = taskService.save(task);

            // Then
            assertThat(result.getPriority()).isEqualTo(priority);
        }
    }

    @Test
    void testSaveCompletedTask() {
        // Given
        Task task = createTask();
        task.setCompleted(true);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task result = taskService.save(task);

        // Then
        assertThat(result.getCompleted()).isTrue();
        verify(taskRepository).save(task);
    }

    @Test
    void testSaveTaskWithPastDueDate() {
        // Given
        Task task = createTask();
        task.setDueDate(LocalDate.now().minusDays(1));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task result = taskService.save(task);

        // Then
        assertThat(result.getDueDate()).isBefore(LocalDate.now());
        verify(taskRepository).save(task);
    }

    @Test
    void testSaveTaskWithFutureDueDate() {
        // Given
        Task task = createTask();
        task.setDueDate(LocalDate.now().plusDays(30));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task result = taskService.save(task);

        // Then
        assertThat(result.getDueDate()).isAfter(LocalDate.now());
        verify(taskRepository).save(task);
    }

    @Test
    void testUpdateTaskCompletion() {
        // Given
        Task existingTask = createTask();
        existingTask.setId(DEFAULT_ID);
        existingTask.setCompleted(false);

        Task updatedTask = createTask();
        updatedTask.setId(DEFAULT_ID);
        updatedTask.setCompleted(true);

        when(taskRepository.existsById(DEFAULT_ID)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        // When
        Task result = taskService.update(updatedTask);

        // Then
        assertThat(result.getCompleted()).isTrue();
        verify(taskRepository).save(updatedTask);
    }

    @Test
    void testFindAllEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);
        when(taskRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<Task> result = taskService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(taskRepository).findAll(pageable);
    }

    private Task createTask() {
        Task task = new Task();
        task.setDescription(DEFAULT_DESCRIPTION);
        task.setDueDate(DEFAULT_DUE_DATE);
        task.setPriority(DEFAULT_PRIORITY);
        task.setCompleted(DEFAULT_COMPLETED);
        return task;
    }

    private User createUser() {
        User user = new User();
        user.setLogin("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActivated(true);
        return user;
    }
}
