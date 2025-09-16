package bessa.morangon.rafael.TaskFlow.service;

import bessa.morangon.rafael.TaskFlow.domain.dto.UserDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
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
public class UserService {

    private UserRepository userRepository;
    private ModelMapper modelMapper;
    private PasswordEncoder passwordEncoder;

    public ResponseEntity<UserDTO> getById(Long id){
        Optional<User> byId = userRepository.findById(id);
        if(byId.isPresent()){
            return ResponseEntity.ok(modelMapper.map(byId.get(), UserDTO.class));
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<Page<UserDTO>> getAllUsers(Pageable pageable){
        Page<User> all = userRepository.findAll(pageable);
        if(all.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(all.map(user -> modelMapper.map(user, UserDTO.class)));
    }

    @Transactional
    public ResponseEntity<UserDTO> createNewUser(User user, UriComponentsBuilder uriComponentsBuilder){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User save = userRepository.save(user);
        URI uri = uriComponentsBuilder.path("/{id}").buildAndExpand(save.getId()).toUri();
        return ResponseEntity.created(uri).body(modelMapper.map(save, UserDTO.class));
    }

    @Transactional
    public ResponseEntity<?> updateUser(User user, Long id){
        Optional<User> byId = userRepository.findById(id);

        if(byId.isPresent()){
            byId.get().setAge(user.getAge());
            byId.get().setEmail(user.getEmail());
            byId.get().setFullName(user.getFullName());
            byId.get().setPassword(user.getPassword());
            return ResponseEntity.ok(modelMapper.map(byId.get(),UserDTO.class));
        }
        return ResponseEntity.notFound().build();
    }

    @Transactional
    public ResponseEntity<?> deleteUser(Long id){
        Optional<User> byId = userRepository.findById(id);
        if(byId.isPresent()){
            userRepository.delete(byId.get());
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
