package bessa.morangon.rafael.TaskFlow.controller;
import bessa.morangon.rafael.TaskFlow.domain.dto.TaskDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.Task;
import bessa.morangon.rafael.TaskFlow.service.TaskService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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
public class TaskController {

    private TaskService taskService;

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    // OPÇÃO 1: Com paginação (para uso futuro)
    @GetMapping("/paged")
    public ResponseEntity<Page<TaskDTO>> getAllTasksPaged(
            @PageableDefault(size = 8, sort = {"createdAt"}) Pageable pageable,
            Principal principal) {
        return taskService.getAllTasks(pageable, principal);
    }

    // OPÇÃO 2: Sem paginação (para o Angular atual)
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks(Principal principal) {
        return taskService.getAllTasksWithoutPagination(principal);
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @RequestBody @Valid Task task,
            UriComponentsBuilder uriComponentsBuilder,
            Principal principal) {
        return taskService.createNewTask(task, uriComponentsBuilder, principal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(
            @RequestBody @Valid Task task,
            @PathVariable Long id,
            Principal principal) {
        return taskService.updateTask(task, id, principal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Principal principal) {
        return taskService.deleteTask(id, principal);
    }
}