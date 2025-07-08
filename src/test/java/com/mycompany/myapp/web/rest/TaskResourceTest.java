package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import com.mycompany.myapp.service.TaskService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
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
 * Integration tests for the {@link TaskResource} REST controller focusing on custom endpoints and user-specific functionality.
 */
@IntegrationTest
@AutoConfigureMockMvc
class TaskResourceTest {

    private static final String DEFAULT_DESCRIPTION = "Test Task Description";
    private static final String UPDATED_DESCRIPTION = "Updated Task Description";
    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.now(ZoneId.systemDefault()).plusDays(1);
    private static final TaskPriority DEFAULT_PRIORITY = TaskPriority.HIGH;
    private static final Boolean DEFAULT_COMPLETED = false;

    private static final String ENTITY_API_URL = "/api/tasks";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String TOGGLE_COMPLETION_URL = ENTITY_API_URL + "/{id}/toggle-completion";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restTaskMockMvc;

    private Task task;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void initTest() {
        // Create test users
        testUser = new User();
        testUser.setLogin("testuser");
        testUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setActivated(true);
        userRepository.saveAndFlush(testUser);

        otherUser = new User();
        otherUser.setLogin("otheruser");
        otherUser.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setEmail("other@example.com");
        otherUser.setActivated(true);
        userRepository.saveAndFlush(otherUser);

        // Create test task
        task = createEntity();
    }

