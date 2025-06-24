package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.Task;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.TaskPriority;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.TaskRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link TaskService}.
 */
@IntegrationTest
@Transactional
class TaskServiceIT {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser1;
    private User testUser2;
    private Task task1;
    private Task task2;
    private Task task3;
    private Authority userAuthority;

    @BeforeEach
    void setUp() {
        // Clean up
        taskRepository.deleteAll();
        userRepository.deleteAll();
        authorityRepository.deleteAll();
        entityManager.flush();

        // Create authority
        userAuthority = new Authority();
        userAuthority.setName(AuthoritiesConstants.USER);
        userAuthority = authorityRepository.saveAndFlush(userAuthority);

        // Create test users
        testUser1 = createUser("testuser1", "test1@example.com");
        testUser1.getAuthorities().add(userAuthority);
        testUser1 = userRepository.saveAndFlush(testUser1);

        testUser2 = createUser("testuser2", "test2@example.com");
        testUser2.getAuthorities().add(userAuthority);
        testUser2 = userRepository.saveAndFlush(testUser2);

        // Create test tasks
        task1 = createTask("Task 1 for user1", TaskPriority.HIGH, false, testUser1);
        task1 = taskRepository.saveAndFlush(task1);

        task2 = createTask("Task 2 for user1", TaskPriority.MEDIUM, true, testUser1);
        task2 = taskRepository.saveAndFlush(task2);

        task3 = createTask("Task 3 for user2", TaskPriority.LOW, false, testUser2);
        task3 = taskRepository.saveAndFlush(task3);

        entityManager.clear();
    }

