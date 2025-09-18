package bessa.morangon.rafael.TaskFlow.controller;

import bessa.morangon.rafael.TaskFlow.domain.dto.TaskDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.Task;
import bessa.morangon.rafael.TaskFlow.service.TaskService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@AllArgsConstructor
@Slf4j
public class TaskController {

    private TaskService taskService;

    // CORRIGIDO: Agora passa Principal para verificar propriedade da task
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id, Principal principal) {
        log.info("GET /tasks/{} - Usuário: {}", id, principal.getName());
        return taskService.getTaskById(id, principal);
    }

    // Com paginação (para uso futuro)
    @GetMapping("/paged")
    public ResponseEntity<Page<TaskDTO>> getAllTasksPaged(
            @PageableDefault(size = 8, sort = {"createdAt"}) Pageable pageable,
            Principal principal) {

        log.info("GET /tasks/paged - Usuário: {} - Página: {}, Tamanho: {}",
                principal.getName(), pageable.getPageNumber(), pageable.getPageSize());

        return taskService.getAllTasks(pageable, principal);
    }

    // Sem paginação (para o Angular atual)
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks(Principal principal) {
        log.info("GET /tasks - Usuário: {}", principal.getName());
        return taskService.getAllTasksWithoutPagination(principal);
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @RequestBody @Valid Task task,
            UriComponentsBuilder uriComponentsBuilder,
            Principal principal) {

        log.info("POST /tasks - Usuário: {} - Título: {}", principal.getName(), task.getTitle());
        return taskService.createNewTask(task, uriComponentsBuilder, principal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(
            @RequestBody @Valid Task task,
            @PathVariable Long id,
            Principal principal) {

        log.info("PUT /tasks/{} - Usuário: {} - Título: {}", id, principal.getName(), task.getTitle());
        return taskService.updateTask(task, id, principal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Principal principal) {
        log.info("DELETE /tasks/{} - Usuário: {}", id, principal.getName());
        return taskService.deleteTask(id, principal);
    }
}