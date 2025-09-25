package bessa.morangon.rafael.TaskFlow.service;


import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.ResourceNotFoundException;
import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.UnauthorizedAccessException;
import bessa.morangon.rafael.TaskFlow.domain.dto.TaskDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.Task;
import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.domain.repository.TaskRepository;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class TaskService {

    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private ModelMapper modelMapper;

    public ResponseEntity<TaskDTO> getTaskById(Long id, Principal principal) {
        // log.debug("Buscando task com ID: {} para usuário: {}", id, principal.getName());

        // Buscar task
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        // Verificar se task pertence ao usuário
        User user = getUserByEmail(principal.getName());
        validateTaskOwnership(task, user);

        return ResponseEntity.ok(modelMapper.map(task, TaskDTO.class));
    }

    public ResponseEntity<Page<TaskDTO>> getAllTasks(Pageable pageable, Principal principal) {

        User user = getUserByEmail(principal.getName());
        Page<Task> userTasks = taskRepository.findByUserId(user.getId(), pageable);

        return ResponseEntity.ok(userTasks.map(task -> modelMapper.map(task, TaskDTO.class)));
    }

    public ResponseEntity<List<TaskDTO>> getAllTasksWithoutPagination(Principal principal) {

        User user = getUserByEmail(principal.getName());
        List<Task> userTasks = taskRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<TaskDTO> taskDTOs = userTasks.stream()
                .map(task -> modelMapper.map(task, TaskDTO.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskDTOs);
    }

    @Transactional
    public ResponseEntity<TaskDTO> createNewTask(
            Task task,
            UriComponentsBuilder uriComponentsBuilder,
            Principal principal) {

        User user = getUserByEmail(principal.getName());
        task.setUser(user);

        Task savedTask = taskRepository.save(task);

        URI uri = uriComponentsBuilder.path("/tasks/{id}")
                .buildAndExpand(savedTask.getId())
                .toUri();

        TaskDTO dto = modelMapper.map(savedTask, TaskDTO.class);

        return ResponseEntity.created(uri).body(dto);
    }

    @Transactional
    public ResponseEntity<TaskDTO> updateTask(Task task, Long id, Principal principal) {

        User user = getUserByEmail(principal.getName());

        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        // Verificar se task pertence ao usuário
        validateTaskOwnership(existingTask, user);

        // Atualizar campos
        existingTask.setStatus(task.getStatus());
        existingTask.setTitle(task.getTitle());
        existingTask.setDescription(task.getDescription());
        existingTask.setDueDate(task.getDueDate());
        existingTask.setPriority(task.getPriority());

        // Save é automático por causa do @Transactional
        TaskDTO updatedTaskDTO = modelMapper.map(existingTask, TaskDTO.class);

        return ResponseEntity.ok(updatedTaskDTO);
    }

    @Transactional
    public ResponseEntity<Void> deleteTask(Long id, Principal principal) {

        User user = getUserByEmail(principal.getName());

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        // Verificar se task pertence ao usuário
        validateTaskOwnership(task, user);

        taskRepository.delete(task);

        return ResponseEntity.noContent().build();
    }

    // MÉTODOS AUXILIARES PRIVADOS


    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void validateTaskOwnership(Task task, User user) {
        if (!task.getUser().getId().equals(user.getId())) {
            log.warn("Usuário {} tentou acessar task {} que pertence ao usuário {}",
                    user.getEmail(), task.getId(), task.getUser().getEmail());
            throw new UnauthorizedAccessException(
                    "You don't have permission to access this task. It belongs to another user.");
        }
    }
}