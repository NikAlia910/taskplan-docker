package com.mycompany.myapp.web.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.domain.Task;
import com.mycompany.myapp.domain.enumeration.TaskPriority;
import com.mycompany.myapp.repository.TaskRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.TaskService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskResource.class)
@WithMockUser(authorities = AuthoritiesConstants.USER)
class TaskResourceTest {

    private static final String DEFAULT_DESCRIPTION = "Test task description";
    private static final String UPDATED_DESCRIPTION = "Updated task description";
    private static final LocalDate DEFAULT_DUE_DATE = LocalDate.of(2024, 1, 1);
    private static final LocalDate UPDATED_DUE_DATE = LocalDate.of(2024, 2, 1);
    private static final TaskPriority DEFAULT_PRIORITY = TaskPriority.HIGH;
    private static final TaskPriority UPDATED_PRIORITY = TaskPriority.LOW;
    private static final Boolean DEFAULT_COMPLETED = false;
    private static final Boolean UPDATED_COMPLETED = true;
    private static final Instant DEFAULT_CREATED_DATE = Instant.ofEpochMilli(0L);
    private static final String ENTITY_API_URL = "/api/tasks";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private MockMvc restTaskMockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Task task;

    @BeforeEach
    void initTest() {
        task = createEntity();
    }

    public static Task createEntity() {
        Task task = new Task()
            .description(DEFAULT_DESCRIPTION)
            .dueDate(DEFAULT_DUE_DATE)
            .priority(DEFAULT_PRIORITY)
            .completed(DEFAULT_COMPLETED)
            .createdDate(DEFAULT_CREATED_DATE);
        return task;
    }

    @Test
    void createTask() throws Exception {
        // Given
        Task savedTask = new Task();
        savedTask.setId(1L);
        savedTask.setDescription(DEFAULT_DESCRIPTION);
        savedTask.setDueDate(DEFAULT_DUE_DATE);
        savedTask.setPriority(DEFAULT_PRIORITY);
        savedTask.setCompleted(DEFAULT_COMPLETED);
        savedTask.setCreatedDate(DEFAULT_CREATED_DATE);

        when(taskService.save(any(Task.class))).thenReturn(savedTask);

        // When & Then
        restTaskMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(task))
            )
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.dueDate").value(DEFAULT_DUE_DATE.toString()))
            .andExpect(jsonPath("$.priority").value(DEFAULT_PRIORITY.toString()))
            .andExpect(jsonPath("$.completed").value(DEFAULT_COMPLETED));

        verify(taskService).save(any(Task.class));
    }

    @Test
    void createTaskWithExistingId() throws Exception {
        // Given
        task.setId(1L);

        // When & Then
        restTaskMockMvc
            .perform(
                post(ENTITY_API_URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(task))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTasks() throws Exception {
        // Given
        task.setId(1L);
        List<Task> tasks = Arrays.asList(task);
        Page<Task> page = new PageImpl<>(tasks, PageRequest.of(0, 20), 1);
        when(taskService.findAllByCurrentUser(any(Pageable.class))).thenReturn(page);

        // When & Then
        restTaskMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].dueDate").value(hasItem(DEFAULT_DUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].priority").value(hasItem(DEFAULT_PRIORITY.toString())))
            .andExpect(jsonPath("$.[*].completed").value(hasItem(DEFAULT_COMPLETED)));
    }

    @Test
    void getTask() throws Exception {
        // Given
        task.setId(1L);
        when(taskService.findOne(1L)).thenReturn(Optional.of(task));

        // When & Then
        restTaskMockMvc
            .perform(get(ENTITY_API_URL_ID, 1L))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.dueDate").value(DEFAULT_DUE_DATE.toString()))
            .andExpect(jsonPath("$.priority").value(DEFAULT_PRIORITY.toString()))
            .andExpect(jsonPath("$.completed").value(DEFAULT_COMPLETED));
    }

    @Test
    void getNonExistingTask() throws Exception {
        // Given
        when(taskService.findOne(anyLong())).thenReturn(Optional.empty());

        // When & Then
        restTaskMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void putExistingTask() throws Exception {
        // Given
        task.setId(1L);
        task.setDescription(UPDATED_DESCRIPTION);
        task.setDueDate(UPDATED_DUE_DATE);
        task.setPriority(UPDATED_PRIORITY);
        task.setCompleted(UPDATED_COMPLETED);

        when(taskRepository.existsById(1L)).thenReturn(true);
        when(taskService.update(any(Task.class))).thenReturn(task);

        // When & Then
        restTaskMockMvc
            .perform(
                put(ENTITY_API_URL_ID, 1L)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(task))
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.description").value(UPDATED_DESCRIPTION))
            .andExpect(jsonPath("$.dueDate").value(UPDATED_DUE_DATE.toString()))
            .andExpect(jsonPath("$.priority").value(UPDATED_PRIORITY.toString()))
            .andExpect(jsonPath("$.completed").value(UPDATED_COMPLETED));

        verify(taskService).update(any(Task.class));
        verify(taskRepository).existsById(1L);
    }

    @Test
    void putNonExistingTask() throws Exception {
        // Given
        task.setId(2L);
        when(taskRepository.existsById(2L)).thenReturn(false);

        // When & Then
        restTaskMockMvc
            .perform(
                put(ENTITY_API_URL_ID, 2L)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(task))
            )
            .andExpect(status().isBadRequest());

        verify(taskRepository).existsById(2L);
    }

    @Test
    void deleteTask() throws Exception {
        // Given
        doNothing().when(taskService).delete(1L);

        // When & Then
        restTaskMockMvc
            .perform(delete(ENTITY_API_URL_ID, 1L).with(csrf()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(taskService).delete(1L);
    }

    @Test
    void partialUpdateTask() throws Exception {
        // Given
        Task updatedTask = new Task();
        updatedTask.setId(1L);
        updatedTask.setDescription(UPDATED_DESCRIPTION);
        updatedTask.setCompleted(UPDATED_COMPLETED);

        when(taskRepository.existsById(1L)).thenReturn(true);
        when(taskService.partialUpdate(any(Task.class))).thenReturn(Optional.of(updatedTask));

        // When & Then
        restTaskMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, 1L)
                    .with(csrf())
                    .contentType("application/merge-patch+json")
                    .content(objectMapper.writeValueAsString(updatedTask))
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.description").value(UPDATED_DESCRIPTION))
            .andExpect(jsonPath("$.completed").value(UPDATED_COMPLETED));

        verify(taskService).partialUpdate(any(Task.class));
        verify(taskRepository).existsById(1L);
    }
}
