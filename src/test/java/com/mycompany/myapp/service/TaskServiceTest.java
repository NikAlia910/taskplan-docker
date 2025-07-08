package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        user.setEmail("test@example.com");

        task = new Task();
        task.setId(1L);
        task.setDescription("Test task");
        task.setDueDate(LocalDate.now().plusDays(1));
        task.setPriority(TaskPriority.HIGH);
        task.setCompleted(false);
        task.setCreatedDate(Instant.now());
        task.setUser(user);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void save_WhenTaskHasUser_ShouldSaveTask() {
        // Given
        Task taskWithUser = new Task();
        taskWithUser.setDescription("Task with user");
        taskWithUser.setUser(user);

        when(taskRepository.save(any(Task.class))).thenReturn(taskWithUser);

        // When
        Task result = taskService.save(taskWithUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        verify(taskRepository).save(taskWithUser);
        verifyNoInteractions(userRepository);
    }

    @Test
    void save_WhenTaskHasNoUser_ShouldSetCurrentUserAndSave() {
        // Given
        Task taskWithoutUser = new Task();
        taskWithoutUser.setDescription("Task without user");

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));
            when(taskRepository.save(any(Task.class))).thenReturn(taskWithoutUser);

            // When
            Task result = taskService.save(taskWithoutUser);

            // Then
            assertThat(result).isNotNull();
            verify(taskRepository).save(taskWithoutUser);
            verify(userRepository).findOneByLogin("testuser");
            assertThat(taskWithoutUser.getUser()).isEqualTo(user);
        }
    }

    @Test
    void save_WhenNoCurrentUser_ShouldSaveTaskWithoutUser() {
        // Given
        Task taskWithoutUser = new Task();
        taskWithoutUser.setDescription("Task without user");

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());
            when(taskRepository.save(any(Task.class))).thenReturn(taskWithoutUser);

            // When
            Task result = taskService.save(taskWithoutUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isNull();
            verify(taskRepository).save(taskWithoutUser);
            verifyNoInteractions(userRepository);
        }
    }

    @Test
    void save_WhenUserNotFound_ShouldSaveTaskWithoutUser() {
        // Given
        Task taskWithoutUser = new Task();
        taskWithoutUser.setDescription("Task without user");

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("nonexistentuser"));
            when(userRepository.findOneByLogin("nonexistentuser")).thenReturn(Optional.empty());
            when(taskRepository.save(any(Task.class))).thenReturn(taskWithoutUser);

            // When
            Task result = taskService.save(taskWithoutUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isNull();
            verify(taskRepository).save(taskWithoutUser);
            verify(userRepository).findOneByLogin("nonexistentuser");
        }
    }

    @Test
    void update_ShouldSaveTask() {
        // Given
        Task updatedTask = new Task();
        updatedTask.setId(1L);
        updatedTask.setDescription("Updated task");

        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        // When
        Task result = taskService.update(updatedTask);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Updated task");
        verify(taskRepository).save(updatedTask);
    }

    @Test
    void partialUpdate_WhenTaskExists_ShouldUpdatePartialFields() {
        // Given
        Task existingTask = new Task();
        existingTask.setId(1L);
        existingTask.setDescription("Original description");
        existingTask.setDueDate(LocalDate.now());
        existingTask.setPriority(TaskPriority.LOW);
        existingTask.setCompleted(false);
        existingTask.setCreatedDate(Instant.now().minusSeconds(3600));

        Task partialTask = new Task();
        partialTask.setId(1L);
        partialTask.setDescription("Updated description");
        partialTask.setPriority(TaskPriority.HIGH);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // When
        Optional<Task> result = taskService.partialUpdate(partialTask);

        // Then
        assertThat(result).isPresent();
        verify(taskRepository).findById(1L);
        verify(taskRepository).save(existingTask);

        // Verify that only non-null fields were updated
        assertThat(existingTask.getDescription()).isEqualTo("Updated description");
        assertThat(existingTask.getPriority()).isEqualTo(TaskPriority.HIGH);
        // These should remain unchanged since they were null in partialTask
        assertThat(existingTask.getDueDate()).isEqualTo(LocalDate.now());
        assertThat(existingTask.getCompleted()).isFalse();
    }

    @Test
    void partialUpdate_WhenTaskExistsWithAllFields_ShouldUpdateAllFields() {
        // Given
        Task existingTask = new Task();
        existingTask.setId(1L);
        existingTask.setDescription("Original description");
        existingTask.setDueDate(LocalDate.now());
        existingTask.setPriority(TaskPriority.LOW);
        existingTask.setCompleted(false);
        existingTask.setCreatedDate(Instant.now().minusSeconds(3600));
        existingTask.setLastModifiedDate(Instant.now().minusSeconds(1800));

        Task partialTask = new Task();
        partialTask.setId(1L);
        partialTask.setDescription("Updated description");
        partialTask.setDueDate(LocalDate.now().plusDays(1));
        partialTask.setPriority(TaskPriority.HIGH);
        partialTask.setCompleted(true);
        partialTask.setCreatedDate(Instant.now());
        partialTask.setLastModifiedDate(Instant.now());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // When
        Optional<Task> result = taskService.partialUpdate(partialTask);

        // Then
        assertThat(result).isPresent();
        verify(taskRepository).findById(1L);
        verify(taskRepository).save(existingTask);

        // Verify all fields were updated
        assertThat(existingTask.getDescription()).isEqualTo("Updated description");
        assertThat(existingTask.getDueDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(existingTask.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(existingTask.getCompleted()).isTrue();
        assertThat(existingTask.getCreatedDate()).isEqualTo(partialTask.getCreatedDate());
        assertThat(existingTask.getLastModifiedDate()).isEqualTo(partialTask.getLastModifiedDate());
    }

    @Test
    void partialUpdate_WhenTaskNotFound_ShouldReturnEmpty() {
        // Given
        Task partialTask = new Task();
        partialTask.setId(999L);
        partialTask.setDescription("Updated description");

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Task> result = taskService.partialUpdate(partialTask);

        // Then
        assertThat(result).isEmpty();
        verify(taskRepository).findById(999L);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void findAll_ShouldReturnPageOfTasks() {
        // Given
        List<Task> tasks = Arrays.asList(task, new Task());
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
    void findAllByCurrentUser_WhenTasksExist_ShouldReturnPaginatedTasks() {
        // Given
        List<Task> userTasks = Arrays.asList(createTask(1L, "Task 1"), createTask(2L, "Task 2"), createTask(3L, "Task 3"));

        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getNumber()).isEqualTo(0);
        verify(taskRepository).findByUserIsCurrentUser();
    }

    @Test
    void findAllByCurrentUser_WhenOffsetExceedsSize_ShouldReturnEmptyPage() {
        // Given
        List<Task> userTasks = Arrays.asList(createTask(1L, "Task 1"));
        Pageable largePageable = PageRequest.of(5, 10); // offset = 50, but only 1 task

        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(largePageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(taskRepository).findByUserIsCurrentUser();
    }

    @Test
    void findAllByCurrentUser_WithPagination_ShouldReturnCorrectSlice() {
        // Given
        List<Task> userTasks = Arrays.asList(
            createTask(1L, "Task 1"),
            createTask(2L, "Task 2"),
            createTask(3L, "Task 3"),
            createTask(4L, "Task 4"),
            createTask(5L, "Task 5")
        );
        Pageable secondPage = PageRequest.of(1, 2); // offset = 2, size = 2

        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(secondPage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(3L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(4L);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getNumber()).isEqualTo(1);
    }

    @Test
    void findAllByCurrentUserAndCompleted_WhenCompletedTasksExist_ShouldReturnCompletedTasks() {
        // Given
        List<Task> allUserTasks = Arrays.asList(
            createTask(1L, "Completed Task 1", true),
            createTask(2L, "Incomplete Task", false),
            createTask(3L, "Completed Task 2", true)
        );

        when(taskRepository.findByUserIsCurrentUser()).thenReturn(allUserTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUserAndCompleted(true, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(Task::getCompleted);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(taskRepository).findByUserIsCurrentUser();
    }

    @Test
    void findAllByCurrentUserAndCompleted_WhenIncompleteTasksExist_ShouldReturnIncompleteTasks() {
        // Given
        List<Task> allUserTasks = Arrays.asList(
            createTask(1L, "Completed Task", true),
            createTask(2L, "Incomplete Task 1", false),
            createTask(3L, "Incomplete Task 2", false)
        );

        when(taskRepository.findByUserIsCurrentUser()).thenReturn(allUserTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUserAndCompleted(false, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(task -> !task.getCompleted());
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(taskRepository).findByUserIsCurrentUser();
    }

    @Test
    void findAllByCurrentUserAndCompleted_WithPaginationAndOffsetExceedsSize_ShouldReturnEmptyPage() {
        // Given
        List<Task> allUserTasks = Arrays.asList(createTask(1L, "Completed Task", true));
        Pageable largePageable = PageRequest.of(5, 10);

        when(taskRepository.findByUserIsCurrentUser()).thenReturn(allUserTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUserAndCompleted(true, largePageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(taskRepository).findByUserIsCurrentUser();
    }

    @Test
    void findAllWithEagerRelationships_ShouldReturnPageFromRepository() {
        // Given
        Page<Task> taskPage = new PageImpl<>(Arrays.asList(task), pageable, 1);

        when(taskRepository.findAllWithEagerRelationships(pageable)).thenReturn(taskPage);

        // When
        Page<Task> result = taskService.findAllWithEagerRelationships(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findAllWithEagerRelationships(pageable);
    }

    @Test
    void findOne_WhenTaskExists_ShouldReturnTask() {
        // Given
        when(taskRepository.findOneWithEagerRelationships(1L)).thenReturn(Optional.of(task));

        // When
        Optional<Task> result = taskService.findOne(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(1L);
        verify(taskRepository).findOneWithEagerRelationships(1L);
    }

    @Test
    void findOne_WhenTaskNotFound_ShouldReturnEmpty() {
        // Given
        when(taskRepository.findOneWithEagerRelationships(999L)).thenReturn(Optional.empty());

        // When
        Optional<Task> result = taskService.findOne(999L);

        // Then
        assertThat(result).isEmpty();
        verify(taskRepository).findOneWithEagerRelationships(999L);
    }

    @Test
    void delete_ShouldCallRepositoryDelete() {
        // Given
        Long taskId = 1L;

        // When
        taskService.delete(taskId);

        // Then
        verify(taskRepository).deleteById(taskId);
    }

    private Task createTask(Long id, String description) {
        return createTask(id, description, false);
    }

    private Task createTask(Long id, String description, boolean completed) {
        Task task = new Task();
        task.setId(id);
        task.setDescription(description);
        task.setCompleted(completed);
        task.setCreatedDate(Instant.now());
        task.setUser(user);
        return task;
    }
}
