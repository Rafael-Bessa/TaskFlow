package bessa.morangon.rafael.TaskFlow.controller;

import bessa.morangon.rafael.TaskFlow.domain.dto.UserDTO;
import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id){
        return userService.getById(id);
    }
    @GetMapping
    public ResponseEntity<Page<UserDTO>> getAllUsers(@PageableDefault(size = 8, sort = {"id"}) Pageable pageable){
        return userService.getAllUsers(pageable);
    }
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody @Valid User user, UriComponentsBuilder uriComponentsBuilder){
        return userService.createNewUser(user,uriComponentsBuilder);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@RequestBody @Valid User user,@PathVariable Long id){
        return userService.updateUser(user,id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id){
        return userService.deleteUser(id);
    }
}
