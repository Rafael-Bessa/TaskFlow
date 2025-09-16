package bessa.morangon.rafael.TaskFlow.service;

import bessa.morangon.rafael.TaskFlow.domain.dto.TaskDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.Task;
import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.domain.repository.TaskRepository;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TaskService {

    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private ModelMapper modelMapper;

    public ResponseEntity<TaskDTO> getTaskById(Long id) {
        Optional<Task> task = taskRepository.findById(id);

        if (task.isPresent()) {
            return ResponseEntity.ok(modelMapper.map(task.get(), TaskDTO.class));
        }
        return ResponseEntity.notFound().build();
    }

    // CORRIGIDO: Agora filtra tasks apenas do usuário logado
    public ResponseEntity<Page<TaskDTO>> getAllTasks(Pageable pageable, Principal principal) {
        System.out.println("Buscando tasks para usuário: " + principal.getName());

        // Busca o usuário pelo email do token
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));

        // BUSCA APENAS TASKS DO USUÁRIO LOGADO
        Page<Task> userTasks = taskRepository.findByUserId(user.getId(), pageable);

        System.out.println("Encontradas " + userTasks.getTotalElements() + " tasks para o usuário " + email);

        if (userTasks.isEmpty()) {
            // Retorna lista vazia em vez de 404 - é normal não ter tasks
            return ResponseEntity.ok(Page.empty(pageable));
        }

        return ResponseEntity.ok(userTasks.map(task -> modelMapper.map(task, TaskDTO.class)));
    }

    // ADICIONADO: Método para buscar todas as tasks sem paginação (para Angular)
    public ResponseEntity<List<TaskDTO>> getAllTasksWithoutPagination(Principal principal) {
        System.out.println("Buscando todas as tasks para usuário: " + principal.getName());

        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));

        // BUSCA TODAS AS TASKS DO USUÁRIO (sem paginação)
        List<Task> userTasks = taskRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        System.out.println("Encontradas " + userTasks.size() + " tasks para o usuário " + email);

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

        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        task.setUser(user);
        Task saved = taskRepository.save(task);

        URI uri = uriComponentsBuilder.path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        TaskDTO dto = modelMapper.map(saved, TaskDTO.class);
        System.out.println("Task criada para usuário " + email + ": " + dto.getTitle());

        return ResponseEntity.created(uri).body(dto);
    }

    @Transactional
    public ResponseEntity<?> updateTask(Task task, Long id, Principal principal) {
        // ADICIONADO: Verificação de segurança - usuário só pode editar suas próprias tasks
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Optional<Task> fetchedTask = taskRepository.findById(id);

        if (fetchedTask.isPresent()) {
            Task existingTask = fetchedTask.get();

            // VERIFICAÇÃO: Task pertence ao usuário logado?
            if (!existingTask.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body("Acesso negado: task não pertence ao usuário");
            }

            existingTask.setStatus(task.getStatus());
            existingTask.setTitle(task.getTitle());
            existingTask.setDescription(task.getDescription());
            existingTask.setDueDate(task.getDueDate());
            existingTask.setPriority(task.getPriority());

            return ResponseEntity.ok(modelMapper.map(existingTask, TaskDTO.class));
        }
        return ResponseEntity.notFound().build();
    }

    @Transactional
    public ResponseEntity<?> deleteTask(Long id, Principal principal) {
        // ADICIONADO: Verificação de segurança
        String email = principal.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Optional<Task> taskOpt = taskRepository.findById(id);

        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();

            // VERIFICAÇÃO: Task pertence ao usuário logado?
            if (!task.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body("Acesso negado: task não pertence ao usuário");
            }

            taskRepository.delete(task);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}