package bessa.morangon.rafael.TaskFlow.controller;

import bessa.morangon.rafael.TaskFlow.domain.model.User;
import bessa.morangon.rafael.TaskFlow.domain.repository.UserRepository;
import bessa.morangon.rafael.TaskFlow.domain.configuration.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should authenticate successfully and return token with user data")
    void auth_ShouldReturnTokenAndUser_WhenCredentialsAreValid() throws Exception {
        String email = "test@example.com";
        String password = "123456";

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setFullName("Test User");
        mockUser.setEmail(email);

        String jwtToken = "mock-jwt-token";

        AuthController.AuthRequest request = new AuthController.AuthRequest(email, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken(email)).thenReturn(jwtToken);

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(jwtToken))
                .andExpect(jsonPath("$.user.id").value(1L))
                .andExpect(jsonPath("$.user.fullName").value("Test User"))
                .andExpect(jsonPath("$.user.email").value(email));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(email);
        verify(jwtUtil, times(1)).generateToken(email);
    }

    @Test
    @DisplayName("Should return 401 when user is not found")
    void auth_ShouldReturn401_WhenUserNotFound() throws Exception {
        String email = "notfound@example.com";
        String password = "123456";

        AuthController.AuthRequest request = new AuthController.AuthRequest(email, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(email);
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Should return 401 when credentials are invalid")
    void auth_ShouldReturn401_WhenInvalidCredentials() throws Exception {
        String email = "wrong@example.com";
        String password = "wrongpass";

        AuthController.AuthRequest request = new AuthController.AuthRequest(email, password);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtUtil, never()).generateToken(anyString());
    }
}