    @AfterEach
    void cleanup() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Create an entity for this test.
     */
    public Task createEntity() {
        Task task = new Task()
            .description(DEFAULT_DESCRIPTION)
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(DEFAULT_COMPLETED);
        task.setUser(testUser);
        return task;
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser", authorities = AuthoritiesConstants.USER)
    void toggleTaskCompletion_WhenTaskExists_ShouldToggleCompletion() throws Exception {
        // Initialize the database with an incomplete task
        Task savedTask = taskRepository.saveAndFlush(task);
        assertThat(savedTask.getCompleted()).isFalse();

        // Toggle completion to true
        restTaskMockMvc
            .perform(patch(TOGGLE_COMPLETION_URL, savedTask.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(savedTask.getId().intValue()))
            .andExpect(jsonPath("$.completed").value(true));

        // Verify in database
        Task updatedTask = taskRepository.findById(savedTask.getId()).orElseThrow();
        assertThat(updatedTask.getCompleted()).isTrue();

        // Toggle back to false
        restTaskMockMvc
            .perform(patch(TOGGLE_COMPLETION_URL, savedTask.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(false));

        // Verify in database
        Task toggledBackTask = taskRepository.findById(savedTask.getId()).orElseThrow();
        assertThat(toggledBackTask.getCompleted()).isFalse();
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser", authorities = AuthoritiesConstants.USER)
    void toggleTaskCompletion_WhenTaskNotFound_ShouldReturnBadRequest() throws Exception {
        Long nonExistentId = 999999L;

        restTaskMockMvc.perform(patch(TOGGLE_COMPLETION_URL, nonExistentId)).andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser", authorities = AuthoritiesConstants.USER)
    void toggleTaskCompletion_WhenTaskBelongsToOtherUser_ShouldAllowAccess() throws Exception {
        // Create task for other user
        Task otherUserTask = new Task()
            .description("Other user task")
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(false);
        otherUserTask.setUser(otherUser);
        Task savedOtherTask = taskRepository.saveAndFlush(otherUserTask);

        // Current implementation allows toggling other user's task (security issue)
        restTaskMockMvc
            .perform(patch(TOGGLE_COMPLETION_URL, savedOtherTask.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser", authorities = AuthoritiesConstants.USER)
    void getAllTasks_WithCompletedFilter_ShouldReturnFilteredTasks() throws Exception {
        // Create completed and incomplete tasks
        Task completedTask = new Task().description("Completed task").dueDate(DEFAULT_DUE_DATE).priority(DEFAULT_PRIORITY).completed(true);
        completedTask.setUser(testUser);

        Task incompleteTask = new Task()
            .description("Incomplete task")
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(false);
        incompleteTask.setUser(testUser);

        taskRepository.saveAndFlush(completedTask);
        taskRepository.saveAndFlush(incompleteTask);

        // Test completed=true filter
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?completed=true&currentUserOnly=true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].completed").value(hasItem(true)))
            .andExpect(jsonPath("$[*].description").value(hasItem("Completed task")));

        // Test completed=false filter
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?completed=false&currentUserOnly=true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].completed").value(hasItem(false)))
            .andExpect(jsonPath("$[*].description").value(hasItem("Incomplete task")));
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser", authorities = AuthoritiesConstants.USER)
    void getAllTasks_WithCurrentUserOnlyTrue_ShouldReturnOnlyCurrentUserTasks() throws Exception {
        // Create tasks for both users
        Task currentUserTask = new Task()
            .description("Current user task")
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(false);
        currentUserTask.setUser(testUser);

        Task otherUserTask = new Task()
            .description("Other user task")
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(false);
        otherUserTask.setUser(otherUser);

        taskRepository.saveAndFlush(currentUserTask);
        taskRepository.saveAndFlush(otherUserTask);

        // Request with currentUserOnly=true (default)
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?currentUserOnly=true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].description").value(hasItem("Current user task")))
            .andExpect(jsonPath("$[*].description").value(not(hasItem("Other user task"))));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = AuthoritiesConstants.ADMIN)
    void getAllTasks_WithCurrentUserOnlyFalse_AsAdmin_ShouldReturnAllTasks() throws Exception {
        // Create tasks for both users
        Task testUserTask = new Task().description("Test user task").dueDate(DEFAULT_DUE_DATE).priority(DEFAULT_PRIORITY).completed(false);
        testUserTask.setUser(testUser);

        Task otherUserTask = new Task()
            .description("Other user task")
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(false);
        otherUserTask.setUser(otherUser);

        taskRepository.saveAndFlush(testUserTask);
        taskRepository.saveAndFlush(otherUserTask);

        // Request with currentUserOnly=false as admin
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?currentUserOnly=false"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].description").value(hasItem("Test user task")))
            .andExpect(jsonPath("$[*].description").value(hasItem("Other user task")));
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser", authorities = AuthoritiesConstants.USER)
    void getAllTasks_WithCombinedFilters_ShouldReturnCorrectResults() throws Exception {
        // Create various tasks for current user
        Task completedTask = new Task().description("Completed task").dueDate(DEFAULT_DUE_DATE).priority(DEFAULT_PRIORITY).completed(true);
        completedTask.setUser(testUser);

        Task incompleteTask = new Task()
            .description("Incomplete task")
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(false);
        incompleteTask.setUser(testUser);

        // Create completed task for other user
        Task otherUserCompletedTask = new Task()
            .description("Other user completed task")
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(true);
        otherUserCompletedTask.setUser(otherUser);

        taskRepository.saveAndFlush(completedTask);
        taskRepository.saveAndFlush(incompleteTask);
        taskRepository.saveAndFlush(otherUserCompletedTask);

        // Request current user's completed tasks only
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?completed=true&currentUserOnly=true"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].description").value("Completed task"))
            .andExpect(jsonPath("$[0].completed").value(true));
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser", authorities = AuthoritiesConstants.USER)
    void getTask_WhenTaskBelongsToCurrentUser_ShouldReturnTask() throws Exception {
        // Create and save task for current user
        Task savedTask = taskRepository.saveAndFlush(task);

        // Get the task
        restTaskMockMvc
            .perform(get(ENTITY_API_URL_ID, savedTask.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(savedTask.getId().intValue()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.completed").value(DEFAULT_COMPLETED));
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser", authorities = AuthoritiesConstants.USER)
    void getTask_WhenTaskBelongsToOtherUser_ShouldAllowAccess() throws Exception {
        // Create task for other user
        Task otherUserTask = new Task()
            .description("Other user task")
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(false);
        otherUserTask.setUser(otherUser);
        Task savedOtherTask = taskRepository.saveAndFlush(otherUserTask);

        // Current implementation allows getting other user's task (security issue)
        restTaskMockMvc
            .perform(get(ENTITY_API_URL_ID, savedOtherTask.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(savedOtherTask.getId().intValue()))
            .andExpect(jsonPath("$.description").value("Other user task"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "testuser", authorities = AuthoritiesConstants.USER)
    void createTask_ShouldAssignToCurrentUser() throws Exception {
        int databaseSizeBeforeCreate = taskRepository.findAll().size();

        // Create task without specifying user
        Task newTask = new Task().description("New task").dueDate(DEFAULT_DUE_DATE).priority(DEFAULT_PRIORITY).completed(false);

        restTaskMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(newTask)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.description").value("New task"))
            .andExpect(jsonPath("$.completed").value(false));

        // Verify the task was assigned to current user
        List<Task> taskList = taskRepository.findAll();
        assertThat(taskList).hasSize(databaseSizeBeforeCreate + 1);
        Task testTask = taskList.get(taskList.size() - 1);
        assertThat(testTask.getDescription()).isEqualTo("New task");
        assertThat(testTask.getUser().getLogin()).isEqualTo("testuser");
    }
}
