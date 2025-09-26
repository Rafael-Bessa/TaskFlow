package bessa.morangon.rafael.TaskFlow.controller;

import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.GlobalExceptionHandler;
import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.ResourceNotFoundException;
import bessa.morangon.rafael.TaskFlow.domain.dto.TaskDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.Priority;
import bessa.morangon.rafael.TaskFlow.domain.model.Status;
import bessa.morangon.rafael.TaskFlow.domain.model.Task;
import bessa.morangon.rafael.TaskFlow.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskController Tests")
class TaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private ObjectMapper objectMapper;
    private Task validTask;
    private TaskDTO taskDTO;
    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        mockPrincipal = () -> "john@email.com";

        validTask = new Task();
        validTask.setId(1L);
        validTask.setTitle("Test Task");
        validTask.setDescription("Description");
        validTask.setDueDate(LocalDateTime.now().plusDays(1));
        validTask.setPriority(Priority.MEDIUM);
        validTask.setStatus(Status.PENDING);

        taskDTO = new TaskDTO();
        taskDTO.setId(1L);
        taskDTO.setTitle("Test Task");
        taskDTO.setDescription("Description");
        taskDTO.setDueDate(validTask.getDueDate());
        taskDTO.setPriority(Priority.MEDIUM);
        taskDTO.setStatus(Status.PENDING);
    }

    // ================== GET BY ID ==================
    @Test
    @DisplayName("Should return task when valid ID is provided")
    void getTaskById_ShouldReturnTask_WhenValidId() throws Exception {
        when(taskService.getTaskById(1L, mockPrincipal)).thenReturn(ResponseEntity.ok(taskDTO));

        mockMvc.perform(get("/tasks/1").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));

        verify(taskService, times(1)).getTaskById(1L, mockPrincipal);
    }

    @Test
    @DisplayName("Should return 404 when task not found")
    void getTaskById_ShouldReturn404_WhenTaskNotFound() throws Exception {
        when(taskService.getTaskById(999L, mockPrincipal))
                .thenThrow(new ResourceNotFoundException("Task", "id", 999L));

        mockMvc.perform(get("/tasks/999").principal(mockPrincipal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: '999'"));

        verify(taskService, times(1)).getTaskById(999L, mockPrincipal);
    }

    // ================== GET ALL TASKS (WITHOUT PAGINATION) ==================
    @Test
    @DisplayName("Should return all tasks for user")
    void getAllTasks_ShouldReturnTasks() throws Exception {
        List<TaskDTO> taskList = Arrays.asList(taskDTO);
        when(taskService.getAllTasksWithoutPagination(mockPrincipal))
                .thenReturn(ResponseEntity.ok(taskList));

        mockMvc.perform(get("/tasks").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Task"));

        verify(taskService, times(1)).getAllTasksWithoutPagination(mockPrincipal);
    }

    // ================== GET ALL TASKS PAGED ==================
    @Test
    @DisplayName("Should return paged tasks")
    void getAllTasksPaged_ShouldReturnPagedTasks() throws Exception {
        // Criar PageRequest igual ao que o MockMvc vai construir
        Pageable pageable = PageRequest.of(0, 5);
        Page<TaskDTO> taskPage = new PageImpl<>(Arrays.asList(taskDTO), pageable, 1);

        when(taskService.getAllTasks(any(Pageable.class), eq(mockPrincipal)))
                .thenReturn(ResponseEntity.ok(taskPage));

        mockMvc.perform(get("/tasks/paged")
                        .principal(mockPrincipal)
                        .param("size", "5")
                        .param("page", "0")
                        .param("sort", "createdAt,asc") // Melhor explicitar
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Test Task"));

        verify(taskService, times(1)).getAllTasks(any(Pageable.class), eq(mockPrincipal));
    }

    // ================== CREATE TASK ==================
    @Test
    @DisplayName("Should create task successfully")
    void createTask_ShouldCreateTask_WhenValid() throws Exception {
        URI location = URI.create("/tasks/1");
        when(taskService.createNewTask(any(Task.class), any(UriComponentsBuilder.class), eq(mockPrincipal)))
                .thenReturn(ResponseEntity.created(location).body(taskDTO));

        String json = objectMapper.writeValueAsString(validTask);

        mockMvc.perform(post("/tasks")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/tasks/1"))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));

        verify(taskService, times(1)).createNewTask(any(Task.class), any(UriComponentsBuilder.class), eq(mockPrincipal));
    }

    // ================== UPDATE TASK ==================
    @Test
    @DisplayName("Should update task successfully")
    void updateTask_ShouldUpdateTask_WhenValid() throws Exception {
        when(taskService.updateTask(any(Task.class), eq(1L), eq(mockPrincipal)))
                .thenReturn(ResponseEntity.ok(taskDTO));

        String json = objectMapper.writeValueAsString(validTask);

        mockMvc.perform(put("/tasks/1")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));

        verify(taskService, times(1)).updateTask(any(Task.class), eq(1L), eq(mockPrincipal));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent task")
    void updateTask_ShouldReturn404_WhenTaskNotFound() throws Exception {
        when(taskService.updateTask(any(Task.class), eq(999L), eq(mockPrincipal)))
                .thenThrow(new ResourceNotFoundException("Task", "id", 999L));

        String json = objectMapper.writeValueAsString(validTask);

        mockMvc.perform(put("/tasks/999")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: '999'"));

        verify(taskService, times(1)).updateTask(any(Task.class), eq(999L), eq(mockPrincipal));
    }

    // ================== DELETE TASK ==================
    @Test
    @DisplayName("Should delete task successfully")
    void deleteTask_ShouldDeleteTask_WhenValidId() throws Exception {
        when(taskService.deleteTask(1L, mockPrincipal)).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/tasks/1").principal(mockPrincipal))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(1L, mockPrincipal);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent task")
    void deleteTask_ShouldReturn404_WhenTaskNotFound() throws Exception {
        when(taskService.deleteTask(999L, mockPrincipal))
                .thenThrow(new ResourceNotFoundException("Task", "id", 999L));

        mockMvc.perform(delete("/tasks/999").principal(mockPrincipal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: '999'"));

        verify(taskService, times(1)).deleteTask(999L, mockPrincipal);
    }
}