    @AfterEach
    void cleanup() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        authorityRepository.deleteAll();
    }

    private User createUser(String login, String email) {
        User user = new User();
        user.setLogin(login);
        user.setPassword("$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vFedUPOdxOjPWa"); // 60 character BCrypt hash
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setActivated(true);
        user.setLangKey("en");
        user.setCreatedBy("system");
        user.setCreatedDate(Instant.now());
        return user;
    }

    private Task createTask(String description, TaskPriority priority, boolean completed, User user) {
        Task task = new Task();
        task.setDescription(description);
        task.setDueDate(LocalDate.now().plusDays(7));
        task.setPriority(priority);
        task.setCompleted(completed);
        task.setCreatedDate(Instant.now());
        task.setUser(user);
        return task;
    }

    @Test
    @WithMockUser(username = "testuser1")
    void save_withCurrentUser_shouldSetUserAutomatically() {
        // Given
        Task newTask = new Task();
        newTask.setDescription("New task without user");
        newTask.setCompleted(false);
        newTask.setPriority(TaskPriority.HIGH);

        // When
        Task savedTask = taskService.save(newTask);

        // Then
        assertThat(savedTask.getId()).isNotNull();
        assertThat(savedTask.getUser()).isNotNull();
        assertThat(savedTask.getUser().getLogin()).isEqualTo("testuser1");
        assertThat(savedTask.getCreatedDate()).isNotNull();

        // Verify persistence
        Optional<Task> foundTask = taskRepository.findById(savedTask.getId());
        assertThat(foundTask).isPresent();
        assertThat(foundTask.get().getUser().getLogin()).isEqualTo("testuser1");
    }

    @Test
    void save_withExistingUser_shouldNotOverrideUser() {
        // Given
        Task newTask = new Task();
        newTask.setDescription("Task with predefined user");
        newTask.setCompleted(false);
        newTask.setUser(testUser2);

        // When
        Task savedTask = taskService.save(newTask);

        // Then
        assertThat(savedTask.getUser()).isEqualTo(testUser2);
        assertThat(savedTask.getUser().getLogin()).isEqualTo("testuser2");
    }

    @Test
    void update_shouldModifyExistingTask() {
        // Given
        task1.setDescription("Updated description");
        task1.setPriority(TaskPriority.LOW);
        task1.setCompleted(true);

        // When
        Task updatedTask = taskService.update(task1);

        // Then
        assertThat(updatedTask.getDescription()).isEqualTo("Updated description");
        assertThat(updatedTask.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(updatedTask.getCompleted()).isTrue();

        // Verify persistence
        Optional<Task> foundTask = taskRepository.findById(task1.getId());
        assertThat(foundTask).isPresent();
        assertThat(foundTask.get().getDescription()).isEqualTo("Updated description");
    }

    @Test
    void partialUpdate_shouldUpdateOnlyProvidedFields() {
        // Given
        Task partialUpdate = new Task();
        partialUpdate.setId(task1.getId());
        partialUpdate.setDescription("Partially updated");
        partialUpdate.setCompleted(true);
        // Don't set priority - should remain unchanged

        // When
        Optional<Task> result = taskService.partialUpdate(partialUpdate);

        // Then
        assertThat(result).isPresent();
        Task updatedTask = result.get();
        assertThat(updatedTask.getDescription()).isEqualTo("Partially updated");
        assertThat(updatedTask.getCompleted()).isTrue();
        assertThat(updatedTask.getPriority()).isEqualTo(TaskPriority.HIGH); // Should remain unchanged

        // Verify persistence
        Optional<Task> foundTask = taskRepository.findById(task1.getId());
        assertThat(foundTask).isPresent();
        assertThat(foundTask.get().getDescription()).isEqualTo("Partially updated");
        assertThat(foundTask.get().getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void partialUpdate_shouldReturnEmptyForNonExistentTask() {
        // Given
        Task nonExistentTask = new Task();
        nonExistentTask.setId(999L);
        nonExistentTask.setDescription("Does not exist");

        // When
        Optional<Task> result = taskService.partialUpdate(nonExistentTask);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Task> result = taskService.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @WithMockUser(username = "testuser1")
    void findAllByCurrentUser_shouldReturnOnlyCurrentUserTasks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .allMatch(task -> task.getUser().getLogin().equals("testuser1"))
            .extracting(Task::getDescription)
            .containsExactlyInAnyOrder("Task 1 for user1", "Task 2 for user1");
    }

    @Test
    @WithMockUser(username = "testuser2")
    void findAllByCurrentUser_shouldReturnDifferentUserTasks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUser().getLogin()).isEqualTo("testuser2");
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Task 3 for user2");
    }

    @Test
    @WithMockUser(username = "testuser1")
    void findAllByCurrentUserAndCompleted_shouldFilterByCompletionStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When - find completed tasks
        Page<Task> completedTasks = taskService.findAllByCurrentUserAndCompleted(true, pageable);

        // Then
        assertThat(completedTasks.getContent()).hasSize(1);
        assertThat(completedTasks.getContent().get(0).getCompleted()).isTrue();
        assertThat(completedTasks.getContent().get(0).getDescription()).isEqualTo("Task 2 for user1");

        // When - find incomplete tasks
        Page<Task> incompleteTasks = taskService.findAllByCurrentUserAndCompleted(false, pageable);

        // Then
        assertThat(incompleteTasks.getContent()).hasSize(1);
        assertThat(incompleteTasks.getContent().get(0).getCompleted()).isFalse();
        assertThat(incompleteTasks.getContent().get(0).getDescription()).isEqualTo("Task 1 for user1");
    }

    @Test
    void findAllWithEagerRelationships_shouldLoadUserRelationships() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Task> result = taskService.findAllWithEagerRelationships(pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);

        // Clear entity manager to test eager loading
        entityManager.clear();

        // All tasks should have users loaded
        assertThat(result.getContent()).allMatch(task -> task.getUser() != null);
        assertThat(result.getContent())
            .extracting(task -> task.getUser().getLogin())
            .containsExactlyInAnyOrder("testuser1", "testuser1", "testuser2");
    }

    @Test
    void findOne_shouldReturnTaskWithEagerRelationships() {
        // When
        Optional<Task> result = taskService.findOne(task1.getId());

        // Then
        assertThat(result).isPresent();
        Task foundTask = result.get();

        // Clear entity manager to test eager loading
        entityManager.clear();

        assertThat(foundTask.getUser()).isNotNull();
        assertThat(foundTask.getUser().getLogin()).isEqualTo("testuser1");
        assertThat(foundTask.getDescription()).isEqualTo("Task 1 for user1");
    }

    @Test
    void findOne_shouldReturnEmptyForNonExistentTask() {
        // When
        Optional<Task> result = taskService.findOne(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void delete_shouldRemoveTaskFromDatabase() {
        // Given
        Long taskId = task1.getId();
        assertThat(taskRepository.findById(taskId)).isPresent();

        // When
        taskService.delete(taskId);
        entityManager.flush();

        // Then
        assertThat(taskRepository.findById(taskId)).isEmpty();
        assertThat(taskRepository.count()).isEqualTo(2); // Should have 2 tasks left
    }

    @Test
    @WithMockUser(username = "testuser1")
    void integrationTest_completeTaskWorkflow() {
        // Step 1: Create a new task
        Task newTask = new Task();
        newTask.setDescription("Complete workflow test");
        newTask.setPriority(TaskPriority.HIGH);
        newTask.setDueDate(LocalDate.now().plusDays(5));

        Task savedTask = taskService.save(newTask);
        assertThat(savedTask.getUser().getLogin()).isEqualTo("testuser1");
        assertThat(savedTask.getCompleted()).isFalse();

        // Step 2: Partially update the task
        Task partialUpdate = new Task();
        partialUpdate.setId(savedTask.getId());
        partialUpdate.setCompleted(true);

        Optional<Task> updatedTask = taskService.partialUpdate(partialUpdate);
        assertThat(updatedTask).isPresent();
        assertThat(updatedTask.get().getCompleted()).isTrue();
        assertThat(updatedTask.get().getDescription()).isEqualTo("Complete workflow test");

        // Step 3: Find the task by current user
        Page<Task> userTasks = taskService.findAllByCurrentUser(PageRequest.of(0, 10));
        assertThat(userTasks.getContent()).hasSize(3); // 2 existing + 1 new
        assertThat(userTasks.getContent()).anyMatch(task -> task.getDescription().equals("Complete workflow test"));

        // Step 4: Find completed tasks for current user
        Page<Task> completedTasks = taskService.findAllByCurrentUserAndCompleted(true, PageRequest.of(0, 10));
        assertThat(completedTasks.getContent()).hasSize(2); // 1 existing + 1 new
        assertThat(completedTasks.getContent()).anyMatch(task -> task.getDescription().equals("Complete workflow test"));

        // Step 5: Delete the task
        taskService.delete(savedTask.getId());
        entityManager.flush();

        Optional<Task> deletedTask = taskService.findOne(savedTask.getId());
        assertThat(deletedTask).isEmpty();
    }

    @Test
    @WithMockUser(username = "nonexistentuser")
    void findAllByCurrentUser_shouldReturnEmptyForNonExistentUser() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Task> result = taskService.findAllByCurrentUser(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void taskPagination_shouldWorkCorrectly() {
        // Given
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        // When
        Page<Task> page1 = taskService.findAll(firstPage);
        Page<Task> page2 = taskService.findAll(secondPage);

        // Then
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.hasNext()).isTrue();

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getTotalElements()).isEqualTo(3);
        assertThat(page2.hasNext()).isFalse();

        // Ensure no overlap between pages
        List<Long> page1Ids = page1.getContent().stream().map(Task::getId).toList();
        List<Long> page2Ids = page2.getContent().stream().map(Task::getId).toList();
        assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);
    }

    @Test
    @WithMockUser(username = "testuser1")
    void userTaskPagination_shouldWorkCorrectly() {
        // Given
        Pageable firstPage = PageRequest.of(0, 1);
        Pageable secondPage = PageRequest.of(1, 1);

        // When
        Page<Task> page1 = taskService.findAllByCurrentUser(firstPage);
        Page<Task> page2 = taskService.findAllByCurrentUser(secondPage);

        // Then
        assertThat(page1.getContent()).hasSize(1);
        assertThat(page1.getTotalElements()).isEqualTo(2);
        assertThat(page1.hasNext()).isTrue();

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getTotalElements()).isEqualTo(2);
        assertThat(page2.hasNext()).isFalse();

        // Both tasks should belong to testuser1
        assertThat(page1.getContent().get(0).getUser().getLogin()).isEqualTo("testuser1");
        assertThat(page2.getContent().get(0).getUser().getLogin()).isEqualTo("testuser1");
    }
}
