package bessa.morangon.rafael.TaskFlow.repository;

import bessa.morangon.rafael.TaskFlow.domain.model.*;
import bessa.morangon.rafael.TaskFlow.domain.repository.TaskRepository;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de integração para TaskRepository.
 *
 * Testa todos os métodos do repository com dados válidos que passam
 * nas validações Bean Validation, incluindo cenários de paginação,
 * filtros e performance.
 */
@DataJpaTest
@ActiveProfiles("test")
@EntityScan(basePackages = "bessa.morangon.rafael.TaskFlow.domain.model")
@EnableJpaRepositories(basePackages = "bessa.morangon.rafael.TaskFlow.domain.repository")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TaskRepository Integration Tests")
class TaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    // Dados válidos para Task
    private static final String VALID_TITLE = "Complete Project Documentation";
    private static final String VALID_DESCRIPTION = "Write comprehensive documentation for the project";
    private static final Priority VALID_PRIORITY = Priority.HIGH;
    private static final Status VALID_STATUS = Status.PENDING;

    @BeforeEach
    void setUp() {
        entityManager.clear();

        // Cria usuários de teste válidos
        testUser1 = createValidUser("john.doe@example.com", "John Doe");
        testUser2 = createValidUser("jane.smith@example.com", "Jane Smith");

        entityManager.flush();
        entityManager.clear();
    }

    // =====================================================
    // MÉTODOS AUXILIARES
    // =====================================================

    /**
     * Cria usuário válido que passa em todas as validações
     */
    private User createValidUser(String email, String fullName) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);              //  Só letras e espaços
        user.setAge(25);                         //  0-120
        user.setPassword("ValidPass123@");       //  Maiúscula+minúscula+número+especial
        return entityManager.persist(user);
    }

    /**
     * Cria task válida que passa em todas as validações
     */
    private Task createValidTask(User user, String title) {
        Task task = new Task();
        task.setTitle(title);                    //  NotBlank
        task.setDescription("Description for " + title);
        task.setDueDate(LocalDateTime.now().plusDays(7)); //  Future
        task.setPriority(VALID_PRIORITY);        //  NotNull
        task.setStatus(VALID_STATUS);
        task.setUser(user);
        return task;
    }

    /**
     * Cria task com status específico - VERSÃO CORRIGIDA
     */
    private Task createTaskWithStatus(User user, String title, Status status) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription("Description for " + title);
        task.setDueDate(LocalDateTime.now().plusDays(7));
        task.setPriority(VALID_PRIORITY);
        task.setStatus(status); // Define status ANTES de persistir
        task.setUser(user);

        // Persiste e força o flush
        Task savedTask = entityManager.persist(task);
        entityManager.flush();

        // Verifica se o status foi salvo corretamente
        entityManager.refresh(savedTask);

        System.out.println("DEBUG - Created task: " + savedTask.getTitle() + " with status: " + savedTask.getStatus());

        return savedTask;
    }

    /**
     * Cria task com data específica (para testes de ordenação) - VERSÃO SIMPLES
     */
    private Task createTaskWithDate(User user, String title, LocalDateTime createdAt) {
        // Cria task básica
        Task task = new Task();
        task.setTitle(title);
        task.setDescription("Description for " + title);
        task.setDueDate(LocalDateTime.now().plusDays(7));
        task.setPriority(VALID_PRIORITY);
        task.setStatus(VALID_STATUS);
        task.setUser(user);

        // Persiste
        Task savedTask = entityManager.persist(task);
        entityManager.flush();

        // Simula diferentes tempos de criação usando pequenas diferenças
        // Para o teste de ordenação, vamos usar uma abordagem diferente
        try {
            Thread.sleep(10); // Pequena pausa entre criações
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return savedTask;
    }

    // =====================================================
    // TESTE DE CONECTIVIDADE
    // =====================================================

    @Test
    @Order(1)
    @DisplayName("Should create tasks and establish relationships correctly")
    void shouldCreateTasksAndEstablishRelationships() {
        // Given - task válida
        Task task = createValidTask(testUser1, "Test Task");

        // When - salva task
        Task saved = entityManager.persistAndFlush(task);
        entityManager.clear();

        // Then - deve salvar corretamente com relacionamento
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Test Task");
        assertThat(saved.getUser().getId()).isEqualTo(testUser1.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING); // Default do @PrePersist
    }

    // =====================================================
    // TESTES - findByUserId() com paginação
    // =====================================================

    @Test
    @Order(2)
    @DisplayName("Should find tasks by user ID with pagination")
    void shouldFindTasksByUserIdWithPagination() {
        // Given - 5 tasks para user1, 3 tasks para user2
        for (int i = 1; i <= 5; i++) {
            Task task = createValidTask(testUser1, "User1 Task " + i);
            entityManager.persist(task);
        }
        for (int i = 1; i <= 3; i++) {
            Task task = createValidTask(testUser2, "User2 Task " + i);
            entityManager.persist(task);
        }
        entityManager.flush();
        entityManager.clear();

        // When - busca primeira página (3 items) para user1
        Pageable pageable = PageRequest.of(0, 3);
        Page<Task> result = taskRepository.findByUserId(testUser1.getId(), pageable);

        // Then - deve retornar apenas tasks do user1 paginadas
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.isFirst()).isTrue();

        // Todas as tasks devem ser do user1
        result.getContent().forEach(task ->
                assertThat(task.getUser().getId()).isEqualTo(testUser1.getId()));
    }

    @Test
    @Order(3)
    @DisplayName("Should return empty page for user without tasks")
    void shouldReturnEmptyPageForUserWithoutTasks() {
        // Given - tasks apenas para user1
        Task task = createValidTask(testUser1, "Only Task");
        entityManager.persistAndFlush(task);
        entityManager.clear();

        // When - busca tasks para user2 (que não tem tasks)
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> result = taskRepository.findByUserId(testUser2.getId(), pageable);

        // Then - deve retornar página vazia
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.hasNext()).isFalse();
    }

    // =====================================================
    // TESTES - findByUserIdOrderByCreatedAtDesc()
    // =====================================================

    @Test
    @Order(4)
    @DisplayName("Should order tasks by created date descending")
    void shouldOrderTasksByCreatedAtDesc() {
        // Given - cria tasks em sequência (com pequenas pausas para garantir ordem)
        Task task1 = createTaskWithDate(testUser1, "First Task", null);
        Task task2 = createTaskWithDate(testUser1, "Second Task", null);
        Task task3 = createTaskWithDate(testUser1, "Third Task", null);

        entityManager.flush();
        entityManager.clear();

        // When - busca todas as tasks ordenadas por data
        List<Task> result = taskRepository.findByUserIdOrderByCreatedAtDesc(testUser1.getId());

        // Then - deve ter 3 tasks e estar em ordem decrescente
        assertThat(result).hasSize(3);

        // Como as tasks foram criadas em sequência, a última criada deve vir primeiro
        assertThat(result.get(0).getTitle()).isEqualTo("Third Task");
        assertThat(result.get(1).getTitle()).isEqualTo("Second Task");
        assertThat(result.get(2).getTitle()).isEqualTo("First Task");

        // Verifica ordenação temporal (mais recente primeiro)
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getCreatedAt())
                    .isAfterOrEqualTo(result.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should return empty list for user with no tasks")
    void shouldReturnEmptyListForUserWithNoTasks() {
        // Given - tasks apenas para user1
        Task task = createValidTask(testUser1, "User1 Task");
        entityManager.persistAndFlush(task);
        entityManager.clear();

        // When - busca tasks do user2 (sem tasks)
        List<Task> result = taskRepository.findByUserIdOrderByCreatedAtDesc(testUser2.getId());

        // Then - deve retornar lista vazia
        assertThat(result).isEmpty();
    }

    // =====================================================
    // TESTES - countByUserId()
    // =====================================================

    @Test
    @Order(6)
    @DisplayName("Should count tasks correctly by user ID")
    void shouldCountTasksByUserId() {
        // Given - 4 tasks para user1, 2 tasks para user2
        for (int i = 1; i <= 4; i++) {
            Task task = createValidTask(testUser1, "User1 Task " + i);
            entityManager.persist(task);
        }
        for (int i = 1; i <= 2; i++) {
            Task task = createValidTask(testUser2, "User2 Task " + i);
            entityManager.persist(task);
        }
        entityManager.flush();
        entityManager.clear();

        // When - conta tasks de cada usuário
        long countUser1 = taskRepository.countByUserId(testUser1.getId());
        long countUser2 = taskRepository.countByUserId(testUser2.getId());

        // Then - deve contar corretamente
        assertThat(countUser1).isEqualTo(4);
        assertThat(countUser2).isEqualTo(2);
    }

    @Test
    @Order(7)
    @DisplayName("Should return zero count for user without tasks")
    void shouldReturnZeroCountForUserWithoutTasks() {
        // Given - tasks apenas para user1
        Task task = createValidTask(testUser1, "Only Task");
        entityManager.persistAndFlush(task);
        entityManager.clear();

        // When - conta tasks do user2
        long count = taskRepository.countByUserId(testUser2.getId());

        // Then - deve ser zero
        assertThat(count).isZero();
    }

    // =====================================================
    // TESTES - findByUserIdAndStatus() - CORRIGIDOS
    // =====================================================

    @Test
    @Order(8)
    @DisplayName("Should filter tasks by user ID and status")
    void shouldFilterTasksByUserIdAndStatus() {
        // Given - limpa qualquer dado anterior
        entityManager.clear();

        // Cria tasks com diferentes status para user1
        Task pendingTask1 = createTaskWithStatus(testUser1, "Pending Task 1", Status.PENDING);
        Task pendingTask2 = createTaskWithStatus(testUser1, "Pending Task 2", Status.PENDING);
        Task doneTask1 = createTaskWithStatus(testUser1, "Done Task 1", Status.DONE);
        Task doneTask2 = createTaskWithStatus(testUser1, "Done Task 2", Status.DONE);

        // Task de outro usuário (não deve aparecer)
        Task otherUserTask = createTaskWithStatus(testUser2, "User2 Pending Task", Status.PENDING);

        entityManager.flush();
        entityManager.clear();

        // DEBUG: Verifica o que foi salvo
        List<Task> allTasks = taskRepository.findAll();
        System.out.println("DEBUG - Total tasks created: " + allTasks.size());
        allTasks.forEach(task ->
                System.out.println("Task: " + task.getTitle() + ", Status: " + task.getStatus() + ", UserId: " + task.getUser().getId())
        );

        // When - busca apenas tasks PENDING do user1
        List<Task> result = taskRepository.findByUserIdAndStatus(testUser1.getId(), Status.PENDING);

        // DEBUG
        System.out.println("DEBUG - Found PENDING tasks for user1: " + result.size());
        result.forEach(task -> System.out.println("Found: " + task.getTitle() + " - " + task.getStatus()));

        // Then - deve retornar apenas as 2 tasks PENDING do user1
        assertThat(result).hasSize(2);
        result.forEach(task -> {
            assertThat(task.getUser().getId()).isEqualTo(testUser1.getId());
            assertThat(task.getStatus()).isEqualTo(Status.PENDING);
            assertThat(task.getTitle()).startsWith("Pending Task");
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should return empty list when no tasks match status")
    void shouldReturnEmptyListWhenNoTasksMatchStatus() {
        // Given - apenas tasks PENDING
        createTaskWithStatus(testUser1, "Pending Task 1", Status.PENDING);
        createTaskWithStatus(testUser1, "Pending Task 2", Status.PENDING);
        entityManager.flush();
        entityManager.clear();

        // When - busca tasks DONE (que não existem)
        List<Task> result = taskRepository.findByUserIdAndStatus(testUser1.getId(), Status.DONE);

        // Then - deve retornar lista vazia
        assertThat(result).isEmpty();
    }

    @Test
    @Order(10)
    @DisplayName("Should filter tasks by both status types (PENDING and DONE)")
    void shouldFilterTasksByBothStatusTypes() {
        // Given - limpa dados anteriores
        entityManager.clear();

        // Cria tasks com ambos os status
        createTaskWithStatus(testUser1, "Pending Task 1", Status.PENDING);
        createTaskWithStatus(testUser1, "Pending Task 2", Status.PENDING);
        createTaskWithStatus(testUser1, "Done Task 1", Status.DONE);
        createTaskWithStatus(testUser1, "Done Task 2", Status.DONE);
        createTaskWithStatus(testUser1, "Done Task 3", Status.DONE);

        entityManager.flush();
        entityManager.clear();

        // DEBUG: Verifica o que foi salvo
        List<Task> allTasks = taskRepository.findAll();
        System.out.println("DEBUG - Total tasks: " + allTasks.size());
        allTasks.forEach(task ->
                System.out.println("Task: " + task.getTitle() + ", Status: " + task.getStatus())
        );

        // When/Then - testa ambos os status
        List<Task> pendingTasks = taskRepository.findByUserIdAndStatus(testUser1.getId(), Status.PENDING);
        List<Task> doneTasks = taskRepository.findByUserIdAndStatus(testUser1.getId(), Status.DONE);

        System.out.println("DEBUG - Pending tasks found: " + pendingTasks.size());
        System.out.println("DEBUG - Done tasks found: " + doneTasks.size());

        assertThat(pendingTasks).hasSize(2);
        assertThat(doneTasks).hasSize(3);

        // Verifica títulos corretos
        pendingTasks.forEach(task ->
                assertThat(task.getTitle()).startsWith("Pending Task"));
        doneTasks.forEach(task ->
                assertThat(task.getTitle()).startsWith("Done Task"));

        // Verifica status corretos
        pendingTasks.forEach(task ->
                assertThat(task.getStatus()).isEqualTo(Status.PENDING));
        doneTasks.forEach(task ->
                assertThat(task.getStatus()).isEqualTo(Status.DONE));
    }

    // =====================================================
    // TESTES DE INTEGRAÇÃO E PERFORMANCE
    // =====================================================

    @Test
    @Order(11)
    @DisplayName("Should work efficiently with large dataset")
    void shouldWorkEfficientlyWithLargeDataset() {
        // Given - dataset grande (30 tasks para user1, 10 para user2)
        for (int i = 1; i <= 30; i++) {
            // Alterna entre PENDING e DONE
            Status status = i % 2 == 0 ? Status.DONE : Status.PENDING;
            createTaskWithStatus(testUser1, "Task Number " + i, status);

            // Flush periódico para performance
            if (i % 10 == 0) {
                entityManager.flush();
            }
        }

        // 10 tasks para user2
        for (int i = 1; i <= 10; i++) {
            Task task = createValidTask(testUser2, "User2 Task " + i);
            entityManager.persist(task);

            if (i % 5 == 0) {
                entityManager.flush();
            }
        }

        entityManager.flush();
        entityManager.clear();

        // When - executa várias operações
        long startTime = System.currentTimeMillis();

        long totalCountUser1 = taskRepository.countByUserId(testUser1.getId());
        long totalCountUser2 = taskRepository.countByUserId(testUser2.getId());

        List<Task> pendingTasks = taskRepository.findByUserIdAndStatus(testUser1.getId(), Status.PENDING);
        List<Task> doneTasks = taskRepository.findByUserIdAndStatus(testUser1.getId(), Status.DONE);

        Page<Task> paginatedTasks = taskRepository.findByUserId(testUser1.getId(), PageRequest.of(0, 10));

        long endTime = System.currentTimeMillis();

        // Then - deve ser performático e retornar resultados corretos
        assertThat(totalCountUser1).isEqualTo(30);
        assertThat(totalCountUser2).isEqualTo(10);

        assertThat(pendingTasks).hasSize(15); // 30/2 = 15 tasks PENDING (ímpares)
        assertThat(doneTasks).hasSize(15);    // 30/2 = 15 tasks DONE (pares)

        assertThat(paginatedTasks.getContent()).hasSize(10);
        assertThat(paginatedTasks.getTotalElements()).isEqualTo(30);

        // Performance check
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(2000) // menos de 2 segundos
                .as("Operations took %d ms, expected < 2000ms", duration);
    }

    @Test
    @Order(12)
    @DisplayName("Should maintain consistency between count and find methods")
    void shouldMaintainConsistencyBetweenCountAndFindMethods() {
        // Given - limpa dados anteriores
        entityManager.clear();

        // Cria mix de tasks com diferentes status
        createTaskWithStatus(testUser1, "Task 1", Status.PENDING);
        createTaskWithStatus(testUser1, "Task 2", Status.DONE);
        createTaskWithStatus(testUser1, "Task 3", Status.PENDING);
        createTaskWithStatus(testUser1, "Task 4", Status.DONE);
        createTaskWithStatus(testUser1, "Task 5", Status.PENDING);

        entityManager.flush();
        entityManager.clear();

        // When - usa métodos count e find
        long totalCount = taskRepository.countByUserId(testUser1.getId());
        List<Task> allTasks = taskRepository.findByUserIdOrderByCreatedAtDesc(testUser1.getId());
        List<Task> pendingTasks = taskRepository.findByUserIdAndStatus(testUser1.getId(), Status.PENDING);
        List<Task> doneTasks = taskRepository.findByUserIdAndStatus(testUser1.getId(), Status.DONE);

        // DEBUG
        System.out.println("DEBUG - Total count: " + totalCount);
        System.out.println("DEBUG - All tasks: " + allTasks.size());
        System.out.println("DEBUG - Pending tasks: " + pendingTasks.size());
        System.out.println("DEBUG - Done tasks: " + doneTasks.size());

        // Then - deve haver consistência
        assertThat(allTasks).hasSize((int) totalCount);
        assertThat(pendingTasks).hasSize(3); // 3 tasks PENDING
        assertThat(doneTasks).hasSize(2);    // 2 tasks DONE

        // Total deve bater
        assertThat(pendingTasks.size() + doneTasks.size()).isEqualTo((int) totalCount);

        // Verifica que todas as tasks pertencem ao usuário correto
        allTasks.forEach(task ->
                assertThat(task.getUser().getId()).isEqualTo(testUser1.getId()));
    }

    // =====================================================
    // TESTES DE VALIDAÇÃO BEAN VALIDATION
    // =====================================================

    @Test
    @Order(13)
    @DisplayName("Should enforce Bean Validation constraints")
    void shouldEnforceBeanValidationConstraints() {
        // Given - task com dados inválidos
        Task invalidTask = new Task();
        invalidTask.setTitle(""); //  NotBlank
        invalidTask.setDueDate(LocalDateTime.now().minusDays(1)); //  Future
        invalidTask.setPriority(null); //  NotNull
        invalidTask.setUser(testUser1);

        // When/Then - deve falhar na validação
        assertThatThrownBy(() -> {
            entityManager.persist(invalidTask);
            entityManager.flush(); // Força a validação
        }).isInstanceOf(jakarta.validation.ConstraintViolationException.class)
                .hasMessageContaining("Validation failed");
    }

    @Test
    @Order(14)
    @DisplayName("Should accept valid task data")
    void shouldAcceptValidTaskData() {
        // Given - task completamente válida
        Task validTask = new Task();
        validTask.setTitle("Valid Task Title");              //  NotBlank
        validTask.setDescription("Valid description");
        validTask.setDueDate(LocalDateTime.now().plusWeeks(2)); //  Future
        validTask.setPriority(Priority.MEDIUM);             //  NotNull
        validTask.setStatus(Status.PENDING);
        validTask.setUser(testUser1);

        // When/Then - deve salvar sem problemas
        assertThatCode(() -> {
            Task saved = entityManager.persistAndFlush(validTask);
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();

            // Verifica se @PrePersist funcionou
            assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
        }).doesNotThrowAnyException();
    }

    @Test
    @Order(15)
    @DisplayName("Should test PrePersist and PreUpdate callbacks")
    void shouldTestPrePersistAndPreUpdateCallbacks() {
        // Given - task válida sem status
        Task task = new Task();
        task.setTitle("Test Callbacks");
        task.setDescription("Testing lifecycle callbacks");
        task.setDueDate(LocalDateTime.now().plusDays(5));
        task.setPriority(Priority.LOW);
        // Não define status - deve ser setado pelo @PrePersist
        task.setUser(testUser1);

        // When - salva task
        Task saved = entityManager.persistAndFlush(task);
        LocalDateTime originalCreatedAt = saved.getCreatedAt();
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();

        // Then - @PrePersist deve ter funcionado
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING); // Default
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        // Pequena pausa para garantir diferença de timestamp
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When - atualiza task (testa @PreUpdate)
        saved.setTitle("Updated Title");
        Task updated = entityManager.merge(saved);
        entityManager.flush();

        // Then - @PreUpdate deve ter funcionado
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt); // Não muda
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt); // Muda
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
    }
}