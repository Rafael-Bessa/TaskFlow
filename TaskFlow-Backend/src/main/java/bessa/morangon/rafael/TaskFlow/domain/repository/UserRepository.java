package bessa.morangon.rafael.TaskFlow.domain.repository;

import bessa.morangon.rafael.TaskFlow.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // ADICIONADO: Método para verificar se email existe (performance)
    boolean existsByEmail(String email);
}
