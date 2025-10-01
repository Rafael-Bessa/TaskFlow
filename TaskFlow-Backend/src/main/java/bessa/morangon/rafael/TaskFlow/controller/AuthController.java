package bessa.morangon.rafael.TaskFlow.controller;

import bessa.morangon.rafael.TaskFlow.domain.configuration.security.JwtUtil;
import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository; // ADICIONADO

    @PostMapping
    public ResponseEntity<?> auth(@RequestBody AuthRequest req) {
        try {

            // Autentica o usuário
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password())
            );

            //Busca dados do usuário para retornar no response
            Optional<User> userOpt = userRepository.findByEmail(req.email());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            String token = jwtUtil.generateToken(req.email());

            // Retorna token + dados do usuário (como esperado pelo Angular)
            Map<String, Object> response = Map.of(
                    "token", token,
                    "user", Map.of(
                            "id", user.getId(),
                            "fullName", user.getFullName(),
                            "email", user.getEmail()
                    )
            );
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            ex.printStackTrace(); // ADICIONADO: Para debug
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    public record AuthRequest(String email, String password) {}
}