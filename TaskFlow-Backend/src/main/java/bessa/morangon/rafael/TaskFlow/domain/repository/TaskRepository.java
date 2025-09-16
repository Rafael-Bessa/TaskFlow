package bessa.morangon.rafael.TaskFlow.domain.repository;

import bessa.morangon.rafael.TaskFlow.domain.model.Status;
import bessa.morangon.rafael.TaskFlow.domain.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Buscar tasks de um usuário específico com paginação
    Page<Task> findByUserId(Long userId, Pageable pageable);

    // Buscar todas as tasks de um usuário (sem paginação) ordenadas por data
    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Contar tasks de um usuário
    long countByUserId(Long userId);

    // Buscar por status e usuário
    List<Task> findByUserIdAndStatus(Long userId, Status status);
}
