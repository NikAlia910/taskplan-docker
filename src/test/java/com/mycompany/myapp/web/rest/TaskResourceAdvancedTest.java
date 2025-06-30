package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.TaskAsserts.assertTaskUpdatableFieldsEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Task;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.TaskPriority;
import com.mycompany.myapp.repository.TaskRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for advanced {@link TaskResource} functionality.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class TaskResourceAdvancedTest {

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DUE_DATE = LocalDate.now();

    private static final TaskPriority DEFAULT_PRIORITY = TaskPriority.LOW;
    private static final TaskPriority UPDATED_PRIORITY = TaskPriority.HIGH;

    private static final Boolean DEFAULT_COMPLETED = false;
    private static final Boolean UPDATED_COMPLETED = true;

    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/tasks";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTaskMockMvc;

    private Task task;
    private User user;
    private User otherUser;
    private Task insertedTask;

    @BeforeEach
    void initTest() {
        user = createAndSaveUser("testuser", "test@example.com");
        otherUser = createAndSaveUser("otheruser", "other@example.com");
        task = createEntityForUser(user);
    }

    @AfterEach
    void cleanup() {
        if (insertedTask != null) {
            taskRepository.delete(insertedTask);
        }
    }

    @Test
    @Transactional
    @WithMockUser("testuser")
    void toggleTaskCompletion() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);
        Long taskId = insertedTask.getId();
        Boolean originalStatus = insertedTask.getCompleted();

        // Toggle completion
        restTaskMockMvc
            .perform(patch(ENTITY_API_URL + "/{id}/toggle-completion", taskId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(taskId))
            .andExpect(jsonPath("$.completed").value(!originalStatus));

        // Verify in database
        Task updatedTask = taskRepository.findById(taskId).orElseThrow();
        assertThat(updatedTask.getCompleted()).isEqualTo(!originalStatus);
    }

    @Test
    @Transactional
    @WithMockUser("testuser")
    void toggleTaskCompletionTwice() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);
        Long taskId = insertedTask.getId();
        Boolean originalStatus = insertedTask.getCompleted();

        // Toggle completion first time
        restTaskMockMvc
            .perform(patch(ENTITY_API_URL + "/{id}/toggle-completion", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(!originalStatus));

        // Toggle completion second time
        restTaskMockMvc
            .perform(patch(ENTITY_API_URL + "/{id}/toggle-completion", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(originalStatus));

        // Verify back to original state
        Task finalTask = taskRepository.findById(taskId).orElseThrow();
        assertThat(finalTask.getCompleted()).isEqualTo(originalStatus);
    }

    @Test
    @Transactional
    void toggleNonExistentTask() throws Exception {
        // Try to toggle a non-existent task
        restTaskMockMvc.perform(patch(ENTITY_API_URL + "/{id}/toggle-completion", Long.MAX_VALUE)).andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithMockUser("testuser")
    void getAllTasksWithCompletedFilter() throws Exception {
        // Create completed and uncompleted tasks
        Task completedTask = createEntityForUser(user);
        completedTask.setCompleted(true);
        completedTask.setDescription("Completed Task");
        taskRepository.saveAndFlush(completedTask);

        Task uncompletedTask = createEntityForUser(user);
        uncompletedTask.setCompleted(false);
        uncompletedTask.setDescription("Uncompleted Task");
        taskRepository.saveAndFlush(uncompletedTask);

        // Test filter for completed tasks only
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?completed=true&currentUserOnly=true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].completed").value(hasItem(true)))
            .andExpect(jsonPath("$.[*].description").value(hasItem("Completed Task")));

        // Test filter for uncompleted tasks only
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?completed=false&currentUserOnly=true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].completed").value(hasItem(false)))
            .andExpect(jsonPath("$.[*].description").value(hasItem("Uncompleted Task")));
    }

    @Test
    @Transactional
    @WithMockUser("testuser")
    void getAllTasksCurrentUserOnly() throws Exception {
        // Create task for current user
        Task userTask = createEntityForUser(user);
        userTask.setDescription("User Task");
        taskRepository.saveAndFlush(userTask);

        // Create task for other user
        Task otherUserTask = createEntityForUser(otherUser);
        otherUserTask.setDescription("Other User Task");
        taskRepository.saveAndFlush(otherUserTask);

        // Test currentUserOnly=true (default)
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?currentUserOnly=true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].description").value(hasItem("User Task")));

        // Test currentUserOnly=false (should return all tasks)
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?currentUserOnly=false"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    @Transactional
    void createTaskWithValidation() throws Exception {
        Task invalidTask = new Task();
        // Missing required description
        invalidTask.setDueDate(DEFAULT_DUE_DATE);
        invalidTask.setPriority(DEFAULT_PRIORITY);

        restTaskMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(invalidTask)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithMockUser("testuser")
    void createTaskWithDefaultValues() throws Exception {
        Task newTask = new Task();
        newTask.setDescription("New Task");
        newTask.setPriority(TaskPriority.MEDIUM);
        newTask.setDueDate(LocalDate.now().plusDays(7));
        // Not setting completed or createdDate - should be set automatically

        var response = restTaskMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(newTask)))
            .andExpect(status().isCreated())
            .andReturn();

        Task returnedTask = om.readValue(response.getResponse().getContentAsString(), Task.class);

        // Verify default values are set
        assertThat(returnedTask.getCompleted()).isFalse();
        assertThat(returnedTask.getCreatedDate()).isNotNull();
        assertThat(returnedTask.getUser()).isNotNull();
        assertThat(returnedTask.getUser().getLogin()).isEqualTo("testuser");

        insertedTask = returnedTask; // For cleanup
    }

    @Test
    @Transactional
    @WithMockUser("testuser")
    void partialUpdateTaskDescription() throws Exception {
        // Initialize the database
        insertedTask = taskRepository.saveAndFlush(task);

        int databaseSizeBeforeUpdate = taskRepository.findAll().size();

        // Create partial update with only description
        Task partialUpdateTask = new Task();
        partialUpdateTask.setId(insertedTask.getId());
        partialUpdateTask.setDescription("Partially Updated Description");

        restTaskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdateTask.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdateTask))
            )
            .andExpect(status().isOk());

        // Validate that only description was updated
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeUpdate);
        Task testTask = taskList.get(taskList.size() - 1);
        assertThat(testTask.getDescription()).isEqualTo("Partially Updated Description");
        assertThat(testTask.getDueDate()).isEqualTo(DEFAULT_DUE_DATE); // Should remain unchanged
        assertThat(testTask.getPriority()).isEqualTo(DEFAULT_PRIORITY); // Should remain unchanged
    }

    @Test
    @Transactional
    void putTaskWithMismatchedId() throws Exception {
        task.setId(longCount.incrementAndGet());

        restTaskMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(task))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void putTaskWithNullId() throws Exception {
        task.setId(null);

        restTaskMockMvc
            .perform(put(ENTITY_API_URL_ID, 1L).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(task)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithMockUser("testuser")
    void getAllTasksWithPagination() throws Exception {
        // Create multiple tasks
        for (int i = 0; i < 25; i++) {
            Task task = createEntityForUser(user);
            task.setDescription("Task " + i);
            taskRepository.saveAndFlush(task);
        }

        // Test pagination
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?currentUserOnly=true&page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?currentUserOnly=true&page=1&size=10"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    @Transactional
    @WithMockUser("testuser")
    void getAllTasksWithSorting() throws Exception {
        // Create tasks with different priorities
        Task lowPriorityTask = createEntityForUser(user);
        lowPriorityTask.setPriority(TaskPriority.LOW);
        lowPriorityTask.setDescription("Low Priority");
        taskRepository.saveAndFlush(lowPriorityTask);

        Task highPriorityTask = createEntityForUser(user);
        highPriorityTask.setPriority(TaskPriority.HIGH);
        highPriorityTask.setDescription("High Priority");
        taskRepository.saveAndFlush(highPriorityTask);

        // Test sorting by priority
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?currentUserOnly=true&sort=priority,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    private User createAndSaveUser(String login, String email) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActivated(true);
        user.setLangKey("en");
        return userRepository.saveAndFlush(user);
    }

    private Task createEntityForUser(User user) {
        Task task = new Task();
        task.setDescription(DEFAULT_DESCRIPTION);
        task.setDueDate(DEFAULT_DUE_DATE);
        task.setPriority(DEFAULT_PRIORITY);
        task.setCompleted(DEFAULT_COMPLETED);
        task.setCreatedDate(DEFAULT_CREATED_DATE);
        task.setUser(user);
        return task;
    }
}
