package bessa.morangon.rafael.TaskFlow.service;

import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.ResourceNotFoundException;
import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.UnauthorizedAccessException;
import bessa.morangon.rafael.TaskFlow.domain.dto.TaskDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.Priority;
import bessa.morangon.rafael.TaskFlow.domain.model.Status;
import bessa.morangon.rafael.TaskFlow.domain.model.Task;
import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.domain.repository.TaskRepository;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private Principal principal;

    @InjectMocks
    private TaskService taskService;

    // Dados de teste reutilizáveis
    private User validUser;
    private User anotherUser;
    private Task validTask;
    private TaskDTO taskDTO;
    private UriComponentsBuilder uriBuilder;

    @BeforeEach
    void setUp() {
        // Usuario proprietário da task
        validUser = new User();
        validUser.setId(1L);
        validUser.setFullName("João Silva");
        validUser.setEmail("joao@email.com");

        // Outro usuário (para testar autorização)
        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setFullName("Maria Santos");
        anotherUser.setEmail("maria@email.com");

        // Task válida
        validTask = new Task();
        validTask.setId(1L);
        validTask.setTitle("Estudar Spring Boot");
        validTask.setDescription("Criar testes unitários");
        validTask.setDueDate(LocalDateTime.now().plusDays(7));
        validTask.setPriority(Priority.HIGH);
        validTask.setStatus(Status.PENDING);
        validTask.setUser(validUser);

        // TaskDTO
        taskDTO = new TaskDTO();
        taskDTO.setId(1L);
        taskDTO.setTitle("Estudar Spring Boot");
        taskDTO.setDescription("Criar testes unitários");
        taskDTO.setPriority(Priority.HIGH);
        taskDTO.setStatus(Status.PENDING);

        // URI Builder
        uriBuilder = UriComponentsBuilder.newInstance();

        // Mock Principal - sempre retorna o email do usuário válido
        lenient().when(principal.getName()).thenReturn("joao@email.com");
    }

    @Nested
    @DisplayName("getTaskById Tests")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Should return task when user is owner")
        void shouldReturnTaskWhenUserIsOwner() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.of(validTask));
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(modelMapper.map(validTask, TaskDTO.class)).thenReturn(taskDTO);

            // When
            ResponseEntity<TaskDTO> response = taskService.getTaskById(1L, principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(1L);
            assertThat(response.getBody().getTitle()).isEqualTo("Estudar Spring Boot");

            verify(taskRepository).findById(1L);
            verify(userRepository).findByEmail("joao@email.com");
            verify(modelMapper).map(validTask, TaskDTO.class);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.getTaskById(999L, principal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task")
                    .hasMessageContaining("id")
                    .hasMessageContaining("999");

            verify(taskRepository).findById(999L);
            verify(userRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.of(validTask));
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.getTaskById(1L, principal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("email");

            verify(taskRepository).findById(1L);
            verify(userRepository).findByEmail("joao@email.com");
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException when user is not owner")
        void shouldThrowExceptionWhenUserNotOwner() {
            // Given - Task pertence ao anotherUser, mas principal é validUser
            Task taskFromAnotherUser = new Task();
            taskFromAnotherUser.setId(1L);
            taskFromAnotherUser.setTitle("Task de outro usuário");
            taskFromAnotherUser.setUser(anotherUser); // Task pertence ao outro usuário

            when(taskRepository.findById(1L)).thenReturn(Optional.of(taskFromAnotherUser));
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));

            // When & Then
            assertThatThrownBy(() -> taskService.getTaskById(1L, principal))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("You don't have permission to access this task");

            verify(taskRepository).findById(1L);
            verify(userRepository).findByEmail("joao@email.com");
            verify(modelMapper, never()).map(any(), any());
        }
    }

    @Nested
    @DisplayName("getAllTasks Tests")
    class GetAllTasksTests {

        @Test
        @DisplayName("Should return paginated tasks for user")
        void shouldReturnPaginatedTasksForUser() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Task> userTasks = List.of(validTask);
            Page<Task> taskPage = new PageImpl<>(userTasks, pageable, 1);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findByUserId(1L, pageable)).thenReturn(taskPage);
            when(modelMapper.map(validTask, TaskDTO.class)).thenReturn(taskDTO);

            // When
            ResponseEntity<Page<TaskDTO>> response = taskService.getAllTasks(pageable, principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getTotalElements()).isEqualTo(1);

            verify(userRepository).findByEmail("joao@email.com");
            verify(taskRepository).findByUserId(1L, pageable);
            verify(modelMapper).map(validTask, TaskDTO.class);
        }

        @Test
        @DisplayName("Should return empty page when user has no tasks")
        void shouldReturnEmptyPageWhenUserHasNoTasks() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> emptyPage = Page.empty(pageable);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findByUserId(1L, pageable)).thenReturn(emptyPage);

            // When
            ResponseEntity<Page<TaskDTO>> response = taskService.getAllTasks(pageable, principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).isEmpty();
            assertThat(response.getBody().getTotalElements()).isEqualTo(0);

            verify(modelMapper, never()).map(any(), any());
        }
    }

    @Nested
    @DisplayName("getAllTasksWithoutPagination Tests")
    class GetAllTasksWithoutPaginationTests {

        @Test
        @DisplayName("Should return all tasks for user without pagination")
        void shouldReturnAllTasksForUserWithoutPagination() {
            // Given
            List<Task> userTasks = List.of(validTask);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(userTasks);
            when(modelMapper.map(validTask, TaskDTO.class)).thenReturn(taskDTO);

            // When
            ResponseEntity<List<TaskDTO>> response = taskService.getAllTasksWithoutPagination(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getTitle()).isEqualTo("Estudar Spring Boot");

            verify(userRepository).findByEmail("joao@email.com");
            verify(taskRepository).findByUserIdOrderByCreatedAtDesc(1L);
            verify(modelMapper).map(validTask, TaskDTO.class);
        }

        @Test
        @DisplayName("Should return empty list when user has no tasks")
        void shouldReturnEmptyListWhenUserHasNoTasks() {
            // Given
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            // When
            ResponseEntity<List<TaskDTO>> response = taskService.getAllTasksWithoutPagination(principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isEmpty();

            verify(modelMapper, never()).map(any(), any());
        }
    }

    @Nested
    @DisplayName("createNewTask Tests")
    class CreateNewTaskTests {

        @Test
        @DisplayName("Should create new task successfully")
        void shouldCreateNewTaskSuccessfully() {
            // Given
            Task newTask = new Task();
            newTask.setTitle("Nova Task");
            newTask.setDescription("Descrição da nova task");
            newTask.setPriority(Priority.MEDIUM);

            Task savedTask = new Task();
            savedTask.setId(2L);
            savedTask.setTitle("Nova Task");
            savedTask.setDescription("Descrição da nova task");
            savedTask.setPriority(Priority.MEDIUM);
            savedTask.setUser(validUser);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.save(newTask)).thenReturn(savedTask);
            when(modelMapper.map(savedTask, TaskDTO.class)).thenReturn(taskDTO);

            // When
            ResponseEntity<TaskDTO> response = taskService.createNewTask(newTask, uriBuilder, principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getHeaders().getLocation()).isNotNull();
            assertThat(newTask.getUser()).isEqualTo(validUser); // Verifica se user foi setado

            verify(userRepository).findByEmail("joao@email.com");
            verify(taskRepository).save(newTask);
            verify(modelMapper).map(savedTask, TaskDTO.class);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFoundOnCreate() {
            // Given
            Task newTask = new Task();
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.createNewTask(newTask, uriBuilder, principal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("email");

            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateTask Tests")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should update task successfully when user is owner")
        void shouldUpdateTaskSuccessfullyWhenUserIsOwner() {
            // Given
            Task updateData = new Task();
            updateData.setTitle("Título Atualizado");
            updateData.setDescription("Descrição Atualizada");
            updateData.setStatus(Status.DONE);
            updateData.setPriority(Priority.LOW);
            updateData.setDueDate(LocalDateTime.now().plusDays(10));

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findById(1L)).thenReturn(Optional.of(validTask));
            when(modelMapper.map(validTask, TaskDTO.class)).thenReturn(taskDTO);

            // When
            ResponseEntity<TaskDTO> response = taskService.updateTask(updateData, 1L, principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            // Verificar se os campos foram atualizados
            assertThat(validTask.getTitle()).isEqualTo("Título Atualizado");
            assertThat(validTask.getDescription()).isEqualTo("Descrição Atualizada");
            assertThat(validTask.getStatus()).isEqualTo(Status.DONE);
            assertThat(validTask.getPriority()).isEqualTo(Priority.LOW);

            verify(userRepository).findByEmail("joao@email.com");
            verify(taskRepository).findById(1L);
            verify(modelMapper).map(validTask, TaskDTO.class);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task not found")
        void shouldThrowExceptionWhenTaskNotFoundOnUpdate() {
            // Given
            Task updateData = new Task();
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.updateTask(updateData, 999L, principal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task")
                    .hasMessageContaining("id")
                    .hasMessageContaining("999");

            verify(modelMapper, never()).map(any(), any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException when user is not owner")
        void shouldThrowExceptionWhenUserNotOwnerOnUpdate() {
            // Given
            Task updateData = new Task();
            Task taskFromAnotherUser = new Task();
            taskFromAnotherUser.setId(1L);
            taskFromAnotherUser.setUser(anotherUser);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findById(1L)).thenReturn(Optional.of(taskFromAnotherUser));

            // When & Then
            assertThatThrownBy(() -> taskService.updateTask(updateData, 1L, principal))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("You don't have permission to access this task");

            verify(modelMapper, never()).map(any(), any());
        }
    }

    @Nested
    @DisplayName("deleteTask Tests")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should delete task successfully when user is owner")
        void shouldDeleteTaskSuccessfullyWhenUserIsOwner() {
            // Given
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findById(1L)).thenReturn(Optional.of(validTask));

            // When
            ResponseEntity<Void> response = taskService.deleteTask(1L, principal);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();

            verify(userRepository).findByEmail("joao@email.com");
            verify(taskRepository).findById(1L);
            verify(taskRepository).delete(validTask);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task not found")
        void shouldThrowExceptionWhenTaskNotFoundOnDelete() {
            // Given
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> taskService.deleteTask(999L, principal))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task")
                    .hasMessageContaining("id")
                    .hasMessageContaining("999");

            verify(taskRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException when user is not owner")
        void shouldThrowExceptionWhenUserNotOwnerOnDelete() {
            // Given
            Task taskFromAnotherUser = new Task();
            taskFromAnotherUser.setId(1L);
            taskFromAnotherUser.setUser(anotherUser);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(validUser));
            when(taskRepository.findById(1L)).thenReturn(Optional.of(taskFromAnotherUser));

            // When & Then
            assertThatThrownBy(() -> taskService.deleteTask(1L, principal))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("You don't have permission to access this task");

            verify(taskRepository, never()).delete(any());
        }
    }
}