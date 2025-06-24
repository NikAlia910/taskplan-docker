package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.Task;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.TaskPriority;
import com.mycompany.myapp.repository.TaskRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link TaskService}.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private User user;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Create test user
        user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        user.setEmail("test@example.com");

        // Create test task
        task = new Task();
        task.setId(1L);
        task.setDescription("Test task");
        task.setDueDate(LocalDate.now().plusDays(7));
        task.setPriority(TaskPriority.HIGH);
        task.setCompleted(false);
        task.setCreatedDate(Instant.now());

        pageable = PageRequest.of(0, 20);
    }

    @Test
    void save_shouldSetCurrentUserWhenUserIsNull() {
        // Given
        task.setUser(null);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            // When
            Task savedTask = taskService.save(task);

            // Then
            verify(userRepository).findOneByLogin("testuser");
            verify(taskRepository).save(task);
            assertThat(savedTask.getUser()).isEqualTo(user);
        }
    }

    @Test
    void save_shouldNotOverrideExistingUser() {
        // Given
        task.setUser(user);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            // When
            Task savedTask = taskService.save(task);

            // Then
            verify(taskRepository).save(task);
            verifyNoInteractions(userRepository);
            securityUtilsMock.verifyNoInteractions();
            assertThat(savedTask.getUser()).isEqualTo(user);
        }
    }

    @Test
    void save_shouldHandleNoCurrentUser() {
        // Given
        task.setUser(null);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            // When
            Task savedTask = taskService.save(task);

            // Then
            verify(taskRepository).save(task);
            verifyNoInteractions(userRepository);
            assertThat(savedTask.getUser()).isNull();
        }
    }

    @Test
    void update_shouldCallRepositorySave() {
        // Given
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task updatedTask = taskService.update(task);

        // Then
        verify(taskRepository).save(task);
        assertThat(updatedTask).isEqualTo(task);
    }

    @Test
    void partialUpdate_shouldUpdateOnlyProvidedFields() {
        // Given
        Task existingTask = new Task();
        existingTask.setId(1L);
        existingTask.setDescription("Old description");
        existingTask.setCompleted(false);
        existingTask.setPriority(TaskPriority.LOW);

        Task updateTask = new Task();
        updateTask.setId(1L);
        updateTask.setDescription("New description");
        updateTask.setCompleted(true);
        // Priority not set - should remain unchanged

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // When
        Optional<Task> result = taskService.partialUpdate(updateTask);

        // Then
        assertThat(result).isPresent();
        Task updatedTask = result.get();
        assertThat(updatedTask.getDescription()).isEqualTo("New description");
        assertThat(updatedTask.getCompleted()).isTrue();
        assertThat(updatedTask.getPriority()).isEqualTo(TaskPriority.LOW); // Should remain unchanged
        verify(taskRepository).save(existingTask);
    }

    @Test
    void partialUpdate_shouldReturnEmptyWhenTaskNotFound() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<Task> result = taskService.partialUpdate(task);

        // Then
        assertThat(result).isEmpty();
        verify(taskRepository, never()).save(any());
    }

    @Test
    void findAll_shouldReturnPagedResults() {
        // Given
        List<Task> tasks = Arrays.asList(task);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, 1);
        when(taskRepository.findAll(pageable)).thenReturn(taskPage);

        // When
        Page<Task> result = taskService.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(task);
        verify(taskRepository).findAll(pageable);
    }

    @Test
    void findAllByCurrentUser_shouldReturnUserTasks() {
        // Given
        List<Task> userTasks = Arrays.asList(task);
        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(task);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(taskRepository).findByUserIsCurrentUser();
    }

    @Test
    void findAllByCurrentUser_shouldHandlePagination() {
        // Given
        Task task2 = new Task();
        task2.setId(2L);
        task2.setDescription("Task 2");

        Task task3 = new Task();
        task3.setId(3L);
        task3.setDescription("Task 3");

        List<Task> userTasks = Arrays.asList(task, task2, task3);
        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        Pageable pageableSize2 = PageRequest.of(0, 2);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(pageableSize2);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void findAllByCurrentUser_shouldReturnEmptyWhenStartBeyondTotal() {
        // Given
        List<Task> userTasks = Arrays.asList(task);
        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        Pageable beyondRange = PageRequest.of(1, 20); // Start at offset 20, but only 1 task

        // When
        Page<Task> result = taskService.findAllByCurrentUser(beyondRange);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAllByCurrentUserAndCompleted_shouldFilterByCompletionStatus() {
        // Given
        Task completedTask = new Task();
        completedTask.setId(2L);
        completedTask.setDescription("Completed task");
        completedTask.setCompleted(true);

        List<Task> allUserTasks = Arrays.asList(task, completedTask); // task is not completed
        when(taskRepository.findByUserIsCurrentUser()).thenReturn(allUserTasks);

        // When - find completed tasks
        Page<Task> completedResults = taskService.findAllByCurrentUserAndCompleted(true, pageable);

        // Then
        assertThat(completedResults.getContent()).hasSize(1);
        assertThat(completedResults.getContent().get(0).getCompleted()).isTrue();

        // When - find incomplete tasks
        Page<Task> incompleteResults = taskService.findAllByCurrentUserAndCompleted(false, pageable);

        // Then
        assertThat(incompleteResults.getContent()).hasSize(1);
        assertThat(incompleteResults.getContent().get(0).getCompleted()).isFalse();
    }

    @Test
    void findAllWithEagerRelationships_shouldDelegateToRepository() {
        // Given
        List<Task> tasks = Arrays.asList(task);
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, 1);
        when(taskRepository.findAllWithEagerRelationships(pageable)).thenReturn(taskPage);

        // When
        Page<Task> result = taskService.findAllWithEagerRelationships(pageable);

        // Then
        assertThat(result).isEqualTo(taskPage);
        verify(taskRepository).findAllWithEagerRelationships(pageable);
    }

    @Test
    void findOne_shouldReturnTaskWithEagerRelationships() {
        // Given
        when(taskRepository.findOneWithEagerRelationships(1L)).thenReturn(Optional.of(task));

        // When
        Optional<Task> result = taskService.findOne(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(task);
        verify(taskRepository).findOneWithEagerRelationships(1L);
    }

    @Test
    void findOne_shouldReturnEmptyWhenNotFound() {
        // Given
        when(taskRepository.findOneWithEagerRelationships(1L)).thenReturn(Optional.empty());

        // When
        Optional<Task> result = taskService.findOne(1L);

        // Then
        assertThat(result).isEmpty();
        verify(taskRepository).findOneWithEagerRelationships(1L);
    }

    @Test
    void delete_shouldCallRepositoryDeleteById() {
        // When
        taskService.delete(1L);

        // Then
        verify(taskRepository).deleteById(1L);
    }
}
