package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    void save_ShouldSaveTaskWithCurrentUser_WhenUserNotSet() {
        // Given
        Task taskWithoutUser = new Task();
        taskWithoutUser.setDescription("Test task");
        taskWithoutUser.setCompleted(false);

        try (MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));
            when(taskRepository.save(any(Task.class))).thenReturn(task);

            // When
            Task result = taskService.save(taskWithoutUser);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).findOneByLogin("testuser");
            verify(taskRepository).save(any(Task.class));
        }
    }

    @Test
    void save_ShouldSaveTaskWithoutModifyingUser_WhenUserAlreadySet() {
        // Given
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task result = taskService.save(task);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        verify(userRepository, never()).findOneByLogin(anyString());
        verify(taskRepository).save(task);
    }

    @Test
    void update_ShouldUpdateTask() {
        // Given
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // When
        Task result = taskService.update(task);

        // Then
        assertThat(result).isNotNull();
        verify(taskRepository).save(task);
    }

    @Test
    void partialUpdate_ShouldUpdateOnlyProvidedFields_WhenTaskExists() {
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

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // When
        Optional<Task> result = taskService.partialUpdate(updateTask);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getDescription()).isEqualTo("New description");
        assertThat(result.orElseThrow().getCompleted()).isTrue();
        assertThat(result.orElseThrow().getPriority()).isEqualTo(TaskPriority.LOW);
        verify(taskRepository).save(existingTask);
    }

    @Test
    void partialUpdate_ShouldReturnEmpty_WhenTaskNotExists() {
        // Given
        Task updateTask = new Task();
        updateTask.setId(999L);

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Task> result = taskService.partialUpdate(updateTask);

        // Then
        assertThat(result).isEmpty();
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void findAll_ShouldReturnPagedTasks() {
        // Given
        List<Task> tasks = Arrays.asList(task);
        Page<Task> page = new PageImpl<>(tasks, pageable, 1);
        when(taskRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<Task> result = taskService.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(taskRepository).findAll(pageable);
    }

    @Test
    void findAllByCurrentUser_ShouldReturnUserTasks() {
        // Given
        List<Task> userTasks = Arrays.asList(task);
        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(taskRepository).findByUserIsCurrentUser();
    }

    @Test
    void findAllByCurrentUser_ShouldReturnEmptyPage_WhenOffsetExceedsSize() {
        // Given
        List<Task> userTasks = Arrays.asList(task);
        Pageable largeOffsetPageable = PageRequest.of(10, 10); // Offset 100, but only 1 task
        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(largeOffsetPageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAllByCurrentUserAndCompleted_ShouldReturnFilteredTasks() {
        // Given
        Task completedTask = new Task();
        completedTask.setId(2L);
        completedTask.setCompleted(true);

        List<Task> userTasks = Arrays.asList(task, completedTask);
        when(taskRepository.findByUserIsCurrentUser()).thenReturn(userTasks);

        // When
        Page<Task> result = taskService.findAllByCurrentUserAndCompleted(true, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCompleted()).isTrue();
        verify(taskRepository).findByUserIsCurrentUser();
    }

    @Test
    void findAllWithEagerRelationships_ShouldCallRepository() {
        // Given
        Page<Task> page = new PageImpl<>(Arrays.asList(task), pageable, 1);
        when(taskRepository.findAllWithEagerRelationships(pageable)).thenReturn(page);

        // When
        Page<Task> result = taskService.findAllWithEagerRelationships(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findAllWithEagerRelationships(pageable);
    }

    @Test
    void findOne_ShouldReturnTask_WhenExists() {
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
    void findOne_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(taskRepository.findOneWithEagerRelationships(999L)).thenReturn(Optional.empty());

        // When
        Optional<Task> result = taskService.findOne(999L);

        // Then
        assertThat(result).isEmpty();
        verify(taskRepository).findOneWithEagerRelationships(999L);
    }

    @Test
    void delete_ShouldDeleteTask() {
        // When
        taskService.delete(1L);

        // Then
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void partialUpdate_ShouldUpdateAllFields_WhenAllFieldsProvided() {
        // Given
        Task existingTask = new Task();
        existingTask.setId(1L);
        existingTask.setDescription("Old description");
        existingTask.setDueDate(LocalDate.now());
        existingTask.setPriority(TaskPriority.LOW);
        existingTask.setCompleted(false);
        existingTask.setCreatedDate(Instant.now().minusSeconds(3600));
        existingTask.setLastModifiedDate(Instant.now().minusSeconds(1800));

        Task updateTask = new Task();
        updateTask.setId(1L);
        updateTask.setDescription("New description");
        updateTask.setDueDate(LocalDate.now().plusDays(1));
        updateTask.setPriority(TaskPriority.HIGH);
        updateTask.setCompleted(true);
        updateTask.setCreatedDate(Instant.now());
        updateTask.setLastModifiedDate(Instant.now());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        // When
        Optional<Task> result = taskService.partialUpdate(updateTask);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getDescription()).isEqualTo("New description");
        assertThat(result.orElseThrow().getDueDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(result.orElseThrow().getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(result.orElseThrow().getCompleted()).isTrue();
        assertThat(result.orElseThrow().getCreatedDate()).isEqualTo(updateTask.getCreatedDate());
        assertThat(result.orElseThrow().getLastModifiedDate()).isEqualTo(updateTask.getLastModifiedDate());
    }
}
