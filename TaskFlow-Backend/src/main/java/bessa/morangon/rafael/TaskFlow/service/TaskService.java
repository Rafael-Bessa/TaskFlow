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
        log.debug("Buscando task com ID: {} para usuário: {}", id, principal.getName());

        // Buscar task
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        // Verificar se task pertence ao usuário
        User user = getUserByEmail(principal.getName());
        validateTaskOwnership(task, user);

        return ResponseEntity.ok(modelMapper.map(task, TaskDTO.class));
    }

    public ResponseEntity<Page<TaskDTO>> getAllTasks(Pageable pageable, Principal principal) {
        log.debug("Buscando tasks paginadas para usuário: {}", principal.getName());

        User user = getUserByEmail(principal.getName());
        Page<Task> userTasks = taskRepository.findByUserId(user.getId(), pageable);

        log.info("Encontradas {} tasks para o usuário {}",
                userTasks.getTotalElements(), principal.getName());

        return ResponseEntity.ok(userTasks.map(task -> modelMapper.map(task, TaskDTO.class)));
    }

    public ResponseEntity<List<TaskDTO>> getAllTasksWithoutPagination(Principal principal) {
        log.debug("Buscando todas as tasks para usuário: {}", principal.getName());

        User user = getUserByEmail(principal.getName());
        List<Task> userTasks = taskRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        log.info("Encontradas {} tasks para o usuário {}", userTasks.size(), principal.getName());

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

        log.info("Criando nova task '{}' para usuário: {}", task.getTitle(), principal.getName());

        User user = getUserByEmail(principal.getName());
        task.setUser(user);

        Task savedTask = taskRepository.save(task);

        URI uri = uriComponentsBuilder.path("/tasks/{id}")
                .buildAndExpand(savedTask.getId())
                .toUri();

        TaskDTO dto = modelMapper.map(savedTask, TaskDTO.class);

        log.info("Task criada com sucesso: ID {}, Título '{}' para usuário {}",
                savedTask.getId(), dto.getTitle(), principal.getName());

        return ResponseEntity.created(uri).body(dto);
    }

    @Transactional
    public ResponseEntity<TaskDTO> updateTask(Task task, Long id, Principal principal) {
        log.info("Atualizando task ID: {} para usuário: {}", id, principal.getName());

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

        log.info("Task atualizada com sucesso: ID {}, Título '{}'",
                existingTask.getId(), existingTask.getTitle());

        return ResponseEntity.ok(updatedTaskDTO);
    }

    @Transactional
    public ResponseEntity<Void> deleteTask(Long id, Principal principal) {
        log.info("Deletando task ID: {} para usuário: {}", id, principal.getName());

        User user = getUserByEmail(principal.getName());

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        // Verificar se task pertence ao usuário
        validateTaskOwnership(task, user);

        taskRepository.delete(task);

        log.info("Task deletada com sucesso: ID {}", id);

        return ResponseEntity.noContent().build();
    }

    // MÉTODOS AUXILIARES PRIVADOS

    /**
     * Busca usuário por email e lança exception se não encontrar
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Valida se a task pertence ao usuário logado
     */
    private void validateTaskOwnership(Task task, User user) {
        if (!task.getUser().getId().equals(user.getId())) {
            log.warn("Usuário {} tentou acessar task {} que pertence ao usuário {}",
                    user.getEmail(), task.getId(), task.getUser().getEmail());
            throw new UnauthorizedAccessException(
                    "You don't have permission to access this task. It belongs to another user.");
        }
    }
}