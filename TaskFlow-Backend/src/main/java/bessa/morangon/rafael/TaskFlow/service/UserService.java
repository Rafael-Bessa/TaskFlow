package bessa.morangon.rafael.TaskFlow.service;


import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.ResourceNotFoundException;
import bessa.morangon.rafael.TaskFlow.domain.configuration.exceptions.UserAlreadyExistsException;
import bessa.morangon.rafael.TaskFlow.domain.dto.UserDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private UserRepository userRepository;
    private ModelMapper modelMapper;
    private PasswordEncoder passwordEncoder;

    public ResponseEntity<UserDTO> getById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        return ResponseEntity.ok(modelMapper.map(user, UserDTO.class));
    }

    public ResponseEntity<Page<UserDTO>> getAllUsers(Pageable pageable) {

        Page<User> users = userRepository.findAll(pageable);
        return ResponseEntity.ok(users.map(user -> modelMapper.map(user, UserDTO.class)));
    }

    @Transactional
    public ResponseEntity<UserDTO> createNewUser(User user, UriComponentsBuilder uriComponentsBuilder) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException(user.getEmail());
        }

        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userRepository.save(user);
            URI uri = uriComponentsBuilder.path("/users/{id}")
                    .buildAndExpand(savedUser.getId())
                    .toUri();
            return ResponseEntity.created(uri).body(modelMapper.map(savedUser, UserDTO.class));

        } catch (DataIntegrityViolationException ex) {
            log.error("Erro de integridade de dados ao criar usuário: {}", ex.getMessage());
            throw new UserAlreadyExistsException(user.getEmail());
        }
    }

    @Transactional
    public ResponseEntity<UserDTO> updateUser(User user, Long id) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Verificar se o novo email já está em uso por outro usuário
        if (!existingUser.getEmail().equals(user.getEmail())) {
            Optional<User> userWithSameEmail = userRepository.findByEmail(user.getEmail());
            if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(id)) {
                throw new UserAlreadyExistsException(user.getEmail());
            }
        }

        try {

            existingUser.setAge(user.getAge());
            existingUser.setEmail(user.getEmail());
            existingUser.setFullName(user.getFullName());

            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            // Save é automático por causa do @Transactional
            log.info("Usuário atualizado com sucesso: ID {}", id);

            return ResponseEntity.ok(modelMapper.map(existingUser, UserDTO.class));

        } catch (DataIntegrityViolationException ex) {
            log.error("Erro de integridade de dados ao atualizar usuário {}: {}", id, ex.getMessage());
            throw new UserAlreadyExistsException(user.getEmail());
        }
    }

    @Transactional
    public ResponseEntity<Void> deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    // MÉTODO AUXILIAR: Buscar usuário por email (usado em outros services)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}